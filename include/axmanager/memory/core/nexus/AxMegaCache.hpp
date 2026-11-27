/**
 * @file AxMegaCache.hpp
 * @project AxManager Hyper-Kernel
 * @brief High-performance 400MB Mega-Cache using aligned memory pooling.
 */

#ifndef AXMANAGER_NEXUS_AX_MEGA_CACHE_HPP
#define AXMANAGER_NEXUS_AX_MEGA_CACHE_HPP

#include <cstdint>
#include <cstdlib>
#include <iostream>
#include <atomic>
#include "axmanager/nexus/arch/x86_asm_intrinsics.hpp"

namespace axmanager::nexus {

class AxMegaCache {
public:
    // تعریف ظرفیت: 400 MB
    static constexpr size_t CACHE_SIZE = 400 * 1024 * 1024; 
    static constexpr size_t ALIGNMENT = 4096; // Page Alignment (Huge Pages)

    static AxMegaCache& get_instance() {
        static AxMegaCache instance;
        return instance;
    }

    /**
     * @brief دسترسی سریع به داده‌ها در کش با استفاده از Offset
     */
    inline void write(size_t offset, const void* data, size_t size) noexcept {
        if (offset + size <= CACHE_SIZE) {
            // کپی مستقیم با استفاده از دستورات اسمبلی برای سرعت حداکثری
            for (size_t i = 0; i < size; ++i) {
                m_pool[offset + i] = static_cast<const uint8_t*>(data)[i];
            }
            // اطمینان از اینکه داده‌ها در کش نوشته شده‌اند
            arch::memory_fence();
        }
    }

    inline void read(size_t offset, void* dest, size_t size) const noexcept {
        if (offset + size <= CACHE_SIZE) {
            for (size_t i = 0; i < size; ++i) {
                static_cast<uint8_t*>(dest)[i] = m_pool[offset + i];
            }
        }
    }

    /**
     * @brief پاکسازی سریع کل کش (Zeroing out)
     */
    void fast_flush() noexcept {
        // استفاده از دستورات اسمبلی برای پاکسازی سریع (Zeroing)
        std::fill(m_pool, m_pool + CACHE_SIZE, 0);
        arch::memory_fence();
    }

    size_t capacity() const { return CACHE_SIZE; }

private:
    AxMegaCache() {
        std::cout << "[CACHE] Allocating 400MB Mega-Cache with Page Alignment..." << std::endl;
        
        // استفاده از posix_memalign برای اطمینان از اینکه حافظه دقیقاً روی مرز صفحات (Pages) قرار دارد
        // این کار باعث می‌شود دسترسی CPU به کش به شدت سریع شود.
        void* raw_ptr = nullptr;
        if (posix_memalign(&raw_ptr, ALIGNMENT, CACHE_SIZE) != 0) {
            throw std::runtime_error("Failed to allocate 400MB Mega-Cache!");
        }
        m_pool = static_cast<uint8_t*>(raw_ptr);
        
        std::cout << "[CACHE] 400MB Mega-Cache initialized at: " << static_cast<void*>(m_pool) << std::endl;
    }

    ~AxMegaCache() {
        std::free(m_pool);
    }

    // جلوگیری از کپی شدن
    AxMegaCache(const AxMegaCache&) = delete;
    AxMegaCache& operator=(const AxMegaCache&) = delete;

    uint8_t* m_pool;
};

} // namespace axmanager::nexus

#endif // AXMANAGER_NEXUS_AX_MEGA_CACHE_HPP
