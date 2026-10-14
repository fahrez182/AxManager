package frb.axeron.manager.adb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import frb.axeron.api.Axeron
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The target side of OTG activation: this phone gets activated by another phone running
 * [OtgActivator]. adbd does the actual work here, so this only watches what the target can see —
 * USB debugging, the cable's USB state, whether this phone wrongly became the USB host, and the
 * AxManager server coming up — and turns each change into a status + log line.
 */
object OtgTargetMonitor {

    private const val TAG = "OtgTargetMonitor"

    // Hidden but long-stable UsbManager sticky broadcast and extras.
    private const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"
    private const val EXTRA_CONNECTED = "connected"
    private const val EXTRA_CONFIGURED = "configured"
    private const val EXTRA_ADB = "adb"

    data class Status(
        val usbDebugging: Boolean = false,
        /** Cable plugged into a host (this phone is the USB device side). */
        val cableToHost: Boolean = false,
        /** The host enumerated us with the ADB function — the other phone can reach adbd. */
        val adbLink: Boolean = false,
        /** This phone became the USB host instead (common with USB-C ↔ USB-C cables). */
        val actingAsHost: Boolean = false,
        val serverRunning: Boolean = false,
    )

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _log = MutableStateFlow<List<ActivationLog.Line>>(emptyList())
    val log: StateFlow<List<ActivationLog.Line>> = _log.asStateFlow()

    private var appContext: Context? = null
    private var usbReceiver: BroadcastReceiver? = null
    private var adbObserver: ContentObserver? = null
    private val binderReceived = Axeron.OnBinderReceivedListener { update { it.copy(serverRunning = true) } }
    private val binderDead = Axeron.OnBinderDeadListener { update { it.copy(serverRunning = false) } }

    fun start(context: Context) {
        stop()
        val app = context.applicationContext
        appContext = app
        _log.value = emptyList()
        _status.value = Status(
            usbDebugging = isUsbDebuggingOn(app),
            actingAsHost = isActingAsHost(app),
            serverRunning = Axeron.pingBinder()
        )
        val initial = _status.value
        log(
            if (initial.usbDebugging) "USB debugging is on" else "USB debugging is off — turn it on in Developer options",
            if (initial.usbDebugging) ActivationLog.Level.SUCCESS else ActivationLog.Level.WARN
        )
        if (initial.serverRunning) log("AxManager is already running on this phone", ActivationLog.Level.SUCCESS)

        usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    ACTION_USB_STATE -> {
                        val connected = intent.getBooleanExtra(EXTRA_CONNECTED, false)
                        val configured = intent.getBooleanExtra(EXTRA_CONFIGURED, false)
                        val adb = intent.getBooleanExtra(EXTRA_ADB, false)
                        update { it.copy(cableToHost = connected, adbLink = connected && configured && adb) }
                    }

                    else -> update { it.copy(actingAsHost = isActingAsHost(ctx)) }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_STATE)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        // USB_STATE is sticky, so this also delivers the current cable state right away.
        ContextCompat.registerReceiver(app, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        adbObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                update { it.copy(usbDebugging = isUsbDebuggingOn(app)) }
            }
        }.also {
            app.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, it
            )
        }

        Axeron.addBinderReceivedListener(binderReceived)
        Axeron.addBinderDeadListener(binderDead)
    }

    fun stop() {
        val app = appContext ?: return
        usbReceiver?.let { runCatching { app.unregisterReceiver(it) } }
        adbObserver?.let { app.contentResolver.unregisterContentObserver(it) }
        Axeron.removeBinderReceivedListener(binderReceived)
        Axeron.removeBinderDeadListener(binderDead)
        usbReceiver = null
        adbObserver = null
        appContext = null
    }

    /** Applies a change and logs only what actually changed. */
    private fun update(change: (Status) -> Status) {
        val before = _status.value
        val after = change(before)
        if (after == before) return
        _status.value = after

        if (after.usbDebugging != before.usbDebugging) {
            if (after.usbDebugging) log("USB debugging turned on", ActivationLog.Level.SUCCESS)
            else log("USB debugging turned off", ActivationLog.Level.WARN)
        }
        if (after.actingAsHost != before.actingAsHost && after.actingAsHost) {
            log(
                "This phone became the USB host — the other phone must control USB. " +
                        "Open the USB notification and set \"USB controlled by\" to the connected device",
                ActivationLog.Level.ERROR
            )
        }
        if (after.cableToHost != before.cableToHost) {
            if (after.cableToHost) log("Cable connected to the other phone")
            else log("Cable disconnected", ActivationLog.Level.WARN)
        }
        if (after.adbLink != before.adbLink && after.adbLink) {
            log("USB debugging link is up — tap \"Allow\" if this phone asks", ActivationLog.Level.SUCCESS)
            log("Waiting for the other phone to start AxManager…")
        }
        if (after.serverRunning != before.serverRunning) {
            if (after.serverRunning) log("AxManager server started on this phone", ActivationLog.Level.SUCCESS)
            else log("AxManager server stopped", ActivationLog.Level.WARN)
        }
    }

    private fun log(text: String, level: ActivationLog.Level = ActivationLog.Level.INFO) {
        Log.i(TAG, text)
        _log.update { (it + ActivationLog.Line(System.currentTimeMillis(), level, text)).takeLast(200) }
    }

    /** Cable currently plugged into a USB host, from the sticky USB_STATE broadcast. */
    fun isConnectedToHost(context: Context): Boolean =
        ContextCompat.registerReceiver(
            context, null, IntentFilter(ACTION_USB_STATE), ContextCompat.RECEIVER_NOT_EXPORTED
        )?.getBooleanExtra(EXTRA_CONNECTED, false) == true

    private fun isUsbDebuggingOn(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1

    private fun isActingAsHost(context: Context): Boolean =
        context.getSystemService(UsbManager::class.java)?.deviceList?.isNotEmpty() == true
}
