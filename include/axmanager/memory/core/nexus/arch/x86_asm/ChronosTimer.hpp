/**
 * @file ChronosTimer.hpp
 * @brief High-precision timing and APIC management.
 */

#ifndef AXMANAGER_NEXUS_ARCH_X86_ASM_CHRONOS_TIMER_HPP
#define AXMANAGER_NEXUS_ARCH_X86_ASM_CHRONOS_TIMER_HPP

#include <cstdint>

namespace axmanager::nexus::arch::x86_asm {

class ChronosEngine {
public:
    /**
     * @brief Returns the current CPU cycle count (TSC).
     * Essential for measuring execution jitter and task latency.
     */
    static inline uint64_t get_cpu_cycles() noexcept {
        uint64_t cycles;
        asm volatile ("rdtsc" : "=a" (cycles) : : "rdx"); // Simplified inline for C++ logic
        return cycles;
    }

    /**
     * @brief High-resolution timestamp in nanoseconds.
     * Note: Requires knowledge of CPU frequency (GHz).
     */
    static uint64_t get_nanoseconds(double cpu_ghz) noexcept {
        return static_cast<uint64_t>(get_cpu_cycles() / cpu_ghz);
    }

    // APIC Initialization methods
    static void init_apic_timer(uintptr_t apic_base) noexcept {
        ax_apic_timer_init(apic_base);
    }

    static void set_timer_frequency(uintptr_t apic_base, uint32_t ticks) noexcept {
        ax_apic_set_periodic(apic_base, ticks);
    }

private:
    ChronosEngine() = default;
};

} // namespace axmanager::nexus::arch::x86_asm

#endif // AXMANAGER_NEXUS_ARCH_X86_ASM_CHRONOS_TIMER_HPP
