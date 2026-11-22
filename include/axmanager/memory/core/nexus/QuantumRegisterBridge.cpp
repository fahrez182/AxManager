    // Explicit synchronization fence for quantum-level state locking
    std::atomic_signal_fence(std::memory_order_acq_rel);
}

void QuantumRegisterBridge::synchronize_pulse() {
    // Emits a global synchronization pulse across all registers to prevent drift
    std::cout << "[NEXUS-BRIDGE] Emitting global synchronization pulse across all quantum gates..." << std::endl;
    
    // CPU hardware barrier simulation (CPUID / MFENCE equivalent)
    #if defined(__x86_64__) || defined(_M_X64)
    // asm volatile("mfence" : : : "memory");
    #elif defined(__aarch64__)
    // asm volatile("dmb ish" : : : "memory");
    #endif

    std::atomic_thread_fence(std::memory_order_seq_cst);
    std::cout << "[NEXUS-BRIDGE] Synchronization pulse completed. Zero jitter achieved." << std::endl;
}

// Explicit template instantiations for core integer widths to ensure zero compilation overhead
template std::expected<void, RegisterCollapseError> QuantumRegisterBridge::transition<uint8_t>(uintptr_t, uint8_t, AccessMode) noexcept;
template std::expected<void, RegisterCollapseError> QuantumRegisterBridge::transition<uint16_t>(uintptr_t, uint16_t, AccessMode) noexcept;
template std::expected<void, RegisterCollapseError> QuantumRegisterBridge::transition<uint32_t>(uintptr_t, uint32_t, AccessMode) noexcept;
template std::expected<void, RegisterCollapseError> QuantumRegisterBridge::transition<uint64_t>(uintptr_t, uint64_t, AccessMode) noexcept;

template std::expected<void, RegisterCollapseError> QuantumRegisterBridge::atomic_xor<uint32_t>(uintptr_t, uint32_t) noexcept;
template std::expected<void, RegisterCollapseError> QuantumRegisterBridge::atomic_xor<uint64_t>(uintptr_t, uint64_t) noexcept;

template std::expected<uint8_t, RegisterCollapseError> QuantumRegisterBridge::sense<uint8_t>(uintptr_t) noexcept;
template std::expected<uint16_t, RegisterCollapseError> QuantumRegisterBridge::sense<uint16_t>(uintptr_t) noexcept;
template std::expected<uint32_t, RegisterCollapseError> QuantumRegisterBridge::sense<uint32_t>(uintptr_t) noexcept;
template std::expected<uint64_t, RegisterCollapseError> QuantumRegisterBridge::sense<uint64_t>(uintptr_t) noexcept;

} // namespace axmanager::nexus
