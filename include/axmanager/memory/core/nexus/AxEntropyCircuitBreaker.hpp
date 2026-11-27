/**
 * @file AxEntropyCircuitBreaker.hpp
 * @project AxManager Enterprise Hyper-Engine (God-Tier Architecture)
 * @brief Hardware-level Shannon Entropy monitor and automated Circuit Breaker for anomaly isolation.
 * @details Calculates bit-level entropy in the 1GB Mega-Cache and CPU register traces to 
 *          detect unauthorized modifications, injections, or memory corruptions in real-time.
 */

#ifndef AXMANAGER_NEXUS_AX_ENTROPY_CIRCUIT_BREAKER_HPP
#define AXMANAGER_NEXUS_AX_ENTROPY_CIRCUIT_BREAKER_HPP

#include <cstdint>
#include <atomic>
#include <cmath>
#include "axmanager/nexus/arch/x86_asm_intrinsics.hpp"

namespace axmanager::nexus {

enum class CircuitState : uint8_t {
    CLOSED = 0,    // Normal operation, high-speed telemetry flowing
    HALF_OPEN = 1, // Probing phase after anomaly detection
    TRIPPED = 2    // Instant isolation and sandbox quarantine active
};

class alignas(64) AxEntropyCircuitBreaker {
public:
    static AxEntropyCircuitBreaker& get_instance() {
        static AxEntropyCircuitBreaker instance;
        return instance;
    }

    /**
     * @brief Computes Shannon Entropy over a memory segment to detect malicious 
     *        code injection or memory corruption patterns.
     * Formula: H(X) = - sum(p(x) * log2(p(x)))
     */
    [[nodiscard]] float calculate_memory_entropy(const uint8_t* data, size_t size) const noexcept {
        if (size == 0) return 0.0f;

        uint32_t frequency[256] = {0};
        for (size_t i = 0; i < size; ++i) {
            frequency[data[i]]++;
        }

        float entropy = 0.0f;
        float inv_size = 1.0f / static_cast<float>(size);

        for (int i = 0; i < 256; ++i) {
            if (frequency[i] > 0) {
                float p = static_cast<float>(frequency[i]) * inv_size;
                entropy -= p * std::log2(p);
            }
        }

        return entropy; // Max entropy for random data/encryption is ~8.0
    }

    /**
     * @brief Inspects a memory block and triggers the circuit breaker if entropy 
     *        exceeds safe execution thresholds.
     */
    bool inspect_and_evaluate(const uint8_t* region, size_t size, float threshold = 7.8f) noexcept {
        float current_entropy = calculate_memory_entropy(region, size);
        
        if (current_entropy > threshold) {
            // Anomaly detected: High entropy indicates packed payload, encryption, or corruption
            m_state.store(CircuitState::TRIPPED, std::memory_order_release);
            arch::memory_fence();
            return false; // Block execution
        }

        // If system was half-open and metrics are stable, close the circuit
        if (m_state.load(std::memory_order_relaxed) == CircuitState::HALF_OPEN) {
            m_state.store(CircuitState::CLOSED, std::memory_order_release);
        }

        return true; // Safe
    }

    void force_trip() noexcept {
        m_state.store(CircuitState::TRIPPED, std::memory_order_release);
        arch::memory_fence();
    }

    void reset() noexcept {
        m_state.store(CircuitState::HALF_OPEN, std::memory_order_release);
        arch::memory_fence();
    }

    [[nodiscard]] CircuitState get_state() const noexcept {
        return m_state.load(std::memory_order_acquire);
    }

private:
    AxEntropyCircuitBreaker() : m_state(CircuitState::CLOSED) {}
    ~AxEntropyCircuitBreaker() = default;

    std::atomic<CircuitState> m_state;

    AxEntropyCircuitBreaker(const AxEntropyCircuitBreaker&) = delete;
    AxEntropyCircuitBreaker& operator=(const AxEntropyCircuitBreaker&) = delete;
};

} // namespace axmanager::nexus

#endif // AXMANAGER_NEXUS_AX_ENTROPY_CIRCUIT_BREAKER_HPP
