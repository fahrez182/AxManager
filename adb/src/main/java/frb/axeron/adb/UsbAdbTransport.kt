package frb.axeron.adb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbRequest
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeoutException

/**
 * Byte I/O for [AdbClient] over USB Host bulk endpoints, used when this device acts as the
 * "computer" end of an OTG cable. The ADB messages (CNXN/AUTH/OPEN/WRTE…) are the same as over
 * TCP; only the transport differs, and USB needs no TLS.
 *
 * adbd exposes two bulk endpoints on its interface (class 0xFF / subclass 0x42 / protocol 0x01):
 * [bulkOut] host → device and [bulkIn] device → host.
 *
 * Transfers go through [UsbRequest] rather than `bulkTransfer`: some hosts fail a transfer
 * while adbd hasn't queued its side yet, and the synchronous path then also drops whatever
 * packet did arrive — a lost WRTE header desyncs the whole stream. A request reports the
 * bytes actually moved regardless, so an early failure can be retried without losing data.
 */
class UsbAdbTransport(
    private val connection: UsbDeviceConnection,
    bulkIn: UsbEndpoint,
    bulkOut: UsbEndpoint,
    /**
     * How long one read or write may wait. Generous because the first read after sending our
     * public key blocks until the user answers "Allow USB debugging?" on the target.
     */
    private val timeoutMs: Int = 60_000,
) : Closeable {

    private companion object {
        /** Pause before retrying a transfer that moved nothing. */
        const val RETRY_DELAY_MS = 5L

        /** Refused queues in a row (~100 ms) after which the device is taken as gone from the bus. */
        const val MAX_QUEUE_FAILURES = 20
    }

    private val inRequest = UsbRequest().apply { initialize(connection, bulkIn) }
    private val outRequest = UsbRequest().apply { initialize(connection, bulkOut) }

    /**
     * Each IN transfer asks for exactly one max-size packet: asking for less (e.g. a 24-byte
     * header) overflows when adbd sends header and payload in one packet, and asking for more
     * can block waiting for a zero-length packet adbd never sends. Leftover bytes stay here
     * for the next [readFully].
     */
    private val inBuffer = ByteBuffer.allocate(bulkIn.maxPacketSize)
    private var readPos = 0
    private var readEnd = 0

    /** One packet per OUT transfer, so a retried one is never partly delivered twice. */
    private val outBuffer = ByteBuffer.allocate(bulkOut.maxPacketSize)

    fun write(data: ByteArray) {
        var offset = 0
        while (offset < data.size) {
            outBuffer.clear()
            outBuffer.put(data, offset, minOf(data.size - offset, outBuffer.capacity()))
            outBuffer.flip()
            val sent = transfer(outRequest, outBuffer, timeoutMs)
            if (sent < 0) throw IOException("USB bulk write failed at $offset/${data.size} after $timeoutMs ms")
            offset += sent
        }
    }

    /**
     * Discards whatever adbd still has queued from an earlier session (e.g. the CNXN it sends
     * once "Allow" is tapped after the previous host gave up), so this session's replies line
     * up. Returns the number of bytes thrown away.
     */
    fun drain(): Int {
        readPos = 0
        readEnd = 0
        var discarded = 0
        repeat(64) {
            inBuffer.clear()
            val read = transfer(inRequest, inBuffer, 100)
            if (read <= 0) return discarded
            discarded += read
        }
        return discarded
    }

    /** Reads exactly [length] bytes into [buffer] at [offset]. */
    fun readFully(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size - offset) {
        var total = 0
        while (total < length) {
            if (readPos == readEnd) {
                inBuffer.clear()
                val read = transfer(inRequest, inBuffer, timeoutMs)
                if (read < 0) throw IOException("USB bulk read failed ($total/$length bytes) after $timeoutMs ms")
                readPos = 0
                readEnd = read
                continue
            }
            val n = minOf(length - total, readEnd - readPos)
            inBuffer.array().copyInto(buffer, offset + total, readPos, readPos + n)
            readPos += n
            total += n
        }
    }

    /** Whether the packet being read filled a whole max-size packet (its payload may go on). */
    val lastPacketFull: Boolean get() = readEnd == inBuffer.capacity()

    /** The unread rest of the current packet, consumed. */
    fun takeRestOfPacket(): ByteArray =
        inBuffer.array().copyOfRange(readPos, readEnd).also { readPos = readEnd }

    /**
     * Loads the next packet if the current one is used up and returns up to its first [n]
     * bytes without consuming them; null when nothing arrives within [timeout] ms.
     */
    fun peek(n: Int, timeout: Int): ByteArray? {
        if (readPos == readEnd) {
            inBuffer.clear()
            val read = transfer(inRequest, inBuffer, timeout)
            if (read < 0) return null
            readPos = 0
            readEnd = read
        }
        return inBuffer.array().copyOfRange(readPos, minOf(readEnd, readPos + n))
    }

    /**
     * Runs one transfer of [buffer]'s remaining bytes on [request]'s endpoint, waiting up to
     * [timeout] ms. Returns the bytes moved, or -1 once the deadline passes. A completion that
     * moved nothing (an early host-side failure) is retried until then; a request that can't
     * even be queued for a while means the device left the bus (e.g. adbd restarting for
     * `tcpip:`), which fails right away instead of waiting out the deadline.
     */
    private fun transfer(request: UsbRequest, buffer: ByteBuffer, timeout: Int): Int {
        val start = buffer.position()
        val deadline = System.currentTimeMillis() + timeout
        var queueFailures = 0
        while (true) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) return -1
            buffer.position(start)
            if (!request.queue(buffer)) {
                if (++queueFailures >= MAX_QUEUE_FAILURES) return -1
                Thread.sleep(RETRY_DELAY_MS)
                continue
            }
            queueFailures = 0
            try {
                connection.requestWait(remaining)
            } catch (_: TimeoutException) {
                // Reap the cancelled request so the next wait doesn't return it.
                request.cancel()
                runCatching { connection.requestWait(100) }
                return -1
            }
            val moved = buffer.position() - start
            if (moved > 0) return moved
            Thread.sleep(RETRY_DELAY_MS)
        }
    }

    override fun close() {
        inRequest.close()
        outRequest.close()
        try {
            connection.close()
        } catch (_: Exception) {
        }
    }
}
