package frb.axeron.manager.adb

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.ContentResolver
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
import frb.axeron.api.Axeron
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

object AdbStarter {
    private const val TAG = "AdbStarter"

    /**
     * The native starter kills a running axeron_server before starting a new one. Two
     * activations at once (the pairing notification retrying while the UI starts the server)
     * would then race and could leave no server at all, so only one runs the starter at a time.
     */
    private val starterMutex = Mutex()

    /**
     * This app's ADB identity: stored in its preferences, falling back to the copy a previous
     * activation saved in Settings.Global. Shared by wireless, TCP and OTG activation.
     */
    fun adbKeyStore(context: Context) = PreferenceAdbKeyStore(
        AxeronSettings.getPreferences(),
        Settings.Global.getString(context.contentResolver, Starter.KEY_PAIR)
    )

    /**
     * What the Developer options screen toggles, written directly (WRITE_SECURE_SETTINGS), in
     * the order it depends on: developer options, then USB debugging, then wireless debugging.
     * adbd and the settings service watch these values, unlike Wi-Fi's.
     */
    private fun enableAdbSettings(cr: ContentResolver, wireless: Boolean) {
        Settings.Global.putInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)
        Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
        if (wireless) Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
        Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun startAdbWireless(
        context: Context,
        onStep: (ActivationStep) -> Unit = {},
        result: (AdbStateInfo) -> Unit = {}
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

            // Always through wireless debugging, even with adbd already on a TCP port: TCP mode
            // only adds the switch to tcpip once connected (see startAdbClient).
            ActivationLog.info("Waiting for Wi-Fi…")
            onStep(ActivationStep.WIFI)

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

                        onStep(ActivationStep.WIRELESS_DEBUGGING)
                        val cr = context.contentResolver
                        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivationLog.info("Turning on wireless debugging (WRITE_SECURE_SETTINGS granted)")
                            enableAdbSettings(cr, wireless = true)
                        } else {
                            ActivationLog.warn("No WRITE_SECURE_SETTINGS — wireless debugging must already be on")
                        }

                        Log.d(TAG, "callbackFlow: start AdbWifiGate")

                        AdbWifiGate(
                            context,
                            onReady = {
                                ActivationLog.success("Wireless debugging is on")
                                ActivationLog.info("Looking up the ADB port (mDNS)…")
                                onStep(ActivationStep.PORT)
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


        startAdbClient(context, port, onStep, result)
    }

    suspend fun startAdbClient(
        context: Context,
        port: Int,
        onStep: (ActivationStep) -> Unit = {},
        result: (AdbStateInfo) -> Unit = {}
    ) {
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
        // Not activePort: that is the wireless debugging port, a different one every time. What
        // matters is the TCP port adbd listens on now, if any; switching restarts adbd and drops
        // the connection, so it only happens when TCP is off or on another port.
        val liveTcpPort = AdbEnvironment.getAdbTcpPort()
        ActivationLog.info(
            "TCP mode ${if (tcpMode) "on" else "off"}, configured port $tcpPort, adbd on TCP port " +
                    "${if (liveTcpPort > 0) liveTcpPort else "none"}, reached on port $activePort" +
                    when {
                        !tcpMode -> " — no switch"
                        liveTcpPort == tcpPort -> " — already on the configured port"
                        else -> " — switching first"
                    }
        )
        if (tcpMode && liveTcpPort != tcpPort) {
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
        onStep(ActivationStep.CONNECT)

        // The starter kills a running server before starting the new one: one at a time.
        var starterRan = false
        val attempt = starterMutex.withLock {
            runCatching {
                withContext(Dispatchers.IO) {
                    AdbClient(
                        key,
                        activePort
                    ).use { client ->
                        client.connect()
                        ActivationLog.success("Connected to adbd")
                        ActivationLog.info("Running the AxManager starter…")
                        onStep(ActivationStep.STARTER)
                        starterRan = true
                        // The starter's own output is what explains a failed start.
                        client.shellCommand(Starter.internalAdbCommand(keyStore.getBase64())) { chunk ->
                            ActivationLog.output(String(chunk, Charsets.UTF_8))
                        }
                    }
                }
            }
        }
        attempt.onSuccess {
            ActivationLog.success("Starter finished")
            AxeronSettings.setLastLaunchMode(AxeronSettings.LaunchMethod.ADB)
            result(AdbStateInfo.Success(context.getString(R.string.adb_client_connected)))
        }.onFailure {
            // The starter may have started the server and the connection dropped afterwards
            // (adbd restarting, wireless debugging reconnecting). Retrying would run the
            // starter again, and it kills a running server first: keep the one that is up.
            if (starterRan && awaitServer()) {
                ActivationLog.success("Starter ran; the new server is reachable")
                AxeronSettings.setLastLaunchMode(AxeronSettings.LaunchMethod.ADB)
                result(AdbStateInfo.Success(context.getString(R.string.adb_client_connected)))
            } else {
                ActivationLog.error("ADB connection failed", it)
                result(AdbStateInfo.Failed(context.getString(R.string.failed_to_connect_adb_client)))
            }
        }
    }

    /** How long the server started by the starter gets to push its binder. */
    private const val SERVER_WAIT_MS = 5_000L

    private suspend fun awaitServer(): Boolean = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + SERVER_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            if (Axeron.pingBinder()) return@withContext true
            delay(200)
        }
        Axeron.pingBinder()
    }

    suspend fun stopTcp(context: Context, port: Int, result: (AdbStateInfo) -> Unit = {}) {
        runCatching {
            val cr = context.contentResolver
            if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                enableAdbSettings(cr, wireless = false)
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


/** The stages of a wireless activation, in order, for whoever shows its progress. */
enum class ActivationStep { WIFI, WIRELESS_DEBUGGING, PORT, CONNECT, STARTER, SERVER }

sealed class AdbStateInfo(val message: String, val cause: Throwable? = null) {
    class Success(message: String) : AdbStateInfo(message)

    class Process(message: String, cause: Throwable? = null) :
        AdbStateInfo(message, cause)

    class Failed(message: String, cause: Throwable? = null) :
        AdbStateInfo(message, cause)
}