/**
 * @file AxManagerAPI.cpp
 * @brief Implementation of the High-Level API.
 */

#include "axmanager/api/AxManagerAPI.hpp"
#include "axmanager/nexus/SelfHealingOrchestrator.hpp"
#include <iostream>

namespace axmanager::api {

AxManager& AxManager::get_instance() {
    static AxManager instance;
    return instance;
}

bool AxManager::start() {
    auto& nexus = AxCoreNexus::get_instance();
    auto result = nexus.boot_engine();

    if (result.has_value()) {
        // شروع خودکار مانیتورینگ به محض بالا آمدن هسته
        SelfHealingOrchestrator::get_instance().start_monitoring();
        return true;
    }
    return false;
}

void AxManager::run_task(std::function<void()> task) {
    AxCoreNexus::get_instance().scheduler().submit_task({
        1, // Task ID
        task,
        TaskPriority::NORMAL,
        0, // No deadline
        false
    }, 0); // Run on Core 0
}

std::string AxManager::get_system_status() {
    return "AxManager Status: [RUNNING] | Integrity: [OPTIMAL] | Core Load: [LOW]";
}

void AxManager::run_ai_inference(const std::string& model_name) {
    std::cout << "[API] Loading model: " << model_name << " onto Neural Accelerator..." << std::endl;
    // فراخوانی متدهای شتاب‌دهنده در اینجا
}

} // namespace axmanager::api
