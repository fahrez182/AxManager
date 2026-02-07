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
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rikka.core.util.BuildUtils
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.net.ssl.SSLSocket

private const val TAG = "AdbClient"

class AdbClient(private val key: AdbKey, private val port: Int, private val host: String = "127.0.0.1") : Closeable {

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isClosed = false
    private var socket: Socket? = null
    private var plainInputStream: DataInputStream? = null
    private var plainOutputStream: DataOutputStream? = null

    private var useTls = false

    private var tlsSocket: SSLSocket? = null
    private var tlsInputStream: DataInputStream? = null
    private var tlsOutputStream: DataOutputStream? = null

    private val inputStream get() = if (useTls) tlsInputStream ?: throw AdbException("TLS InputStream not initialized")
                              else plainInputStream ?: throw AdbException("Plain InputStream not initialized")
    private val outputStream get() = if (useTls) tlsOutputStream ?: throw AdbException("TLS OutputStream not initialized")
                               else plainOutputStream ?: throw AdbException("Plain OutputStream not initialized")

    private var localIdCounter = 1
    private var shellLocalId = -1
    private var shellRemoteId = -1

    private val _shellOutput = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    val shellOutput: SharedFlow<ByteArray> = _shellOutput

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    var onConnectionChanged: ((String) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(_connectionStatus.value)
        }

    private val writeMutex = Mutex()

    suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            updateStatus("Connecting...")
            useTls = false
            val s = Socket()
            socket = s
            s.connect(InetSocketAddress(host, port), 5000)
            s.tcpNoDelay = true

            val pins = DataInputStream(s.getInputStream())
            val pouts = DataOutputStream(s.getOutputStream())
            plainInputStream = pins
            plainOutputStream = pouts

            write(A_CNXN, A_VERSION, A_MAXDATA, "host::")

            var message = read()
            if (message.command == A_STLS) {
                if (!BuildUtils.atLeast29) {
                    throw AdbException("Connect to adb with TLS is not supported before Android 9")
                }
                write(A_STLS, A_STLS_VERSION, 0)

                val sslContext = key.sslContext
                val ts = sslContext.socketFactory.createSocket(s, host, port, true) as SSLSocket
                tlsSocket = ts
                ts.startHandshake()
                Log.d(TAG, "Handshake succeeded.")

                tlsInputStream = DataInputStream(ts.inputStream)
                tlsOutputStream = DataOutputStream(ts.outputStream)
                useTls = true

                message = read()
            } else if (message.command == A_AUTH) {
                if (message.arg0 != ADB_AUTH_TOKEN) throw AdbException("not A_AUTH ADB_AUTH_TOKEN")
                write(A_AUTH, ADB_AUTH_SIGNATURE, 0, key.sign(message.data))

                message = read()
                if (message.command != A_CNXN) {
                    write(A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, key.adbPublicKey)
                    message = read()
                }
            }

            if (message.command != A_CNXN) throw AdbException("not A_CNXN")

            updateStatus("Connected")
        } catch (e: Exception) {
            updateStatus("Failed: ${e.message}")
            throw e
        }
    }

    private fun updateStatus(status: String) {
        _connectionStatus.value = status
        onConnectionChanged?.invoke(status)
    }

    suspend fun shellCommand(cmd: String, listener: ((ByteArray) -> Unit)? = null) {
        command("shell:$cmd", listener)
    }

    fun startShell(autoReconnect: Boolean = true, listener: ((ByteArray) -> Unit)? = null) {
        if (shellLocalId != -1) return

        clientScope.launch {
            openShell(autoReconnect, listener)
        }
    }

    private suspend fun openShell(autoReconnect: Boolean, listener: ((ByteArray) -> Unit)?) {
        shellLocalId = localIdCounter++
        try {
            write(A_OPEN, shellLocalId, 0, "shell:")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open shell", e)
            if (autoReconnect) retryReconnect(listener)
            return
        }

        try {
            while (shellLocalId != -1 && !isClosed && coroutineContext.isActive) {
                val message = try {
                    read()
                } catch (e: Exception) {
                    Log.e(TAG, "Read error, handling disconnect", e)
                    break
                }
                when (message.command) {
                    A_OKAY -> {
                        shellRemoteId = message.arg0
                        Log.d(TAG, "Shell opened: local=$shellLocalId, remote=$shellRemoteId")
                    }
                    A_WRTE -> {
                        if (message.data_length > 0) {
                            val data = message.data!!
                            listener?.invoke(data)
                            _shellOutput.emit(data)
                        }
                        write(A_OKAY, shellLocalId, shellRemoteId)
                    }
                    A_CLSE -> {
                        Log.d(TAG, "Shell closed by remote")
                        val remoteId = message.arg0
                        write(A_CLSE, shellLocalId, remoteId)
                        shellLocalId = -1
                        shellRemoteId = -1
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shell coroutine error", e)
        } finally {
            shellLocalId = -1
            shellRemoteId = -1
            if (autoReconnect && !isClosed && coroutineContext.isActive) {
                retryReconnect(listener)
            }
        }
    }

    private suspend fun retryReconnect(listener: ((ByteArray) -> Unit)?) {
        if (isClosed || !coroutineContext.isActive) return

        updateStatus("Reconnecting...")
        delay(2000)
        if (isClosed || !coroutineContext.isActive) return

        try {
            connect()
            openShell(true, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Reconnect failed", e)
            if (!isClosed && coroutineContext.isActive) retryReconnect(listener)
        }
    }

    fun sendShellCommand(cmd: String) {
        if (shellLocalId == -1 || shellRemoteId == -1) return
        writeAsync(AdbMessage(A_WRTE, shellLocalId, shellRemoteId, cmd + "\n"))
    }

    fun sendShellRaw(data: ByteArray) {
        if (shellLocalId == -1 || shellRemoteId == -1) return
        writeAsync(AdbMessage(A_WRTE, shellLocalId, shellRemoteId, data))
    }

    private fun writeAsync(message: AdbMessage) {
        clientScope.launch {
            writeMutex.withLock {
                try {
                    val data = message.toByteArray()
                    withContext(Dispatchers.IO) {
                        outputStream.write(data)
                        outputStream.flush()
                    }
                    Log.d(TAG, "write async ${message.toStringShort()}")
                } catch (e: Exception) {
                    Log.e(TAG, "Write async failed", e)
                }
            }
        }
    }

    suspend fun command(cmd: String, listener: ((ByteArray) -> Unit)? = null) = withContext(Dispatchers.IO) {
        val localId = localIdCounter++
        write(A_OPEN, localId, 0, cmd)

        var message = read()
        when (message.command) {
            A_OKAY -> {
                while (true) {
                    message = read()
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
                        throw AdbException("not A_WRTE or A_CLSE")
                    }
                }
            }
            A_CLSE -> {
                val remoteId = message.arg0
                write(A_CLSE, localId, remoteId)
            }
            else -> {
                throw AdbException("not A_OKAY or A_CLSE")
            }
        }
    }

    private suspend fun write(command: Int, arg0: Int, arg1: Int, data: ByteArray? = null) = write(AdbMessage(command, arg0, arg1, data))

    private suspend fun write(command: Int, arg0: Int, arg1: Int, data: String) = write(AdbMessage(command, arg0, arg1, data))

    private suspend fun write(message: AdbMessage) = writeMutex.withLock {
        withContext(Dispatchers.IO) {
            outputStream.write(message.toByteArray())
            outputStream.flush()
        }
        Log.d(TAG, "write ${message.toStringShort()}")
    }

    private suspend fun read(): AdbMessage = withContext(Dispatchers.IO) {
        val headerBuffer = ByteArray(24)
        inputStream.readFully(headerBuffer, 0, 24)

        val buffer = ByteBuffer.wrap(headerBuffer).order(ByteOrder.LITTLE_ENDIAN)
        val command = buffer.int
        val arg0 = buffer.int
        val arg1 = buffer.int
        val dataLength = buffer.int
        val checksum = buffer.int
        val magic = buffer.int

        val data: ByteArray?
        if (dataLength > 0) {
            data = ByteArray(dataLength)
            inputStream.readFully(data, 0, dataLength)
        } else {
            data = null
        }
        val message = AdbMessage(command, arg0, arg1, dataLength, checksum, magic, data)
        message.validateOrThrow()
        Log.d(TAG, "read ${message.toStringShort()}")
        message
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        clientScope.cancel()

        try { plainInputStream?.close() } catch (e: Throwable) {}
        try { plainOutputStream?.close() } catch (e: Throwable) {}
        try { socket?.close() } catch (e: Exception) {}

        try { tlsInputStream?.close() } catch (e: Throwable) {}
        try { tlsOutputStream?.close() } catch (e: Throwable) {}
        try { tlsSocket?.close() } catch (e: Exception) {}

        updateStatus("Disconnected")
    }
}
