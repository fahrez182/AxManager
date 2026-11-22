/**
 * @file EntropyShieldEngine.cpp
 * @brief Implementation of the Entropy Shield and Anti-Tamper Engine.
 */

#include "axmanager/nexus/EntropyShieldEngine.hpp"
#include <iostream>
#include <chrono>
#include <algorithm>

namespace axmanager::nexus {

EntropyShieldEngine& EntropyShieldEngine::get_instance() {
    static EntropyShieldEngine instance;
    return instance;
}

std::expected<void, ShieldError> EntropyShieldEngine::protect_region(uintptr_t addr, size_t size) {
    if (addr == 0 || size == 0) {
        return std::unexpected(ShieldError::PROTECTION_DOMAIN_LEAK);
    }

    uint64_t initial_hash = calculate_hardware_hash(addr, size);
    uint64_t current_time = std::chrono::duration_cast<std::chrono::nanoseconds>(
        std::chrono::high_resolution_clock::now().time_since_epoch()
    ).count();

    m_protected_regions.push_back(IntegritySnapshot{
        .base_address = addr,
        .region_size = size,
        .entropy_hash = initial_hash,
        .timestamp = current_time
    });

    m_is_active.store(true, std::memory_order_release);
    std::cout << "[NEXUS-SHIELD] Protected memory region at 0x" << std::hex << addr 
              << " | Size: " << std::dec << size << " bytes | Initial Hash: 0x" << std::hex << initial_hash << std::endl;

    return {};
}

std::expected<void, ShieldError> EntropyShieldEngine::perform_integrity_sweep() noexcept {
    if (!m_is_active.load(std::memory_order_acquire)) {
        return {};
    }

    for (const auto& region : m_protected_regions) {
        uint64_t fresh_hash = calculate_hardware_hash(region.base_address, region.region_size);
        if (fresh_hash != region.entropy_hash) {
            std::cerr << "[CRITICAL-SECURITY] Integrity violation detected at address: 0x" 
                      << std::hex << region.base_address << "!" << std::endl;
            return std::unexpected(ShieldError::INTEGRITY_VIOLATION);
        }
    }

    return {};
}

bool EntropyShieldEngine::verify_integrity(uintptr_t addr, size_t size) noexcept {
    auto it = std::find_if(m_protected_regions.begin(), m_protected_regions.end(), [addr](const IntegritySnapshot& snap) {
        return snap.base_address == addr;
    });

    if (it == m_protected_regions.end()) {
        return false;
    }

    uint64_t fresh_hash = calculate_hardware_hash(addr, size);
    return (fresh_hash == it->entropy_hash);
}

uint64_t EntropyShieldEngine::calculate_hardware_hash(uintptr_t addr, size_t size) noexcept {
    // In a real bare-metal environment, this utilizes hardware instructions like 
    // CRC32 intrinsics (_mm_crc32_u64) or cryptographic accelerators.
    // Here we simulate a fast FNTV-style or Fowler-Noll-Vo hash over the memory block:
    
    const volatile uint8_t* ptr = reinterpret_cast<const volatile uint8_t*>(addr);
    uint64_t hash = 14695981039346656037ULL; // FNV offset basis
    
    for (size_t i = 0; i < size; ++i) {
        hash ^= static_cast<uint64_t>(ptr[i]);
        hash *= 1099511628211ULL; // FNV prime
    }

    return hash;
}

} // namespace axmanager::nexus
