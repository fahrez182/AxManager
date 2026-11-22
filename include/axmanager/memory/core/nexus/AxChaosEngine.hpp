/**
 * @file AxChaosEngine.hpp
 * @project AxManager Hyper-Kernel
 * @brief Chaos Engineering Module for stress-testing the Nexus integrity.
 */

#ifndef AXMANAGER_NEXUS_AX_CHAOS_ENGINE_HPP
#define AXMANAGER_NEXUS_AX_CHAOS_ENGINE_HPP

#include <random>
#include <cstdint>
#include <expected>

namespace axmanager::nexus {

enum class ChaosType : uint8_t {
    MEMORY_CORRUPTION,    // شبیه‌سازی نشت یا تغییر ناخواسته در حافظه
    INTERRUPT_STORM,      // شبیه‌سازی حملات DoS در سطح وقفه
    THERMAL_THROTTLING,   // شبیه‌سازی کاهش سرعت به دلیل گرما
    CLOCK_DRIFT,          // شبیه‌سازی نوسان در کلاک پردازنده
    VOLTAGE_FLUCTUATION   // شبیه‌سازی نوسان ولتاژ
};

struct ChaosEvent {
    ChaosType type;
    float intensity;      // شدت از 0.0 تا 1.0
    uint64_t target_id;   // شناسه هدف (هسته، کانال حافظه یا ترد)
};

/**
 * @class AxChaosEngine
 * @brief A tool to inject artificial faults into the running system to test resilience.
 */
class AxChaosEngine {
public:
    static AxChaosEngine& get_instance();

    /**
     * @brief Injects a randomized or specific fault into the system.
     */
    void inject_fault(const ChaosEvent& event) noexcept;

    /**
     * @brief Runs a continuous stress test session.
     */
    void run_stress_test(uint32_t duration_seconds) noexcept;

private:
    AxChaosEngine() : m_rng(std::random_device{}()) {}

    std::mt19937 m_rng;
    
    void simulate_memory_corruption(uint64_t target) noexcept;
    void simulate_interrupt_storm(uint8_t core_id) noexcept;
    void simulate_thermal_event(float intensity) noexcept;
};

} // namespace axmanager::nexus

#endif // AXMANAGER_NEXUS_AX_CHAOS_ENGINE_HPP
