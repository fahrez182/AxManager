/**
 * @file HardwareTelemetryBus.hpp
 * @project AxManager Hyper-Kernel
 * @brief Zero-Overhead Hardware Telemetry & Performance Counter (PMU) Bus.
 */

#ifndef AXMANAGER_NEXUS_HARDWARE_TELEMETRY_BUS_HPP
#define AXMANAGER_NEXUS_TELEMETRY_BUS_HPP

#include <atomic>
#include <cstdint>
#include <expected>
#include <span>
#include <array>

namespace axmanager::nexus {

alignas(64) constexpr size_t TELEMETRY_CACHE_LINE = 64;
constexpr size_t MAX_METRIC_CHANNELS = 256;

enum class TelemetryError : uint8_t {
    BUS_SATURATED,
    SENSOR_OFFLINE,
    INVALID_CHANNEL,
    OVERFLOW_FAULT
};

/**
 * @brief Represents a single hardware performance metric point.
 * Aligned to 64 bytes to prevent cache contention across cores.
 */
struct alignas(TELEMETRY_CACHE_LINE) TelemetryMetric {
    uint64_t channel_id;
    std::atomic<uint64_t> sample_value;
    uint64_t timestamp_tsc;
    uint32_t flags; // Status flags (e.g., thermal warning, voltage drop)
};

/**
 * @class HardwareTelemetryBus
 * @brief High-frequency telemetry collection unit interfacing directly with CPU PMUs.
 */
class HardwareTelemetryBus {
public:
    static HardwareTelemetryBus& get_instance();

    HardwareTelemetryBus(const HardwareTelemetryBus&) = delete;
    void operator=(const HardwareTelemetryBus&) = delete;

    /**
     * @brief Initializes the telemetry channels and PMU performance counters.
     */
    void initialize_bus();

    /**
     * @brief Atomically updates a telemetry channel with zero lock overhead.
     */
    std::expected<void, TelemetryError> emit_sample(uint64_t channel_id, uint64_t value, uint32_t flags) noexcept;

    /**
     * @brief Retrieves a snapshot of a specific telemetry channel.
     */
    std::expected<TelemetryMetric, TelemetryError> read_channel(uint64_t channel_id) const noexcept;

private:
    HardwareTelemetryBus() : m_active_channels(0) {}

    std::atomic<size_t> m_active_channels;
    std::array<TelemetryMetric, MAX_METRIC_CHANNELS> m_metric_registry;

    static inline uint64_t get_cpu_tsc() noexcept {
        uint32_t lo, hi;
        #if defined(__x86_64__) || defined(_M_X64)
        // asm volatile("rdtsc" : "=a"(lo), "=d"(hi));
        #else
        lo = 0; hi = 0;
        #endif
        return (static_cast<uint64_t>(hi) << 32) | lo;
    }
};

} // namespace axmanager::nexus

#endif // AXMANAGER_NEXUS_HARDWARE_TELEMETRY_BUS_HPP
