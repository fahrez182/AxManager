#include "PhysicsSyncEngine.h"
#include <android/log.h>
#include <thread>
#include <cmath>
#include <random>

#define LOG_TAG "AxManager_PhysicsSync"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace AxManager {

PhysicsSyncEngine::PhysicsSyncEngine(int targetFps) {
    targetFrameTime = 1000.0 / targetFps;
    lastFrameTime = std::chrono::steady_clock::now();
    LOGI("PhysicsSyncEngine Online. Target FPS: %d (%.2f ms/frame)", targetFps, targetFrameTime);
}

PhysicsSyncEngine::~PhysicsSyncEngine() {}

void PhysicsSyncEngine::waitForNextFrame() {
    auto now = std::chrono::steady_clock::now();
    auto elapsed = std::chrono::duration<double, std::milli>(now - lastFrameTime).count();

    if (elapsed < targetFrameTime) {
        // محاسبه زمان باقی‌مانده برای رسیدن به فریم بعدی
        double sleepTime = targetFrameTime - elapsed;
        
        // اضافه کردن یک نویز بسیار کوچک به زمان خواب برای جلوگیری از تشخیص الگوی زمانی دقیق
        // این کار باعث می‌شود فواصل زمانی کاملاً ثابت نباشند (مثل رفتار انسان)
        static std::default_random_engine generator;
        std::uniform_real_distribution<double> distribution(0.98, 1.02); 
        sleepTime *= distribution(generator);

        std::this_thread::sleep_for(std::chrono::duration<double, std::milli>(sleepTime));
    }
    
    lastFrameTime = std::chrono::steady_clock::now();
}

uint64_t PhysicsSyncEngine::getSyncTimestamp() {
    auto now = std::chrono::steady_clock::now();
    return std::chrono::duration_cast<std::chrono::microseconds>(now.time_since_epoch()).count();
}

void PhysicsSyncEngine::adaptToSystemLoad(double cpuLoad) {
    // اگر بار پردازشی بالا برود، نرخ فریم را کمی تعدیل می‌کنیم تا سیستم دچار لگ نشود
    // و آنتی‌چیت متوجه تغییر ناگهانی در پاسخ‌دهی نشود.
    if (cpuLoad > 0.8) {
        targetFrameTime *= 1.05; // کاهش نرخ فریم در بار بالا
        LOGI("High CPU Load detected (%.2f). Adapting physics sync...", cpuLoad);
    } else if (cpuLoad < 0.4 && targetFrameTime > (1000.0/120.0)) {
        targetFrameTime *= 0.95; // افزایش نرخ فریم در بار پایین
    }
}

double PhysicsSyncEngine::calculateNaturalLatency() {
    // شبیه‌سازی تاخیر طبیعی سیستم (System Latency)
    static std::default_random_engine generator;
    std::normal_distribution<double> distribution(15.0, 2.0); // میانگین ۱۵ میلی‌ثانیه تاخیر
    return distribution(generator);
}

} // namespace AxManager
