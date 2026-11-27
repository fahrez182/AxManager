#ifndef SHADOW_MEMORY_ENGINE_H
#define SHADOW_MEMORY_ENGINE_H

#include <cstdint>
#include <vector>

namespace AxManager {

class ShadowMemoryEngine {
private:
    bool isShadowActive;
    uintptr_t targetBaseAddress;

public:
    ShadowMemoryEngine();
    ~ShadowMemoryEngine();

    bool initializeShadow(uintptr_t baseAddress, size_t size);
    uint32_t readShadowMemory(uintptr_t address);
    bool writeShadowMemory(uintptr_t address, uint32_t value);
    void flushToMainMemory();

    inline bool isActive() const { return isShadowActive; }
};

} // namespace AxManager

#endif // SHADOW_MEMORY_ENGINE_H
