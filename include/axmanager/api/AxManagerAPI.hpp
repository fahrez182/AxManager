/**
 * @file AxManagerAPI.hpp
 * @brief ورودی اصلی و سطح‌بالای AxManager برای توسعه‌دهنده
 */
#pragma once
#ifndef AXMANAGER_API_AXMANAGERAPI_HPP
#define AXMANAGER_API_AXMANAGERAPI_HPP

#include <string>
#include <functional>
#include <future>
#include <mutex>
#include <atomic>

namespace axmanager::api {

struct ApiConfig {
  bool enableShield = true;
  bool enableTelemetry = true;
  std::string processMask = "system_server";
};

struct SystemStatus {
  bool running = false;
  std::string integrity = "UNKNOWN";
  int coreLoadPercent = 0;
  int64_t freeRamBytes = 0;
  std::string to_string() const;
};

/**
 * @class AxManager
 * @brief نقطه‌ی ورود اصلی. ترد-سیف و سینگلتون.
 */
class AxManager {
public:
  static AxManager& get_instance();

  AxManager(const AxManager&) = delete;
  AxManager& operator=(const AxManager&) = delete;

  bool init(const ApiConfig& cfg);
  bool start();
  void shutdown() noexcept;
  bool is_running() const noexcept { return m_running.load(); }

  std::future<void> run_task(std::function<void()> task);
  std::future<void> run_task_on_core(std::function<void()> task, int coreId);

  SystemStatus get_system_status() const;
  std::string get_system_status_string() const;

  bool run_ai_inference(const std::string& model_name, std::string& err);

  static const char* version() noexcept { return "2.1.0-api-hardened"; }
  std::string last_error() const;

private:
  AxManager() = default;
  ~AxManager() { shutdown(); }

  std::string check_task(const std::function<void()>& t, int coreId) const;

  mutable std::mutex m_mtx;
  std::atomic<bool> m_inited{false};
  std::atomic<bool> m_running{false};
  ApiConfig m_cfg;
  std::string m_lastError;
};

} // namespace axmanager::api

#endif
