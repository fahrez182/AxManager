/**
 * @file IOPortEngine.hpp
 * @project AxManager Hyper-Engine
 * @brief Direct hardware port I/O primitives using inline x86 assembly.
 */

#ifndef AXMANAGER_NEXUS_ARCH_X86_ASM_IO_PORT_ENGINE_HPP
#define AXMANAGER_NEXUS_ARCH_X86_ASM_IO_PORT_ENGINE_HPP

#include <cstdint>

namespace axmanager::nexus::arch::x86_asm {

class IOPortEngine {
public:
    // Write a byte to a hardware I/O port
    static inline void outb(uint16_t port, uint8_t value) noexcept {
        asm volatile ("outb %0, %1" : : "a"(value), "Nd"(port));
    }

    // Read a byte from a hardware I/O port
    static inline uint8_t inb(uint16_t port) noexcept {
        uint8_t value;
        asm volatile ("inb %1, %0" : "=a"(value) : "Nd"(port));
        return value;
    }

    // Write a 32-bit double word to a hardware port
    static inline void outl(uint16_t port, uint32_t value) noexcept {
        asm volatile ("outl %0, %1" : : "a"(value), "Nd"(port));
    }

    // Read a 32-bit double word from a hardware port
    static inline uint32_t inl(uint16_t port) noexcept {
        uint32_t value;
        asm volatile ("inl %1, %0" : "=a"(value) : "Nd"(port));
        return value;
    }

    // CPU Pause instruction for spinlocks to prevent pipeline starvation
    static inline void cpu_pause() noexcept {
        asm volatile ("pause" ::: "memory");
    }
};

} // namespace axmanager::nexus::arch::x86_asm

#endif // AXMANAGER_NEXUS_ARCH_X86_ASM_IO_PORT_ENGINE_HPP
