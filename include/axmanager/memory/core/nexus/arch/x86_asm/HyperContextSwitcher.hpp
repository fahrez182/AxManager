/**
 * @file HyperContextSwitcher.hpp
 * @brief C++ Wrapper for the Assembly-based context switching.
 */

#ifndef AXMANAGER_NEXUS_ARCH_X86_ASM_HYPER_CONTEXT_SWITCHER_HPP
#define AXMANAGER_NEXUS_ARCH_X86_ASM_HYPER_CONTEXT_SWITCHER_HPP

#include <cstdint>

namespace axmanager::nexus::arch::x86_asm {

// The structure must match the memory layout used in the .asm file exactly.
struct alignas(128) AxTaskContext {
    uint64_t regs[16]; // Holds all GPRs + RSP + RIP
};

extern "C" {
    /**
     * @brief Saves all current registers into the provided context struct.
     * @param context Pointer to the destination structure.
     */
    void ax_hyper_context_save(AxTaskContext* context) noexcept;

    /**
     * @brief Restores all registers from the provided context struct.
     * @param context Pointer to the source structure.
     * @note This function does not return; it jumps directly to the new task's RIP.
     */
    void ax_hyper_context_restore(const AxTaskContext* context) noexcept;
}

} // namespace axmanager::nexus::arch::x86_asm

#endif // AXMANAGER_NEXUS_ARCH_X86_ASM_HYPER_CONTEXT_SWITCHER_HPP
