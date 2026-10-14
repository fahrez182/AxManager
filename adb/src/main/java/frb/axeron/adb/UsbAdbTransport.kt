package frb.axeron.adb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import java.io.Closeable
import java.io.IOException

/**
 * Byte I/O for [AdbClient] over USB Host bulk endpoints, used when this device acts as the
 * "computer" end of an OTG cable. The ADB messages (CNXN/AUTH/OPEN/WRTE…) are the same as over
 * TCP; only the transport differs, and USB needs no TLS.
 *
 * adbd exposes two bulk endpoints on its interface (class 0xFF / subclass 0x42 / protocol 0x01):
 * [bulkOut] host → device and [bulkIn] device → host.
 */
class UsbAdbTransport(
    private val connection: UsbDeviceConnection,
    private val bulkIn: UsbEndpoint,
    private val bulkOut: UsbEndpoint,
    /**
     * Per-transfer timeout. Generous because the first read after sending our public key
     * blocks until the user answers "Allow USB debugging?" on the target.
     */
    private val timeoutMs: Int = 60_000,
) : Closeable {

    private companion object {
        /** Max bytes per bulk write. */
        const val WRITE_CHUNK = 16_384
    }

    fun write(data: ByteArray) {
        var offset = 0
        while (offset < data.size) {
            val chunk = minOf(data.size - offset, WRITE_CHUNK)
            val sent = connection.bulkTransfer(bulkOut, data, offset, chunk, timeoutMs)
            if (sent < 0) throw IOException("USB bulk write failed at $offset/${data.size}")
            offset += sent
        }
    }

    /** Reads exactly [length] bytes into [buffer] at [offset]. */
    fun readFully(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size - offset) {
        if (length == 0) return
        val tmp = ByteArray(length)
        var total = 0
        while (total < length) {
            val read = connection.bulkTransfer(bulkIn, tmp, total, length - total, timeoutMs)
            if (read < 0) throw IOException("USB bulk read failed ($total/$length bytes)")
            total += read
        }
        tmp.copyInto(buffer, offset, 0, length)
    }

    override fun close() {
        try {
            connection.close()
        } catch (_: Exception) {
        }
    }
}
