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
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import javax.net.ssl.SSLSocket

private const val TAG = "AdbClient"

class AdbClient(private val key: AdbKey, private val port: Int, private val host: String = "127.0.0.1") : Closeable {

    private val writeExecutor = Executors.newSingleThreadExecutor()
    private lateinit var socket: Socket
    private lateinit var plainInputStream: DataInputStream
    private lateinit var plainOutputStream: DataOutputStream

    private var useTls = false

    private lateinit var tlsSocket: SSLSocket
    private lateinit var tlsInputStream: DataInputStream
    private lateinit var tlsOutputStream: DataOutputStream

    private val inputStream get() = if (useTls) tlsInputStream else plainInputStream
    private val outputStream get() = if (useTls) tlsOutputStream else plainOutputStream

    private var localIdCounter = 1
    private var shellLocalId = -1
    private var shellRemoteId = -1
    private var shellListener: ((ByteArray) -> Unit)? = null
    var onConnectionChanged: ((String) -> Unit)? = null

    fun connect() {
        val socket = Socket()
        val address = InetSocketAddress(host, port)
        socket.connect(address, 5000)

        socket.tcpNoDelay = true
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
            if (message.command != A_AUTH && message.arg0 != ADB_AUTH_TOKEN) error("not A_AUTH ADB_AUTH_TOKEN")
            write(A_AUTH, ADB_AUTH_SIGNATURE, 0, key.sign(message.data))

            message = read()
            if (message.command != A_CNXN) {
                write(A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, key.adbPublicKey)
                message = read()
            }
        }

        if (message.command != A_CNXN) error("not A_CNXN")
    }

    fun shellCommand(cmd: String, listener: ((ByteArray) -> Unit)? = null) {
        command("shell:$cmd", listener)
    }

    @Synchronized
    fun startShell(autoReconnect: Boolean = true, listener: (ByteArray) -> Unit) {
        if (shellLocalId != -1) return
        shellListener = listener
        openShell(autoReconnect)
    }

    private fun openShell(autoReconnect: Boolean) {
        shellLocalId = localIdCounter++
        try {
            write(A_OPEN, shellLocalId, 0, "shell:")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open shell", e)
            if (autoReconnect) retryReconnect(listener = shellListener!!)
            return
        }

        Thread {
            try {
                while (shellLocalId != -1) {
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
                                shellListener?.invoke(message.data!!)
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
                Log.e(TAG, "Shell thread error", e)
            } finally {
                shellLocalId = -1
                shellRemoteId = -1
                if (autoReconnect) {
                    retryReconnect(listener = shellListener!!)
                }
            }
        }.start()
    }

    private fun retryReconnect(listener: (ByteArray) -> Unit) {
        Thread {
            onConnectionChanged?.invoke("Reconnecting...")
            Thread.sleep(2000)
            try {
                connect()
                onConnectionChanged?.invoke("Connected")
                openShell(true)
            } catch (e: Exception) {
                Log.e(TAG, "Reconnect failed", e)
                retryReconnect(listener)
            }
        }.start()
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
        writeExecutor.execute {
            synchronized(this@AdbClient) {
                try {
                    outputStream.write(message.toByteArray())
                    outputStream.flush()
                    Log.d(TAG, "write async ${message.toStringShort()}")
                } catch (e: Exception) {
                    Log.e(TAG, "Write async failed", e)
                }
            }
        }
    }

    @Synchronized
    fun command(cmd: String, listener: ((ByteArray) -> Unit)? = null) {
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

    @Synchronized
    private fun write(command: Int, arg0: Int, arg1: Int, data: ByteArray? = null) = write(AdbMessage(command, arg0, arg1, data))

    @Synchronized
    private fun write(command: Int, arg0: Int, arg1: Int, data: String) = write(AdbMessage(command, arg0, arg1, data))

    @Synchronized
    private fun write(message: AdbMessage) {
        outputStream.write(message.toByteArray())
        outputStream.flush()
        Log.d(TAG, "write ${message.toStringShort()}")
    }

    private fun read(): AdbMessage {
        val buffer = ByteBuffer.allocate(AdbMessage.HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN)

        inputStream.readFully(buffer.array(), 0, 24)

        val command = buffer.int
        val arg0 = buffer.int
        val arg1 = buffer.int
        val dataLength = buffer.int
        val checksum = buffer.int
        val magic = buffer.int
        val data: ByteArray?
        if (dataLength >= 0) {
            data = ByteArray(dataLength)
            inputStream.readFully(data, 0, dataLength)
        } else {
            data = null
        }
        val message = AdbMessage(command, arg0, arg1, dataLength, checksum, magic, data)
        message.validateOrThrow()
        Log.d(TAG, "read ${message.toStringShort()}")
        return message
    }

    override fun close() {
        writeExecutor.shutdownNow()
        try {
            plainInputStream.close()
        } catch (e: Throwable) {
        }
        try {
            plainOutputStream.close()
        } catch (e: Throwable) {
        }
        try {
            socket.close()
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