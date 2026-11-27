/**
 * @file IOMMUConfig.hpp
 * @project AxManager Hyper-Engine
 * @brief Configuration for Intel VT-d (IOMMU) to enable direct device assignment.
 */

#ifndef AXMANAGER_NEXUS_ARCH_X86_ASM_IOMMU_CONFIG_HPP
#define AXMANAGER_NEXUS_ARCH_X86_ASM_IOMMU_CONFIG_HPP

#include <cstdint>
#include <vector>

namespace axmanager::nexus::arch::x86_asm {

struct IOMMU_Domain {
    uint64_t domain_id;
    uintptr_t root_table_phys_addr; // آدرس فیزیکی جدول ریشه IOMMU
};

class IOMMUController {
public:
    /**
     * @brief ایجاد یک Domain ایزوله برای یک دستگاه سخت‌افزاری خاص.
     * این کار باعث می‌شود دستگاه فقط بتواند به محدوده مشخصی از حافظه دسترسی داشته باشد.
     */
    void create_device_domain(uint16_t bus, uint8_t device, uint8_t function, uintptr_t guest_mem_base) {
        // 1. ساخت جدول‌های ترجمه آدرس (I/O Page Tables) مشابه Paging Engine
        // 2. تخصیص یک Domain ID برای این ترکیب (Bus/Device/Function)
        // 3. ثبت در حافظه‌ی IOMMU سخت‌افزاری
    }

    /**
     * @brief متصل کردن یک دستگاه سخت‌افزاری به یک ماشین مجازی (Guest).
     */
    void assign_device_to_guest(uint16_t bdf, uint64_t guest_id) {
        // عملیات اتصال سخت‌افزاری (Passthrough)
    }

private:
    void update_iotlb() {
        // دستور هوشمند برای پاکسازی کش ترجمه I/O (IOTLB Invalidation)
    }
};

} // namespace axmanager::nexus::arch::x86_asm

#endif // AXMANAGER_NEXUS_ARCH_X86_ASM_IOMMU_CONFIG_HPP
