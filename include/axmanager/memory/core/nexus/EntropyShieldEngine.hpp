/**
 * @file EntropyShieldEngine.hpp
 * @project AxManager Hyper-Kernel
 * @brief Hardware-Integrated Integrity & Anti-Tamper Engine.
 * 
 * This engine provides continuous, non-intrusive monitoring of critical 
 * kernel memory regions using cryptographic entropy signatures.
 */

#ifndef AXMANAGER_NEXUS_ENTROPY_SHIELD_ENGINE_HPP
#define AXMANAGER_NEXUS_ENTROPY_SHIELD_ENGINE_HPP

#include <cstdint>
#include <expected>
#include <span>
#include <vector>
#include <atomic>

namespace axmanager::nexus {

enum class ShieldError : uint8_t {
    INTEGRITY_VIOLATION,    // Detected unauthorized memory modification
    SIGNATURE_MISMATCH,     // Hash/Signature does not match expected entropy
    PROTECTION_DOMAIN_LEAK, // Attempt to access restricted kernel space
    HARDWARE_FAULT          // Failure in the hardware security module
};

/**
 * @brief A cryptographic snapshot of a memory region.
 */
struct alignas(64) IntegritySnapshot {
    uintptr_t base_address;
    size_t region_size;
    uint64_t entropy_hash; // High-speed hardware hash (e.g., CRC64 or AES-NI based)
    uint64_t timestamp;
};

/**
 * @class EntropyShieldEngine
 * @brief The ultimate guardian of kernel-space sanctity.
 */
class EntropyShieldEngine {
public:
    static EntropyShieldEngine& get_instance();

    EntropyShieldEngine(const EntropyShieldEngine&) = delete;
    void operator=(const EntropyShieldEngine&) = delete;

    /**
     * @brief Registers a memory region for continuous integrity monitoring.
     */
    std::expected<void, ShieldError> protect_region(uintptr_t addr, size_t size);

    /**
     * @brief Performs a high-speed integrity scan across all protected regions.
     * This is a heavy-duty operation that must be called via a high-priority interrupt.
     */
    std::expected<void, ShieldError> perform_integrity_sweep() noexcept;

    /**
     * @brief Manually verifies a specific memory block.
     */
    bool verify_integrity(uintptr_t addr, size_t size) noexcept;

private:
    EntropyShieldEngine() : m_is_active(false) {}

    std::atomic<bool> m_is_active;
    std::vector<IntegritySnapshot> m_protected_regions;

    uint64_t calculate_hardware_hash(uintptr_t addr, size_t size) noexcept;
};

} // namespace axmanager::nexus

#endif // AXMANAGER_NEXUS_ENTROPY_SHIELD_ENGINE_HPP
