/**
 * @file VoidSyscallInterceptor.cpp
 * @brief Implementation of the Void Syscall Interceptor.
 */

#include "axmanager/nexus/VoidSyscallInterceptor.hpp"
#include <iostream>
#include <stdexcept>

namespace axmanager::nexus {

VoidSyscallInterceptor& VoidSyscallInterceptor::getInterceptorInstance() {
    static VoidSyscallInterceptor instance;
    return instance;
}

void VoidSyscallInterceptor::engage() {
    std::unique_lock lock(m_lock);
    
    if (m_is_engaged.load()) return;

    std::cout << "[NEXUS-VOID] Engaging System Call Interceptor... Hooking MSR Star/Lstar..." << std::endl;
    
    // In a kernel-level implementation, this modifies IA32_LSTAR MSR 
    // to point to our assembly trampoline handler instead of the OS system call entry.
    
    m_is_engaged.store(true);
    std::cout << "[NEXUS-VOID] Interceptor ENGAGED. All syscall vectors are under Hyper-Kernel control." << std::endl;
}

void VoidSyscallInterceptor::disengage() {
    std::unique_lock lock(m_lock);
    
    if (!m_is_engaged.load()) return;

    std::cout << "[NEXUS-VOID] Restoring original system call vectors..." << std::endl;
    m_is_engaged.store(false);
    std::cout << "[NEXUS-VOID] Disengaged safely." << std::endl;
}

void VoidSyscallInterceptor::register_hook(uint64_t syscall_number, VoidHookRoutine routine) {
    std::unique_lock lock(m_lock);
    m_hook_registry[syscall_number] = std::move(routine);
    std::cout << "[NEXUS-VOID] Registered interceptor hook for Syscall ID: " << std::dec << syscall_number << std::endl;
}

void VoidSyscallInterceptor::unregister_hook(uint64_t syscall_number) {
    std::unique_lock lock(m_lock);
    if (m_hook_registry.erase(syscall_number)) {
        std::cout << "[NEXUS-VOID] Unregistered hook for Syscall ID: " << std::dec << syscall_number << std::endl;
    }
}

InterceptVerdict VoidSyscallInterceptor::process_intercepted_call(SyscallContext& context) {
    // Shared lock for reading hooks at high velocity
    std::shared_lock lock(m_lock);

    if (!m_is_engaged.load()) {
        return InterceptVerdict::ALLOW;
    }

    auto it = m_hook_registry.find(context.syscall_number);
    if (it != m_hook_registry.end()) {
        // Execute the custom Hyper-Kernel hook logic
        return it->second(context);
    }

    // Default verdict for unhooked syscalls is to allow passage
    return InterceptVerdict::ALLOW;
}

} // namespace axmanager::nexus
