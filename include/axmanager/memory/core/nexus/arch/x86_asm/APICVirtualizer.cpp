/**
 * @file APICVirtualizer.cpp
 * @project AxManager Hyper-Engine
 * @brief Low-level initialization of virtual APIC access pages and Local APIC sync.
 */

#include "axmanager/nexus/arch/x86_asm/APICVirtualizer.hpp"
#include <cstring>

namespace axmanager::nexus::arch::x86_asm {

class APICEngineImpl : public APICVirtualizer {
private:
    VirtualAPICPage v_apic_page;

public:
    APICEngineImpl() {
        std::memset(&v_apic_page, 0, sizeof(v_apic_page));
    }

    /**
     * @brief نگاشت صفحه APIC مجازی به فضای حافظه مهمان (Guest Physical Address).
     */
    void map_virtual_apic_page(uint64_t guest_phys_addr) noexcept {
        // اتصال آدرس فیزیکی APIC به ساختار VMCS (APIC-access address field)
        uintptr_t v_apic_phys = reinterpret_cast<uintptr_t>(&v_apic_page);
        
        asm volatile (
            "vmwrite %0, %1" 
            : 
            : "r"(v_apic_phys), "c"(0x00002012) // شناسه فیمت APIC-access address در VMCS
            : "memory"
        );
    }
};

} // namespace axmanager::nexus::arch::x86_asm
