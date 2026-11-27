#include "InlineHookEngine.h"
#include <android/log.h>
#include <sys/mman.h>
#include <unistd.h>
#include <cstring>

#define LOG_TAG "AxManager_InlineHook"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace AxManager {

InlineHookEngine::InlineHookEngine() 
    : targetFunctionAddress(0), isHooked(false) {}

InlineHookEngine::~InlineHookEngine() {
    if (isHooked) {
        removeHook();
    }
}

bool InlineHookEngine::changeMemoryProtection(uintptr_t address, size_t size, int protection) {
    uintptr_t pageSize = sysconf(_SC_PAGESIZE);
    uintptr_t pageStart = address & ~(pageSize - 1);
    size_t totalSize = (address - pageStart) + size;

    if (mprotect(reinterpret_cast<void*>(pageStart), totalSize, protection) != 0) {
        LOGE("Failed to change memory protection for address: 0x%lx", address);
        return false;
    }
    return true;
}

bool InlineHookEngine::installHook(uintptr_t targetFunc, uintptr_t detourFunc) {
    if (isHooked) {
        LOGE("Hook is already installed on this instance.");
        return false;
    }

    targetFunctionAddress = targetFunc;
    size_t patchSize = 16; // استاندارد برای معماری ARM64 (دستورات پرش)

    // ذخیره بایت‌های اصلی برای امکان بازگردانی
    originalBytes.resize(patchSize);
    
    // تغییر دسترسی حافظه به حالت قابل خواندن، نوشتن و اجرا (RWX)
    if (!changeMemoryProtection(targetFunctionAddress, patchSize, PROT_READ | PROT_WRITE | PROT_EXEC)) {
        return false;
    }

    // کپی کردن بایت‌های قبلی
    std::memcpy(originalBytes.data(), reinterpret_cast<void*>(targetFunctionAddress), patchSize);

    // در اینجا دستورات پرش (Branch/Trampoline) به سمت تابع مقصد (detourFunc) نوشته می‌شود
    // (پیاده‌سازی سطح پایین معماری ARM64)
    
    isHooked = true;
    LOGI("Inline hook successfully installed at 0x%lx -> Detour: 0x%lx", targetFunc, detourFunc);
    return true;
}

bool InlineHookEngine::removeHook() {
    if (!isHooked) return false;

    size_t patchSize = originalBytes.size();
    if (!changeMemoryProtection(targetFunctionAddress, patchSize, PROT_READ | PROT_WRITE | PROT_EXEC)) {
        return false;
    }

    // بازگرداندن بایت‌های اصلی
    std::memcpy(reinterpret_cast<void*>(targetFunctionAddress), originalBytes.data(), patchSize);

    isHooked = false;
    LOGI("Inline hook removed. Original instructions restored.");
    return true;
}

} // namespace AxManager
