#include "TactileInputEngine.h"
#include <android/log.h>
#include <random>
#include <cmath>

#define LOG_TAG "AxManager_Tactile"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace AxManager {

TactileInputEngine::TactileInputEngine(float jitter, bool bypass) 
    : jitterIntensity(jitter), bypassKernelProtection(bypass) {
    LOGI("Tactile Input Engine Online. Jitter: %.2f, KernelBypass: %s", 
         jitter, bypass ? "ENABLED" : "DISABLED");
}

TactileInputEngine::~TactileInputEngine() {}

void TactileInputEngine::applyMicroJitter(int32_t& value, int32_t range) {
    static std::default_random_engine generator;
    // استفاده از توزیع نرمال برای شبیه‌سازی لرزش دست انسان
    std::normal_distribution<double> distribution(0.0, jitterIntensity * range);

    double jitter = distribution(generator);
    value += static_cast<int32_t>(jitter);

    // اطمینان از عدم خروج از محدوده
    if (value < 0) value = 0;
    if (value > range) value = range;
}

void TactileInputEngine::dispatchTouch(int32_t x, int32_t y, int32_t pressure) {
    int32_t jitterX = x;
    int32_t jitterY = y;

    applyMicroJitter(jitterX, 1080); 
    applyMicroJitter(jitterY, 1920);

    // در اینجا در لایه نهایی، داده‌ها به /dev/input ارسال می‌شوند
    LOGI("Tactile Touch Dispatched: [X:%d, Y:%d, P:%d]", jitterX, jitterY, pressure);
}

void TactileInputEngine::dispatchAxis(int32_t axis, int32_t position) {
    int32_t jitterPos = position;
    applyMicroJitter(jitterPos, 255);

    LOGI("Tactile Axis Dispatched: [Axis:%d, Pos:%d]", axis, jitterPos);
}

void TactileInputEngine::synchronizeWithPhysics(int hz) {
    LOGI("Tactile Engine synchronized to %d Hz physics loop.", hz);
}

} // namespace AxManager
