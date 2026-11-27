#ifndef INLINE_HOOK_ENGINE_H
#define INLINE_HOOK_ENGINE_H

#include <cstdint>
#include <vector>

namespace AxManager {

class InlineHookEngine {
private:
    uintptr_t targetFunctionAddress;
    std::vector<uint8_t> originalBytes;
    bool isHooked;

    bool changeMemoryProtection(uintptr_t address, size_t size, int protection);

public:
    InlineHookEngine();
    ~InlineHookEngine();

    // نصب هوک بر روی تابع هدف
    bool installHook(uintptr_t targetFunc, uintptr_t detourFunc);
    
    // برداشتن هوک و بازگرداندن کدهای اصلی
    bool removeHook();

    inline bool isActive() const { return isHooked; }
};

} // namespace AxManager

#endif // INLINE_HOOK_ENGINE_H
