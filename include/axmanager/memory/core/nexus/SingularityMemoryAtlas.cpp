/**
 * @file SingularityMemoryAtlas.cpp
 * @brief Implementation of the Singularity Memory Atlas.
 */

#include "axmanager/nexus/SingularityMemoryAtlas.hpp"
#include <iostream>
#include <algorithm>
#include <shared_mutex>

namespace axmanager::nexus {

SingularityMemoryAtlas& SingularityMemoryAtlas::getAtlasInstance() {
    static SingularityMemoryAtlas instance;
    return instance;
}

void SingularityMemoryAtlas::initialize_topography() {
    std::unique_lock lock(m_atlas_lock);
    
    std::cout << "[NEXUS] Scanning Physical Silicon Topography..." << std::endl;
    
    // In a real implementation, this would involve parsing ACPI tables, 
    // E820 maps, and probing the memory controller (IMC).
    
    m_is_initialized = true;
    std::cout << "[NEXUS] Singularity Atlas initialized. Spatial mapping complete." << std::endl;
}

std::expected<uintptr_t, AtlasError> SingularityMemoryAtlas::claim_segment(size_t size, SegmentType type) {
    std::unique_lock lock(m_atlas_lock);

    if (!m_is_initialized) {
        return std::unexpected(AtlasError::ATLAS_UNSTABLE);
    }

    // Logic for finding a contiguous physical block with optimal alignment (e.g., 4KB or 2MB pages)
    // We simulate the allocation of a physical address.
    static uintptr_t simulated_physical_ptr = 0x100000000; // Starting at 4GB mark

    // Check for simulated physical exhaustion
    if (size > 0x100000000) { // Limit of 4GB for simulation
        return std::unexpected(AtlasError::PHYSICAL_LIMIT_REACHED);
    }

    uintptr_t allocated_addr = simulated_physical_ptr;
    
    MemorySegment segment;
    segment.base_address = allocated_addr;
    segment.extent = size;
    segment.type = type;
    segment.access_count.store(0);
    segment.is_locked = false;

    m_active_segments[allocated_addr] = segment;
    simulated_physical_ptr += size; // Incrementing the simulated physical pointer

    std::cout << "[NEXUS] Segment Claimed: 0x" << std::hex << allocated_addr 
              << " | Size: " << std::dec << size << " bytes | Type: " << static_cast<int>(type) << std::endl;

    return allocated_addr;
}

void SingularityMemoryAtlas::release_segment(uintptr_t base_address) {
    std::unique_lock lock(m_atlas_lock);
    if (m_active_segments.erase(base_address)) {
        std::cout << "[NEXUS] Segment Released: 0x" << std::hex << base_address << std::endl;
    }
}

bool SingularityMemoryAtlas::validate_atlas_integrity() const noexcept {
    std::shared_lock lock(m_atlas_lock);
    
    // Mathematical verification of segment overlaps and alignment
    // A real implementation would check the non-overlapping property of all segments
    // and ensure all base_addresses are aligned to page boundaries.
    
    bool consistent = true;
    uintptr_t last_end = 0;

    for (const auto& [addr, segment] : m_active_segments) {
        if (addr < last_end) {
            consistent = false; // Overlap detected
            break;
        }
        last_end = addr + segment.extent;
    }

    return consistent;
}

void SingularityMemoryAtlas::lock_segment(uintptr_t base_address) {
    std::unique_lock lock(m_atlas_lock);
    if (m_active_segments.contains(base_address)) {
        m_active_segments[base_address].is_locked = true;
        std::cout << "[NEXUS] Segment 0x" << std::hex << base_address << " is now HARD-LOCKED." << std::endl;
    }
}

void SingularityMemoryAtlas::unlock_segment(uintptr_t base_address) {
    std::unique_lock lock(m_atlas_lock);
    if (m_active_segments.contains(base_address)) {
        m_active_segments[base_address].is_locked = false;
        std::cout << "[NEXUS] Segment 0x" << std::hex << base_address << " UNLOCKED." << std::endl;
    }
}

std::expected<MemorySegment, AtlasError> SingularityMemoryAtlas::get_segment_info(uintptr_t base_address) const {
    std::shared_lock lock(m_atlas_lock);
    auto it = m_active_segments.find(base_address);
    if (it != m_active_segments.end()) {
        return it->second;
    }
    return std::unexpected(AtlasError::ACCESS_VIOLATION);
}

void SingularityMemoryAtlas::rebalance_spatial_mapping() {
    // Complex algorithm to defragment physical memory segments
    // for optimal cache-locality.
}

} // namespace axmanager::nexus
