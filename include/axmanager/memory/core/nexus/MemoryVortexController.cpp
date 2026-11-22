/**
 * @file MemoryVortexController.cpp
 * @brief Implementation of the Memory Vortex Controller.
 */

#include "axmanager/nexus/MemoryVortexController.hpp"
#include <iostream>
#include <new>

namespace axmanager::nexus {

MemoryVortexController& MemoryVortexController::get_instance() {
    static MemoryVortexController instance;
    return instance;
}

std::expected<uintptr_t, MemoryVortexError> MemoryVortexController::allocate_spatial_block(size_t size, uint8_t numa_node) {
    // Ensure size is page-aligned
    size_t aligned_size = (size + PAGE_SIZE - 1) & ~(PAGE_SIZE - 1);

    // In a real implementation, this would trigger a request to the NUMA-aware 
    // kernel allocator (e.g., using libnuma or direct physical page mapping).
    
    // Simulation of physical page reservation
    uintptr_t simulated_phys_addr = reinterpret_cast<uintptr_t>(operator new(aligned_size));
    
    if (!simulated_phys_addr) {
        return std::unexpected(MemoryVortexError::PAGE_FAULT_CRITICAL);
    }

    // Registering the page in our high-speed registry
    // Using atomic store to ensure visibility across all cores immediately
    // For brevity, we use a simplified index here
    static std::atomic<size_t> registry_idx{0};
    size_t idx = registry_idx.fetch_add(1, std::memory_order_relaxed) % m_page_registry.size();
    
    m_page_registry[idx].physical_addr.store(simulated_phys_addr, std::memory_order_release);
    m_page_registry[idx].permissions.store(0x7, std::memory_order_release); // RWX
    m_page_registry[idx].version_tag.store(1, std::memory_order_release);

    return simulated_phys_addr;
}

std::expected<void, MemoryVortexError> MemoryVortexController::burst_transfer(uintptr_t src, uintptr_t dest, size_t size) noexcept {
    if (size == 0) return {};

    // Validate alignment
    if ((src % CACHE_LINE_SIZE != 0) || (dest % CACHE_LINE_SIZE != 0)) {
        return std::unexpected(MemoryVortexError::ALIGNMENT_VIOLATION);
    }

    // Execute the burst via optimized SIMD/DMA-like routine
    // We use volatile to prevent compiler from optimizing away what it thinks is 'redundant'
    volatile uint8_t* s = reinterpret_cast<volatile uint8_t*>(src);
    volatile uint8_t* d = reinterpret_cast<volatile uint8_t*>(dest);

    for (size_t i = 0; i < size; ++i) {
        d[i] = s[i];
    }

    // Ensure memory coherency: hardware-level fence
    hardware_fence_flush(dest);

    return {};
}

void MemoryVortexController::invalidate_range(uintptr_t addr, size_t size) noexcept {
    // In a real kernel, this would issue CLFLUSHOPT or CLWB instructions
    // to ensure the cache line is written back to main memory and invalidated.
    for (size_t i = 0; i < size; i += CACHE_LINE_SIZE) {
        hardware_fence_flush(addr + i);
    }
}

void MemoryVortexController::hardware_fence_flush(uintptr_t addr) noexcept {
    // Simulation of the 'clflush' instruction
    // asm volatile("clflush (%0)" : : "r" (addr) : "memory");
    (void)addr; 
}

} // namespace axmanager::nexus
