/**
 * @file VMCSStructure.hpp
 * @project AxManager Hyper-Engine
 * @brief Definition of VMCS field offsets for Intel VT-x control.
 */

#ifndef AXMANAGER_NEXUS_ARCH_X86_ASM_VMCS_STRUCTURE_HPP
#define AXMANAGER_NEXUS_ARCH_X86_ASM_VMCS_STRUCTURE_HPP

#include <cstdint>

namespace axmanager::nexus::arch::x86_asm {

// VMCS Field Offsets (Simplified for AxManager implementation)
struct VMCSField {
    static constexpr uint32_t PIN_BASED_VM_EXEC_CONTROL = 0x0000201A;
    static constexpr uint32_t PRIMARY_CONTAINER_CONTROL = 0x00002004;
    static constexpr uint32_t SECONDARY_CONTAINER_CONTROL = 0x00002006;
    static constexpr uint32_t ENTRY_CONTROL_HOST_STATE = 0x00002010;
    static constexpr uint32_t ENTRY_CONTROL_GUEST_STATE = 0x00002014;
    static constexpr uint32_t EXIT_CONTROL_HOST_STATE = 0x00002018;
    static constexpr uint32_t EXIT_CONTROL_GUEST_STATE = 0x0000201C;
    static constexpr uint32_t GUEST_RIP = 0x00006400;
    static constexpr uint32_t GUEST_RSP = 0x00006800;
    static constexpr uint32_t EXIT_REASON = 0x00004404;
    static constexpr uint32_t EXIT_QUALIFIER = 0x00004406;
};

} // namespace axmanager::nexus::arch::x86_asm

#endif // AXMANAGER_NEXUS_ARCH_X86_ASM_VMCS_STRUCTURE_HPP
