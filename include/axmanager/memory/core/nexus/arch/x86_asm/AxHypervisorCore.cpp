/**
 * @file AxHypervisorCore.cpp
 * @project AxManager Hyper-Engine
 * @brief The main execution loop of the Hypervisor.
 */

#include "axmanager/nexus/arch/x86_asm/VmxEngine.hpp"
#include "axmanager/nexus/arch/x86_asm/VMCSManager.cpp"

namespace axmanager::nexus {

class AxHypervisorCore {
public:
    void run_hypervisor_loop() {
        // 1. Initialize VMX hardware
        arch::x86_asm::VmxEngine::initialize_hypervisor();

        // 2. Setup the VMCS Manager
        arch::x86_asm::VMCSManager vmcs_manager;

        // 3. Start the infinite Hypervisor cycle
        while (true) {
            // Attempt to launch the Guest
            // VMLAUNCH is used for the first entry, VMRESUME for subsequent entries
            
            /* 
               ASSEMBLY PSEUDO-CODE:
               vmlaunch
               if (error) {
                   handle_vmx_failure();
               }
            */

            // Once VMLAUNCH returns, it means a VM-Exit occurred (Guest gave control back)
            vmcs_manager.handle_vm_exit();
            
            // Re-configure context and resume (logic implemented via VMRESUME)
        }
    }
};

} // namespace axmanager::nexus
