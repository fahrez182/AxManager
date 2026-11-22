/**
 * @file HardwareFaultHandler.cpp
 * @project AxManager Hyper-Kernel
 * @brief Low-level Hardware Fault & Interrupt Exception Routing.
 */

#include "axmanager/nexus/arch/x86_asm_intrinsics.hpp"
#include "axmanager/nexus/HardwareTelemetryBus.hpp"
#include <iostream>

namespace axmanager::nexus::arch {

/**
 * @brief Intercepts hardware-level faults and logs telemetry indicators.
 */
extern "C" void ax_handle_machine_check_exception(uint64_t error_code, uint64_t failing_address) noexcept {
    std::cout << "[HARDWARE-FATAL] MCE (Machine Check Exception) Triggered! Error Code: 0x" 
              << std::hex << error_code << " | Fault Address: 0x" << failing_address << std::dec << std::endl;

    // Emit telemetry immediately using the low-level bus
    auto& telemetry = HardwareTelemetryBus::get_instance();
    telemetry.emit_sample(0xFF, error_code, 0x80); // 0x80 stands for Fatal Hardware Flag

    // Enforce memory fence and halt risky pipelines
    memory_fence();
}

/**
 * @brief Low-level verification routine for core register state.
 */
void verify_core_registers() noexcept {
    uint64_t current_tsc = read_rdtsc();
    if (current_tsc == 0) {
        std::cout << "[HARDWARE-WARN] TSC failure detected on active core." << std::endl;
    }
}

} // namespace axmanager::nexus::arch
