/**
 * @file VmxEngine.hpp
 * @project AxManager Hyper-Engine
 * @brief High-level controller for Intel VT-x hardware virtualization state.
 */

#ifndef AXMANAGER_NEXUS_ARCH_X86_ASM_VMX_ENGINE_HPP
#define AXMANAGER_NEXUS_ARCH_X86_ASM_VMX_ENGINE_HPP

#include <cstdint>
#include <stdexcept>

namespace axmanager::nexus::arch::x86_asm {

extern "C" {
    uint64_t ax_vmx_check_support() noexcept;
    void ax_vmx_enable() noexcept;
    uint64_t ax_vmx_on(const uint64_t* vmxon_region_phys) noexcept;
}

class VmxEngine {
public:
    /**
     * @brief Verifies and initializes the CPU into a hardware-level Hypervisor.
     */
    static void initialize_hypervisor() {
        if (ax_vmx_check_support() == 0) {
            throw std::runtime_error("Hardware Error: Intel VT-x (VMX) is not supported by this CPU.");
        }

        // Enable VMX extension in CR4
        ax_vmx_enable();

        // Allocate a 4KB aligned physical region required for VMXON
        alignas(4096) static uint64_t vmxon_region[512] = {0};
        
        // Set revision identifier in the VMXON region (retrieved from IA32_VMX_BASIC MSR)
        // For demonstration, we assume standard layout
        vmxon_region[0] = 0x00000001; // Example VMX Basic Revision ID

        uintptr_t phys_addr = reinterpret_cast<uintptr_t>(vmxon_region);
        if (ax_vmx_on(&phys_addr) != 0) {
            throw std::runtime_error("Critical Error: Failed to execute VMXON instruction.");
        }
    }
};

} // namespace axmanager::nexus::arch::x86_asm

#endif // AXMANAGER_NEXUS_ARCH_X86_ASM_VMX_ENGINE_HPP
