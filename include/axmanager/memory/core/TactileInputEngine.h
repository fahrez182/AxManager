#ifndef TACTILE_INPUT_ENGINE_H
#define TACTILE_INPUT_ENGINE_H

#include <cstdint>
#include <vector>

namespace AxManager {

struct TactileEvent {
    int32_t type;
    int32_t code;
    int32_t value;
    uint64_t timestamp;
};

class TactileInputEngine {
private:
    float jitterIntensity; 
    bool bypassKernelProtection;

    void applyMicroJitter(int32_t& value, int32_t range);

public:
    TactileInputEngine(float jitter = 0.05f, bool bypass = true);
    ~TactileInputEngine();

    // شبیه‌سازی لمس با اعمال لرزش‌های میکروسکوپی انسانی
    void dispatchTouch(int32_t x, int32_t y, int32_t pressure);
    
    // کنترل حرکت محورهای کنترلر/جوی‌استیک
    void dispatchAxis(int32_t axis, int32_t position);

    // تنظیم نرخ فریم ورودی برای هماهنگی با فیزیک بازی
    void synchronizeWithPhysics(int hz);
};

} // namespace AxManager

#endif // TACTILE_INPUT_ENGINE_H
