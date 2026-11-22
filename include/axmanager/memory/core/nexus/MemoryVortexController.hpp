/**
 * @file MemoryVortexController.hpp
 * @project AxManager Hyper-Kernel
 * @brief NUMA-Aware, Lock-Free Spatial Memory Controller.
 */

#ifndef AXMANAGER_NEXUS_MEMORY_VORTEX_CONTROLLER_HPP
#define AXMANAGER_NEXUS_MEMORY_VORTEX_CONTROLLER_HPP

#include <atomic>
#include <cstdint>
#include <expected>
#include <span>
#include <array>

namespace axmanager::nexus {

alignas(64) constexpr size_t PAGE_SIZE = 4096;
alignas(64) constexpr size_t CACHE_LINE_SIZE = 64;

enum class MemoryVortexError : uint8_t {
    NUMA_NODE_UNAVAILABLE,
    PAGE_FAULT_CRITICAL,
    ALIGNMENT_VIOLATION,
    VORTEX_OVERFLOW
};

struct alignas(CACHE_LINE_SIZE) MemoryCell {
    std::atomic<uint64_t> physical_addr;
    std::atomic<uint32_t> permissions; // RWX bitmask
    std::atomic<uint32_t> version_tag; // For ABA protection in lock-free ops
};

/**
 * @class MemoryVortexController
 * @brief Orchestrates memory movement across NUMA nodes with zero-copy semantics.
 */
class MemoryVortexController {
public:
    static MemoryVortexController& get_instance();

    MemoryVortexController(const MemoryVortexController&) = delete;
    void operator=(const MemoryVortexController&) = delete;

    /**
     * @brief Allocates a spatially contiguous block of memory aligned to NUMA nodes.
     */
    std::expected<uintptr_t, MemoryVortexError> allocate_spatial_block(size_t size, uint8_t numa_node);

    /**
     * @brief Transfers data between memory zones using hardware-level burst mode.
     */
    std::expected<void, MemoryVortexError> burst_transfer(uintptr_t src, uintptr_t dest, size_t size) noexcept;

    /**
     * @brief Invalidates cache lines for a specific range to ensure coherency.
     */
    void invalidate_range(uintptr_t addr, size_t size) noexcept;

private:
    MemoryVortexController() : m_active_nodes(0) {}

    alignas(CACHE_LINE_SIZE) std::atomic<uint32_t> m_active_nodes;
    
    // Internal mapping of physical pages to vortex-managed cells
    // In a real hyper-kernel, this would interface with the IOMMU
    std::array<MemoryCell, 1048576> m_page_registry; 

    void hardware_fence_flush(uintptr_t addr) noexcept;
};

} // namespace axmanager::nexus

#endif
