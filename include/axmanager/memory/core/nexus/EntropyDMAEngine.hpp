/**
 * @file EntropyDMAEngine.hpp
 * @project AxManager Hyper-Kernel
 * @author Amirhossein Tavakoli
 * @brief Entropy-Class Direct Memory Access (DMA) & Zero-Copy Engine.
 * 
 * This module bypasses the CPU for massive block memory transfers, 
 * utilizing dedicated PCIe/Bus mastering channels to move data across 
 * spatial memory boundaries at maximum hardware bandwidth with zero CPU overhead.
 */

#ifndef AXMANAGER_NEXUS_ENTROPY_DMA_ENGINE_HPP
#define AXMANAGER_NEXUS_ENTROPY_DMA_ENGINE_HPP

#include <cstdint>
#include <cstddef>
#include <atomic>
#include <expected>
#include <span>
#include <shared_mutex>

namespace axmanager::nexus {

/**
 * @brief DMA transfer failure modes.
 */
enum class DMAError : uint8_t {
    BUS_COLLISION,       // Hardware bus contention detected
    CHANNEL_EXHAUSTION,  // All hardware DMA channels are busy
    INVALID_BOUNDARY,    // Source/Destination addresses unaligned
    ENTROPY_FAULT        // Parity or bus parity check failure
};

/**
 * @brief DMA transfer priority levels.
 */
enum class DMAPriority : uint8_t {
    REALTIME = 0,
    HIGH = 1,
    NORMAL = 2,
    BACKGROUND = 3
};

/**
 * @struct DMATransferDescriptor
 * @brief Describes a zero-copy spatial memory transfer job.
 */
struct DMATransferDescriptor {
    uintptr_t source_physical_address;
    uintptr_t destination_physical_address;
    size_t transfer_size_bytes;
    DMAPriority priority;
    std::atomic<bool> is_complete{false};
};

/**
 * @class EntropyDMAEngine
 * @brief The hardware controller for autonomous memory transmission.
 */
class EntropyDMAEngine {
public:
    static EntropyDMAEngine& getEngineInstance();

    EntropyDMAEngine(const EntropyDMAEngine&) = delete;
    EntropyDMAEngine& operator=(const EntropyDMAEngine&) = delete;

    /**
     * @brief Initializes the DMA controller channels.
     */
    void calibrate_channels();

    /**
     * @brief Initiates an asynchronous zero-copy memory transfer via hardware bus.
     * @param descriptor The transfer specification.
     * @return Success or a DMAError code.
     */
    std::expected<void, DMAError> dispatch_transfer(const DMATransferDescriptor& descriptor);

    /**
     * @brief Synchronously flushes all pending DMA queues until complete.
     */
    void synchronize_bus();

    /**
     * @brief Returns the total bytes transferred since system boot.
     */
    uint64_t get_entropy_throughput() const noexcept { return m_total_bytes_transferred.load(); }

private:
    EntropyDMAEngine() : m_is_calibrated(false), m_total_bytes_transferred(0) {}
    ~EntropyDMAEngine() = default;

    mutable std::shared_mutex m_dma_lock;
    std::atomic<bool> m_is_calibrated;
    std::atomic<uint64_t> m_total_bytes_transferred;

    void execute_hardware_burst(uintptr_t src, uintptr_t dest, size_t size);
};

} // namespace axmanager::nexus

#endif // AXMANAGER_NEXUS_ENTROPY_DMA_ENGINE_HPP
