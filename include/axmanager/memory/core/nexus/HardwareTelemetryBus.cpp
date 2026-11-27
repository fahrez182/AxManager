/**
 * @file HardwareTelemetryBus.cpp
 * @brief Implementation of the Hardware Telemetry Bus.
 */

#include "axmanager/nexus/HardwareTelemetryBus.hpp"
#include <iostream>

namespace axmanager::nexus {

HardwareTelemetryBus& HardwareTelemetryBus::get_instance() {
    static HardwareTelemetryBus instance;
    return instance;
}

void HardwareTelemetryBus::initialize_bus() {
    std::cout << "[NEXUS-TELEMETRY] Initializing PMU performance counters and telemetry channels..." << std::endl;
    
    for (size_t i = 0; i < MAX_METRIC_CHANNELS; ++i) {
        m_metric_registry[i].channel_id = i;
        m_metric_registry[i].sample_value.store(0, std::memory_order_relaxed);
        m_metric_registry[i].timestamp_tsc = 0;
        m_metric_registry[i].flags = 0;
    }

    m_active_channels.store(MAX_METRIC_CHANNELS, std::memory_order_release);
    std::cout << "[NEXUS-TELEMETRY] Bus online. Zero-overhead metric streaming active." << std::endl;
}

std::expected<void, TelemetryError> HardwareTelemetryBus::emit_sample(uint64_t channel_id, uint64_t value, uint32_t flags) noexcept {
    if (channel_id >= MAX_METRIC_CHANNELS) {
        return std::unexpected(TelemetryError::INVALID_CHANNEL);
    }

    auto& metric = m_metric_registry[channel_id];
    metric.sample_value.store(value, std::memory_order_release);
    metric.timestamp_tsc = get_cpu_tsc();
    metric.flags = flags;

    return {};
}

std::expected<TelemetryMetric, TelemetryError> HardwareTelemetryBus::read_channel(uint64_t channel_id) const noexcept {
    if (channel_id >= MAX_METRIC_CHANNELS) {
        return std::unexpected(TelemetryError::INVALID_CHANNEL);
    }

    const auto& metric = m_metric_registry[channel_id];
    TelemetryMetric copy;
    copy.channel_id = metric.channel_id;
    copy.sample_value.store(metric.sample_value.load(std::memory_order_acquire), std::memory_order_relaxed);
    copy.timestamp_tsc = metric.timestamp_tsc;
    copy.flags = metric.flags;

    return copy;
}

} // namespace axmanager::nexus
