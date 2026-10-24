package frb.axeron.adb

import android.util.Log
import frb.axeron.adb.AdbProtocol.ADB_AUTH_RSAPUBLICKEY
import frb.axeron.adb.AdbProtocol.ADB_AUTH_SIGNATURE
import frb.axeron.adb.AdbProtocol.ADB_AUTH_TOKEN
import frb.axeron.adb.AdbProtocol.A_AUTH
import frb.axeron.adb.AdbProtocol.A_CLSE
import frb.axeron.adb.AdbProtocol.A_CNXN
import frb.axeron.adb.AdbProtocol.A_MAXDATA
import frb.axeron.adb.AdbProtocol.A_OKAY
import frb.axeron.adb.AdbProtocol.A_OPEN
import frb.axeron.adb.AdbProtocol.A_STLS
import frb.axeron.adb.AdbProtocol.A_STLS_VERSION
import frb.axeron.adb.AdbProtocol.A_VERSION
import frb.axeron.adb.AdbProtocol.A_WRTE
import rikka.core.util.BuildUtils
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.net.ssl.SSLSocket

private const val TAG = "AdbClient"

/** adb's own payload ceiling; anything larger in a header is garbage, not data. */
private const val MAX_PAYLOAD = 1024 * 1024

/**
 * Minimal ADB host-side client. Talks to adbd over TCP ([host]:[port], TLS when adbd asks for it)
 * or, via the secondary constructor, over a [UsbAdbTransport] for OTG activation.
 */
class AdbClient(private val key: AdbKey, private val port: Int, private val host: String = "127.0.0.1") : Closeable {

    /** Non-null when running over USB bulk endpoints instead of a socket. */
    private var usbTransport: UsbAdbTransport? = null

    /** USB (OTG) client: same protocol, no socket and no TLS. */
    constructor(key: AdbKey, usbTransport: UsbAdbTransport) : this(key, -1, "usb") {
        this.usbTransport = usbTransport
    }

    /** Optional trace of the USB handshake steps, for showing progress to the user. */
    var usbHandshakeLog: ((String) -> Unit)? = null

    private var socket: Socket? = null
    private lateinit var plainInputStream: DataInputStream
    private lateinit var plainOutputStream: DataOutputStream

    private var useTls = false

    private lateinit var tlsSocket: SSLSocket
    private lateinit var tlsInputStream: DataInputStream
    private lateinit var tlsOutputStream: DataOutputStream

    private val inputStream get() = if (useTls) tlsInputStream else plainInputStream
    private val outputStream get() = if (useTls) tlsOutputStream else plainOutputStream

    fun connect() {
        if (usbTransport != null) connectViaUsb() else connectViaTcp()
    }

    private fun connectViaTcp() {
        // Kept on the field so close() can actually release it (it used to be a shadowing
        // local, which leaked one socket per connect).
        val socket = Socket().also { this.socket = it }
        socket.connect(InetSocketAddress(host, port), 5000)

        socket.tcpNoDelay = true
        // Bound handshake reads so a stalled adbd can't hang the caller forever.
        socket.soTimeout = 5000
        plainInputStream = DataInputStream(socket.getInputStream())
        plainOutputStream = DataOutputStream(socket.getOutputStream())

        write(A_CNXN, A_VERSION, A_MAXDATA, "host::")

        var message = read()
        if (message.command == A_STLS) {
            if (!BuildUtils.atLeast29) {
                error("Connect to adb with TLS is not supported before Android 9")
            }
            write(A_STLS, A_STLS_VERSION, 0)

            val sslContext = key.sslContext
            tlsSocket = sslContext.socketFactory.createSocket(socket, host, port, true) as SSLSocket
            tlsSocket.startHandshake()
            Log.d(TAG, "Handshake succeeded.")

            tlsInputStream = DataInputStream(tlsSocket.inputStream)
            tlsOutputStream = DataOutputStream(tlsSocket.outputStream)
            useTls = true

            message = read()
        } else if (message.command == A_AUTH) {
            if (message.arg0 != ADB_AUTH_TOKEN) error("not A_AUTH ADB_AUTH_TOKEN")
            write(A_AUTH, ADB_AUTH_SIGNATURE, 0, key.sign(message.data))

            message = read()
            if (message.command != A_CNXN) {
                write(A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, key.adbPublicKey)
                message = read()
            }
        }

        if (message.command != A_CNXN) error("not A_CNXN")

        // Commands (e.g. the starter) can stay quiet for longer than a handshake should;
        // keep their reads unbounded as before.
        socket.soTimeout = 0
    }

    /**
     * USB handshake: RSA challenge only. If adbd doesn't trust our key yet it asks for the public
     * key and shows "Allow USB debugging?" on the target; the next read waits for that answer.
     *
     * Unlike TCP, a USB pipe outlives the session: adbd may still hold packets for a host that
     * gave up (a CNXN sent after a late "Allow", a stray WRTE). Those are drained first, and
     * anything else that isn't part of the handshake is skipped instead of taken as the reply.
     */
    private fun connectViaUsb() {
        val usb = usbTransport!!
        val stale = usb.drain()
        if (stale > 0) usbLog("Discarded $stale stale bytes from an earlier session")

        usbLog("Sending connect request to the target…")
        write(A_CNXN, A_VERSION, A_MAXDATA, "host::")

        var signed = false
        while (true) {
            val message = read()
            when (message.command) {
                A_CNXN -> break
                A_STLS -> error("Unexpected TLS negotiation over USB")
                A_AUTH -> {
                    if (message.arg0 != ADB_AUTH_TOKEN) error("USB auth: expected token, got ${message.arg0}")
                    if (!signed) {
                        write(A_AUTH, ADB_AUTH_SIGNATURE, 0, key.sign(message.data))
                        signed = true
                    } else {
                        // A second token means our signature didn't match a trusted key.
                        usbLog("Key not trusted yet — tap \"Allow\" on the target's USB debugging dialog")
                        write(A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, key.adbPublicKey)
                    }
                }

                else -> usbLog("Skipping a packet left from an earlier session")
            }
        }
        usbLog("Connected to the target's adbd")
    }

    private fun usbLog(line: String) {
        Log.d(TAG, "USB: $line")
        usbHandshakeLog?.invoke(line)
    }

    fun shellCommand(cmd: String, listener: ((ByteArray) -> Unit)? = null) {
        command("shell:$cmd", listener)
    }

    /** Local stream ids, one per [command]: lets a late packet of an earlier stream be told apart. */
    private var lastLocalId = 0

    fun command(cmd: String, listener: ((ByteArray) -> Unit)? = null) {
        val localId = ++lastLocalId
        write(A_OPEN, localId, 0, cmd)

        // adbd may still send a packet for the previous stream (e.g. a second CLSE after ours);
        // it's addressed to that stream's id, not this one.
        var message = read()
        while (message.command in intArrayOf(A_OKAY, A_WRTE, A_CLSE) && message.arg1 != localId) {
            Log.d(TAG, "Ignoring a late packet for stream ${message.arg1}")
            message = read()
        }
        when (message.command) {
            A_OKAY -> {
                val streamId = message.arg0
                while (true) {
                    message = try {
                        read()
                    } catch (e: OrphanPayload) {
                        // Its header was lost: deliver the output anyway and acknowledge it, or
                        // adbd waits for this OKAY forever.
                        usbLog("Recovered ${e.payload.size} output bytes whose header was lost")
                        listener?.invoke(e.payload)
                        write(A_OKAY, localId, streamId)
                        continue
                    }
                    if (message.arg1 != localId) continue
                    val remoteId = message.arg0
                    if (message.command == A_WRTE) {
                        if (message.data_length > 0) {
                            listener?.invoke(message.data!!)
                        }
                        write(A_OKAY, localId, remoteId)
                    } else if (message.command == A_CLSE) {
                        write(A_CLSE, localId, remoteId)
                        break
                    } else {
                        error("not A_WRTE or A_CLSE")
                    }
                }
            }
            A_CLSE -> {
                val remoteId = message.arg0
                write(A_CLSE, localId, remoteId)
            }
            else -> {
                error("not A_OKAY or A_CLSE")
            }
        }
    }

    private fun write(command: Int, arg0: Int, arg1: Int, data: ByteArray? = null) = write(AdbMessage(command, arg0, arg1, data))

    private fun write(command: Int, arg0: Int, arg1: Int, data: String) = write(AdbMessage(command, arg0, arg1, data))

    private fun write(message: AdbMessage) {
        val bytes = message.toByteArray()
        val usb = usbTransport
        if (usb != null) {
            // Header and payload go as separate transfers, like adb's own host does: adbd before
            // Android 10 reads exactly 24 header bytes per transfer, so a combined one loses the
            // payload and adbd stalls waiting for it.
            usb.write(bytes.copyOfRange(0, AdbMessage.HEADER_LENGTH))
            if (bytes.size > AdbMessage.HEADER_LENGTH) {
                usb.write(bytes.copyOfRange(AdbMessage.HEADER_LENGTH, bytes.size))
            }
            usbLog("→ ${message.toStringShort().substringBefore(", data=")}")
        } else {
            outputStream.write(bytes)
            outputStream.flush()
        }
        Log.d(TAG, "write ${message.toStringShort()}")
    }

    private fun read(): AdbMessage {
        val usb = usbTransport
        val header = ByteArray(AdbMessage.HEADER_LENGTH)
        if (usb != null) usb.readFully(header, 0, header.size)
        else inputStream.readFully(header, 0, header.size)

        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val command = buffer.int
        val arg0 = buffer.int
        val arg1 = buffer.int
        val dataLength = buffer.int
        val checksum = buffer.int
        val magic = buffer.int
        // Logged before the payload read, so a stalled payload still shows which header announced it.
        if (usb != null) {
            usbLog("← ${AdbMessage(command, arg0, arg1, dataLength, checksum, magic, null).toStringShort().substringBefore(", data=")}")
        }
        // A bad magic means we're reading payload bytes as a header; never allocate whatever
        // "length" those bytes spell.
        if (!isHeader(header)) {
            if (usb != null) throw OrphanPayload(collectOrphanPayload(usb, header))
            throw IOException("ADB stream out of sync: bad header (command=0x%08X, length=$dataLength)".format(command))
        }
        val data: ByteArray?
        if (dataLength >= 0) {
            data = ByteArray(dataLength)
            if (usb != null) usb.readFully(data, 0, dataLength)
            else inputStream.readFully(data, 0, dataLength)
        } else {
            data = null
        }
        val message = AdbMessage(command, arg0, arg1, dataLength, checksum, magic, data)
        message.validateOrThrow()
        Log.d(TAG, "read ${message.toStringShort()}")
        return message
    }

    private fun isHeader(bytes: ByteArray): Boolean {
        if (bytes.size < AdbMessage.HEADER_LENGTH) return false
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val command = buffer.getInt(0)
        val dataLength = buffer.getInt(12)
        return buffer.getInt(20) == command.inv() && dataLength in 0..MAX_PAYLOAD
    }

    /**
     * Over USB some host/adbd pairs lose a WRTE header while its payload still arrives (seen
     * on the first output of a shell, Android 9 target). adbd sends every header as its own
     * packet, so what we took for a header starts that payload: gather it up to its last
     * (short) packet, stopping early if the next packet is a real header.
     */
    private fun collectOrphanPayload(usb: UsbAdbTransport, start: ByteArray): ByteArray {
        val payload = ByteArrayOutputStream()
        payload.write(start)
        payload.write(usb.takeRestOfPacket())
        while (usb.lastPacketFull) {
            val next = usb.peek(AdbMessage.HEADER_LENGTH, 200) ?: break
            if (isHeader(next)) break
            payload.write(usb.takeRestOfPacket())
        }
        return payload.toByteArray()
    }

    /** Stream payload that reached us without its WRTE header (USB only). */
    private class OrphanPayload(val payload: ByteArray) : IOException("ADB payload without a header")

    override fun close() {
        usbTransport?.let {
            it.close()
            return
        }

        try {
            plainInputStream.close()
        } catch (e: Throwable) {
        }
        try {
            plainOutputStream.close()
        } catch (e: Throwable) {
        }
        try {
            socket?.close()
        } catch (e: Exception) {
        }

        if (useTls) {
            try {
                tlsInputStream.close()
            } catch (e: Throwable) {
            }
            try {
                tlsOutputStream.close()
            } catch (e: Throwable) {
            }
            try {
                tlsSocket.close()
            } catch (e: Exception) {
            }
        }
    }
}
