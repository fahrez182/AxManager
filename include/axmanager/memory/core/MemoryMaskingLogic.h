#ifndef MEMORY_MASKING_LOGIC_H
#define MEMORY_MASKING_LOGIC_H

#include <cstdint>

namespace AxManager {

class MemoryMaskingLogic {
public:
    // Dynamic XOR Obfuscation key for memory values
    static constexpr uint32_t XOR_MASK_KEY = 0x5A3C9E1F;

    inline static uint32_t maskValue(uint32_t val) {
        return val ^ XOR_MASK_KEY;
    }

    inline static uint32_t unmaskValue(uint32_t maskedVal) {
        return maskedVal ^ XOR_MASK_KEY;
    }

    // Pointer redirection masking for anti-cheat bypass
    template <typename T>
    static T* getSpoofedPointer(T* realPtr, uintptr_t offset) {
        if (!realPtr) return nullptr;
        return reinterpret_cast<T*>(reinterpret_cast<uintptr_t>(realPtr) + offset);
    }
};

} // namespace AxManager

#endif // MEMORY_MASKING_LOGIC_H
