#ifndef PATTERN_SCANNER_H
#define PATTERN_SCANNER_H

#include <vector>
#include <string>
#include <cstdint>

namespace AxManager {

class PatternScanner {
private:
    bool parsePattern(const std::string& pattern, std::vector<uint8_t>& outBytes, std::vector<bool>& outMask);

public:
    PatternScanner();
    ~PatternScanner();

    // جستجوی الگو در یک محدوده از حافظه
    uintptr_t scanRegion(uintptr_t startAddress, size_t size, const std::string& pattern);
    
    // جستجو در تمام نقشه‌های حافظه فرآیند
    uintptr_t findPatternInRange(uintptr_t start, uintptr_t end, const std::string& pattern);
};

} // namespace AxManager

#endif // PATTERN_SCANNER_H
