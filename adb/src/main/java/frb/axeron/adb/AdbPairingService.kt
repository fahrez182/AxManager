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
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.core.ktx.unsafeLazy
import java.net.ConnectException

@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingService : Service() {

    companion object {

        const val NOTIFICATION_CHANNEL = "adb_pairing"

        private const val TAG = "AdbPairingService"

        private const val NOTIFICATION_ID = 1
        private const val REPLY_REQUEST_CODE = 1
        private const val STOP_REQUEST_CODE = 2
        private const val RETRY_REQUEST_CODE = 3
        private const val START_ACTION = "start"
        private const val STOP_ACTION = "stop"
        private const val REPLY_ACTION = "reply"
        private const val REMOTE_INPUT_RESULT_KEY = "pairing_code"
        private const val HOST_KEY = "pairing_host"
        private const val PORT_KEY = "pairing_port"

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

    private var adbMdns: AdbMdns? = null

    private val observerPairing = Observer<Int> { port ->
        Log.i(TAG, "Pairing service port: $port")
        if (port <= 0) return@Observer

        // Since the service could be killed before user finishing input,
        // we need to put the port into Intent
        val notification = createInputNotification(port)

        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private var started = false

    override fun onCreate() {
        super.onCreate()

        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL,
                "Wireless Debugging Pairing",
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
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun startSearch() {
        if (started) return
        started = true
        adbMdns = AdbMdns(this, AdbMdns.TLS_PAIRING, observerPairing).apply { start() }
    }

    private fun stopSearch() {
        if (!started) return
        started = false
        adbMdns?.stop()
    }

    override fun onDestroy() {
        stopSearch()
        super.onDestroy()
    }

    private fun onStart(): Notification {
        startSearch()
        return searchingNotification
    }

    private fun onInput(code: String, host: String, port: Int): Notification {
        CoroutineScope(Dispatchers.IO).launch {

            val keyStore = PreferenceAdbKeyStore(
                AxeronSettings.getPreferences(),
                Settings.Global.getString(contentResolver, Starter.KEY_PAIR)
            )
            val key = try {
                AdbKey(keyStore, "axeron")
            } catch (e: Throwable) {
                e.printStackTrace()
                return@launch
            }

            AdbPairingClient(host, port, code, key).runCatching {
                start()
            }.onFailure {
                handleResult(false, it)
            }.onSuccess {
                handleResult(it, null)
            }
        }

        return workingNotification
    }

    private fun handleResult(success: Boolean, exception: Throwable?) {
        stopForeground(STOP_FOREGROUND_REMOVE)

        val title: String
        val text: String?

        if (success) {
            Log.i(TAG, "Pair succeed")

            title = "Pairing successfully"
            text = "You can start AxeronService now"

            stopSearch()
        } else {
            title = "Pairing failed"

            text = when (exception) {
                is ConnectException -> {
                    "Can't connect to wireless debugging service."
                }
                is AdbInvalidPairingCodeException -> {
                    "Pairing code is wrong"
                }
                is AdbKeyException -> {
                    "Unable to generate key for wireless debugging service. This may.."
                }
                else -> {
                    exception?.let { Log.getStackTraceString(it) }
                }
            }

            if (exception != null) {
                Log.w(TAG, "Pair failed", exception)
            } else {
                Log.w(TAG, "Pair failed")
            }
        }

        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            Notification.Builder(this, NOTIFICATION_CHANNEL)
                .setColor(getColor(R.color.notification))
                .setSmallIcon(R.drawable.ic_axeron)
                .setContentTitle(title)
                .setContentText(text)
                .apply {
                    if (!success) {
                        addAction(retryNotificationAction)
                    }
                }
                .build()
        )
        stopSelf()
    }

    private val stopNotificationAction by unsafeLazy {
        val pendingIntent = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            stopIntent(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_IMMUTABLE
            else
                0
        )

        Notification.Action.Builder(
            null,
            "Stop searching",
            pendingIntent
        )
            .build()
    }

    private val retryNotificationAction by unsafeLazy {
        val pendingIntent = PendingIntent.getService(
            this,
            RETRY_REQUEST_CODE,
            startIntent(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_IMMUTABLE
            else
                0
        )

        Notification.Action.Builder(
            null,
            "Retry",
            pendingIntent
        )
            .build()
    }

    private val replyNotificationAction by unsafeLazy {
        val remoteInput = RemoteInput.Builder(REMOTE_INPUT_RESULT_KEY).run {
            setLabel("Pairing code")
            build()
        }

        val pendingIntent = PendingIntent.getForegroundService(
            this,
            REPLY_REQUEST_CODE,
            replyIntent(this, -1),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        Notification.Action.Builder(
            null,
            "Enter pairing code",
            pendingIntent
        ).addRemoteInput(remoteInput).build()
    }

    private fun replyNotificationAction(port: Int): Notification.Action {
        // Ensure pending intent is created
        val action = replyNotificationAction

        PendingIntent.getForegroundService(
            this,
            REPLY_REQUEST_CODE,
            replyIntent(this,port),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return action
    }

    private val searchingNotification by unsafeLazy {
        Notification.Builder(this, NOTIFICATION_CHANNEL)
            .setColor(getColor(R.color.notification))
            .setSmallIcon(R.drawable.ic_axeron)
            .setContentTitle("Searching for pairing service")
            .addAction(stopNotificationAction)
            .build()
    }

    private fun createInputNotification(port: Int): Notification {
        return Notification.Builder(this, NOTIFICATION_CHANNEL)
            .setColor(getColor(R.color.notification))
            .setContentTitle("Pairing service found")
            .setSmallIcon(R.drawable.ic_axeron)
            .addAction(replyNotificationAction(port))
            .build()
    }

    private val workingNotification by unsafeLazy {
        Notification.Builder(this, NOTIFICATION_CHANNEL)
            .setColor(getColor(R.color.notification))
            .setContentTitle("Pairing in progress")
            .setSmallIcon(R.drawable.ic_axeron)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
