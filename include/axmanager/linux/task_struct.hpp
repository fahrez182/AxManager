/**
 * @file task_struct.hpp
 * @project AxManager Linux-Emu v2
 * @brief Exact x86_64 task + mm + jit, Android 16 16K page ready
 */
#ifndef AXMANAGER_LINUX_TASK_STRUCT_HPP
#define AXMANAGER_LINUX_TASK_STRUCT_HPP
#include <cstdint>
#include <atomic>
#include <cstddef>
#ifdef __cplusplus
extern "C" {
#endif

typedef int32_t ax_pid_t;
typedef uint64_t u64;

#define AX_PAGE_SHIFT 14
#define AX_PAGE_SIZE (1ULL<<AX_PAGE_SHIFT) // 16384 Android 16
#define AX_COMM_LEN 16
#define AX_JIT_SLOTS 1024

enum ax_task_state_t {
    TASK_RUNNING = 0,
    TASK_INTERRUPTIBLE = 1,
    TASK_UNINTERRUPTIBLE = 2,
    __TASK_STOPPED = 4,
    __TASK_TRACED = 8,
    EXIT_ZOMBIE = 16,
    EXIT_DEAD = 32
};

struct ax_pt_regs {
    u64 r15,r14,r13,r12,rbp,rbx,r11,r10,r9,r8;
    u64 rax,rcx,rdx,rsi,rdi,orig_rax,rip,cs,eflags,rsp,ss;
};

struct ax_fpu_state {
    u64 mxcsr;
    // 16 XMM regs, low 128-bit. AVX upper halves for JIT -> NEON/SVE mapping
    __attribute__((aligned(16))) unsigned char xmm[16][16];
    unsigned char ymm_hi[16][16];
};

struct ax_vma_struct {
    u64 start, end;
    u64 prot; // 1=R 2=W 4=X
    u64 flags;
    int fd;
    u64 offset;
    ax_vma_struct* next;
};

struct ax_mm_struct {
    u64 start_brk, brk;
    u64 start_stack;
    u64 total_vm;
    ax_vma_struct* mmap_list;
    std::atomic<int> refcnt;
};

struct ax_jit_block {
    u64 guest_rip;
    void* host_pc;
    u64 size;
    u64 exec_count;
    int valid;
};

struct ax_task_struct {
    volatile long state;
    void* stack;
    std::atomic<int> usage;
    ax_pid_t pid;
    ax_pid_t tgid;
    char comm[AX_COMM_LEN];
    ax_pt_regs* thread_regs;
    ax_fpu_state* fpu;
    u64 flags;
    // TLS bases - critical for x86 .so
    u64 fs_base;
    u64 gs_base;
    u64 clear_child_tid;
    u64 robust_list;
    ax_mm_struct* mm;
    // fast JIT cache for repeated blocks
    ax_jit_block jit[AX_JIT_SLOTS];
    ax_task_struct* next;
};

// scheduler / lifecycle (implemented in AxOrchestrator / ax_task.cpp)
ax_task_struct* ax_task_create(const char* comm);
ax_task_struct* ax_task_current(void);
void ax_task_switch(ax_task_struct* next);
void ax_task_exit(int code);
ax_mm_struct* ax_mm_alloc(void);
void ax_mm_free(ax_mm_struct* mm);

// helpers
static inline u64 ax_align16k(u64 v){ return (v + AX_PAGE_SIZE - 1) & ~(AX_PAGE_SIZE-1); }
static inline int ax_jit_slot(u64 rip){ return (int)((rip >> 4) & (AX_JIT_SLOTS-1)); }

#ifdef __cplusplus
}
#endif
#endif
