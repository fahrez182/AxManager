/**
 * @file x86_asm_intrinsics.hpp
 * @project AxManager Hyper-Kernel
 * @brief Low-level x86/x86_64 Assembly Intrinsics and Hardware Control Directives.
 */

#ifndef AXMANAGER_NEXUS_ARCH_X86_ASM_INTRINSICS_HPP
#define AXMANAGER_NEXUS_ARCH_X86_ASM_INTRINSICS_HPP

#include <cstdint>
#include <immintrin.h>

namespace axmanager::nexus::arch {

/**
 * @brief Reads the Time-Stamp Counter (TSC) with zero overhead.
 */
[[nodiscard]] inline uint64_t read_rdtsc() noexcept {
    unsigned int lo, hi;
#if defined(__x86_64__) || defined(_M_X64)
    __asm__ __volatile__ ("rdtsc" : "=a" (lo), "=d" (hi));
#else
    lo = 0; hi = 0;
#endif
    return (static_cast<uint64_t>(hi) << 32) | lo;
}

/**
 * @brief Issues a Memory Fence (MFENCE) to enforce strict store/load ordering.
 */
inline void memory_fence() noexcept {
#if defined(__x86_64__) || defined(_M_X64)
    _mm_mfence();
#else
    std::atomic_thread_fence(std::memory_order_seq_cst);
#endif
}

/**
 * @brief Flushes a specific cache line from the CPU caches (CLFLUSH).
 */
inline void flush_cache_line(const volatile void* addr) noexcept {
#if defined(__x86_64__) || defined(_M_X64)
    _mm_clflush(const_cast<void*>(static_cast<const volatile void*>(addr)));
#else
    (void)addr;
#endif
}

/**
 * @brief Reads Model Specific Registers (MSR) safely.
 */
[[nodiscard]] inline uint64_t read_msr(uint32_t msr_id) noexcept {
    uint32_t lo, hi;
#if defined(__x86_64__) || defined(_M_X64)
    __asm__ __volatile__ ("rdmsr" : "=a" (lo), "=d" (hi) : "c" (msr_id));
#else
    lo = 0; hi = 0;
#endif
    return (static_cast<uint64_t>(hi) << 32) | lo;
}

/**
 * @brief Writes to Model Specific Registers (MSR).
 */
inline void write_msr(uint32_t msr_id, uint64_t value) noexcept {
    uint32_t lo = static_cast<uint32_t>(value & 0xFFFFFFFF);
    uint32_t hi = static_cast<uint32_t>(value >> 32);
#if defined(__x86_64__) || defined(_M_X64)
    __asm__ __volatile__ ("wrmsr" : : "c" (msr_id), "a" (lo), "d" (hi));
#else
    (void)msr_id; (void)value;
#endif
}

} // namespace axmanager::nexus::arch

#endif // AXMANAGER_NEXUS_ARCH_X86_ASM_INTRINSICS_HPP
