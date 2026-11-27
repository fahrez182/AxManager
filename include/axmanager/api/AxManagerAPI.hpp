#pragma once
#ifndef AXMANAGER_API_HPP
#define AXMANAGER_API_HPP
#include <string>
#include <functional>
#include <future>
#include <vector>
#include <memory>
#include <cstdint>
namespace axmanager::api {
enum class TaskPriority:int{LOW=0,NORMAL=1,HIGH=2,CRITICAL=3};
enum class AxError:int{OK=0,NOT_RUNNING=1,BAD_ARG=2,BOOT_FAIL=3,QUEUE_FULL=4};
struct ApiConfig{bool shield=true;bool telemetry=true;int workers=4;std::string mask="system_server";};
struct SystemStatus{bool running=false;float cpu=0;std::vector<float> perCore;int64_t freeRam=0;int64_t totalRam=0;int temp=0;uint64_t uptime=0;std::string to_string() const;std::string to_json() const;};
struct TaskOptions{TaskPriority prio=TaskPriority::NORMAL;int core=-1;};
struct InferenceResult{bool ok=false;float conf=0;std::string label;std::string err;};
class IInferenceBackend{public:virtual ~IInferenceBackend()=default;virtual bool load(const std::string&n,std::string&e)=0;virtual float predict(const float in[8])=0;};
class AxManager{public:static AxManager& get();AxManager(const AxManager&)=delete;void operator=(const AxManager&)=delete;
bool init(const ApiConfig&c);bool start();void shutdown() noexcept;bool is_running() const noexcept;
std::future<void> run_task(std::function<void()>t);std::future<void> run_task_ex(std::function<void()>t,TaskOptions o);
uint64_t schedule(uint32_t ms,std::function<void()>t);void cancel_periodic(uint64_t id);void cancel_all();
SystemStatus status() const;std::string status_string() const;
void set_backend(std::shared_ptr<IInferenceBackend>b);InferenceResult infer(const std::string&m,const float in[8]);bool infer_simple(const std::string&m,std::string&e);
std::string last_error() const;AxError last_code() const;static const char* version(){return "4.0.0-api-full";}private:AxManager();~AxManager();struct Impl;std::unique_ptr<Impl> p_;};
#ifdef __cplusplus
extern "C" {
#endif
void* axm_get();int axm_start();void axm_shutdown();const char* axm_status_json();int axm_run_task(void(*fn)(void*),void*arg);
#ifdef __cplusplus
}
#endif
}
#endif
