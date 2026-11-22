#ifndef NEURAL_HARDWARE_BRIDGE_H
#define NEURAL_HARDWARE_BRIDGE_H

#include <cstdint>
#include <string>
#include <vector>

namespace AxManager {

// ساختار داده‌ای برای ارسال مستقیم به درایور
struct RawKernelPacket {
    uint32_t magic_header;
    uint32_t command_id;
    uint64_t payload_addr;
    uint32_t payload_size;
    uint64_t checksum;
};

class NeuralHardwareBridge {
private:
    int device_fd;
    std::string device_path;
    bool is_connected;

    bool validate_checksum(const RawKernelPacket& packet);

public:
    NeuralHardwareBridge(const std::string& path = "/dev/input/event0");
    ~NeuralHardwareBridge();

    // اتصال به درایور یا فایل سیستم سطح پایین
    bool establishLink();

    // ارسال دستورات مستقیم به هسته (Direct Kernel Injection)
    bool transmitRawCommand(uint32_t cmd, void* data, size_t size);

    // دریافت وضعیت لحظه‌ای سخت‌افزار (مثل دمای CPU یا نرخ پاسخ‌دهی)
    uint64_t fetchHardwareTelemetry();

    void disconnect();
};

} // namespace AxManager

#endif // NEURAL_HARDWARE_BRIDGE_H
