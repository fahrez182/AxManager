/**
 * @file syscall_dispatcher.cpp
 * @project AxManager Linux-Compatible Emulator Subsystem
 * @brief System Call Dispatch Table and Handler (x86_64 Linux ABI compatible).
 */

#include <iostream>
#include <cstdint>

#define __NR_read 0
#define __NR_write 1
#define __NR_open 2
#define __NR_close 3
#define __NR_exit 60

extern "C" {

typedef int64_t (*sys_call_t)(uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t);

int64_t sys_ni_syscall(uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t) {
    std::cout << "[LINUX-EMU] ENOSYS: Not implemented system call." << std::endl;
    return -38; // -ENOSYS
}

int64_t ax_sys_write(uint64_t fd, uint64_t buf, uint64_t count, uint64_t, uint64_t, uint64_t) {
    // شبیه‌ساز دقیق رفتار سیستم‌کال write لینوکس (FD 1 = stdout)
    if (fd == 1 && buf != 0) {
        std::cout << "[AX-STDOUT] " << reinterpret_cast<const char*>(buf) << std::endl;
        return static_cast<int64_t>(count);
    }
    return -9; // -EBADF
}

int64_t ax_sys_exit(uint64_t error_code, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t) {
    std::cout << "[LINUX-EMU] Process exit triggered with code: " << static_cast<int>(error_code) << std::endl;
    return 0;
}

// جدول سیستم‌کال‌های لینوکس (Linux Syscall Dispatch Table)
const sys_call_t ax_sys_call_table[] = {
    /* 0 */ reinterpret_cast<sys_call_t>(sys_ni_syscall), // read
    /* 1 */ ax_sys_write,                                 // write
    /* 2 */ reinterpret_cast<sys_call_t>(sys_ni_syscall), // open
    /* 3 */ reinterpret_cast<sys_call_t>(sys_ni_syscall), // close
    /* ... */
    /* 60 */ ax_sys_exit                                  // exit
};

/**
 * @brief Dispatcher entry point called from low-level trap/interrupt handler.
 */
int64_t ax_invoke_syscall(uint64_t syscall_number, uint64_t arg1, uint64_t arg2, 
                          uint64_t arg3, uint64_t arg4, uint64_t arg5, uint64_t arg6) {
    if (syscall_number >= (sizeof(ax_sys_call_table) / sizeof(sys_call_t))) {
        return -38; // -ENOSYS
    }
    
    return ax_sys_call_table[syscall_number](arg1, arg2, arg3, arg4, arg5, arg6);
}

} // extern "C"
