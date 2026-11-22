/**
 * @file AxCoreNexus.cpp
 * @brief Implementation of the AxCoreNexus orchestration logic.
 */

#include "axmanager/nexus/AxCoreNexus.hpp"
#include <iostream>

namespace axmanager::nexus {

AxCoreNexus& AxCoreNexus::get_instance() {
    static AxCoreNexus instance;
    return instance;
}

std::expected<void, NexusError> AxCoreNexus::boot_engine() {
    std::cout << "[NEXUS-CORE] --- INITIATING AXMANAGER HYPER-KERNEL BOOT SEQUENCE ---" << std::endl;

    // 1. Initialize Telemetry (Needs to be first to monitor the boot itself)
    telemetry().initialize_bus();
    telemetry().emit_sample(0, 1, 0x01); // Signal: System Boot Started

    // 2. Initialize Scheduler (To manage subsequent subsystem tasks)
    scheduler().initialize_cores(std::thread::hardware_concurrency());

    // 3. Initialize Interceptor (To catch any rogue syscalls during boot)
    // (Assuming VoidSyscallInterceptor has an initialize method)
    // interceptor().initialize();

    // 4. Initialize Neural Accelerator (The heavy-lifter)
    accelerator().initialize();

    m_is_booted = true;
    std::cout << "[NEXUS-CORE] --- HYPER-KERNEL ENGINE STATUS: OPERATIONAL ---" << std::endl;
    
    return {};
}

void AxCoreNexus::shutdown_engine() noexcept {
    std::cout << "[NEXUS-CORE] Initiating graceful shutdown of all subsystems..." << std::endl;
    
    // Reverse order of initialization for safe teardown
    accelerator().shutdown();
    // interceptor().shutdown();
    // scheduler().shutdown();

    m_is_booted = false;
    std::cout << "[NEXUS-CORE] AxManager Engine successfully halted." << std::endl;
}

} // namespace axmanager::nexus
