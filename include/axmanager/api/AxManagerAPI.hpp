/**
 * @file AxManagerAPI.hpp
 * @brief The user-facing high-level API for AxManager.
 */

#ifndef AXMANAGER_API_AX_MANAGER_API_HPP
#define AXMANAGER_API_AX_MANAGER_API_HPP

#include "axmanager/nexus/AxCoreNexus.hpp"
#include <string>
#include <functional>

namespace axmanager::api {

/**
 * @class AxManager
 * @brief The primary entry point for developers.
 */
class AxManager {
public:
    static AxManager& get_instance();

    // --- ساده‌ترین دستورات برای کاربر ---

    /**
     * @brief روشن کردن موتور AxManager.
     */
    bool start();

    /**
     * @brief اجرای یک وظیفه (Task) در پس‌زمینه با بالاترین اولویت.
     */
    void run_task(std::function<void()> task);

    /**
     * @brief دریافت وضعیت سلامت سیستم به صورت متنی ساده.
     */
    std::string get_system_status();

    /**
     * @brief اجرای یک مدل هوش مصنوعی روی شتاب‌دهنده.
     */
    void run_ai_inference(const std::string& model_name);

private:
    AxManager() = default;
};

} // namespace axmanager::api

#endif // AXMANAGER_API_AX_MANAGER_API_HPP
