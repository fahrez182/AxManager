/**
 * @file VoidSyscallInterceptor.hpp
 * @project AxManager Hyper-Kernel
 * @author Amirhossein Tavakoli
 * @brief Void-class System Call Interception & Transformation Engine.
 * 
 * This engine operates in the space between user-space applications and 
 * the host kernel. It intercepts every syscall (via MSR/SYSENTER/SYSCALL hooks), 
 * inspects parameters in zero-copy memory, and can modify, redirect, 
 * or annihilate the execution flow before the OS even realizes.
 */

#ifndef AXMANAGER_NEXUS_VOID_SYSCALL_INTERCEPTOR_HPP
#define AXMANAGER_NEXUS_VOID_SYSCALL_INTERCEPTOR_HPP

#include <cstdint>
#include <functional>
#include <unordered_map>
#include <shared_mutex>
#include <atomic>
#include <expected>
#include <span>

namespace axmanager::nexus {

/**
 * @brief Interception Verdict codes.
 */
enum class InterceptVerdict : uint8_t {
    ALLOW,      // Let syscall pass unmodified
    MODIFY,     // Alter parameters on the fly
    REDIRECT,   // Redirect execution to hyper-kernel hook
    ANNIHILATE  // Terminate execution stream instantly (Security violation)
};

/**
 * @brief Detailed Syscall Context for deep inspection.
 */
struct SyscallContext {
    uint64_t syscall_number;
    uint64_t args[6]; // Standard x86_64 ABI argument registers (rdi, rsi, rdx, r10, r8, r9)
    uint64_t stack_pointer;
    uint64_t instruction_pointer;
    pid_t source_pid;
};

/**
 * @brief Signature for custom Hyper-Kernel hook handlers.
 */
using VoidHookRoutine = std::function<InterceptVerdict(SyscallContext&)>;

/**
 * @class VoidSyscallInterceptor
 * @brief The ultimate gatekeeper of software execution boundaries.
 */
class VoidSyscallInterceptor {
public:
    static VoidSyscallInterceptor& getInterceptorInstance();

    VoidSyscallInterceptor(const VoidSyscallInterceptor&) = delete;
    VoidSyscallInterceptor& operator=(const VoidSyscallInterceptor&) = delete;

    /**
     * @brief Activates the interception layer by hooking the CPU's Model Specific Registers (MSR).
     */
    void engage();

    /**
     * @brief Deactivates the interception layer and restores original vectors.
     */
    void disengage();

    /**
     * @brief Registers a specialized hook routine for a specific system call number.
     */
    void register_hook(uint64_t syscall_number, VoidHookRoutine routine);

    /**
     * @brief Removes a registered hook.
     */
    void unregister_hook(uint64_t syscall_number);

    /**
     * @brief Main processing pipeline called upon intercepting a system call.
     */
    InterceptVerdict process_intercepted_call(SyscallContext& context);

    bool is_engaged() const noexcept { return m_is_engaged.load(); }

private:
    VoidSyscallInterceptor() : m_is_engaged(false) {}
    ~VoidSyscallInterceptor() = default;

    mutable std::shared_mutex m_lock;
    std::atomic<bool> m_is_engaged;
    std::unordered_map<uint64_t, VoidHookRoutine> m_hook_registry;
};

} // namespace axmanager::nexus

#endif // AXMANAGER_NEXUS_VOID_SYSCALL_INTERCEPTOR_HPP
