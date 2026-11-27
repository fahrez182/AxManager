#ifndef SOC_TUNER_H
#define SOC_TUNER_H

#include <string>
#include <vector>

namespace AxManager {

struct CPUPerformanceProfile {
    std::string profileName;
    int targetFrequencyMHz;
    int coreAffinityMask;
    bool enableTurboBoost;
};

class SoCTuner {
private:
    bool isTuningActive;
    std::string currentSoCType; // e.g., "Exynos", "Snapdragon"

public:
    SoCTuner();
    ~SoCTuner();

    bool detectSoC();
    bool applyPerformanceProfile(const CPUPerformanceProfile& profile);
    bool optimizeThermalThrottling(); // جلوگیری از کاهش فرکانس به دلیل گرما
    void stabilizePowerConsumption();  // برای جلوگیری از شناسایی توسط سیستم‌های امنیتی
};

} // namespace AxManager

#endif // SOC_TUNER_H
