/**
 * @file syscall_dispatcher.cpp
 * @project AxManager Linux-Emu v2 - Android 16 ready
 * @brief x86_64 syscall dispatch -> bionic bridge, 16KB page, fast path
 */
#include "axmanager/linux/task_struct.hpp"
#include <cstdint>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/mman.h>
#include <sys/syscall.h>
#include <sys/random.h>
#include <time.h>

extern "C" {

// x86_64 Linux numbers we need for games
#define __NR_read 0
#define __NR_write 1
#define __NR_open 2
#define __NR_close 3
#define __NR_lseek 8
#define __NR_mmap 9
#define __NR_mprotect 10
#define __NR_munmap 11
#define __NR_brk 12
#define __NR_clone 56
#define __NR_exit 60
#define __NR_uname 63
#define __NR_fcntl 72
#define __NR_getuid 102
#define __NR_getgid 104
#define __NR_geteuid 107
#define __NR_getegid 108
#define __NR_arch_prctl 158
#define __NR_futex 202
#define __NR_set_tid_address 218
#define __NR_exit_group 231
#define __NR_openat 257
#define __NR_newfstatat 262
#define __NR_prctl 302
#define __NR_getrandom 318
#define __NR_statx 332

#define ARCH_SET_FS 0x1002
#define ARCH_SET_GS 0x1001
#define ARCH_GET_FS 0x1003
#define ARCH_GET_GS 0x1004

typedef int64_t (*sys_call_t)(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t);

static uint64_t g_fast_hits = 0;
static uint64_t g_enosys = 0;

static inline uint64_t align16k(uint64_t v){ return (v + 16383ULL) & ~16383ULL; }

int64_t sys_ni_syscall(uint64_t nr,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){
    __atomic_fetch_add(&g_enosys,1,__ATOMIC_RELAXED);
    (void)nr;
    return -38; // -ENOSYS
}

int64_t ax_sys_read(uint64_t fd,uint64_t buf,uint64_t count,uint64_t,uint64_t,uint64_t){
    if(!buf) return -14;
    ssize_t r = ::read((int)fd,(void*)(uintptr_t)buf,(size_t)count);
    return r < 0 ? -errno : (int64_t)r;
}

int64_t ax_sys_write(uint64_t fd,uint64_t buf,uint64_t count,uint64_t,uint64_t,uint64_t){
    if(!buf) return -14;
    if(count > (1ULL<<20)) count = (1ULL<<20);
    ssize_t r = ::write((int)fd,(const void*)(uintptr_t)buf,(size_t)count);
    return r < 0 ? -errno : (int64_t)r;
}

int64_t ax_sys_close(uint64_t fd,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){
    int r = ::close((int)fd);
    return r < 0 ? -errno : 0;
}

int64_t ax_sys_lseek(uint64_t fd,uint64_t off,uint64_t whence,uint64_t,uint64_t,uint64_t){
    off_t r = ::lseek((int)fd,(off_t)off,(int)whence);
    return r < 0 ? -errno : (int64_t)r;
}

int64_t ax_sys_openat(uint64_t dfd,uint64_t path,uint64_t flags,uint64_t mode,uint64_t,uint64_t){
    if(!path) return -14;
    int r = ::openat((int)dfd,(const char*)(uintptr_t)path,(int)flags,(mode_t)mode);
    return r < 0 ? -errno : (int64_t)r;
}

int64_t ax_sys_mmap(uint64_t addr,uint64_t len,uint64_t prot,uint64_t flags,uint64_t fd,uint64_t off){
    if(len==0) return -22;
    uint64_t alen = align16k(len); // Android 16: 16KB page
    // W^X fix: EXEC + WRITE together not allowed, do WRITE then mprotect EXEC
    int hprot = 0;
    if(prot & 1) hprot |= PROT_READ;
    if(prot & 2) hprot |= PROT_WRITE;
    // defer EXEC to second step
    int hflags = MAP_PRIVATE | MAP_ANONYMOUS;
    if(flags & 0x20) hflags |= MAP_ANONYMOUS;
    if(flags & 0x10) hflags |= MAP_FIXED;
    if((int)fd >= 0) hflags = (flags & 0x0F) | MAP_PRIVATE;
    void* want = addr ? (void*)(uintptr_t)addr : nullptr;
    void* p;
    if((int)fd >= 0){
        p = ::mmap(want,(size_t)alen,hprot | ((prot&4)?PROT_EXEC:0),hflags,(int)fd,(off_t)off);
    } else {
        p = ::mmap(want,(size_t)alen,hprot,hflags,-1,0);
    }
    if(p==MAP_FAILED) return -errno;
    if((prot & 6)==6){
        // WRITE+EXEC: remap to EXEC after write phase via mprotect lazily
        // keep as RW now, game will call mprotect later
    }
    ax_task_struct* t = ax_task_current();
    if(t && t->mm) t->mm->total_vm += alen;
    return (int64_t)(uintptr_t)p;
}

int64_t ax_sys_mprotect(uint64_t addr,uint64_t len,uint64_t prot,uint64_t,uint64_t,uint64_t){
    int h=0;
    if(prot&1) h|=PROT_READ;
    if(prot&2) h|=PROT_WRITE;
    if(prot&4) h|=PROT_EXEC;
    uint64_t alen = align16k(len);
    uint64_t aaddr = (addr) & ~16383ULL;
    int r = ::mprotect((void*)(uintptr_t)aaddr,(size_t)alen,h);
    return r<0?-errno:0;
}

int64_t ax_sys_munmap(uint64_t addr,uint64_t len,uint64_t,uint64_t,uint64_t,uint64_t){
    int r = ::munmap((void*)(uintptr_t)addr,(size_t)align16k(len));
    return r<0?-errno:0;
}

int64_t ax_sys_brk(uint64_t brk,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){
    ax_task_struct* t = ax_task_current();
    if(!t || !t->mm) return -12;
    if(brk==0) return (int64_t)t->mm->brk;
    if(brk < t->mm->start_brk) return (int64_t)t->mm->brk;
    // grow via mmap anonymous
    uint64_t cur = align16k(t->mm->brk);
    uint64_t want = align16k(brk);
    if(want > cur){
        void* p = ::mmap((void*)(uintptr_t)cur,(size_t)(want-cur),PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS|MAP_FIXED,-1,0);
        if(p==MAP_FAILED) return -12;
    }
    t->mm->brk = brk;
    return (int64_t)brk;
}

int64_t ax_sys_clone(uint64_t flags,uint64_t stack,uint64_t ptid,uint64_t tls,uint64_t ctid,uint64_t){
    (void)ptid; (void)ctid;
    // map guest clone to host clone for threads: CLONE_VM|FS|FILES|SIGHAND|THREAD|SYSVSEM
    // minimal: create new ax_task sharing mm
    ax_task_struct* cur = ax_task_current();
    ax_task_struct* child = ax_task_create(cur ? cur->comm : "axemu");
    if(!child) return -11;
    if(cur && cur->mm) child->mm = cur->mm; // share
    child->thread_regs->rsp = stack;
    // TLS from arch_prctl area
    child->fs_base = tls;
    return child->pid;
}

int64_t ax_sys_futex(uint64_t uaddr,uint64_t op,uint64_t val,uint64_t timeout,uint64_t uaddr2,uint64_t val3){
    // bridge directly to host futex - fast and correct for games
    long r = ::syscall(SYS_futex,(void*)(uintptr_t)uaddr,(int)op,(int)val,(void*)(uintptr_t)timeout,(void*)(uintptr_t)uaddr2,(int)val3);
    return r<0?-errno:(int64_t)r;
}

int64_t ax_sys_getrandom(uint64_t buf,uint64_t len,uint64_t flags,uint64_t,uint64_t,uint64_t){
    if(!buf) return -14;
    ssize_t r = ::getrandom((void*)(uintptr_t)buf,(size_t)len,(unsigned)flags);
    return r<0?-errno:(int64_t)r;
}

int64_t ax_sys_arch_prctl(uint64_t code,uint64_t addr,uint64_t,uint64_t,uint64_t,uint64_t){
    ax_task_struct* t = ax_task_current();
    if(!t) return -22;
    if(code==ARCH_SET_FS){ t->fs_base = addr; return 0; }
    if(code==ARCH_SET_GS){ t->gs_base = addr; return 0; }
    if(code==ARCH_GET_FS){ if(!addr) return -14; *(uint64_t*)(uintptr_t)addr = t->fs_base; return 0; }
    if(code==ARCH_GET_GS){ if(!addr) return -14; *(uint64_t*)(uintptr_t)addr = t->gs_base; return 0; }
    return -22;
}

int64_t ax_sys_exit(uint64_t code,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){
    ax_task_exit((int)code);
    return 0;
}
int64_t ax_sys_exit_group(uint64_t code,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){
    ax_task_exit((int)code);
    return 0;
}
int64_t ax_sys_set_tid_address(uint64_t tidptr,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){
    ax_task_struct* t = ax_task_current();
    if(!t) return -22;
    t->clear_child_tid = tidptr;
    return t->pid;
}
int64_t ax_sys_getuid(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ return ::getuid(); }
int64_t ax_sys_getgid(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ return ::getgid(); }
int64_t ax_sys_geteuid(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ return ::geteuid(); }
int64_t ax_sys_getegid(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ return ::getegid(); }

// table - 512 entries for O(1) dispatch
#define TBL_SIZE 512
static sys_call_t ax_sys_call_table[TBL_SIZE];
static int g_tbl_inited = 0;

void ax_init_syscall_table(void){
    if(g_tbl_inited) return;
    for(int i=0;i<TBL_SIZE;i++) ax_sys_call_table[i]= (sys_call_t)sys_ni_syscall;
    ax_sys_call_table[0]=ax_sys_read;
    ax_sys_call_table[1]=ax_sys_write;
    ax_sys_call_table[2]=ax_sys_openat; // open -> openat shim
    ax_sys_call_table[3]=ax_sys_close;
    ax_sys_call_table[8]=ax_sys_lseek;
    ax_sys_call_table[9]=ax_sys_mmap;
    ax_sys_call_table[10]=ax_sys_mprotect;
    ax_sys_call_table[11]=ax_sys_munmap;
    ax_sys_call_table[12]=ax_sys_brk;
    ax_sys_call_table[56]=ax_sys_clone;
    ax_sys_call_table[60]=ax_sys_exit;
    ax_sys_call_table[72]= (sys_call_t)sys_ni_syscall;
    ax_sys_call_table[102]=ax_sys_getuid;
    ax_sys_call_table[104]=ax_sys_getgid;
    ax_sys_call_table[107]=ax_sys_geteuid;
    ax_sys_call_table[108]=ax_sys_getegid;
    ax_sys_call_table[158]=ax_sys_arch_prctl;
    ax_sys_call_table[202]=ax_sys_futex;
    ax_sys_call_table[218]=ax_sys_set_tid_address;
    ax_sys_call_table[231]=ax_sys_exit_group;
    ax_sys_call_table[257]=ax_sys_openat;
    ax_sys_call_table[318]=ax_sys_getrandom;
    g_tbl_inited=1;
}

__attribute__((always_inline))
inline int64_t ax_invoke_syscall(uint64_t n,uint64_t a1,uint64_t a2,uint64_t a3,uint64_t a4,uint64_t a5,uint64_t a6){
    if(__builtin_expect(!g_tbl_inited,0)) ax_init_syscall_table();
    if(__builtin_expect(n >= TBL_SIZE,0)) return -38;
    __atomic_fetch_add(&g_fast_hits,1,__ATOMIC_RELAXED);
    return ax_sys_call_table[n](a1,a2,a3,a4,a5,a6);
}

void ax_syscall_stats(uint64_t* hits,uint64_t* miss){
    if(hits) *hits = g_fast_hits;
    if(miss) *miss = g_enosys;
}

} // extern C
