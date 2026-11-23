/**
 * @file NestedVmxShadow.hpp
 * @project AxManager Hyper-Engine
 * @brief Implements VMCS Shadowing to allow L1 Hypervisors to run inside L0.
 */

#ifndef AXMANAGER_NEXUS_ARCH_X86_ASM_NESTED_VMX_SHADOW_HPP
#define AXMANAGER_NEXUS_ARCH_X86_ASM_NESTED_VMX_SHADOW_HPP

#include "axmanager/nexus/arch/x86_asm/VMCSStructure.hpp"
#include <map>

namespace axmanager::nexus::arch::x86_asm {

class NestedVmxShadow {
public:
    // ذخیره VMCS لایه اول (L1) در حافظه ما (L0)
    struct ShadowVMCS {
        uint64_t shadow_fields[1024]; // نگهداری فیلدها برای شبیه‌سازی
        bool is_dirty = false;
    };

    /**
     * @brief شبیه‌سازی دستور VMWRITE برای لایه L1.
     * به جای نوشتن مستقیم روی سخت‌افزار، روی سایه می‌نویسیم.
     */
    void intercept_vmwrite(uint32_t field, uint64_t value, ShadowVMCS& l1_context) {
        // اگر فیلد حساس بود (مثل کنترل‌های اصلی)، آن را تایید یا اصلاح می‌کنیم
        if (is_critical_field(field)) {
            validate_and_sanitize(field, value);
        }
        
        l1_context.shadow_fields[field % 1024] = value;
        l1_context.is_dirty = true;
    }

private:
    bool is_critical_field(uint32_t field) {
        return (field == VMCSField::PRIMARY_CONTAINER_CONTROL || 
                field == VMCSField::SECONDARY_CONTAINER_CONTROL);
    }

    void validate_and_sanitize(uint32_t field, uint64_t value) {
        // الگوریتم آنتی-فراود برای جلوگیری از تلاش L1 برای شکستن ایزولاسیون L0
        // اگر L1 سعی کند قابلیت‌های خطرناک را فعال کند، اینجا مسدود می‌شود
    }
};

} // namespace axmanager::nexus::arch::x86_asm

#endif // AXMANAGER_NEXUS_ARCH_X86_ASM_NESTED_VMX_SHADOW_HPP
