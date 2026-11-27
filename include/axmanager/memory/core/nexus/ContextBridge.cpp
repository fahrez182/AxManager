/**
 * @file ContextBridge.cpp
 * @project AxManager Hyper-Kernel + Linux Emulator
 * @brief The glue layer that connects Hardware-level interrupts/context to Linux-mode syscalls.
 */

#include "axmanager/nexus/arch/x86_asm_intrinsics.hpp"
#include "axmanager/linux/task_struct.hpp"
#include "axmanager/linux/syscall_dispatcher.cpp" // Direct integration of dispatcher logic
#include "axmanager/nexus/HardwareTelemetryBus.hpp"
#include <iostream>

namespace axmanager::nexus {

class ContextBridge {
public:
    static ContextBridge& get_instance() {
        static ContextBridge instance;
        return instance;
    }

    /**
     * @brief Performs a full context switch from Hardware/Hypervisor mode to Linux User mode.
     * This is the "Moment of Transition".
     */
    void switch_to_linux_task(ax_task_struct* task, pt_regs* regs) noexcept {
        std::cout << "[BRIDGE] Context Switch: Hypervisor -> Linux Task (PID: " << task->pid << ")" << std::endl;

        // 1. Restore Hardware State (Telemetry & Cache)
        arch::memory_fence();
        
        // 2. Load Task Registers (Simulating the loading of CPU state)
        // In a real kernel, this would be done via assembly: mov rax, [regs->rax] etc.
        // Here we link the software task to the hardware registers.
        
        // 3. Trigger Telemetry for the switch
        HardwareTelemetryBus::get_instance().emit_sample(0x10, task->pid, 0x01); // 0x10 = Context Switch Event

        // 4. Ready to execute the syscall dispatcher
        std::cout << "[BRIDGE] CPU State synchronized. Entering Linux ABI mode." << std::endl;
    }

    /**
     * @brief Handles a system call transition from Linux mode back to Hypervisor control.
     */
    void handle_syscall_interception(ax_task_struct* task, uint64_t syscall_num, uint64_t arg1, uint64_t arg2, 
                                    uint64_t arg3, uint64_t arg4, uint64_t arg5, uint64_t arg6) noexcept {
        
        std::cout << "[BRIDGE] Syscall Interception: PID " << task->pid << " requesting syscall " << syscall_num << std::endl;

        // 1. Record high-precision timing of the syscall start
        uint64_t start_tsc = arch::read_rdtsc();

        // 2. Dispatch the syscall via the Linux Emulator Table
        int64_t result = ax_invoke_syscall(syscall_num, arg1, arg2, arg3, arg4, arg5, arg6);

        // 3. Record timing and telemetry
        uint64_t end_tsc = arch::read_rdtsc();
        uint64_t latency = end_tsc - start_tsc;

        HardwareTelemetryBus::get_instance().emit_sample(0x20, syscall_num, latency); // 0x20 = Syscall Latency

        // 4. Return result to the simulated CPU
        std::cout << "[BRIDGE] Syscall " << syscall_num << " completed with result: " << result 
                  << " (Latency: " << latency << " cycles)" << std::endl;
    }

private:
    ContextBridge() = default;
};

} // namespace axmanager::nexus

// Global Bridge Instance for the Kernel
#define AX_BRIDGE axmanager::nexus::ContextBridge::get_instance()
