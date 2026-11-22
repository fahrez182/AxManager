#ifndef INPUT_RESPONSE_ENGINE_H
#define INPUT_RESPONSE_ENGINE_H

#include <vector>
#include <string>

namespace AxManager {

struct InputEvent {
    float x;
    float y;
    long timestamp;
    int type; // 0 for touch, 1 for key, etc.
};

class InputResponseEngine {
private:
    bool isEngineActive;
    float latencyCompensationFactor;

public:
    InputResponseEngine();
    ~InputResponseEngine();

    void initialize();
    void processInputStream(std::vector<InputEvent>& events);
    void applyHumanizationFilter(std::vector<InputEvent>& events);
    
    // تنظیم سطح حساسیت برای جلوگیری از شناسایی
    void setStealthMode(bool enabled);
};

} // namespace AxManager

#endif // INPUT_RESPONSE_ENGINE_H
