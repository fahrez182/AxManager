package frb.axeron.manager.adb

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import frb.axeron.adb.AdbClient
import frb.axeron.adb.AdbKey
import frb.axeron.adb.AdbMdns
import frb.axeron.adb.PreferenceAdbKeyStore
import frb.axeron.adb.util.AdbEnvironment
import frb.axeron.adb.util.AdbWifiGate
import frb.axeron.adb.util.WifiReadyGate
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

object AdbStarter {
    private const val TAG = "AdbStarter"

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun startAdbWireless(
        context: Context, result: (AdbStateInfo) -> Unit = {}
    ) {
        Log.d(TAG, "startAdb")

        val tcpPort = AdbEnvironment.getAdbTcpPort()
        if (tcpPort > 0 && !AxeronSettings.getTcpMode()) {
            stopTcp(context, tcpPort, result)
        }

        Log.d(TAG, "tcpPort: $tcpPort")

        val port = runCatching {
            Log.d(TAG, "awaitAdbPort: enter")

            if (!AdbEnvironment.isWifiRequired()) {
                Log.d(TAG, "awaitAdbPort: wifi NOT required, use tcpPort=$tcpPort")
                return@runCatching tcpPort
            }

            callbackFlow {
                Log.d(TAG, "callbackFlow: start")

                val closed = AtomicBoolean(false)

                fun safeClose(cause: Throwable? = null) {
                    if (closed.compareAndSet(false, true)) {
                        if (cause != null) {
                            Log.e(TAG, "callbackFlow: closing with error", cause)
                            close(cause)
                        } else {
                            Log.d(TAG, "callbackFlow: closing normally")
                            close()
                        }
                    } else {
                        Log.w(TAG, "callbackFlow: safeClose called but already closed")
                    }
                }

                val adbMdns = AdbMdns(context, AdbMdns.TLS_CONNECT) { p ->
                    Log.d(TAG, "AdbMdns callback: port=$p")

                    if (p <= 0) {
                        Log.e(TAG, "AdbMdns callback: invalid port")
                        safeClose(IllegalStateException(context.getString(R.string.adb_failed_to_get_port)))
                        return@AdbMdns
                    }

                    val sendResult = trySend(p)
                    Log.d(
                        TAG,
                        "callbackFlow: trySend($p) success=${sendResult.isSuccess}"
                    )

                    if (!sendResult.isSuccess) {
                        safeClose(
                            IllegalStateException(context.getString(R.string.failed_to_emit_port))
                        )
                        return@AdbMdns
                    }

                    safeClose()
                }

                Log.d(TAG, "callbackFlow: start WifiReadyGate")

                WifiReadyGate(
                    context,
                    onReady = {
                        Log.d(TAG, "WifiReadyGate: onReady")

                        val cr = context.contentResolver
                        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.d(TAG, "WRITE_SECURE_SETTINGS granted, enabling adb")
                            Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
                        } else {
                            Log.w(TAG, "WRITE_SECURE_SETTINGS NOT granted")
                        }

                        Log.d(TAG, "callbackFlow: start AdbWifiGate")

                        AdbWifiGate(
                            context,
                            onReady = {
                                Log.d(TAG, "AdbWifiGate: onReady")
                                adbMdns.runCatching {
                                    Log.d(TAG, "AdbMdns: start()")
                                    start()
                                }.onFailure {
                                    Log.e(TAG, "AdbMdns: start failed", it)
                                    safeClose(it)
                                }
                            },
                            onFail = {
                                Log.e(TAG, "AdbWifiGate: onFail")
                                safeClose(
                                    IllegalStateException(context.getString(R.string.adb_wifi_failed_to_auto_active))
                                )
                            }
                        ).start()
                    },
                    onFail = {
                        Log.e(TAG, "WifiReadyGate: onFail")
                        safeClose(
                            IllegalStateException(context.getString(R.string.wifi_failed_to_connect))
                        )
                    }
                ).start()

                awaitClose {
                    Log.d(TAG, "callbackFlow: awaitClose -> stop mdns")
                    adbMdns.stop()
                }
            }.first().also {
                Log.d(TAG, "callbackFlow: first() received port=$it")
            }
        }.getOrElse {
            Log.e(TAG, "awaitAdbPort: FAILED", it)
            result(AdbStateInfo.Failed(it.message ?: ""))
            return
        }

        Log.d(TAG, "awaitAdbPort: SUCCESS port=$port")


        startAdbClient(context, port, result)
    }

    suspend fun startAdbClient(context: Context, port: Int, result: (AdbStateInfo) -> Unit = {}) {
        if (port <= 0) {
            result(AdbStateInfo.Failed(context.getString(R.string.adb_failed_to_get_port)))
            return
        }

        val keyStore = PreferenceAdbKeyStore(
            AxeronSettings.getPreferences(),
            Settings.Global.getString(context.contentResolver, Starter.KEY_PAIR)
        )
        val key = runCatching {
            AdbKey(
                keyStore,
                "axeron"
            )
        }
            .getOrElse {
                result(AdbStateInfo.Failed(context.getString(R.string.adb_failed_to_auto_active)))
                return
            }

        var activePort = port
        val tcpMode = AxeronSettings.getTcpMode()
        val tcpPort = AxeronSettings.getTcpPort()
        if (tcpMode && activePort != tcpPort) {
            Log.d(TAG, "Try to switching to TCP mode")
            Log.d(TAG, "Connecting on port $activePort...")

            runCatching {
                withContext(Dispatchers.IO) {
                    AdbClient(key, activePort).use { client ->
                        client.connect()
                        Log.d(TAG, "Restarting in TCP mode port: $tcpPort")
                        client.command("tcpip:$tcpPort")
                    }
                }

                waitTcpReady(context, key, tcpPort)
                activePort = tcpPort
            }.onFailure {
                if (it !is EOFException && it !is SocketException) {
                    result(AdbStateInfo.Failed(context.getString(R.string.adb_failed_to_switch_tcp)))
                }
                return
            }
        }

        Log.d(
            TAG,
            "Connecting on port $activePort..."
        )

        runCatching {
            withContext(Dispatchers.IO) {
                AdbClient(
                    key,
                    activePort
                ).use { client ->
                    Log.d(
                        TAG,
                        "AdbClient running"
                    )
                    client.connect()
                    client.shellCommand(Starter.internalAdbCommand(keyStore.getBase64()))
                }
            }
        }.onSuccess {
            Log.d(
                TAG,
                "AdbClient success"
            )
            AxeronSettings.setLastLaunchMode(AxeronSettings.LaunchMethod.ADB)
            result(AdbStateInfo.Success(context.getString(R.string.adb_client_connected)))
        }.onFailure {
            Log.e(
                TAG,
                "AdbClient failed",
                it
            )
            result(AdbStateInfo.Failed(context.getString(R.string.failed_to_connect_adb_client)))
        }
    }

    suspend fun stopTcp(context: Context, port: Int, result: (AdbStateInfo) -> Unit = {}) {
        runCatching {
            val cr = context.contentResolver
            if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
            }

            val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0)
            if (adbEnabled == 0) throw IllegalStateException(context.getString(R.string.adb_is_not_enabled))

            val keyStore = PreferenceAdbKeyStore(
                AxeronSettings.getPreferences(),
                Settings.Global.getString(context.contentResolver, Starter.KEY_PAIR)
            )
            val key = AdbKey(keyStore, "axeron")
            waitTcpReady(context, key, port)
            withContext(Dispatchers.IO) {
                AdbClient(key, port).use { client ->
                    client.connect()
                    client.command("usb:")
                }
            }
        }.onSuccess {
            Log.d(TAG, "Stop TCP success")
            result(AdbStateInfo.Success(context.getString(R.string.adb_tcp_stopped)))
        }.onFailure {
            Log.e(TAG, "Stop TCP failed", it)
            result(AdbStateInfo.Failed(context.getString(R.string.failed_to_stop_adb_tcp) + ": " + it.message))
        }
    }

    suspend fun waitTcpReady(
        context: Context,
        key: AdbKey,
        port: Int,
        timeoutMs: Long = 5_000,
        intervalMs: Long = 200
    ) {
        val start = System.currentTimeMillis()
        var lastError: Throwable? = null

        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                withContext(Dispatchers.IO) {
                    AdbClient(key, port).use { it.connect() }
                }
                return // SUCCESS
            } catch (e: Throwable) {
                lastError = e
                delay(intervalMs) // ✅ cooperative
            }
        }

        throw IllegalStateException(
            context.getString(R.string.adb_tcp_not_ready, port),
            lastError
        ) as Throwable
    }


}


sealed class AdbStateInfo(val message: String, val cause: Throwable? = null) {
    class Success(message: String) : AdbStateInfo(message)

    class Process(message: String, cause: Throwable? = null) :
        AdbStateInfo(message, cause)

    class Failed(message: String, cause: Throwable? = null) :
        AdbStateInfo(message, cause)
}