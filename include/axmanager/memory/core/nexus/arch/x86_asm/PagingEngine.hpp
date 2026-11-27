/**
 * @file PagingEngine.hpp
 * @brief Controller for the 4-level x86_64 paging hierarchy.
 */

#ifndef AXMANAGER_NEXUS_ARCH_X86_ASM_PAGING_ENGINE_HPP
#define AXMANAGER_NEXUS_ARCH_X86_ASM_PAGING_ENGINE_HPP

#include <cstdint>
#include <array>

namespace axmanager::nexus::arch::x86_asm {

// Structure representing a single Page Table Entry (PTE)
struct alignas(8) PageTableEntry {
    uint64_t value;

    void set_address(uintptr_t phys_addr) {
        // Masking the address to 52 bits (for x86_64) and setting flags
        value = (phys_addr & 0x000FFFFFFFFFF000ULL) | (value & 0xFFF);
    }

    void set_present(bool p) { value |= (p ? 1ULL : 0ULL); }
    void set_writable(bool w) { value |= (w ? 2ULL : 0ULL); }
    void set_user(bool u) { value |= (u ? 4ULL : 0ULL); }
};

// The hierarchy of tables
struct alignas(4096) PageTable {
    std::array<PageTableEntry, 512> entries;
};

class PagingManager {
public:
    /**
     * @brief Initializes a new PML4 table (the root of the paging hierarchy).
     */
    static void initialize_root() {
        // In a real hyper-kernel, this would involve allocating a 
        // physically contiguous 4KB block from the AxMegaCache.
    }

    /**
     * @brief Switches the entire CPU memory context to a new PML4 table.
     */
    static inline void switch_context(uintptr_t pml4_phys_addr) noexcept {
        ax_paging_load_pml4(pml4_phys_addr);
    }

    /**
     * @brief Flushes the TLB to prevent stale translation attacks.
     */
    static inline void invalidate_cache() noexcept {
        ax_paging_flush_tlb();
    }
};

} // namespace axmanager::nexus::arch::x86_asm

#endif // AXMANAGER_NEXUS_ARCH_X86_ASM_PAGING_ENGINE_HPP
