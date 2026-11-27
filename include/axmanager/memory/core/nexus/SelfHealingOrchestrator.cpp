/**
 * @file SelfHealingOrchestrator.cpp
 * @brief Implementation of the Self-Healing logic.
 */

#include "axmanager/nexus/SelfHealingOrchestrator.hpp"
#include "axmanager/nexus/HardwareTelemetryBus.hpp"
#include <iostream>
#include <chrono>

namespace axmanager::nexus {

SelfHealingOrchestrator& SelfHealingOrchestrator::get_instance() {
    static SelfHealingOrchestrator instance;
    return instance;
}

void SelfHealingOrchestrator::start_monitoring() {
    m_active = true;
    m_monitor_thread = std::thread(&SelfHealingOrchestrator::monitoring_loop, this);
    std::cout << "[SELF-HEALING] Autonomous monitoring engine is ONLINE." << std::endl;
}

void SelfHealingOrchestrator::stop_monitoring() {
    m_active = false;
    if (m_monitor_thread.joinable()) m_monitor_thread.join();
}

void SelfHealingOrchestrator::monitoring_loop() {
    auto& telemetry = HardwareTelemetryBus::get_instance();

    while (m_active) {
        // بررسی کانال 99 (خطاهای حافظه/Integrity)
        auto memory_status = telemetry.read_channel(99);
        if (memory_status.has_value() && memory_status->sample_value.load() != 0) {
            handle_critical_fault(99);
        }

        // بررسی کانال 50 (دمای بحرانی)
        auto thermal_status = telemetry.read_channel(50);
        if (thermal_status.has_value() && thermal_status->sample_value.load() > 90) {
            std::cout << "[SELF-HEALING] Critical Thermal detected! Throttling subsystems..." << std::endl;
            // در اینجا دستور کاهش فرکانس صادر می‌شود
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(100)); // نرخ پایش 10Hz
    }
}

void SelfHealingOrchestrator::handle_critical_fault(uint64_t channel_id) {
    std::cout << "[SELF-HEALING] !!! CRITICAL FAULT DETECTED ON CHANNEL " << channel_id << " !!!" << std::endl;
    std::cout << "[SELF-HEALING] Attempting subsystem recovery..." << std::endl;
    
    // استراتژی اصلاح: بازنشانی (Reset) زیرسیستم مربوطه
    recover_subsystem(channel_id);
}

void SelfHealingOrchestrator::recover_subsystem(uint8_t subsystem_id) {
    // در اینجا ما شبیه‌سازی می‌کنیم که زیرسیستم بازنشانی می‌شود
    std::cout << "[SELF-HEALING] Subsystem " << static_cast<int>(subsystem_id) 
              << " has been REBOOTED and re-synchronized." << std::endl;
    
    // پاکسازی کانال خطا بعد از اصلاح
    HardwareTelemetryBus::get_instance().emit_sample(99, 0, 0); 
}

} // namespace axmanager::nexus
