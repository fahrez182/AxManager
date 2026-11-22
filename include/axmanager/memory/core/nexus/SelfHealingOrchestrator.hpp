/**
 * @file SelfHealingOrchestrator.hpp
 * @brief Autonomous error detection and recovery system.
 */

#ifndef AXMANAGER_NEXUS_SELF_HEALING_ORCHESTRATOR_HPP
#define AXMANAGER_NEXUS_SELF_HEALING_ORCHESTRATOR_HPP

#include "axmanager/nexus/AxCoreNexus.hpp"
#include "axmanager/nexus/HardwareTelemetryBus.hpp"
#include <thread>
#include <atomic>

namespace axmanager::nexus {

class SelfHealingOrchestrator {
public:
    static SelfHealingOrchestrator& get_instance();

    void start_monitoring();
    void stop_monitoring();

private:
    SelfHealingOrchestrator() : m_active(false) {}

    void monitoring_loop();
    void handle_critical_fault(uint64_t channel_id);
    void recover_subsystem(uint8_t subsystem_id);

    std::atomic<bool> m_active;
    std::thread m_monitor_thread;
};

} // namespace axmanager::nexus

#endif // AXMANAGER_NEXUS_SELF_HEALING_ORCHESTRATOR_HPP
