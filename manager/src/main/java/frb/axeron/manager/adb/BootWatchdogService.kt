package frb.axeron.manager.adb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import frb.axeron.adb.util.AdbEnvironment
import frb.axeron.api.Axeron
import frb.axeron.manager.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Starts the server after a reboot and shows how far it got, one notification that follows the
 * [ActivationStep]s and goes away once the server is up. Only the boot receiver starts it; the
 * Start button reports in its own sheet. It waits for Wi-Fi, which an app can't switch on, so
 * the notification offers the Wi-Fi panel while it does.
 */
@RequiresApi(Build.VERSION_CODES.R)
class BootWatchdogService : Service() {

    companion object {
        private const val TAG = "BootWatchdog"
        private const val CHANNEL = "boot_watchdog"
        private const val NOTIFICATION_ID = 2
        private const val REQUEST_WIFI = 1
        private const val REQUEST_RETRY = 2
        private const val REQUEST_APP = 3

        /** Inside the 3 minutes Android 14+ gives a short service. */
        private const val WIFI_WAIT_MS = 150_000L
        private const val SERVER_WAIT_MS = 15_000L
        private const val DONE_SHOWN_MS = 5_000L
        private const val UNLOCK_WAIT_MS = 120_000L
        private const val FAILURE_SHOWN_MS = 10 * 60_000L

        /** The boot receiver fires twice (locked boot, boot completed): one run only. */
        private val running = AtomicBoolean(false)

        fun start(context: Context) {
            context.startForegroundService(Intent(context, BootWatchdogService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private var job: Job? = null

    /** The server is up; what is left is only showing it. */
    @Volatile
    private var succeeded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running.compareAndSet(false, true)) return START_NOT_STICKY

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                getString(R.string.boot_watchdog_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
        try {
            startForeground(
                NOTIFICATION_ID, progress(ActivationStep.WIFI),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } catch (e: Throwable) {
            Log.e(TAG, "startForeground failed", e)
        }

        ActivationLog.begin("Start after reboot")
        job = scope.launch { run() }
        return START_NOT_STICKY
    }

    /** Android 14+ stops a short service after about 3 minutes. */
    override fun onTimeout(startId: Int) {
        job?.cancel()
        // Out of time while only showing that the server is up: nothing failed.
        if (succeeded) dismiss() else fail(getString(R.string.boot_watchdog_timeout))
    }

    override fun onDestroy() {
        running.set(false)
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun run() {
        step(ActivationStep.WIFI)
        if (!AdbEnvironment.isWifiConnected(this)) {
            ActivationLog.warn("Wi-Fi is off — waiting for it to be turned on")
            val connected = withTimeoutOrNull(WIFI_WAIT_MS) {
                while (!AdbEnvironment.isWifiConnected(this@BootWatchdogService)) delay(1000)
                true
            }
            if (connected != true) return fail(getString(R.string.boot_watchdog_wifi_timeout))
        }

        var outcome: AdbStateInfo? = null
        AdbStarter.startAdbWireless(applicationContext, ::step) { outcome = it }
        val info = outcome
        if (info !is AdbStateInfo.Success) {
            return fail(info?.message ?: getString(R.string.boot_watchdog_timeout))
        }

        step(ActivationStep.SERVER)
        val up = withTimeoutOrNull(SERVER_WAIT_MS) {
            while (!Axeron.pingBinder()) delay(200)
            true
        }
        if (up != true) return fail(getString(R.string.boot_watchdog_timeout))

        done()
    }

    private fun step(step: ActivationStep) {
        notificationManager.notify(NOTIFICATION_ID, progress(step))
    }

    private fun progress(step: ActivationStep): Notification {
        val total = ActivationStep.entries.size
        val number = step.ordinal + 1
        return builder()
            .setContentTitle(getString(R.string.boot_watchdog_title))
            .setContentText(getString(label(step)))
            .setSubText(getString(R.string.boot_watchdog_progress, number, total))
            .setProgress(total, number, false)
            .setOngoing(true)
            .apply {
                // The one step that waits on the user.
                if (step == ActivationStep.WIFI) {
                    addAction(
                        Notification.Action.Builder(
                            null, getString(R.string.boot_watchdog_action_wifi),
                            PendingIntent.getActivity(
                                this@BootWatchdogService, REQUEST_WIFI,
                                Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                PendingIntent.FLAG_IMMUTABLE
                            )
                        ).build()
                    )
                }
            }
            .build()
    }

    @StringRes
    private fun label(step: ActivationStep) = when (step) {
        ActivationStep.WIFI -> R.string.boot_watchdog_step_wifi
        ActivationStep.WIRELESS_DEBUGGING -> R.string.boot_watchdog_step_wireless_debugging
        ActivationStep.PORT -> R.string.boot_watchdog_step_port
        ActivationStep.CONNECT -> R.string.boot_watchdog_step_connect
        ActivationStep.STARTER -> R.string.boot_watchdog_step_starter
        ActivationStep.SERVER -> R.string.boot_watchdog_step_server
    }

    private suspend fun done() {
        succeeded = true
        ActivationLog.success("AxManager is active")
        notificationManager.notify(
            NOTIFICATION_ID,
            builder()
                .setContentTitle(getString(R.string.boot_watchdog_done))
                .setOngoing(true)
                .build()
        )
        // The locked boot broadcast comes before the screen is unlocked, and the start is over in
        // seconds: hiding the notification right away meant nobody ever saw it. It stays until the
        // device is unlocked (as long as the short service lasts), then a few seconds more.
        val userManager = getSystemService(UserManager::class.java)
        withTimeoutOrNull(UNLOCK_WAIT_MS) {
            while (!userManager.isUserUnlocked) delay(1000)
        }
        delay(DONE_SHOWN_MS)
        dismiss()
    }

    private fun dismiss() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun fail(reason: String) {
        ActivationLog.error(reason)
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.notify(
            NOTIFICATION_ID,
            builder()
                .setContentTitle(getString(R.string.boot_watchdog_failed))
                .setContentText(reason)
                .setAutoCancel(true)
                .setTimeoutAfter(FAILURE_SHOWN_MS)
                .addAction(
                    Notification.Action.Builder(
                        null, getString(R.string.boot_watchdog_action_retry),
                        PendingIntent.getForegroundService(
                            this, REQUEST_RETRY,
                            Intent(this, BootWatchdogService::class.java),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    ).build()
                )
                .build()
        )
        stopSelf()
    }

    private fun builder(): Notification.Builder =
        Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_axeron)
            .setOnlyAlertOnce(true)
            .apply {
                packageManager.getLaunchIntentForPackage(packageName)?.let {
                    setContentIntent(
                        PendingIntent.getActivity(
                            this@BootWatchdogService, REQUEST_APP, it, PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }
}
