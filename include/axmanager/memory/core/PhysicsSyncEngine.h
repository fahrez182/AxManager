#ifndef PHYSICS_SYNC_ENGINE_H
#define PHYSICS_SYNC_ENGINE_H

#include <cstdint>
#include <chrono>

namespace AxManager {

class PhysicsSyncEngine {
private:
    double targetFrameTime; // زمان هدف برای هر فریم (به میلی‌ثانیه)
    std::chrono::steady_clock::time_point lastFrameTime;
    
    // مدیریت انحراف زمانی (Jitter Buffer) برای جلوگیری از تشخیص الگوی زمانی
    double calculateNaturalLatency();

public:
    PhysicsSyncEngine(int targetFps = 60);
    ~PhysicsSyncEngine();

    // انتظار برای رسیدن به فریم بعدی (برای هماهنگی با فیزیک بازی)
    void waitForNextFrame();

    // محاسبه زمان سپری شده از آخرین عملیات با افزودن نویز زمانی طبیعی
    uint64_t getSyncTimestamp();

    // تنظیم نرخ فریم هدف بر اساس وضعیت بار پردازشی سیستم
    void adaptToSystemLoad(double cpuLoad);
};

} // namespace AxManager

#endif // PHYSICS_SYNC_ENGINE_H
