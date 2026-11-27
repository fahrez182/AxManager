/**
 * @file ChronosScheduler.cpp
 * @brief The real-time scheduler that uses ChronosEngine to manage MicroTasks.
 */

#include "axmanager/core/ChronosScheduler.hpp"
#include "axmanager/nexus/arch/x86_asm/ChronosTimer.hpp"
#include "axmanager/nexus/arch/x86_asm/HyperContextSwitcher.hpp"

namespace axmanager::core {

void ChronosScheduler::schedule_next() {
    using namespace axmanager::nexus::arch::x86_asm;

    // 1. Capture exact start time of the scheduling decision
    uint64_t start_cycle = ChronosEngine::get_cpu_cycles();

    // 2. Select the next MicroTask based on the highest priority and least jitter
    AxMicroTask* next_task = m_task_queue.pop_highest_priority();

    if (next_task) {
        // 3. Check if the task has exceeded its allocated time slice (Quantum)
        // This prevents "Hogging" and ensures anti-fragility.
        if (next_task->last_run_cycle > 0) {
            uint64_t elapsed = ChronosEngine::get_cpu_cycles() - next_task->last_run_cycle;
            if (elapsed > m_max_quantum_cycles) {
                // Task is behaving like a heavy process, force a context switch
                next_task->priority_penalty += 1;
            }
        }

        // 4. Perform the atomic context switch
        next_task->last_run_cycle = ChronosEngine::get_cpu_cycles();
        HyperContextSwitcher::restore(next_task->context);
    }
}

} // namespace axmanager::core
