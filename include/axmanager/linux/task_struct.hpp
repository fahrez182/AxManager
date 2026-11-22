/**
 * @file task_struct.hpp
 * @project AxManager Linux-Compatible Emulator Subsystem
 * @brief Exact Linux kernel process/thread descriptor layout (`task_struct`).
 */

#ifndef AXMANAGER_LINUX_TASK_STRUCT_HPP
#define AXMANAGER_LINUX_TASK_STRUCT_HPP

#include <cstdint>
#include <atomic>

#ifdef __cplusplus
extern "C" {
#endif

typedef int32_t pid_t;
typedef uint64_t u64;
typedef unsigned long mm_segment_t;

enum task_state_t {
    TASK_RUNNING = 0,
    TASK_INTERRUPTIBLE = 1,
    TASK_UNINTERRUPTIBLE = 2,
    __TASK_STOPPED = 4,
    __TASK_TRACED = 8,
    EXIT_ZOMBIE = 16,
    EXIT_DEAD = 32
};

struct pt_regs {
    u64 r15;
    u64 r14;
    u64 r13;
    u64 r12;
    u64 rbp;
    u64 rbx;
    u64 r11;
    u64 r10;
    u64 r9;
    u64 r8;
    u64 rax;
    u64 rcx;
    u64 rdx;
    u64 rsi;
    u64 rdi;
    u64 orig_rax;
    u64 rip;
    u64 cs;
    u64 eflags;
    u64 rsp;
    u64 ss;
};

struct ax_task_struct {
    volatile long state;          /* -1 unrunnable, 0 runnable, >0 stopped */
    void* stack;                  /* Kernel stack pointer */
    atomic_t usage;               /* Usage counter */
    pid_t pid;                    /* Process ID */
    pid_t tgid;                   /* Thread Group ID */
    char comm[16];                /* Program executable name */
    struct pt_regs* thread_regs;  /* Saved CPU registers for context switch */
    u64 flags;                    /* Per-process flags (e.g. PF_KTHREAD) */
};

#ifdef __cplusplus
}
#endif

#endif // AXMANAGER_LINUX_TASK_STRUCT_HPP
