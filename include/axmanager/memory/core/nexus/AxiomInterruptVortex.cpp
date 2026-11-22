/**
 * @file AxiomInterruptVortex.cpp
 * @brief Implementation of the Axiom Interrupt Vortex.
 */

#include "axmanager/nexus/AxiomInterruptVortex.hpp"
#include <iostream>
#include <stdexcept>

namespace axmanager::nexus {

AxiomInterruptVortex& AxiomInterruptVortex::getNexusInstance() {
    static AxiomInterruptVortex instance;
    return instance;
}

void AxiomInterruptVortex::stabilize() {
    std::lock_guard<std::recursive_mutex> lock(m_vortex_lock);

    if (m_state == VortexStatus::STABLE) return;

    std::cout << "[NEXUS] Stabilizing Axiom Vortex... Aligning Vectors..." << std::endl;
    
    // In a real hyper-kernel, this would involve modifying the GDT/IDT 
    // and setting up the TSS (Task State Segment).
    
    m_state.store(VortexStatus::STABLE);
    std::cout << "[NEXUS] Vortex is now STABLE. Ready for signal injection." << std::endl;
}

template<AxiomHandler Func>
void AxiomInterruptVortex::injectHandler(uint64_t vector, VortexTier tier, Func&& handler) {
    std::lock_guard<std::recursive_mutex> lock(m_vortex_lock);

    if (m_state != VortexStatus::STABLE) {
        throw std::runtime_error("Vortex is not stable. Injection failed.");
    }

    VortexNode node;
    node.vector = vector;
    node.tier = tier;
    node.logic = std::forward<Func>(handler);
    node.is_active.store(true);

    m_vortex_core[vector] = std::move(node);
    
    std::cout << "[NEXUS] Injected Vector: 0x" << std::hex << vector 
              << " | Tier: " << static_cast<int>(tier) << std::dec << std::endl;
}

// Explicit instantiation
template void AxiomInterruptVortex::injectHandler<void(uint64_t)>(uint64_t, VortexTier, std::function<void(uint64_t)>);

void AxiomInterruptVortex::ejectHandler(uint64_t vector) {
    std::lock_guard<std::recursive_mutex> lock(m_vortex_lock);
    if (m_vortex_core.erase(vector)) {
        std::cout << "[NEXUS] Ejected Vector: 0x" << std::hex << vector << std::endl;
    }
}

void AxiomInterruptVortex::swirl(uint64_t vector) {
    vortexDispatch(vector);
}

void AxiomInterruptVortex::vortexDispatch(uint64_t vector) {
    // High-speed dispatch logic
    auto it = m_vortex_core.find(vector);
    if (it != m_vortex_core.end() && it->second.is_active.load()) {
        // Triggering the logic within the vortex
        it->second.logic(vector);
    }
}

} // namespace axmanager::nexus
