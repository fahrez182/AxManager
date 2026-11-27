#include "PatternScanner.h"
#include <android/log.h>
#include <cstring>
#include <sstream>
#include <iomanip>

#define LOG_TAG "AxManager_PatternScanner"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace AxManager {

PatternScanner::PatternScanner() {}
PatternScanner::~PatternScanner() {}

bool PatternScanner::parsePattern(const std::string& pattern, std::vector<uint8_t>& outBytes, std::vector<bool>& outMask) {
    std::stringstream ss(pattern);
    std::string byteStr;

    while (ss >> byteStr) {
        if (byteStr == "?" || byteStr == "??") {
            outBytes.push_back(0x00);
            outMask.push_back(false); // Wildcard (Ignore)
        } else {
            outBytes.push_back(static_cast<uint8_t>(std::stoi(byteStr, nullptr, 16)));
            outMask.push_back(true);  // Exact match required
        }
    }
    return !outBytes.empty();
}

uintptr_t PatternScanner::scanRegion(uintptr_t startAddress, size_t size, const std::string& pattern) {
    std::vector<uint8_t> patternBytes;
    std::vector<bool> patternMask;

    if (!parsePattern(pattern, patternBytes, patternMask)) {
        LOGE("Failed to parse pattern for scanning.");
        return 0;
    }

    size_t patternSize = patternBytes.size();
    if (size < patternSize) return 0;

    volatile uint8_t* memStream = reinterpret_cast<volatile uint8_t*>(startAddress);

    for (size_t i = 0; i <= size - patternSize; ++i) {
        bool found = true;
        for (size_t j = 0; j < patternSize; ++j) {
            if (patternMask[j] && memStream[i + j] != patternBytes[j]) {
                found = false;
                break;
            }
        }
        if (found) {
            uintptr_t foundAddress = startAddress + i;
            LOGI("Pattern found successfully at address: 0x%lx", foundAddress);
            return foundAddress;
        }
    }

    return 0; // Not found
}

uintptr_t PatternScanner::findPatternInRange(uintptr_t start, uintptr_t end, const std::string& pattern) {
    if (start >= end) return 0;
    size_t regionSize = end - start;
    return scanRegion(start, regionSize, pattern);
}

} // namespace AxManager
