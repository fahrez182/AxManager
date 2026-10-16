package frb.axeron.server.axsh

import android.net.LocalServerSocket
import android.net.LocalSocket
import frb.axeron.server.util.Logger
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Native `axsh` clients connect to the abstract socket [SOCKET_NAME] and get a real
 * `/system/bin/sh` running with this server's privileges (shell or root, whichever the server
 * was started as). The client is a thin, dependency-free relay; all privilege stays here.
 *
 * Wire format is defined in `api/axsh/src/main/cpp/axsh_protocol.h` and must stay in sync.
 *
 * Not yet implemented: PTY (interactive terminals). [FLAG_PTY] requests are served over pipes
 * for now, which is exactly what non-terminal callers such as QuickShell need.
 */
class AxshServer(
    private val environment: () -> Map<String, String>?,
    private val authorize: (uid: Int, pid: Int) -> Boolean,
) {

    companion object {
        private val LOGGER = Logger("AxshServer")

        const val SOCKET_NAME = "axeron"
        const val VERSION = 1

        private const val FLAG_PTY = 1
        private const val FLAG_LOGIN = 2

        private const val STATUS_OK = 0
        private const val STATUS_DENIED = 1
        private const val STATUS_ERROR = 2

        private const val FRAME_DATA = 0x01
        private const val FRAME_EXIT = 0x02
        private const val FRAME_WINCH = 0x03

        private const val REMOTE_SHELL = "/system/bin/sh"

        private const val MAX_ARGS = 4096
        private const val MAX_STRING = 1 shl 20
        private const val BUFFER_SIZE = 8192
    }

    @Volatile
    private var running = false
    private var thread: Thread? = null
    private var server: LocalServerSocket? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({ acceptLoop() }, "axsh-accept").apply {
            isDaemon = true
            start()
        }
        LOGGER.i("axsh listening on @%s", SOCKET_NAME)
    }

    fun stop() {
        running = false
        try {
            server?.close()
        } catch (_: Throwable) {
        }
        server = null
    }

    private fun acceptLoop() {
        val listener = try {
            LocalServerSocket(SOCKET_NAME)
        } catch (e: Throwable) {
            LOGGER.e(e, "can't bind @%s", SOCKET_NAME)
            running = false
            return
        }
        server = listener

        while (running) {
            val client = try {
                listener.accept()
            } catch (e: Throwable) {
                if (running) LOGGER.w(e, "accept failed")
                break
            }
            Thread({ serve(client) }, "axsh-session").apply {
                isDaemon = true
                start()
            }
        }
    }

    private fun serve(client: LocalSocket) {
        try {
            client.use { socket ->
                val input = socket.inputStream
                val output = socket.outputStream

                val request = readRequest(input)
                if (request == null) {
                    writeStatus(output, STATUS_ERROR, "Malformed request")
                    return
                }

                val uid = peerUid(socket)
                val pid = peerPid(socket)
                if (!authorize(uid, pid)) {
                    LOGGER.w("axsh denied uid=%d pid=%d", uid, pid)
                    writeStatus(output, STATUS_DENIED, "AxManager permission required")
                    return
                }

                writeStatus(output, STATUS_OK, null)
                runShell(input, output, request)
            }
        } catch (e: Throwable) {
            LOGGER.w(e, "axsh session failed")
        }
    }

    private data class Request(val argv: List<String>, val flags: Int)

    private fun runShell(clientIn: InputStream, clientOut: OutputStream, request: Request) {
        val builder = ProcessBuilder(request.argv)
        builder.redirectErrorStream(true)
        environment()?.let {
            builder.environment().clear()
            builder.environment().putAll(it)
        }

        val process = try {
            builder.start()
        } catch (e: Throwable) {
            LOGGER.e(e, "can't start %s", request.argv.joinToString(" "))
            writeExit(clientOut, 127)
            return
        }

        val stdinPump = Thread({ pumpClientToProcess(clientIn, process.outputStream) }, "axsh-stdin")
            .apply { isDaemon = true }
        stdinPump.start()

        pumpProcessToClient(process.inputStream, clientOut)
        val code = try {
            process.waitFor()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            process.destroy()
            -1
        }
        writeExit(clientOut, code)
    }

    private fun pumpClientToProcess(input: InputStream, processOut: OutputStream) {
        try {
            loop@ while (true) {
                val type = input.read()
                if (type < 0) break

                when (type) {
                    FRAME_DATA -> {
                        val len = readU32(input) ?: break@loop
                        if (!copyN(input, processOut, len)) break@loop
                    }
                    FRAME_WINCH -> {
                        if (!readFully(input, ByteArray(4))) break@loop
                    }
                    else -> break@loop
                }
            }
        } catch (_: Throwable) {
        } finally {
            try {
                processOut.close()
            } catch (_: Throwable) {
            }
        }
    }

    private fun pumpProcessToClient(processIn: InputStream, clientOut: OutputStream) {
        val buf = ByteArray(BUFFER_SIZE)
        try {
            while (true) {
                val n = processIn.read(buf)
                if (n < 0) break
                writeFrame(clientOut, FRAME_DATA, buf, n)
            }
        } catch (_: Throwable) {
        }
    }

    // ---- wire helpers ----

    private fun readRequest(input: InputStream): Request? {
        val magic = ByteArray(4)
        if (!readFully(input, magic)) return null
        if (magic[0].toInt() != 'A'.code || magic[1].toInt() != 'X'.code ||
            magic[2].toInt() != 'S'.code || magic[3].toInt() != 'H'.code
        ) return null

        val version = input.read()
        val flags = input.read()
        if (version != VERSION || flags < 0) return null

        val argc = readU32(input) ?: return null
        if (argc <= 0 || argc > MAX_ARGS) return null

        val argv = ArrayList<String>(argc)
        for (i in 0 until argc) {
            argv.add(readString(input) ?: return null)
        }

        // Environment is intentionally ignored: the server owns the Axeron environment.
        val envc = readU32(input) ?: return null
        if (envc < 0 || envc > MAX_ARGS) return null
        for (i in 0 until envc) {
            if (readString(input) == null) return null
        }

        return Request(argv, flags)
    }

    private fun writeStatus(output: OutputStream, status: Int, message: String?) {
        output.write(status)
        if (message != null) {
            val bytes = message.toByteArray(Charsets.UTF_8)
            output.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(bytes.size).array())
            output.write(bytes)
        }
        output.flush()
    }

    private fun writeFrame(output: OutputStream, type: Int, data: ByteArray, length: Int) {
        val header = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
        header.put(type.toByte())
        header.putInt(length)
        output.write(header.array())
        if (length > 0) output.write(data, 0, length)
        output.flush()
    }

    private fun writeExit(output: OutputStream, code: Int) {
        val frame = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
        frame.put(FRAME_EXIT.toByte())
        frame.putInt(code)
        output.write(frame.array())
        output.flush()
    }

    private fun readU32(input: InputStream): Int? {
        val b = ByteArray(4)
        if (!readFully(input, b)) return null
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun readString(input: InputStream): String? {
        val len = readU32(input) ?: return null
        if (len < 0 || len > MAX_STRING) return null
        if (len == 0) return ""
        val b = ByteArray(len)
        if (!readFully(input, b)) return null
        return String(b, Charsets.UTF_8)
    }

    private fun copyN(input: InputStream, output: OutputStream, length: Int): Boolean {
        val buf = ByteArray(BUFFER_SIZE)
        var remaining = length
        while (remaining > 0) {
            val n = input.read(buf, 0, minOf(buf.size, remaining))
            if (n < 0) return false
            output.write(buf, 0, n)
            remaining -= n
        }
        output.flush()
        return true
    }

    private fun readFully(input: InputStream, buf: ByteArray): Boolean {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n < 0) return false
            off += n
        }
        return true
    }

    private fun peerUid(socket: LocalSocket): Int = try {
        socket.peerCredentials.uid
    } catch (_: Throwable) {
        -1
    }

    private fun peerPid(socket: LocalSocket): Int = try {
        socket.peerCredentials.pid
    } catch (_: Throwable) {
        -1
    }
}
