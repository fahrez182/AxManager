package frb.axeron.manager.adb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import frb.axeron.adb.AdbClient
import frb.axeron.adb.AdbKey
import frb.axeron.adb.UsbAdbTransport
import frb.axeron.manager.AxManagerProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * OTG activation (ported from AXGC): this phone acts as the "computer". It finds another phone's
 * adbd over a USB OTG cable, authenticates with this app's ADB key (the target shows "Allow USB
 * debugging?" once), then runs AxManager's own starter *on the target* — the same thing
 * "Activate by computer" does with `adb shell`. The target needs AxManager installed and USB
 * debugging on; it doesn't have to do anything else.
 *
 * Unlike AXGC this doesn't write our private ADB key into the target's Settings.Global: any app
 * there can read it, and the one-step activation doesn't need it.
 */
object OtgActivator {

    private const val TAG = "OtgActivator"

    // adbd's USB interface: https://android.googlesource.com/platform/packages/modules/adb/+/main/daemon/usb.cpp
    private const val ADB_CLASS = 0xFF
    private const val ADB_SUBCLASS = 0x42
    private const val ADB_PROTOCOL = 0x01

    // Markers printed by the target-side script so the result can be told apart from starter logs.
    private const val MARK_NOT_INSTALLED = "AX_OTG_NOT_INSTALLED"
    private const val MARK_NO_STARTER = "AX_OTG_NO_STARTER"
    private const val MARK_EXIT = "AX_OTG_EXIT:"
    private const val MARK_TCP_ACTIVE = "AX_OTG_TCP_ACTIVE="

    sealed interface State {
        data object Idle : State
        data object Scanning : State
        data object NoUsbHost : State
        data object NoDevice : State
        data class DeviceFound(val name: String, val vendor: String, val device: UsbDevice) : State
        data object Connecting : State
        data object Activating : State
        data object Success : State
        data class Failed(val reason: Reason) : State
    }

    enum class Reason { PERMISSION_DENIED, CONNECTION_FAILED, NOT_INSTALLED, START_FAILED }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _log = MutableStateFlow<List<ActivationLog.Line>>(emptyList())

    /** Step-by-step trace for the UI. */
    val log: StateFlow<List<ActivationLog.Line>> = _log.asStateFlow()

    val isBusy: Boolean
        get() = _state.value.let { it is State.Connecting || it is State.Activating }

    private fun log(line: String, level: ActivationLog.Level = ActivationLog.Level.INFO) {
        Log.d(TAG, line)
        _log.update { (it + ActivationLog.Line(System.currentTimeMillis(), level, line)).takeLast(200) }
    }

    fun isUsbHostSupported(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

    fun reset() {
        if (isBusy) return
        _log.value = emptyList()
        _state.value = State.Idle
    }

    /**
     * Looks for an attached device exposing adbd's USB interface. Safe to call on hot-plug:
     * unless [force]d (the user's "Scan again"), a finished success is kept — the target's adbd
     * re-enumerating (e.g. after `tcpip:`) or unplugging afterwards must not wipe the result.
     */
    fun scan(context: Context, force: Boolean = false) {
        if (isBusy) return
        if (!force && _state.value is State.Success) return
        _log.value = emptyList()
        _state.value = State.Scanning

        if (!isUsbHostSupported(context)) {
            log("This phone doesn't support USB host (OTG)", ActivationLog.Level.ERROR)
            _state.value = State.NoUsbHost
            return
        }

        val usbManager = context.getSystemService(UsbManager::class.java)
        val attached = usbManager?.deviceList?.values.orEmpty()
        // Tell "nothing on the bus" apart from "something there, but no adbd" — different fixes.
        attached.forEach { log("Attached: ${it.describe()}") }
        val device = attached.firstOrNull { it.adbInterface() != null }
        if (device == null) {
            log(
                if (attached.isEmpty()) "No USB device attached — check the OTG adapter/cable, and the OTG switch in this phone's settings if it has one"
                else "No attached device exposes USB debugging — turn it on in the other phone's Developer options",
                ActivationLog.Level.WARN
            )
            _state.value = State.NoDevice
            return
        }

        val name = device.productName ?: device.deviceName
        val vendor = device.manufacturerName.orEmpty()
        log("Found $vendor $name (${"%04X:%04X".format(device.vendorId, device.productId)})", ActivationLog.Level.SUCCESS)
        _state.value = State.DeviceFound(name, vendor, device)
    }

    /** Outlives the UI: closing the sheet mid-handshake must not strand the state. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** USB permission → ADB handshake → run AxManager's starter on the target. */
    fun activate(context: Context, device: UsbDevice) {
        if (isBusy) return
        _state.value = State.Connecting
        scope.launch { runActivation(context.applicationContext, device) }
    }

    private suspend fun runActivation(context: Context, device: UsbDevice) = withContext(Dispatchers.IO) {

        val usbManager = context.getSystemService(UsbManager::class.java)
        if (!usbManager.hasPermission(device)) {
            log("Asking for USB access on this phone…")
            if (requestPermission(context, usbManager, device) != true) {
                log("USB access was not granted", ActivationLog.Level.ERROR)
                _state.value = State.Failed(Reason.PERMISSION_DENIED)
                return@withContext
            }
        }

        val adbInterface = device.adbInterface()
        val connection = usbManager.openDevice(device)
        if (adbInterface == null || connection == null) {
            connection?.close()
            log("Couldn't open the device — was the cable unplugged?", ActivationLog.Level.ERROR)
            _state.value = State.Failed(Reason.CONNECTION_FAILED)
            return@withContext
        }

        val endpoints = (0 until adbInterface.endpointCount).map { adbInterface.getEndpoint(it) }
            .filter { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK }
        val bulkIn = endpoints.firstOrNull { it.direction == UsbConstants.USB_DIR_IN }
        val bulkOut = endpoints.firstOrNull { it.direction == UsbConstants.USB_DIR_OUT }
        if (bulkIn == null || bulkOut == null || !connection.claimInterface(adbInterface, true)) {
            connection.close()
            log("Couldn't claim the device's debugging interface", ActivationLog.Level.ERROR)
            _state.value = State.Failed(Reason.CONNECTION_FAILED)
            return@withContext
        }

        // One transport for the whole run: a second ADB session after a broken one reuses it.
        val transport = UsbAdbTransport(connection, bulkIn, bulkOut)
        val key = runCatching { AdbKey(AdbStarter.adbKeyStore(context), "axeron") }.getOrElse {
            transport.close()
            log("Couldn't load this app's ADB key: ${it.message}", ActivationLog.Level.ERROR)
            _state.value = State.Failed(Reason.CONNECTION_FAILED)
            return@withContext
        }
        fun connectClient() = AdbClient(key, transport).apply {
            usbHandshakeLog = { log(it) }
            connect()
        }

        val output = StringBuilder()
        var client: AdbClient? = null
        val result = runCatching {
            val session = connectClient().also { client = it }

            _state.value = State.Activating
            log("Starting AxManager on the target…")
            session.shellCommand(targetStartScript(context.packageName)) { chunk ->
                val text = String(chunk, Charsets.UTF_8)
                output.append(text)
                text.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("AX_OTG_") }
                    .forEach { log(it, ActivationLog.Level.OUTPUT) }
            }
        }

        // The starter's exit marker decides, not the link: it can break after the starter
        // finished (a lost close), and the target is active then all the same.
        val text = output.toString()
        if (starterSucceeded(text)) {
            runCatching {
                val session = if (result.isSuccess) client!! else {
                    log("The link broke after the starter finished — reconnecting for the TCP step", ActivationLog.Level.WARN)
                    connectClient()
                }
                // TCP/IP follows the *target's* own TCP mode.
                applyTargetTcpMode(session, context.packageName)
            }.onFailure {
                log("Couldn't apply the target's TCP mode: ${it.message}", ActivationLog.Level.WARN)
            }
        }
        transport.close()

        _state.value = when {
            MARK_NOT_INSTALLED in text || MARK_NO_STARTER in text -> {
                log("AxManager isn't installed on the target", ActivationLog.Level.ERROR)
                State.Failed(Reason.NOT_INSTALLED)
            }

            starterSucceeded(text) -> {
                log("AxManager is now active on the target", ActivationLog.Level.SUCCESS)
                State.Success
            }

            result.isFailure -> {
                Log.w(TAG, "OTG activation failed", result.exceptionOrNull())
                log("Connection lost: ${result.exceptionOrNull()?.message ?: "unknown error"}", ActivationLog.Level.ERROR)
                // Gone from the bus = the target dropped the link (e.g. adbd restarted); still here = host-side error.
                log("Target still attached: ${usbManager.deviceList.containsKey(device.deviceName)}")
                State.Failed(Reason.CONNECTION_FAILED)
            }

            else -> {
                log("The starter didn't finish successfully", ActivationLog.Level.ERROR)
                State.Failed(Reason.START_FAILED)
            }
        }
    }

    /** Ends the `tcpip:` read once adbd has said it's restarting. */
    private class TcpSwitchAcknowledged : Exception()

    private fun starterSucceeded(output: String): Boolean =
        MARK_NOT_INSTALLED !in output && MARK_NO_STARTER !in output &&
                output.substringAfter(MARK_EXIT, "").trim().startsWith("0")

    /**
     * Reads the target's TCP mode from its AxManager (AxManagerProvider answers adb's shell) and,
     * if it's on and adbd isn't on that port yet, switches the target's adbd to TCP on the
     * target's own port. The server we just started has detached from adbd, so the adbd restart
     * this causes doesn't touch it.
     */
    private fun applyTargetTcpMode(client: AdbClient, packageName: String) {
        val reply = StringBuilder()
        runCatching {
            client.shellCommand(
                "content call --uri content://$packageName.server " +
                        "--method ${AxManagerProvider.METHOD_GET_TCP_CONFIG} 2>&1; " +
                        "echo $MARK_TCP_ACTIVE\$(getprop service.adb.tcp.port)"
            ) { reply.append(String(it, Charsets.UTF_8)) }
        }

        val text = reply.toString()
        val mode = Regex("${AxManagerProvider.KEY_TCP_MODE}=(true|false)").find(text)?.groupValues?.get(1)
        val port = Regex("${AxManagerProvider.KEY_TCP_PORT}=(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val activePort = Regex("$MARK_TCP_ACTIVE(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
        log("Target: TCP mode ${mode ?: "?"}, configured port ${port ?: "?"}, adbd on TCP port ${activePort ?: "none"}")

        when {
            mode == null -> {
                log("Couldn't read the target's TCP mode — leaving its ADB on USB", ActivationLog.Level.WARN)
                return
            }

            mode != "true" -> {
                log("Target's TCP mode is off — leaving its ADB on USB")
                return
            }

            port == null || port !in 1024..65535 -> {
                log("Target's TCP port isn't valid ($port) — leaving its ADB on USB", ActivationLog.Level.WARN)
                return
            }

            activePort == port -> {
                log("Target's ADB already listens on TCP port $port", ActivationLog.Level.SUCCESS)
                return
            }
        }

        log(
            if (activePort != null) "Target's adbd is on TCP port $activePort, configured $port — switching it to $port…"
            else "Target's TCP mode is on — switching its ADB to TCP port $port…"
        )
        val ack = StringBuilder()
        try {
            client.command("tcpip:$port") {
                ack.append(String(it, Charsets.UTF_8))
                // adbd restarts right after this and drops USB for a moment: no CLSE will come,
                // so stop reading now instead of waiting on a link that's gone.
                if (ack.contains("restarting", ignoreCase = true)) throw TcpSwitchAcknowledged()
            }
        } catch (e: Exception) {
            // The acknowledgement above, or the link dropping as adbd restarts — both expected.
        }
        if (ack.contains("restarting", ignoreCase = true)) {
            log("Target's ADB now listens on TCP port $port", ActivationLog.Level.SUCCESS)
        } else {
            log("Couldn't confirm the switch to TCP port $port", ActivationLog.Level.WARN)
        }
    }

    /**
     * Runs on the target over adb shell: locates AxManager's APK and its extracted starter by
     * package name, then launches it exactly like `adb shell <libaxeron.so> --apk=…` from a PC.
     */
    private fun targetStartScript(packageName: String): String =
        "apk=\$(pm path $packageName 2>/dev/null | head -n 1 | cut -d: -f2); " +
                "if [ -z \"\$apk\" ]; then echo $MARK_NOT_INSTALLED; exit 0; fi; " +
                "bin=\$(ls \"\${apk%/*}\"/lib/*/libaxeron.so 2>/dev/null | head -n 1); " +
                "if [ -z \"\$bin\" ]; then echo $MARK_NO_STARTER; exit 0; fi; " +
                "\"\$bin\" --apk=\"\$apk\"; echo $MARK_EXIT\$?"

    private suspend fun requestPermission(
        context: Context,
        usbManager: UsbManager,
        device: UsbDevice
    ): Boolean? = withTimeoutOrNull(60_000L) {
        suspendCancellableCoroutine { continuation ->
            val action = "${context.packageName}.OTG_USB_PERMISSION"
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action != action) return
                    runCatching { ctx.unregisterReceiver(this) }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (continuation.isActive) continuation.resume(granted)
                }
            }
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED
            )
            continuation.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }

            // Must be mutable: UsbManager adds EXTRA_PERMISSION_GRANTED to the intent.
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pending = PendingIntent.getBroadcast(
                context, 0, Intent(action).setPackage(context.packageName), flags
            )
            usbManager.requestPermission(device, pending)
        }
    }

    /** "Vendor Product (VID:PID) [class/subclass/protocol, …]" — interfaces in hex. */
    private fun UsbDevice.describe(): String {
        val interfaces = (0 until interfaceCount).map { getInterface(it) }.joinToString {
            "%02X/%02X/%02X".format(it.interfaceClass, it.interfaceSubclass, it.interfaceProtocol)
        }
        val name = listOfNotNull(manufacturerName, productName ?: deviceName).joinToString(" ")
        return "$name (${"%04X:%04X".format(vendorId, productId)}) [$interfaces]"
    }

    private fun UsbDevice.adbInterface(): UsbInterface? =
        (0 until interfaceCount).map { getInterface(it) }.firstOrNull {
            it.interfaceClass == ADB_CLASS &&
                    it.interfaceSubclass == ADB_SUBCLASS &&
                    it.interfaceProtocol == ADB_PROTOCOL
        }
}
