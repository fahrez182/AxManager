#include "NeuralHardwareBridge.h"
#include <android/log.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <cstring>
#include <chrono>

#define LOG_TAG "AxManager_HardwareBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace AxManager {

NeuralHardwareBridge::NeuralHardwareBridge(const std::string& path) 
    : device_path(path), device_fd(-1), is_connected(false) {}

NeuralHardwareBridge::~NeuralHardwareBridge() {
    disconnect();
}

bool NeuralHardwareBridge::establishLink() {
    // باز کردن فایل دستگاه با دسترسی مستقیم
    device_fd = open(device_path.c_str(), O_RDWR | O_NONBLOCK);
    
    if (device_fd < 0) {
        LOGE("Failed to link to hardware device: %s. Check Root privileges.", device_path.c_str());
        return false;
    }

    is_connected = true;
    LOGI("Neural Hardware Bridge established successfully on %s", device_path.c_str());
    return true;
}

bool NeuralHardwareBridge::validate_checksum(const RawKernelPacket& packet) {
    // یک الگوریتم ساده برای اطمینان از سلامت داده در لایه پایین
    return (packet.magic_header == 0xAXM1_MAGIC);
}

bool NeuralHardwareBridge::transmitRawCommand(uint32_t cmd, void* data,
