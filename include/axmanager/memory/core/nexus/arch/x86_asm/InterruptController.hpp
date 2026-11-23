/**
 * @file InterruptController.hpp
 * @brief High-level management of the IDT and CPU Interrupt state.
 */

#ifndef AXMANAGER_NEXUS_ARCH_X86_ASM_INTERRUPT_CONTROLLER_HPP
#define AXMANAGER_NEXUS_ARCH_X86_ASM_INTERRUPT_CONTROLLER_HPP

#include <cstdint>
#include "axmanager/nexus/arch/x86_asm/HyperContextSwitcher.hpp"

namespace axmanager::nexus::arch::x86_asm {

class InterruptController {
public:
    // We use a static table for the IDT to ensure it stays in a fixed, known memory location
    static constexpr size_t IDT_SIZE = 256 * 16;
    alignas(4096) static inline uint8_t idt_table[IDT_SIZE];

    /**
     * @brief Initializes the IDT and installs it into the CPU.
     */
    static void initialize() noexcept {
        ax_idt_install(reinterpret_cast<uintptr_t>(idt_table));
    }

    /**
     * @brief Registers a new handler for a specific interrupt vector.
     * @param vector The interrupt number (e.g., 32 for Timer).
     * @param handler The function pointer to the handler.
     */
    static void register_handler(uint8_t vector, void (*handler)()) noexcept {
        ax_idt_register_handler(
            reinterpret_cast<uintptr_t>(idt_table) + vector, 
            reinterpret_cast<uintptr_t>(handler)
        );
    }

    static inline void disable_interrupts() noexcept { ax_cpu_disable_interrupts(); }
    static inline void enable_interrupts() noexcept { ax_cpu_enable_interrupts(); }
};

} // namespace axmanager::nexus::arch::x86_asm

#endif // AXMANAGER_NEXUS_ARCH_X86_ASM_INTERRUPT_CONTROLLER_HPP
