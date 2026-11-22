#include "StealthMemoryManager.h"
#include <android/log.h>
#include <sys/mman.h>
#include <unistd.h>
#include <cstring>

#define LOG_TAG "AxManager_Stealth"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace AxManager {

StealthMemoryManager::StealthMemoryManager() : isStealthModeActive(true) {}

StealthMemoryManager::~StealthMemoryManager() {
    // تمیزکاری تمام نقشه‌های سایه‌ای هنگام بستن برنامه
    for (auto const& [addr, page] : shadowMap) {
        unmapShadow(addr);
    }
}

bool StealthMemoryManager::createShadowMapping(uintptr_t targetAddr, size_t size) {
    size_t pageSize = sysconf(_SC_PAGESIZE);
    // تراز کردن آدرس به ابتدای صفحه حافظه
    uintptr_t alignedAddr = targetAddr & ~(pageSize - 1);
    size_t alignedSize = ((targetAddr + size + pageSize - 1) & ~(pageSize - 1)) - alignedAddr;

    // تخصیص حافظه جدید برای نسخه سایه‌ای (Shadow Copy)
    void* shadowPtr = mmap(nullptr, alignedSize, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    
    if (shadowPtr == MAP_FAILED) {
        LOGE("Failed to allocate shadow memory at 0x%lx", alignedAddr);
        return false;
    }

    // کپی کردن داده‌های اصلی به حافظه سایه‌ای
    std::memcpy(shadowPtr, reinterpret_cast<void*>(alignedAddr), alignedSize);

    // ذخیره اطلاعات در نقشه سایه‌ای
    shadowMap[targetAddr] = {alignedAddr, reinterpret_cast<uintptr_t>(shadowPtr), alignedSize};

    LOGI("Shadow mapping created: Original(0x%lx) <-> Shadow(0x%lx)", alignedAddr, reinterpret_cast<uintptr_t>(shadowPtr));
    return true;
}

bool StealthMemoryManager::protectRegion(uintptr_t address, size_t size) {
    return createShadowMapping(address, size);
}

bool StealthMemoryManager::writeStealth(uintptr_t address, void* data, size_t size) {
    if (shadowMap.find(address) == shadowMap.end()) {
        LOGE("Attempted to write to non-shadowed memory: 0x%lx", address);
        return false;
    }

    ShadowPage& page = shadowMap[address];
    // نوشتن داده در نسخه سایه‌ای، نه در حافظه اصلی بازی!
    std::memcpy(reinterpret_cast<void*>(page.shadowAddress), data, size);
    
    LOGI("Stealth write successful at 0x%lx (Shadow: 0x%lx)", address, page.shadowAddress);
    return true;
}

bool StealthMemoryManager::readStealth(uintptr_t address, void* buffer, size_t size) {
    if (shadowMap.find(address) == shadowMap.end()) {
        return false;
    }
    ShadowPage& page = shadowMap[address];
    std::memcpy(buffer, reinterpret_cast<void*>(page.shadowAddress), size);
    return true;
}

bool StealthMemoryManager::unmapShadow(uintptr_t address) {
    if (shadowMap.find(address) == shadowMap.end()) return false;

    ShadowPage& page = shadowMap[address];
    munmap(reinterpret_cast<void*>(page.shadowAddress), page.pageSize);
    shadowMap.erase(address);
    
    LOGI("Shadow mapping unmapped for address: 0x%lx", address);
    return true;
}

bool StealthMemoryManager::restoreRegion(uintptr_t address) {
    return unmapShadow(address);
}

} // namespace AxManager
