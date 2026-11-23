/**
 * @file AxOrchestrator.hpp
 * @project AxManager Enterprise Hyper-Engine
 * @brief The central brain controlling memory, scheduling, and syscall interception.
 * @version 3.0.0 (Commercial Grade)
 */

#ifndef AXMANAGER_CORE_AX_ORCHESTRATOR_HPP
#define AXMANAGER_CORE_AX_ORCHESTRATOR_HPP

#include "axmanager/nexus/AxMegaCache.hpp"
#include "axmanager/nexus/AxMicroScheduler.hpp"
#include "axmanager/nexus/ContextBridge.hpp"
#include <mutex>
#include <thread>
#include <vector>

namespace axmanager::core {

/**
 * @brief The Orchestrator manages the lifecycle of the entire Hyper-Engine.
 * It ensures that the 1GB Cache, the Micro-Scheduler, and the Linux-Emulator
 * work in a non-blocking, high-throughput pipeline.
 */
class AxOrchestrator {
public:
    static AxOrchestrator& get_instance() {
        static AxOrchestrator instance;
        return instance;
    }

    // شروع عملیات اصلی موتور
    void boot() {
        std::cout << "[ORCHESTRATOR] Initializing AxManager Enterprise Engine..." << std::endl;
        
        // 1. Warm up the Mega-Cache
        AxMegaCache::get_instance().fast_flush();
        
        // 2. Start the Micro-Scheduler heartbeat
        m_running = true;
        m_scheduler_thread = std::thread(&AxOrchestrator::scheduler_loop, this);
        
        std::cout << "[ORCHESTRATOR] Engine Booted Successfully. Status: ACTIVE" << std::endl;
    }

    void shutdown() {
        m_running = false;
        if (m_scheduler_thread.joinable()) {
            m_scheduler_thread.join();
        }
        std::cout << "[ORCHESTRATOR] Engine Shutdown Cleanly." << std::endl;
    }

    /**
     * @brief High-level API to inject massive workloads.
     * This is the main entry point for "Commercial Grade" tasks.
     */
    void dispatch_workload(uint64_t weight, void* (*func)(void*), void* ctx) {
        // تزریق به میکروسالولر
        nexus::AxMicroScheduler::get_instance().inject_heavy_payload(weight, func, ctx);
    }

private:
    AxOrchestrator() : m_running(false) {}
    ~AxOrchestrator() { shutdown(); }

    void scheduler_loop() {
        while (m_running) {
            // هر تیک، یک چرخه کامل از مدیریت Micro-Ops و مانیتورینگ است.
            // این بخش با دقت میلی‌ثانیه (یا حتی کمتر) اجرا می‌شود.
            nexus::AxMicroScheduler::get_instance().tick();
            
            // جلوگیری از مصرف ۱۰۰٪ CPU در حالت Idle (در نسخه‌های تجاری این از Interrupt استفاده می‌کند)
            std::this_thread::sleep_for(std::chrono::microseconds(500)); 
        }
    }

    std::atomic<bool> m_running;
    std::thread m_scheduler_thread;
    std::mutex m_orchestrator_mutex;
};

} // namespace axmanager::core

#endif // AXMANAGER_CORE_AX_ORCHESTRATOR_HPP
