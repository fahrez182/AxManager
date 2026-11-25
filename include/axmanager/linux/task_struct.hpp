#pragma once
#ifndef AXMANAGER_LINUX_TASK_STRUCT_HPP
#define AXMANAGER_LINUX_TASK_STRUCT_HPP
// v9999999999999999999999999999999-linux-ms-stable : task_struct.hpp
// NDK-safe C++17 - zero-log hot path - ms-stable - no API deleted
#pragma GCC optimize("O3,unroll-loops")
#include <cstdint>
#include <cstring>
#include <string>
#include <vector>
#include <unordered_map>
#include <mutex>
#include <shared_mutex>
#include <chrono>
#include <new>
#include <time.h>
#include <sys/types.h>
#define AX_LINUX_HYPER_LEVEL_STR "9999999999999999999999999999999"
#define AX_LIKELY(x) __builtin_expect(!!(x),1)
#define AX_UNLIKELY(x) __builtin_expect(!!(x),0)
typedef uint64_t u64;
typedef unsigned long mm_segment_t;
#ifndef _ATOMIC_T_DEFINED_
#define _ATOMIC_T_DEFINED_
#include <atomic>
typedef std::atomic<int> atomic_t;
#endif
#define PF_KTHREAD 0x00200000UL
#define PF_FORKNOEXEC 0x40UL
#define PF_SUPERPRIV 0x800UL
#define PF_DUMPCORE 0x200UL
#define PF_EXITING 0x4UL
#define CLONE_VM 0x100UL
#define CLONE_FS 0x200UL
#define CLONE_FILES 0x400UL
#define CLONE_SIGHAND 0x800UL
#define CLONE_THREAD 0x10000UL
#define CLONE_NEWNS 0x20000UL
#define CLONE_NEWPID 0x20000000UL
#define SCHED_NORMAL 0
#define SCHED_FIFO 1
#define SCHED_RR 2
#define SCHED_DEADLINE 6
enum task_state_t { TASK_RUNNING=0,TASK_INTERRUPTIBLE=1,TASK_UNINTERRUPTIBLE=2,__TASK_STOPPED=4,__TASK_TRACED=8,EXIT_ZOMBIE=16,EXIT_DEAD=32 };
inline const char* ax_task_state_str(long s){switch(s){case 0:return "R";case 1:return "S";case 2:return "D";case 4:return "T";case 8:return "t";case 16:return "Z";case 32:return "X";default:return "?";}}
struct pt_regs { u64 r15,r14,r13,r12,rbp,rbx,r11,r10,r9,r8,rax,rcx,rdx,rsi,rdi,orig_rax,rip,cs,eflags,rsp,ss; };
struct ax_arm64_regs { u64 x[31]; u64 sp,pc,pstate; };
enum ax_arch_t { AX_ARCH_X86_64=0, AX_ARCH_AARCH64=1 };
struct alignas(16) ax_cpu_regs { int arch; union { struct pt_regs x86; struct ax_arm64_regs arm64; }; u64 fpcr,fpsr; };
struct ax_cred { uint32_t uid,gid,euid,egid,suid,sgid; uint64_t cap[2]; uint32_t securebits; };
struct ax_mm_area { uint64_t start,end,prot,flags; char name[32]; };
struct ax_mm_struct { uint64_t total_vm,locked_vm,brk,start_brk,start_stack,mmap_base,hiwater; uint64_t minflt,majflt; struct ax_mm_area areas[16]; int nareas; uint64_t aslr_off; };
struct ax_file_entry { int fd; char path[128]; int flags; uint64_t pos; bool used; bool cloexec; };
struct ax_files_struct { struct ax_file_entry fds[64]; int maxfd; uint64_t open_cnt; };
struct ax_fs_struct { char root[128]; char pwd[128]; int umask; };
struct ax_sched_attr { int policy,nice,prio,static_prio,rt_prio; uint64_t vruntime,sum_exec,exec_start,deadline,period,runtime; uint64_t cpus_allowed[2]; int numa_node; uint64_t ctx_sw; };
struct ax_signal_struct { uint64_t pending,blocked; void* handler[64]; int sa_flags[64]; uint64_t sig_cnt[64]; };
struct ax_rlimit { uint64_t cur,mx; };
struct ax_rlimit_struct { struct ax_rlimit r[16]; };
struct ax_ns_struct { uint32_t pid_ns,net_ns,mnt_ns,uts_ns,ipc_ns,user_ns,cgroup_ns,time_ns; int64_t time_off_boot,time_off_mono; };
struct ax_cgroup_struct { char path[128]; int64_t cpu_quota,mem_limit; int pids_max,freezer; uint64_t cpu_used,mem_used; int64_t cpu_weight; };
struct ax_perf_struct { uint64_t ctx_sw,minflt,majflt,read_b,write_b,sysc; };
struct ax_clone3_args { uint64_t flags,pidfd,child_tid,parent_tid,exit_signal,stack,stack_size,tls,set_tid_size; };
#ifdef __cplusplus
extern "C" {
#endif
struct ax_task_struct {
#ifdef __cplusplus
std::atomic<long> state;
#else
volatile long state;
#endif
void* stack;
atomic_t usage;
pid_t pid;
pid_t tgid;
pid_t ppid;
char comm[16];
struct pt_regs* thread_regs;
u64 flags;
uint32_t uid,gid,euid,egid;
int exit_code,exit_sig;
int nice,prio,policy;
int cpu,pgid,sid;
uint64_t start_time_ns;
uint64_t utime_ns,stime_ns;
uint64_t tls,brk,start_brk;
char comm_full[64];
struct ax_cpu_regs cpu_regs;
struct ax_mm_struct* mm;
struct ax_files_struct* files;
struct ax_fs_struct* fs;
struct ax_cred* cred;
struct ax_sched_attr* sched;
struct ax_signal_struct* sig;
struct ax_rlimit_struct* rlim;
struct ax_ns_struct* ns;
struct ax_cgroup_struct* cg;
struct ax_perf_struct* perf;
void* sighand;
int seccomp_mode,personality,loginuid,oom_score;
int frozen;
uint64_t stack_canary;
char hyper_level[32];
};
#ifdef __cplusplus
}
#endif
#ifdef __cplusplus
namespace axmanager { namespace linux {
__attribute__((always_inline)) inline uint64_t ax_now_ns() noexcept { struct timespec ts; ::clock_gettime(CLOCK_MONOTONIC,&ts); return (uint64_t)ts.tv_sec*1000000000ULL+(uint64_t)ts.tv_nsec; }
__attribute__((always_inline)) inline void ax_set_comm(ax_task_struct* t,const char* n) noexcept { if(AX_UNLIKELY(!t||!n)) return; std::strncpy(t->comm,n,15); t->comm[15]=0; std::strncpy(t->comm_full,n,63); t->comm_full[63]=0; }
__attribute__((always_inline)) inline bool ax_valid_fd(uint64_t f) noexcept { return f<64; }
__attribute__((always_inline)) inline bool ax_valid_nr(uint64_t n) noexcept { return n<512; }
inline ax_task_struct* ax_task_alloc(pid_t pid,pid_t tgid,const char* name){
auto* t=new (std::nothrow) ax_task_struct(); if(AX_UNLIKELY(!t)) return nullptr;
t->state.store(TASK_RUNNING,std::memory_order_relaxed); t->stack=nullptr; t->usage.store(1,std::memory_order_relaxed);
t->pid=pid; t->tgid=tgid; t->ppid=0; ax_set_comm(t,name?name:"axTask");
t->thread_regs=nullptr; t->flags=0; t->uid=t->gid=t->euid=t->egid=0;
t->exit_code=0; t->exit_sig=0; t->nice=0; t->prio=120; t->policy=0;
t->cpu=0; t->pgid=tgid; t->sid=tgid; t->start_time_ns=ax_now_ns(); t->utime_ns=t->stime_ns=0;
t->tls=0; t->brk=t->start_brk=0x55000000ULL+(uint64_t)(uint32_t)pid*0x10000ULL;
t->cpu_regs.arch=AX_ARCH_X86_64; std::memset(&t->cpu_regs.x86,0,sizeof(t->cpu_regs.x86));
t->mm=new (std::nothrow) ax_mm_struct(); if(t->mm){ std::memset(t->mm,0,sizeof(*t->mm)); t->mm->brk=t->brk; t->mm->start_brk=t->brk; t->mm->mmap_base=0x7f0000000000ULL; t->mm->aslr_off=(uint64_t)(uint32_t)pid*4096ULL; }
t->files=new (std::nothrow) ax_files_struct(); if(t->files){ std::memset(t->files,0,sizeof(*t->files)); t->files->maxfd=2; for(int i=0;i<=2;++i){ t->files->fds[i].used=true; t->files->fds[i].fd=i; } }
t->fs=new (std::nothrow) ax_fs_struct(); if(t->fs){ std::strncpy(t->fs->root,"/",127); t->fs->root[127]=0; std::strncpy(t->fs->pwd,"/",127); t->fs->pwd[127]=0; t->fs->umask=022; }
t->cred=new (std::nothrow) ax_cred(); if(t->cred) std::memset(t->cred,0,sizeof(*t->cred));
t->sched=new (std::nothrow) ax_sched_attr(); if(t->sched){ std::memset(t->sched,0,sizeof(*t->sched)); t->sched->prio=120; t->sched->cpus_allowed[0]=~0ULL; t->sched->numa_node=-1; }
t->sig=new (std::nothrow) ax_signal_struct(); if(t->sig) std::memset(t->sig,0,sizeof(*t->sig));
t->rlim=new (std::nothrow) ax_rlimit_struct(); if(t->rlim){ for(int i=0;i<16;++i){ t->rlim->r[i].cur=1024ULL*1024ULL; t->rlim->r[i].mx=1024ULL*1024ULL*4ULL; } }
t->ns=new (std::nothrow) ax_ns_struct(); if(t->ns) std::memset(t->ns,0,sizeof(*t->ns));
t->cg=new (std::nothrow) ax_cgroup_struct(); if(t->cg){ std::strncpy(t->cg->path,"/ax",127); t->cg->path[127]=0; t->cg->cpu_quota=-1; t->cg->mem_limit=-1; t->cg->pids_max=4096; t->cg->cpu_weight=100; }
t->perf=new (std::nothrow) ax_perf_struct(); if(t->perf) std::memset(t->perf,0,sizeof(*t->perf));
t->sighand=nullptr; t->seccomp_mode=0; t->personality=0; t->loginuid=-1; t->oom_score=0; t->frozen=0; t->stack_canary=0x5A5A5A5A5A5A5A5AULL;
std::strncpy(t->hyper_level,AX_LINUX_HYPER_LEVEL_STR,31); t->hyper_level[31]=0;
return t; }
inline void ax_task_free(ax_task_struct* t){ if(AX_UNLIKELY(!t)) return; delete t->mm; delete t->files; delete t->fs; delete t->cred; delete t->sched; delete t->sig; delete t->rlim; delete t->ns; delete t->cg; delete t->perf; delete t; }
class AxTaskTable final {
public:
static AxTaskTable& get(){ static AxTaskTable i; return i; }
ax_task_struct* create(const char* name,pid_t ppid=0){ std::unique_lock<std::shared_mutex> l(m_); if(AX_UNLIKELY(map_.size()>=4096)) return nullptr; pid_t pid=next_++; if(pid<1000) pid=next_++; if(pid<=0) return nullptr; auto* t=ax_task_alloc(pid,pid,name); if(!t) return nullptr; t->ppid=ppid; map_[pid]=t; return t; }
ax_task_struct* fork_like(ax_task_struct* p,const char* name,unsigned long fl=0){ if(AX_UNLIKELY(!p)) return create(name); std::unique_lock<std::shared_mutex> l(m_); if(map_.size()>=4096) return nullptr; pid_t pid=next_++; pid_t tgid=(fl&CLONE_THREAD)?p->tgid:pid; auto* t=ax_task_alloc(pid,tgid,name?name:p->comm); if(!t) return nullptr; t->ppid=p->pid; t->flags=p->flags; t->uid=p->uid; t->gid=p->gid; t->euid=p->euid; t->egid=p->egid; t->nice=p->nice; t->cpu_regs=p->cpu_regs; if(t->mm&&p->mm&&!(fl&CLONE_VM)) *t->mm=*p->mm; if(t->files&&p->files&&!(fl&CLONE_FILES)) *t->files=*p->files; if(t->fs&&p->fs&&!(fl&CLONE_FS)) *t->fs=*p->fs; if(t->sig&&p->sig&&(fl&CLONE_SIGHAND)) *t->sig=*p->sig; if(t->ns&&p->ns) *t->ns=*p->ns; map_[pid]=t; return t; }
ax_task_struct* clone3_like(ax_task_struct* p,const ax_clone3_args* a,const char* n){ unsigned long fl=a?(unsigned long)a->flags:0; auto* t=fork_like(p,n,fl); if(t&&a){ t->exit_sig=(int)(a->exit_signal&0xff); t->tls=a->tls; } return t; }
bool exec_like(ax_task_struct* t,const char* path){ if(AX_UNLIKELY(!t||!path||!path[0])) return false; ax_set_comm(t,path); if(t->mm){ std::memset(t->mm->areas,0,sizeof(t->mm->areas)); t->mm->nareas=0; t->mm->total_vm=0; } if(t->sig) std::memset(t->sig,0,sizeof(*t->sig)); std::memset(&t->cpu_regs,0,sizeof(t->cpu_regs)); return true; }
void mark_zombie(ax_task_struct* t,int c){ if(AX_UNLIKELY(!t)) return; t->exit_code=c; t->state.store(EXIT_ZOMBIE,std::memory_order_release); }
bool reap(pid_t pid,int* code=0){ std::unique_lock<std::shared_mutex> l(m_); auto it=map_.find(pid); if(it==map_.end()) return false; long s=it->second->state.load(std::memory_order_acquire); if(s!=EXIT_ZOMBIE&&s!=EXIT_DEAD) return false; if(code) *code=it->second->exit_code; auto* t=it->second; map_.erase(it); if(cache_.ptr==t){cache_.ptr=nullptr;cache_.pid=-1;} l.unlock(); ax_task_free(t); return true; }
bool erase(pid_t pid){ std::unique_lock<std::shared_mutex> l(m_); auto it=map_.find(pid); if(it==map_.end()) return false; auto* t=it->second; map_.erase(it); if(cache_.ptr==t){cache_.ptr=nullptr;cache_.pid=-1;} l.unlock(); ax_task_free(t); return true; }
ax_task_struct* find(pid_t pid){ if(AX_UNLIKELY(pid<=0)) return nullptr; if(AX_LIKELY(cache_.pid==pid&&cache_.ptr!=nullptr)) return cache_.ptr; std::shared_lock<std::shared_mutex> l(m_); auto it=map_.find(pid); if(it==map_.end()) return nullptr; cache_.pid=pid; cache_.ptr=it->second; return it->second; }
std::vector<pid_t> list(){ std::shared_lock<std::shared_mutex> l(m_); std::vector<pid_t> r; r.reserve(map_.size()); for(auto&kv:map_) r.push_back(kv.first); return r; }
size_t count(){ std::shared_lock<std::shared_mutex> l(m_); return map_.size(); }
static void set_current(ax_task_struct* t){ current_=t; }
static ax_task_struct* current(){ return current_; }
bool send_signal(pid_t pid,int sig){ if(AX_UNLIKELY(sig<1||sig>64)) return false; auto* t=find(pid); if(AX_UNLIKELY(!t||!t->sig)) return false; t->sig->pending|=(1ULL<<((unsigned)(sig-1)%64ULL)); t->sig->sig_cnt[(sig-1)%64]++; return true; }
bool freeze(pid_t pid){ auto* t=find(pid); if(!t) return false; t->frozen=1; t->state.store(__TASK_STOPPED,std::memory_order_release); return true; }
bool thaw(pid_t pid){ auto* t=find(pid); if(!t) return false; t->frozen=0; t->state.store(TASK_RUNNING,std::memory_order_release); return true; }
__attribute__((cold)) std::string proc_stat(ax_task_struct* t){ if(!t) return ""; char b[512]; snprintf(b,sizeof(b),"%d (%s) %s %d %d %d %d %llu %llu",t->pid,t->comm,ax_task_state_str(t->state.load(std::memory_order_relaxed)),t->ppid,t->pgid,t->sid,(int)t->flags,(unsigned long long)t->utime_ns,(unsigned long long)t->stime_ns); return b; }
__attribute__((cold)) std::string proc_status(ax_task_struct* t){ if(!t) return ""; char b[768]; snprintf(b,sizeof(b),"Name:\t%s\nPid:\t%d\nPPid:\t%d\nState:\t%s\nUid:\t%u\nLevel:\t%s\n",t->comm,t->pid,t->ppid,ax_task_state_str(t->state.load(std::memory_order_relaxed)),t->uid,t->hyper_level); return b; }
__attribute__((cold)) std::string to_json(){ std::shared_lock<std::shared_mutex> l(m_); std::string s="{\"level\":\""; s+=AX_LINUX_HYPER_LEVEL_STR; s+="\",\"count\":"+std::to_string(map_.size())+",\"tasks\":["; bool f=true; for(auto&kv:map_){ auto* t=kv.second; if(!f) s+=","; f=false; s+="{\"pid\":"+std::to_string(t->pid)+",\"ppid\":"+std::to_string(t->ppid)+",\"st\":\""+ax_task_state_str(t->state.load(std::memory_order_relaxed))+"\",\"comm\":\""+t->comm+"\"}"; } s+="]}"; return s; }
private:
AxTaskTable():next_(1000){ map_.reserve(4096); auto* init=ax_task_alloc(1,1,"init"); if(init){ map_[1]=init; next_=1000; } cache_.pid=-1; cache_.ptr=nullptr; }
mutable std::shared_mutex m_; std::unordered_map<pid_t,ax_task_struct*> map_; pid_t next_;
struct Cache{ pid_t pid; ax_task_struct* ptr; }; static thread_local Cache cache_;
static thread_local ax_task_struct* current_;
};
inline thread_local AxTaskTable::Cache AxTaskTable::cache_{-1,nullptr};
inline thread_local ax_task_struct* AxTaskTable::current_=nullptr;
} }
extern "C" {
inline void* ax_task_create_c(const char* n){ return (void*)axmanager::linux::AxTaskTable::get().create(n); }
inline void* ax_task_find_c(int pid){ return (void*)axmanager::linux::AxTaskTable::get().find((pid_t)pid); }
inline int ax_task_current_pid_c(){ auto* c=axmanager::linux::AxTaskTable::current(); return c?(int)c->pid:-1; }
inline const char* ax_linux_version_c(){ return AX_LINUX_HYPER_LEVEL_STR "-linux-ms-stable"; }
inline const char* ax_linux_level_c(){ return AX_LINUX_HYPER_LEVEL_STR; }
}
#endif
#endif
