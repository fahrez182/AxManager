/**
 * @file AxiomInterruptVortex.hpp
 * @project AxManager Hyper-Kernel
 * @author Amirhossein Tavakoli
 * @brief Axiom-class Interrupt Vortex System.
 * 
 * This component manages the "Vortex" of CPU interrupts, utilizing 
 * non-linear dispatching algorithms to ensure sub-nanosecond latency 
 * in intercepting hardware signals.
 */

#ifndef AXMANAGER_NEXUS_AXIOM_INTERRUPT_VORTEX_HPP
#define AXMANAGER_NEXUS_AXIOM_INTERRUPT_VORTEX_HPP

#include <cstdint>
#include <vector>
#include <functional>
#include <map>
#include <mutex>
#include <atomic>
#include <concepts>
#include <expected> // Requires C++23 capability or polyfill

namespace axmanager::nexus {

/**
 * @brief Cosmic-scale priority levels for the Vortex.
 */
enum class VortexTier : uint8_t {
    SINGULARITY = 0, // Absolute highest priority (Non-maskable)
    EVENT_HORIZON = 1,
    QUARK_LEVEL = 2,
    NEBULA = 3,
    VOID = 4          // Background/Lowest
};

enum class VortexStatus : uint8_t {
    STABLE,
    COLLAPSING,
    SUPERNOVA, // Critical error/Fault
    WARPING    // Initializing
};

/**
 * @brief Constraint for Axiom-compatible handlers.
 */
template<typename T>
concept AxiomHandler = requires(T t, uint64_t vector) {
    { t(vector) } -> std::same_as<void>;
};

/**
 * @class AxiomInterruptVortex
 * @brief The core engine for managing high-velocity interrupt flows.
 * 
 * Employs a tiered-priority vortex mechanism to prevent interrupt starvation 
 * and ensure deterministic execution paths.
 */
class AxiomInterruptVortex {
public:
    static AxiomInterruptVortex& getNexusInstance();

    // Prevent cloning of the vortex
    AxiomInterruptVortex(const AxiomInterruptVortex&) = delete;
    AxiomInterruptVortex& operator=(const AxiomInterruptVortex&) = delete;

    /**
     * @brief Establishes the vortex within the CPU's execution pipeline.
     */
    void stabilize();

    /**
     * @brief Injects a new handler into the vortex flow.
     * @param vector The hardware vector.
     * @param tier The priority tier.
     * @param handler The Axiom-compliant logic.
     */
    template<AxiomHandler Func>
    void injectHandler(uint64_t vector, VortexTier tier, Func&& handler);

    void ejectHandler(uint64_t vector);

    /**
     * @brief Forces a vortex swirl (Triggering an interrupt).
     */
    void swirl(uint64_t vector);

    VortexStatus getVortexState() const noexcept { return m_state.load(); }

private:
    AxiomInterruptVortex() : m_state(VortexStatus::WARPING) {}
    ~AxiomInterruptVortex() = default;

    struct VortexNode {
        uint64_t vector;
        VortexTier tier;
        std::function<void(uint64_t)> logic;
        std::atomic<bool> is_active{true};
    };

    mutable std::recursive_mutex m_vortex_lock;
    std::atomic<VortexStatus> m_state;
    std::map<uint64_t, VortexNode> m_vortex_core;

    void vortexDispatch(uint64_t vector);
};

} // namespace axmanager::nexus

#endif // AXMANAGER_NEXUS_AXIOM_INTERRUPT_VORTEX_HPP
