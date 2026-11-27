/**
 * @file AxCoreNexus.hpp
 * @project AxManager Hyper-Kernel
 * @brief The central nexus that unifies all hardware-aware subsystems.
 */

#ifndef AXMANAGER_NEXUS_AX_CORE_NEXUS_HPP
#define AXMANAGER_NEXUS_AX_CORE_NEXUS_HPP

#include <memory>
#include <expected>
#include "axmanager/nexus/NeuralAcceleratorBundle.hpp"
#include "axmanager/nexus/QuantumSyncScheduler.hpp"
#include "axmanager/nexus/HardwareTelemetryBus.hpp"
#include "axmanager/nexus/VoidSyscallInterceptor.hpp"

namespace axmanager::nexus {

enum class NexusError : uint8_t {
    SUBSYSTEM_FAILURE,
    INITIALIZATION_TIMEOUT,
    HARDWARE_MISMATCH
};

/**
 * @class AxCoreNexus
 * @brief Singleton master controller for the AxManager engine.
 * Orchestrates the lifecycle of all specialized subsystems.
 */
class AxCoreNexus {
public:
    static AxCoreNexus& get_instance();

    AxCoreNexus(const AxCoreNexus&) = delete;
    void operator=(const AxCoreNexus&) = delete;

    /**
     * @brief Performs a full-scale, hardware-validated initialization of all subsystems.
     */
    std::expected<void, NexusError> boot_engine();

    /**
     * @brief Shuts down the engine and safely releases all hardware handles.
     */
    void shutdown_engine() noexcept;

    // Accessors for subsystems
    QuantumSyncScheduler& scheduler() noexcept { return QuantumSyncScheduler::get_instance(); }
    HardwareTelemetryBus& telemetry() noexcept { return HardwareTelemetryBus::get_instance(); }
    NeuralAcceleratorBundle& accelerator() noexcept { return NeuralAcceleratorBundle::get_instance(); }
    VoidSyscallInterceptor& interceptor() noexcept { return VoidSyscallInterceptor::get_instance(); }

private:
    AxCoreNexus() = default;
    bool m_is_booted{false};
};

} // namespace axmanager::nexus

#endif // AXMANAGER_NEXUS_AX_CORE_NEXUS_HPP
