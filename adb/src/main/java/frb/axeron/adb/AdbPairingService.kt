package frb.axeron.adb

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.ktx.unsafeLazy
import java.net.ConnectException

/**
 * Pairs this app's ADB key with wireless debugging through a notification, then starts the
 * server over the connect service. Everything but the ADB I/O runs on the main thread, where
 * [AdbMdns] reports too.
 */
@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingService : Service() {

    companion object {

        const val NOTIFICATION_CHANNEL = "adb_pairing"

        private const val TAG = "AdbPairingService"

        private const val NOTIFICATION_ID = 1
        private const val REPLY_REQUEST_CODE = 1
        private const val STOP_REQUEST_CODE = 2
        private const val RETRY_REQUEST_CODE = 3
        private const val SETTINGS_REQUEST_CODE = 4
        private const val APP_REQUEST_CODE = 5
        private const val START_ACTION = "start"
        private const val STOP_ACTION = "stop"
        private const val REPLY_ACTION = "reply"
        private const val REMOTE_INPUT_RESULT_KEY = "pairing_code"
        private const val HOST_KEY = "pairing_host"
        private const val PORT_KEY = "pairing_port"

        /** How long the connect service may take to show up once paired. */
        private const val CONNECT_TIMEOUT_MS = 30_000L
        private const val CONNECT_TRIES_AFTER_PAIRING = 3

        /**
         * How long a pairing can stay open: Android 14+ stops this short service after about
         * 3 minutes (see [onTimeout]). The app waits that long for the server it starts.
         */
        const val LIFETIME_MS = 3 * 60_000L

        /**
         * Starts the server through adbd on the given port. Set by the app, so activating from
         * this notification takes the same steps as from the app (TCP mode when it is on, the
         * launch mode for start on boot, the activation log) instead of a copy of them.
         */
        var starter: (suspend (context: Context, port: Int) -> Result<Unit>)? = null

        fun startIntent(context: Context): Intent {
            return Intent(context, AdbPairingService::class.java).setAction(START_ACTION)
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, AdbPairingService::class.java).setAction(STOP_ACTION)
        }

        private fun replyIntent(context: Context, port: Int): Intent {
            return Intent(context, AdbPairingService::class.java).apply {
                setAction(REPLY_ACTION)
//                putExtra(HOST_KEY, host)
                putExtra(PORT_KEY, port)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val notificationManager by unsafeLazy { getSystemService(NotificationManager::class.java) }

    private var adbMdns: AdbMdns? = null
    private var adbConnectMdns: AdbMdns? = null
    private var started = false

    // adbd advertises its connect service as soon as wireless debugging is on, so it is
    // usually found before pairing and never reported again. Pairing used to wait for that
    // second report, and then waited forever; now the port is kept and connected to directly.
    private var connectPort = -1
    private var paired = false
    private var pairingJob: Job? = null
    private var connectJob: Job? = null
    private var connectTimeoutJob: Job? = null

    /** Why the last pairing attempt failed, shown until the next pairing service is found. */
    private var lastError: String? = null

    /** The pairing service's port while it is advertised, also during an attempt. */
    private var pairingPort = -1

    private val observerPairing = Observer<Int> { port ->
        Log.i(TAG, "Pairing service port: $port")
        pairingPort = if (port > 0) port else -1
        if (!started || paired || pairingJob?.isActive == true) return@Observer

        // Since the service could be killed before user finishing input,
        // we need to put the port into Intent. Once lost (the pairing dialog was closed),
        // a code typed for the old port can't work, so it's back to searching.
        if (port > 0) lastError = null
        notify(if (port > 0) createInputNotification(port) else searchingNotification())
    }

    private val observerConnect = Observer<Int> { port ->
        Log.i(TAG, "Connect service port: $port")
        if (port <= 0) return@Observer

        connectPort = port
        autoConnect()
    }

    /**
     * Android 13+ applies the app language to every context itself; below that only the
     * activities apply it, so this service does too, or its notification is in the system's.
     */
    override fun attachBaseContext(base: Context) {
        val locale = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            runCatching { AxeronSettings.getLocale() }.getOrNull()
        } else null

        super.attachBaseContext(
            if (locale == null) base
            else base.createConfigurationContext(Configuration().apply {
                setLocale(locale)
                setLayoutDirection(locale)
            })
        )
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL,
                getString(R.string.adb_pairing_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                setShowBadge(false)
                setAllowBubbles(false)
            })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = when (intent?.action) {
            START_ACTION -> {
                onStart()
            }
            REPLY_ACTION -> {
                val code = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(REMOTE_INPUT_RESULT_KEY) ?: ""
                val host = intent.getStringExtra(HOST_KEY) ?: "127.0.0.1"
                val port = intent.getIntExtra(PORT_KEY, -1)
                if (port != -1) {
                    onInput(code.toString(), host, port)
                } else {
                    onStart()
                }
            }
            STOP_ACTION -> {
                stopSearch()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                null
            }
            else -> {
                return START_NOT_STICKY
            }
        }
        if (notification != null) {
            try {
                startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
            } catch (e: Throwable) {
                Log.e(TAG, "startForeground failed", e)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && e is ForegroundServiceStartNotAllowedException) {
                    notify(notification)
                }
            }
        }
        // Redelivering would pair again with a code typed for a pairing port that is long gone.
        return START_NOT_STICKY
    }

    /**
     * The manifest declares a short service, which Android 14+ stops after about 3 minutes;
     * without stopping here the app got an ANR (a crash on Android 15).
     */
    override fun onTimeout(startId: Int) {
        Log.w(TAG, "Short service timed out")
        onFailed(R.string.adb_pairing_timeout_title, getString(R.string.adb_pairing_timeout_text))
    }

    private fun startSearch() {
        if (started) return
        started = true
        adbMdns = AdbMdns(this, AdbMdns.TLS_PAIRING, observerPairing).apply {
            indefinite = true
            start()
        }
        adbConnectMdns = AdbMdns(this, AdbMdns.TLS_CONNECT, observerConnect).apply {
            indefinite = true
            start()
        }
    }

    private fun stopSearch() {
        if (!started) return
        started = false
        adbMdns?.stop()
        adbConnectMdns?.stop()
        adbMdns = null
        adbConnectMdns = null
        pairingJob?.cancel()
        connectJob?.cancel()
        connectTimeoutJob?.cancel()
    }

    override fun onDestroy() {
        stopSearch()
        scope.cancel()
        super.onDestroy()
    }

    private fun onStart(): Notification {
        // Retry starts over, also when a previous attempt is still around.
        stopSearch()
        paired = false
        connectPort = -1
        pairingPort = -1
        lastError = null
        startSearch()
        return searchingNotification()
    }

    /**
     * Starts the server through adbd. Before pairing this only works when the key was paired
     * earlier, so a failure then is expected; after pairing it is the result.
     */
    private fun autoConnect() {
        if (!started || connectPort <= 0 || connectJob?.isActive == true) return
        val port = connectPort
        val afterPairing = paired

        connectJob = scope.launch {
            var result: Result<Unit> = Result.failure(IllegalStateException())
            for (attempt in 0 until if (afterPairing) CONNECT_TRIES_AFTER_PAIRING else 1) {
                if (attempt > 0) delay(1000)
                result = withContext(Dispatchers.IO) {
                    starter?.invoke(applicationContext, port)
                        ?: Result.failure(IllegalStateException("No starter set"))
                }
                if (result.isSuccess) break
            }

            result.onSuccess {
                Log.i(TAG, "Auto connect start success")
                onActivated()
            }.onFailure {
                Log.w(TAG, "Auto connect on port $port failed", it)
                if (afterPairing) onFailed(R.string.adb_pairing_start_failed_title, failureText(it))
            }
        }
    }

    private fun keyStore() = PreferenceAdbKeyStore(
        AxeronSettings.getPreferences(),
        Settings.Global.getString(contentResolver, Starter.KEY_PAIR)
    )

    private fun onInput(code: String, host: String, port: Int): Notification {
        if (code.isBlank()) return createInputNotification(port)
        // The service may have been killed while the code was typed: search for the connect
        // service again, it is needed right after pairing.
        startSearch()

        pairingJob?.cancel()
        pairingJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val key = AdbKey(keyStore(), "axeron")
                    AdbPairingClient(host, port, code.trim(), key).use { it.start() }
                }
            }

            result.onSuccess { ok ->
                if (ok) onPaired()
                else onPairingFailed(getString(R.string.adb_pairing_rejected), port)
            }.onFailure {
                Log.w(TAG, "Pair failed", it)
                if (it is AdbKeyException) {
                    onFailed(R.string.adb_pairing_failed_title, failureText(it))
                } else {
                    onPairingFailed(failureText(it), port)
                }
            }
        }

        return workingNotification()
    }

    /**
     * A failed attempt doesn't stop the service: with the reason shown, the code can be typed
     * again while the pairing dialog is still open, or the search goes on for the next dialog
     * (Settings usually closes it on a failure).
     */
    private fun onPairingFailed(reason: String, port: Int) {
        lastError = reason
        notify(if (pairingPort == port) createInputNotification(port) else searchingNotification())
    }

    private fun onPaired() {
        Log.i(TAG, "Pair succeed")
        paired = true
        lastError = null
        adbMdns?.stop()
        notify(startingNotification())

        scope.launch {
            // A try from before pairing may still be running with the then untrusted key.
            connectJob?.join()
            if (!started) return@launch
            autoConnect()
        }

        connectTimeoutJob?.cancel()
        connectTimeoutJob = scope.launch {
            delay(CONNECT_TIMEOUT_MS)
            if (started && connectJob?.isActive != true) {
                onFailed(R.string.adb_pairing_start_failed_title, getString(R.string.adb_pairing_no_connect))
            }
        }
    }

    private fun onActivated() {
        Log.i(TAG, "Connect succeed")
        stopSearch()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notify(activatedNotification())

        scope.launch {
            delay(3000)
            notificationManager.cancel(NOTIFICATION_ID)
            stopSelf()
        }
    }

    private fun onFailed(@StringRes title: Int, text: String?) {
        Log.w(TAG, "${getString(title)}: $text")
        stopSearch()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notify(failedNotification(title, text))
        stopSelf()
    }

    private fun failureText(exception: Throwable): String = when (exception) {
        is ConnectException -> getString(R.string.adb_pairing_cant_connect)
        is AdbInvalidPairingCodeException -> getString(R.string.adb_pairing_wrong_code)
        is AdbKeyException -> getString(R.string.adb_pairing_key_failed)
        else -> exception.message ?: exception.javaClass.simpleName
    }

    private fun notify(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // region Notifications

    private fun servicePendingIntent(requestCode: Int, intent: Intent) =
        PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)

    /** Developer options, scrolled to the wireless debugging switch. */
    private val settingsPendingIntent by unsafeLazy {
        PendingIntent.getActivity(
            this,
            SETTINGS_REQUEST_CODE,
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                .putExtra(":settings:fragment_args_key", "toggle_adb_wireless")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val appPendingIntent by unsafeLazy {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, APP_REQUEST_CODE, it, PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private val settingsAction by unsafeLazy {
        Notification.Action.Builder(null, getString(R.string.adb_pairing_action_settings), settingsPendingIntent)
            .build()
    }

    private val stopAction by unsafeLazy {
        Notification.Action.Builder(
            null,
            getString(R.string.adb_pairing_action_stop),
            servicePendingIntent(STOP_REQUEST_CODE, stopIntent(this))
        ).build()
    }

    private val retryAction by unsafeLazy {
        Notification.Action.Builder(
            null,
            getString(R.string.adb_pairing_action_retry),
            servicePendingIntent(RETRY_REQUEST_CODE, startIntent(this))
        ).build()
    }

    private fun replyAction(port: Int): Notification.Action {
        val remoteInput = RemoteInput.Builder(REMOTE_INPUT_RESULT_KEY)
            .setLabel(getString(R.string.adb_pairing_code_label))
            .build()

        // A reply needs a mutable PendingIntent; FLAG_UPDATE_CURRENT puts this port into it.
        val pendingIntent = PendingIntent.getForegroundService(
            this,
            REPLY_REQUEST_CODE,
            replyIntent(this, port),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Action.Builder(null, getString(R.string.adb_pairing_action_enter_code), pendingIntent)
            .addRemoteInput(remoteInput)
            .build()
    }

    private fun builder(@StringRes title: Int, text: String?): Notification.Builder =
        Notification.Builder(this, NOTIFICATION_CHANNEL)
            .setColor(getColor(R.color.notification))
            .setSmallIcon(R.drawable.ic_axeron)
            .setContentTitle(getString(title))
            .apply {
                if (text != null) {
                    setContentText(text)
                    setStyle(Notification.BigTextStyle().bigText(text))
                }
            }

    /** A step in progress: no sound or heads-up again when it replaces the previous one. */
    private fun progressBuilder(@StringRes title: Int, text: String?): Notification.Builder =
        builder(title, text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)

    private fun searchingNotification(): Notification =
        progressBuilder(
            R.string.adb_pairing_searching_title,
            lastError ?: getString(R.string.adb_pairing_searching_text)
        )
            .setContentIntent(settingsPendingIntent)
            .addAction(settingsAction)
            .addAction(stopAction)
            .build()

    /** Alerts again on purpose: it has to show over Settings, where the code is. */
    private fun createInputNotification(port: Int): Notification =
        builder(R.string.adb_pairing_found_title, lastError ?: getString(R.string.adb_pairing_found_text))
            .setOngoing(true)
            .addAction(replyAction(port))
            .addAction(stopAction)
            .build()

    private fun workingNotification(): Notification =
        progressBuilder(R.string.adb_pairing_working_title, null).build()

    private fun startingNotification(): Notification =
        progressBuilder(R.string.adb_pairing_paired_title, getString(R.string.adb_pairing_starting_text))
            .build()

    private fun activatedNotification(): Notification =
        builder(R.string.adb_pairing_activated_title, getString(R.string.adb_pairing_activated_text))
            .setContentIntent(appPendingIntent)
            .setAutoCancel(true)
            .build()

    private fun failedNotification(@StringRes title: Int, text: String?): Notification =
        builder(title, text)
            .setContentIntent(settingsPendingIntent)
            .setAutoCancel(true)
            .addAction(retryAction)
            .addAction(settingsAction)
            .build()

    // endregion

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
