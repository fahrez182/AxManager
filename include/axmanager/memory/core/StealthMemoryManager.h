#ifndef STEALTH_MEMORY_MANAGER_H
#define STEALTH_MEMORY_MANAGER_H

#include <cstdint>
#include <string>
#include <map>

namespace AxManager {

struct ShadowPage {
    uintptr_t originalAddress;
    uintptr_t shadowAddress;
    size_t pageSize;
};

class StealthMemoryManager {
private:
    std::map<uintptr_t, ShadowPage> shadowMap;
    bool isStealthModeActive;

    bool createShadowMapping(uintptr_t targetAddr, size_t size);
    bool unmapShadow(uintptr_t targetAddr);

public:
    StealthMemoryManager();
    ~StealthMemoryManager();

    // فعال‌سازی حالت پنهان‌کاری برای یک محدوده حافظه
    bool protectRegion(uintptr_t address, size_t size);

    // بازگرداندن حافظه به حالت عادی
    bool restoreRegion(uintptr_t address);

    // نوشتن در حافظه سایه‌ای (بدون اینکه آنتی‌چیت متوجه شود)
    bool writeStealth(uintptr_t address, void* data, size_t size);

    // خواندن از حافظه سایه‌ای
    bool readStealth(uintptr_t address, void* buffer, size_t size);
};

} // namespace AxManager

#endif // STEALTH_MEMORY_MANAGER_H
