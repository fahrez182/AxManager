/**
 * @file QuantumSyncScheduler.hpp
 * @project AxManager Hyper-Kernel
 * @brief Zero-Jitter, Lock-Free Task Scheduler with Work-Stealing Architecture.
 */

#ifndef AXMANAGER_NEXUS_QUANTUM_SYNC_SCHEDULER_HPP
#define AXMANAGER_NEXUS_QUANTUM_SYNC_SCHEDULER_HPP

#include <atomic>
#include <cstdint>
#include <expected>
#include <span>
#include <functional>
#include <concepts>

namespace axmanager::nexus {

constexpr size_t MAX_CPU_CORES = 64;
alignas(64) constexpr size_t SCHEDULER_CACHE_LINE = 64;

enum class SchedulerError : uint8_t {
    QUEUE_EXHAUSTED,
    CORE_OFFLINE,
    INVALID_TASK_PRIORITY,
    DEADLOCK_PREVENTED
};

enum class TaskPriority : uint8_t {
    CRITICAL_ISR = 0,
    REALTIME     = 1,
    HIGH         = 2,
    NORMAL       = 3,
    IDLE         = 4
};

struct alignas(SCHEDULER_CACHE_LINE) TaskDescriptor {
    uint64_t task_id;
    std::function<void()> execution_routine;
    TaskPriority priority;
    uint64_t target_tsc_deadline;
    std::atomic<bool> completed{false};
};

/**
 * @class QuantumSyncScheduler
 * @brief High-frequency core scheduler designed for deterministic execution.
 */
class QuantumSyncScheduler {
public:
    static QuantumSyncScheduler& get_instance();

    QuantumSyncScheduler(const QuantumSyncScheduler&) = delete;
    void operator=(const QuantumSyncScheduler&) = delete;

    /**
     * @brief Submits a high-priority task to the scheduler ring.
     */
    std::expected<void, SchedulerError> submit_task(TaskDescriptor&& task, uint8_t target_core) noexcept;

    /**
     * @brief Executes the next scheduled task for the calling core.
     */
    void tick_core(uint8_t core_id) noexcept;

    /**
     * @brief Initializes scheduler queues across active physical cores.
     */
    void initialize_cores(uint8_t active_cores);

private:
    QuantumSyncScheduler() : m_core_count(0), m_running(false) {}

    std::atomic<uint8_t> m_core_count;
    std::atomic<bool> m_running;

    // Per-core lock-free work queues would be managed here
    // Utilizing cache-aligned structures to eliminate cross-core cache invalidation storms.
};

} // namespace axmanager::nexus

#endif // AXMANAGER_NEXUS_QUANTUM_SYNC_SCHEDULER_HPP
