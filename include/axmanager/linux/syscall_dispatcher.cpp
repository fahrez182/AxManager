// v9999999999999999999999999999999-linux-ms-stable : syscall_dispatcher.cpp
// zero-lock hot path, ms-stable, NDK-safe C++17 - no API deleted
#pragma GCC optimize("O3,unroll-loops")
#include "task_struct.hpp"
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <array>
#include <atomic>
#include <mutex>
#include <vector>
#include <unistd.h>
#include <fcntl.h>
#include <sched.h>
#include <sys/mman.h>
#include <sys/time.h>
#include <sys/utsname.h>
#include <sys/stat.h>
#include <sys/uio.h>
#include <time.h>
#include <errno.h>
#define AX_LIKELY(x) __builtin_expect(!!(x),1)
#define AX_UNLIKELY(x) __builtin_expect(!!(x),0)
#define AX_NR_read 0
#define AX_NR_write 1
#define AX_NR_open 2
#define AX_NR_close 3
#define AX_NR_stat 4
#define AX_NR_fstat 5
#define AX_NR_lseek 8
#define AX_NR_mmap 9
#define AX_NR_mprotect 10
#define AX_NR_munmap 11
#define AX_NR_brk 12
#define AX_NR_readv 19
#define AX_NR_writev 20
#define AX_NR_access 21
#define AX_NR_pipe 22
#define AX_NR_sched_yield 24
#define AX_NR_madvise 28
#define AX_NR_dup 32
#define AX_NR_dup2 33
#define AX_NR_nanosleep 35
#define AX_NR_getpid 39
#define AX_NR_clone 56
#define AX_NR_fork 57
#define AX_NR_execve 59
#define AX_NR_exit 60
#define AX_NR_wait4 61
#define AX_NR_kill 62
#define AX_NR_uname 63
#define AX_NR_fcntl 72
#define AX_NR_getcwd 79
#define AX_NR_chdir 80
#define AX_NR_unlink 87
#define AX_NR_readlink 89
#define AX_NR_getuid 102
#define AX_NR_getppid 110
#define AX_NR_gettid 186
#define AX_NR_clock_gettime 228
#define AX_NR_exit_group 231
#define AX_NR_openat 257
#define AX_NR_dup3 292
#define AX_NR_getrandom 318
extern "C" {
typedef int64_t (*sys_call_t)(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t);
int64_t sys_ni_syscall(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ return -38; }
int64_t ax_sys_write(uint64_t fd,uint64_t buf,uint64_t count,uint64_t,uint64_t,uint64_t);
int64_t ax_sys_exit(uint64_t code,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t);
int64_t ax_invoke_syscall(uint64_t nr,uint64_t a1,uint64_t a2,uint64_t a3,uint64_t a4,uint64_t a5,uint64_t a6);
}
namespace axm_lin {
using fn_t=int64_t(*)(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t);
struct Stat{ std::atomic<uint64_t> cnt{0},tot{0},mx{0},err{0}; };
struct alignas(64) Box{
fn_t t[512]; const char* n[512]; Stat s[512];
std::mutex mx; std::vector<std::string> trace; size_t ti=0;
std::atomic<bool> trace_on{false};
std::atomic<unsigned char> deny[512];
std::atomic<int> inject_err{0}; std::atomic<uint32_t> inject_rate{0}; std::atomic<uint32_t> delay_us{0};
Box(); int64_t call(uint64_t nr,uint64_t a1,uint64_t a2,uint64_t a3,uint64_t a4,uint64_t a5,uint64_t a6);
void reg(uint64_t nr,fn_t f,const char* nm){ if(nr>=512||!f) return; std::lock_guard<std::mutex> l(mx); t[nr]=f; if(nm) n[nr]=nm; }
__attribute__((cold)) std::string json(); __attribute__((cold)) void log(const char* m);
};
__attribute__((always_inline)) inline uint64_t now_ns() noexcept { struct timespec ts; ::clock_gettime(CLOCK_MONOTONIC,&ts); return (uint64_t)ts.tv_sec*1000000000ULL+(uint64_t)ts.tv_nsec; }
__attribute__((always_inline)) inline axmanager::linux::ax_task_struct* cur() noexcept { auto* c=axmanager::linux::AxTaskTable::current(); if(AX_LIKELY(c!=nullptr)) return c; c=axmanager::linux::AxTaskTable::get().create("axemu"); axmanager::linux::AxTaskTable::set_current(c); return c; }
__attribute__((always_inline)) inline int real_fd(axmanager::linux::ax_task_struct* t,uint64_t vfd) noexcept { if(AX_UNLIKELY(!t||!t->files)) return (int)vfd; if(vfd<64&&vfd>=3&&t->files->fds[vfd].used) return t->files->fds[vfd].fd; return (int)vfd; }
__attribute__((always_inline)) inline int alloc_vfd(axmanager::linux::ax_task_struct* t,int host_fd,const char* path) noexcept { if(AX_UNLIKELY(!t||!t->files||host_fd<0)) return host_fd; for(int i=3;i<64;++i) if(!t->files->fds[i].used){ t->files->fds[i].used=true; t->files->fds[i].fd=host_fd; if(path){ std::strncpy(t->files->fds[i].path,path,127); t->files->fds[i].path[127]=0; } if(i>t->files->maxfd) t->files->maxfd=i; t->files->open_cnt++; return i; } return host_fd; }
static int64_t h_write(uint64_t fd,uint64_t buf,uint64_t cnt,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!buf)) return -14; if(AX_UNLIKELY(cnt>0x200000ULL)) return -22; if(AX_UNLIKELY(cnt==0)) return 0; if(fd==1||fd==2){ const char* p=(const char*)(uintptr_t)buf; ssize_t w=::write((int)fd,p,(size_t)cnt); auto* t=axmanager::linux::AxTaskTable::current(); if(t&&t->perf&&w>0) t->perf->write_b+=(uint64_t)w; return w<0?-5:(int64_t)w; } auto* t=cur(); if(AX_UNLIKELY(!t)) return -9; int rf=real_fd(t,fd); if(AX_UNLIKELY(rf<0)) return -9; ssize_t w=::write(rf,(const void*)(uintptr_t)buf,(size_t)cnt); if(w<0) return -9; if(fd<64&&t->files) t->files->fds[fd].pos+=(uint64_t)w; if(t->perf) t->perf->write_b+=(uint64_t)w; return (int64_t)w; }
static int64_t h_read(uint64_t fd,uint64_t buf,uint64_t cnt,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!buf)) return -14; if(cnt==0) return 0; if(AX_UNLIKELY(cnt>0x200000ULL)) return -22; if(fd==0) return 0; auto* t=cur(); if(AX_UNLIKELY(!t)) return -9; int rf=real_fd(t,fd); if(AX_UNLIKELY(rf<0)) return -9; ssize_t r=::read(rf,(void*)(uintptr_t)buf,(size_t)cnt); if(r<0) return -9; if(t->perf) t->perf->read_b+=(uint64_t)r; return (int64_t)r; }
static int64_t h_readv(uint64_t fd,uint64_t iov,uint64_t n,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!iov||!n||n>16)) return -14; auto* t=cur(); int rf=real_fd(t,fd); ssize_t r=::readv(rf,(const struct iovec*)(uintptr_t)iov,(int)n); return r<0?-9:(int64_t)r; }
static int64_t h_writev(uint64_t fd,uint64_t iov,uint64_t n,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!iov||!n||n>16)) return -14; auto* t=cur(); int rf=real_fd(t,fd); ssize_t r=::writev(rf,(const struct iovec*)(uintptr_t)iov,(int)n); return r<0?-9:(int64_t)r; }
static int64_t h_openat(uint64_t dfd,uint64_t path,uint64_t fl,uint64_t mode,uint64_t,uint64_t){ if(AX_UNLIKELY(!path)) return -14; const char* p=(const char*)(uintptr_t)path; if(AX_UNLIKELY(p[0]==0)) return -2; size_t L=strnlen(p,256); if(AX_UNLIKELY(L==0||L>=256)) return -36; int fd=::openat((int)dfd,p,(int)fl,(mode_t)mode); if(fd<0) fd=::open(p,(int)fl,(mode_t)mode); if(AX_UNLIKELY(fd<0)) return -2; auto* t=cur(); if(t&&t->files){ int v=alloc_vfd(t,fd,p); return v; } return fd; }
static int64_t h_open(uint64_t p,uint64_t f,uint64_t m,uint64_t,uint64_t,uint64_t){ return h_openat((uint64_t)(int64_t)-100,p,f,m,0,0); }
static int64_t h_close(uint64_t fd,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(fd>=64)) return -9; auto* t=cur(); if(t&&t->files&&fd>=3&&fd<64&&t->files->fds[fd].used){ int h=t->files->fds[fd].fd; ::close(h); t->files->fds[fd].used=false; t->files->fds[fd].fd=-1; return 0; } return ::close((int)fd)==0?0:-9; }
static int64_t h_lseek(uint64_t fd,uint64_t off,uint64_t wh,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(wh>2)) return -22; auto* t=cur(); int rf=real_fd(t,fd); if(AX_UNLIKELY(rf<0)) return -9; off_t r=::lseek(rf,(off_t)off,(int)wh); if(r>=0&&t&&t->files&&fd<64) t->files->fds[fd].pos=(uint64_t)r; return r<0?-22:(int64_t)r; }
static int64_t h_mmap(uint64_t a,uint64_t l,uint64_t pr,uint64_t fl,uint64_t,uint64_t){ if(AX_UNLIKELY(!l||l>0x800000ULL)) return -22; int pf=0; if(pr&1) pf|=PROT_READ; if(pr&2) pf|=PROT_WRITE; if(pr&4) pf|=PROT_EXEC; if(!pf) pf=PROT_READ|PROT_WRITE; int ff=MAP_PRIVATE|MAP_ANONYMOUS; if(fl&0x20) ff|=MAP_FIXED; void* r=::mmap((void*)(uintptr_t)a,(size_t)l,pf,ff,-1,0); if(AX_UNLIKELY(r==MAP_FAILED)) return -12; auto* t=axmanager::linux::AxTaskTable::current(); if(t&&t->mm&&t->mm->nareas<16){ auto& e=t->mm->areas[t->mm->nareas++]; e.start=(uint64_t)(uintptr_t)r; e.end=e.start+l; e.prot=pr; e.flags=fl; std::strncpy(e.name,"ax-anon",31); e.name[31]=0; t->mm->total_vm+=l; if(t->mm->total_vm>t->mm->hiwater) t->mm->hiwater=t->mm->total_vm; } return (int64_t)(uintptr_t)r; }
static int64_t h_munmap(uint64_t a,uint64_t l,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!a||!l)) return -22; return ::munmap((void*)(uintptr_t)a,(size_t)l)==0?0:-22; }
static int64_t h_mprotect(uint64_t a,uint64_t l,uint64_t p,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!a||!l)) return -22; int pf=0; if(p&1) pf|=PROT_READ; if(p&2) pf|=PROT_WRITE; if(p&4) pf|=PROT_EXEC; return ::mprotect((void*)(uintptr_t)a,(size_t)l,pf)==0?0:-22; }
static int64_t h_madvise(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ return 0; }
static int64_t h_brk(uint64_t b,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ auto* t=cur(); if(AX_UNLIKELY(!t)) return -12; if(!b) return (int64_t)t->brk; if(AX_UNLIKELY(b<0x10000ULL)) return -22; t->brk=b; if(t->mm) t->mm->brk=b; return (int64_t)b; }
static int64_t h_getpid(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ auto* t=axmanager::linux::AxTaskTable::current(); if(AX_LIKELY(t)) return (int64_t)t->pid; return (int64_t)::getpid(); }
static int64_t h_getppid(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ auto* t=axmanager::linux::AxTaskTable::current(); if(AX_LIKELY(t)) return (int64_t)t->ppid; return (int64_t)::getppid(); }
static int64_t h_gettid(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ auto* t=axmanager::linux::AxTaskTable::current(); if(AX_LIKELY(t)) return (int64_t)t->pid; return (int64_t)::getpid(); }
static int64_t h_getuid(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ auto* t=axmanager::linux::AxTaskTable::current(); if(AX_LIKELY(t)) return (int64_t)t->uid; return (int64_t)::getuid(); }
static int64_t h_exit(uint64_t c,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ auto* t=axmanager::linux::AxTaskTable::current(); if(!t) t=cur(); if(t) axmanager::linux::AxTaskTable::get().mark_zombie(t,(int)(c&0xff)); return 0; }
static int64_t h_execve(uint64_t p,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!p)) return -14; auto* t=cur(); if(AX_UNLIKELY(!t)) return -3; const char* s=(const char*)(uintptr_t)p; if(AX_UNLIKELY(!s[0])) return -2; bool ok=axmanager::linux::AxTaskTable::get().exec_like(t,s); return ok?0:-2; }
static int64_t h_clone(uint64_t fl,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ auto* p=cur(); auto* c=axmanager::linux::AxTaskTable::get().fork_like(p,p?p->comm:"axChild",(unsigned long)fl); return c?(int64_t)c->pid:-12; }
static int64_t h_wait4(uint64_t pid,uint64_t st,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(pid>0x7fffffffULL)) return -10; int code=0; bool ok=axmanager::linux::AxTaskTable::get().reap((pid_t)pid,&code); if(ok&&st) *(int*)(uintptr_t)st=(code&0xff)<<8; return ok?(int64_t)pid:-10; }
static int64_t h_kill(uint64_t p,uint64_t s,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(s>64)) return -22; return axmanager::linux::AxTaskTable::get().send_signal((pid_t)p,(int)s)?0:-3; }
static int64_t h_uname(uint64_t b,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!b)) return -14; struct utsname u; ::uname(&u); std::memcpy((void*)(uintptr_t)b,&u,sizeof(u)); return 0; }
static int64_t h_getcwd(uint64_t b,uint64_t s,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!b||!s||s>8192)) return -14; char* r=::getcwd((char*)(uintptr_t)b,(size_t)s); return r?(int64_t)std::strlen(r)+1:-34; }
static int64_t h_chdir(uint64_t p,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!p)) return -14; return ::chdir((const char*)(uintptr_t)p)==0?0:-2; }
static int64_t h_unlink(uint64_t p,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!p)) return -14; return ::unlink((const char*)(uintptr_t)p)==0?0:-2; }
static int64_t h_readlink(uint64_t p,uint64_t b,uint64_t s,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!p||!b||!s||s>4096)) return -14; ssize_t r=::readlink((const char*)(uintptr_t)p,(char*)(uintptr_t)b,(size_t)s); return r<0?-2:(int64_t)r; }
static int64_t h_clock(uint64_t id,uint64_t tp,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!tp)) return -14; if(AX_UNLIKELY(id>7)) return -22; struct timespec ts; ::clock_gettime((clockid_t)id,&ts); std::memcpy((void*)(uintptr_t)tp,&ts,sizeof(ts)); return 0; }
static int64_t h_nanosleep(uint64_t rq,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ if(!rq) return 0; struct timespec r=*(struct timespec*)(uintptr_t)rq; if(r.tv_sec>0||r.tv_nsec>1000000){ r.tv_sec=0; r.tv_nsec=1000000; } ::nanosleep(&r,nullptr); return 0; }
static int64_t h_yield(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ ::sched_yield(); return 0; }
static int64_t h_dup(uint64_t o,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(o>=64)) return -9; auto* t=cur(); int rf=real_fd(t,o); int h=::dup(rf>=0?rf:(int)o); if(h<0) return -9; if(t&&t->files){ int v=alloc_vfd(t,h,"dup"); return v; } return h; }
static int64_t h_dup2(uint64_t o,uint64_t n,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(o>=64||n>=64)) return -9; auto* t=cur(); int ro=real_fd(t,o); int h=::dup2(ro>=0?ro:(int)o,(int)n); if(h<0) return -9; if(t&&t->files&&n>=3&&n<64){ t->files->fds[n].used=true; t->files->fds[n].fd=h; if((int)n>t->files->maxfd) t->files->maxfd=(int)n; return (int64_t)n; } return h; }
static int64_t h_dup3(uint64_t o,uint64_t n,uint64_t f,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(o>=64||n>=64)) return -9; if(AX_UNLIKELY(f&~01000000)) return -22; if(AX_UNLIKELY(o==n)) return -22; auto* t=cur(); int ro=real_fd(t,o); int h=::dup3(ro>=0?ro:(int)o,(int)n,(int)f); if(h<0) return -9; if(t&&t->files&&n>=3&&n<64){ t->files->fds[n].used=true; t->files->fds[n].fd=h; t->files->fds[n].cloexec=(f!=0); if((int)n>t->files->maxfd) t->files->maxfd=(int)n; return (int64_t)n; } return h; }
static int64_t h_access(uint64_t p,uint64_t m,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!p)) return -14; if(AX_UNLIKELY(m>7)) return -22; return ::access((const char*)(uintptr_t)p,(int)m); }
static int64_t h_fcntl(uint64_t f,uint64_t c,uint64_t a,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(f>=1024)) return -9; return ::fcntl((int)f,(int)c,(long)a); }
static int64_t h_stat(uint64_t p,uint64_t b,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!p||!b)) return -14; struct stat st; int r=::stat((const char*)(uintptr_t)p,&st); if(r!=0) return -2; std::memcpy((void*)(uintptr_t)b,&st,sizeof(st)); return 0; }
static int64_t h_fstat(uint64_t f,uint64_t b,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!b)) return -14; auto* t=axmanager::linux::AxTaskTable::current(); int rf=t?real_fd(t,f):(int)f; if(AX_UNLIKELY(rf<0)) return -9; struct stat st; int r=::fstat(rf,&st); if(r!=0) return -9; std::memcpy((void*)(uintptr_t)b,&st,sizeof(st)); return 0; }
static int64_t h_pipe(uint64_t f,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!f)) return -14; int p[2]; if(::pipe(p)!=0) return -23; auto* t=cur(); if(t&&t->files){ int v0=alloc_vfd(t,p[0],"pipe0"); int v1=alloc_vfd(t,p[1],"pipe1"); ((int*)(uintptr_t)f)[0]=v0; ((int*)(uintptr_t)f)[1]=v1; return 0; } ((int*)(uintptr_t)f)[0]=p[0]; ((int*)(uintptr_t)f)[1]=p[1]; return 0; }
static int64_t h_getrandom(uint64_t b,uint64_t l,uint64_t,uint64_t,uint64_t,uint64_t){ if(AX_UNLIKELY(!b||!l)) return -14; if(l>256) l=256; uint64_t s=now_ns()^((uint64_t)(uintptr_t)b*0x9e3779b97f4a7c15ULL); unsigned char* o=(unsigned char*)(uintptr_t)b; for(uint64_t i=0;i<l;++i){ s^=s<<13; s^=s>>7; s^=s<<17; o[i]=(unsigned char)(s&0xff); } return (int64_t)l; }
Box::Box(){ for(size_t i=0;i<512;++i){ t[i]=sys_ni_syscall; n[i]="ni"; deny[i].store(0,std::memory_order_relaxed); } trace.reserve(256); trace.resize(256); reg(0,h_read,"read"); reg(1,h_write,"write"); reg(2,h_open,"open"); reg(3,h_close,"close"); reg(4,h_stat,"stat"); reg(5,h_fstat,"fstat"); reg(8,h_lseek,"lseek"); reg(9,h_mmap,"mmap"); reg(10,h_mprotect,"mprotect"); reg(11,h_munmap,"munmap"); reg(12,h_brk,"brk"); reg(19,h_readv,"readv"); reg(20,h_writev,"writev"); reg(21,h_access,"access"); reg(22,h_pipe,"pipe"); reg(24,h_yield,"sched_yield"); reg(28,h_madvise,"madvise"); reg(32,h_dup,"dup"); reg(33,h_dup2,"dup2"); reg(35,h_nanosleep,"nanosleep"); reg(39,h_getpid,"getpid"); reg(56,h_clone,"clone"); reg(57,h_clone,"fork"); reg(59,h_execve,"execve"); reg(60,h_exit,"exit"); reg(61,h_wait4,"wait4"); reg(62,h_kill,"kill"); reg(63,h_uname,"uname"); reg(72,h_fcntl,"fcntl"); reg(79,h_getcwd,"getcwd"); reg(80,h_chdir,"chdir"); reg(87,h_unlink,"unlink"); reg(89,h_readlink,"readlink"); reg(102,h_getuid,"getuid"); reg(110,h_getppid,"getppid"); reg(186,h_gettid,"gettid"); reg(228,h_clock,"clock_gettime"); reg(231,h_exit,"exit_group"); reg(257,h_openat,"openat"); reg(292,h_dup3,"dup3"); reg(318,h_getrandom,"getrandom"); }
__attribute__((cold)) void Box::log(const char* m){ if(!trace_on.load(std::memory_order_relaxed)||!m) return; std::lock_guard<std::mutex> l(mx); if(trace.empty()) return; trace[ti%trace.size()]=m; ++ti; }
__attribute__((always_inline)) int64_t Box::call(uint64_t nr,uint64_t a1,uint64_t a2,uint64_t a3,uint64_t a4,uint64_t a5,uint64_t a6){ if(AX_UNLIKELY(nr>=512)) return -38; __builtin_prefetch(&t[nr],0,3); if(AX_UNLIKELY(deny[nr].load(std::memory_order_relaxed))) return -1; uint32_t ir=inject_rate.load(std::memory_order_relaxed); if(AX_UNLIKELY(ir&&((now_ns()%100)<ir))){ int e=inject_err.load(std::memory_order_relaxed); return e?e:-11; } uint32_t du=delay_us.load(std::memory_order_relaxed); if(AX_UNLIKELY(du)){ if(du>1000) du=1000; ::usleep(du); } uint64_t s=now_ns(); int64_t r=t[nr](a1,a2,a3,a4,a5,a6); uint64_t e=now_ns()-s; s_[nr].cnt.fetch_add(1,std::memory_order_relaxed); s_[nr].tot.fetch_add(e,std::memory_order_relaxed); uint64_t c=s_[nr].mx.load(std::memory_order_relaxed); while(e>c&&!s_[nr].mx.compare_exchange_weak(c,e,std::memory_order_relaxed)){} if(AX_UNLIKELY(r<0)) s_[nr].err.fetch_add(1,std::memory_order_relaxed); if(AX_UNLIKELY(trace_on.load(std::memory_order_relaxed))){ char b[192]; snprintf(b,sizeof(b),"%s(%llu)=%lld %lluns",n[nr]?n[nr]:"ni",(unsigned long long)nr,(long long)r,(unsigned long long)e); log(b); } auto* tc=axmanager::linux::AxTaskTable::current(); if(AX_LIKELY(tc)){ tc->utime_ns+=e; if(tc->perf) tc->perf->sysc++; } return r; }
__attribute__((cold)) std::string Box::json(){ std::string o="{\"level\":\""; o+=AX_LINUX_HYPER_LEVEL_STR; o+="\",\"calls\":["; bool f=true; for(size_t i=0;i<512;++i){ uint64_t c=s_[i].cnt.load(std::memory_order_relaxed); if(!c) continue; if(!f) o+=","; f=false; uint64_t tot=s_[i].tot.load(std::memory_order_relaxed); uint64_t mx=s_[i].mx.load(std::memory_order_relaxed); uint64_t er=s_[i].err.load(std::memory_order_relaxed); o+="{\"nr\":"+std::to_string(i)+",\"n\":\""+(n[i]?n[i]:"ni")+"\",\"c\":"+std::to_string(c)+",\"avg\":"+std::to_string(tot/c)+",\"max\":"+std::to_string(mx)+",\"e\":"+std::to_string(er)+"}"; } return o+"]}"; }
inline Box& box(){ static Box b; return b; } }
extern "C" {
int64_t ax_sys_write(uint64_t fd,uint64_t buf,uint64_t count,uint64_t a4,uint64_t a5,uint64_t a6){ return axm_lin::h_write(fd,buf,count,a4,a5,a6); }
int64_t ax_sys_exit(uint64_t c,uint64_t a2,uint64_t a3,uint64_t a4,uint64_t a5,uint64_t a6){ return axm_lin::h_exit(c,a2,a3,a4,a5,a6); }
__attribute__((visibility("default"))) int64_t ax_invoke_syscall(uint64_t nr,uint64_t a1,uint64_t a2,uint64_t a3,uint64_t a4,uint64_t a5,uint64_t a6){ return axm_lin::box().call(nr,a1,a2,a3,a4,a5,a6); }
int ax_register_syscall(uint64_t nr,sys_call_t f){ if(nr>=512||!f) return -1; axm_lin::box().reg(nr,(axm_lin::fn_t)f,"custom"); return 0; }
void ax_syscall_trace_enable(int v){ axm_lin::box().trace_on.store(v!=0,std::memory_order_relaxed); }
void ax_syscall_deny(uint64_t nr,int d){ if(nr<512) axm_lin::box().deny[nr].store(d!=0?1:0,std::memory_order_relaxed); }
void ax_syscall_inject(int err,uint32_t rate){ axm_lin::box().inject_err.store(err,std::memory_order_relaxed); axm_lin::box().inject_rate.store(rate,std::memory_order_relaxed); }
void ax_syscall_set_delay(uint32_t us){ if(us>1000) us=1000; axm_lin::box().delay_us.store(us,std::memory_order_relaxed); }
int64_t ax_syscall_stats_json(char* out,uint64_t cap){ if(!out||!cap||cap<16) return -1; std::string j=axm_lin::box().json(); size_t n=j.size()<(size_t)(cap-1)?j.size():(size_t)(cap-1); std::memcpy(out,j.c_str(),n); out[n]=0; return (int64_t)n; }
sys_call_t ax_sys_call_table[512];
struct AxTblInit{ AxTblInit(){ for(int i=0;i<512;++i) ax_sys_call_table[i]=sys_ni_syscall; ax_sys_call_table[0]=(sys_call_t)axm_lin::h_read; ax_sys_call_table[1]=(sys_call_t)ax_sys_write; ax_sys_call_table[2]=(sys_call_t)axm_lin::h_open; ax_sys_call_table[3]=(sys_call_t)axm_lin::h_close; ax_sys_call_table[33]=(sys_call_t)axm_lin::h_dup2; ax_sys_call_table[59]=(sys_call_t)axm_lin::h_execve; ax_sys_call_table[60]=(sys_call_t)ax_sys_exit; ax_sys_call_table[231]=(sys_call_t)axm_lin::h_exit; ax_sys_call_table[257]=(sys_call_t)axm_lin::h_openat; ax_sys_call_table[292]=(sys_call_t)axm_lin::h_dup3; } };
static AxTblInit ax_tbl_init;
}
