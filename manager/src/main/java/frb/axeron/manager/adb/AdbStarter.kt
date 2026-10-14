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

    /**
     * This app's ADB identity: stored in its preferences, falling back to the copy a previous
     * activation saved in Settings.Global. Shared by wireless, TCP and OTG activation.
     */
    fun adbKeyStore(context: Context) = PreferenceAdbKeyStore(
        AxeronSettings.getPreferences(),
        Settings.Global.getString(context.contentResolver, Starter.KEY_PAIR)
    )

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun startAdbWireless(
        context: Context, result: (AdbStateInfo) -> Unit = {}
    ) {
        Log.d(TAG, "startAdb")

        val tcpPort = AdbEnvironment.getAdbTcpPort()
        if (tcpPort > 0 && !AxeronSettings.getTcpMode()) {
            ActivationLog.info("ADB over TCP is on (port $tcpPort) but TCP mode is off — turning it off first")
            // Internal step only: its "TCP stopped" outcome must not reach [result], or callers
            // take it as the activation result and cancel the activation still in progress.
            stopTcp(context, tcpPort)
        }

        Log.d(TAG, "tcpPort: $tcpPort")

        val port = runCatching {
            Log.d(TAG, "awaitAdbPort: enter")

            if (!AdbEnvironment.isWifiRequired()) {
                ActivationLog.info("TCP mode is on and adbd listens on port $tcpPort — Wi-Fi not needed")
                return@runCatching tcpPort
            }
            ActivationLog.info("Waiting for Wi-Fi…")

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
                    if (sendResult.isSuccess) ActivationLog.success("Found wireless debugging port $p")

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
                        ActivationLog.success("Wi-Fi is connected")

                        val cr = context.contentResolver
                        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivationLog.info("Turning on wireless debugging (WRITE_SECURE_SETTINGS granted)")
                            Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
                        } else {
                            ActivationLog.warn("No WRITE_SECURE_SETTINGS — wireless debugging must already be on")
                        }

                        Log.d(TAG, "callbackFlow: start AdbWifiGate")

                        AdbWifiGate(
                            context,
                            onReady = {
                                ActivationLog.success("Wireless debugging is on")
                                ActivationLog.info("Looking up the ADB port (mDNS)…")
                                adbMdns.runCatching {
                                    start()
                                }.onFailure {
                                    ActivationLog.error("Couldn't start the mDNS lookup", it)
                                    safeClose(it)
                                }
                            },
                            onFail = {
                                ActivationLog.error("Wireless debugging didn't turn on")
                                safeClose(
                                    IllegalStateException(context.getString(R.string.adb_wifi_failed_to_auto_active))
                                )
                            }
                        ).start()
                    },
                    onFail = {
                        ActivationLog.error("Wi-Fi isn't connected")
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
            ActivationLog.error("Couldn't get the ADB port", it)
            result(AdbStateInfo.Failed(it.message ?: ""))
            return
        }

        Log.d(TAG, "awaitAdbPort: SUCCESS port=$port")


        startAdbClient(context, port, result)
    }

    suspend fun startAdbClient(context: Context, port: Int, result: (AdbStateInfo) -> Unit = {}) {
        if (port <= 0) {
            ActivationLog.error("No valid ADB port")
            result(AdbStateInfo.Failed(context.getString(R.string.adb_failed_to_get_port)))
            return
        }

        val keyStore = adbKeyStore(context)
        val key = runCatching {
            AdbKey(
                keyStore,
                "axeron"
            )
        }
            .getOrElse {
                ActivationLog.error("Couldn't load this app's ADB key", it)
                result(AdbStateInfo.Failed(context.getString(R.string.adb_failed_to_auto_active)))
                return
            }

        var activePort = port
        val tcpMode = AxeronSettings.getTcpMode()
        val tcpPort = AxeronSettings.getTcpPort()
        if (tcpMode && activePort != tcpPort) {
            ActivationLog.info("TCP mode is on — switching adbd to TCP port $tcpPort")

            runCatching {
                withContext(Dispatchers.IO) {
                    AdbClient(key, activePort).use { client ->
                        client.connect()
                        client.command("tcpip:$tcpPort")
                    }
                }
            }.onFailure {
                // adbd restarts and drops the current connection when switching to TCP mode, so
                // EOF/SocketException here are expected; any other failure is fatal.
                if (it !is EOFException && it !is SocketException) {
                    ActivationLog.error("Couldn't switch adbd to TCP", it)
                    result(AdbStateInfo.Failed(context.getString(R.string.adb_failed_to_switch_tcp)))
                    return
                }
            }

            runCatching {
                ActivationLog.info("Waiting for adbd on TCP port $tcpPort…")
                waitTcpReady(context, key, tcpPort)
                activePort = tcpPort
                ActivationLog.success("adbd is listening on TCP port $tcpPort")
            }.onFailure {
                ActivationLog.error("adbd didn't come back on TCP port $tcpPort", it)
                result(AdbStateInfo.Failed(context.getString(R.string.adb_failed_to_switch_tcp)))
                return
            }
        }

        ActivationLog.info("Connecting to adbd on 127.0.0.1:$activePort…")

        runCatching {
            withContext(Dispatchers.IO) {
                AdbClient(
                    key,
                    activePort
                ).use { client ->
                    client.connect()
                    ActivationLog.success("Connected to adbd")
                    ActivationLog.info("Running the AxManager starter…")
                    // The starter's own output is what explains a failed start.
                    client.shellCommand(Starter.internalAdbCommand(keyStore.getBase64())) { chunk ->
                        ActivationLog.output(String(chunk, Charsets.UTF_8))
                    }
                }
            }
        }.onSuccess {
            ActivationLog.success("Starter finished")
            AxeronSettings.setLastLaunchMode(AxeronSettings.LaunchMethod.ADB)
            result(AdbStateInfo.Success(context.getString(R.string.adb_client_connected)))
        }.onFailure {
            ActivationLog.error("ADB connection failed", it)
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

            ActivationLog.info("Switching adbd from TCP port $port back to USB…")
            val key = AdbKey(adbKeyStore(context), "axeron")
            waitTcpReady(context, key, port)
            withContext(Dispatchers.IO) {
                AdbClient(key, port).use { client ->
                    client.connect()
                    // adbd restarts on "usb:" and drops this connection, which is expected —
                    // it used to surface as a failure even though TCP did stop.
                    try {
                        client.command("usb:")
                    } catch (e: EOFException) {
                    } catch (e: SocketException) {
                    }
                }
            }
        }.onSuccess {
            ActivationLog.success("ADB over TCP stopped")
            result(AdbStateInfo.Success(context.getString(R.string.adb_tcp_stopped)))
        }.onFailure {
            ActivationLog.error("Couldn't stop ADB over TCP", it)
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