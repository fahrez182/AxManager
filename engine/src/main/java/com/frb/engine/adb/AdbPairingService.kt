package com.frb.engine.adb

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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import com.frb.engine.R
import com.frb.engine.core.AxeronSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.core.ktx.unsafeLazy
import java.net.ConnectException

@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingService : Service() {

    companion object {

        const val notificationChannel = "adb_pairing"

        private const val tag = "AdbPairingService"

        private const val notificationId = 1
        private const val replyRequestId = 1
        private const val stopRequestId = 2
        private const val retryRequestId = 3
        private const val startAction = "start"
        private const val stopAction = "stop"
        private const val replyAction = "reply"
        private const val remoteInputResultKey = "pairing_code"
        private const val hostKey = "pairing_host"
        private const val portKey = "pairing_port"

        fun startIntent(context: Context): Intent {
            return Intent(context, AdbPairingService::class.java).setAction(startAction)
        }


        @JvmStatic
        fun stopIntent(context: Context): Intent {
            return Intent(context, AdbPairingService::class.java).setAction(stopAction)
        }

        private fun replyIntent(context: Context, host: String, port: Int): Intent {
            return Intent(context, AdbPairingService::class.java).apply {
                setAction(replyAction)
                putExtra(hostKey, host)
                putExtra(portKey, port)
            }
        }
    }

    private var adbMdns: AdbMdns? = null

    private val observerPairing = Observer<AdbMdns.AdbData> { data ->
        Log.i(tag, "Pairing service port: ${data.host}")
        if (data.port <= 0) return@Observer

        // Since the service could be killed before user finishing input,
        // we need to put the port into Intent
        val notification = createInputNotification(data.host, data.port)

        getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }

    private var started = false

    override fun onCreate() {
        super.onCreate()

        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                notificationChannel,
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
            startAction -> {
                onStart()
            }
            replyAction -> {
                val code = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(remoteInputResultKey) ?: ""
                val host = intent.getStringExtra(hostKey) ?: "127.0.0.1"
                val port = intent.getIntExtra(portKey, -1)
                if (port != -1) {
                    onInput(code.toString(), host, port)
                } else {
                    onStart()
                }
            }
            stopAction -> {
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
                startForeground(notificationId, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
            } catch (e: Throwable) {
                Log.e(tag, "startForeground failed", e)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && e is ForegroundServiceStartNotAllowedException) {
                    getSystemService(NotificationManager::class.java).notify(notificationId, notification)
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
        super.onDestroy()
        stopSearch()
    }

    private fun onStart(): Notification {
        startSearch()
        return searchingNotification
    }

    private fun onInput(code: String, host: String, port: Int): Notification {
        CoroutineScope(Dispatchers.IO).launch {

            val key = try {
                AdbKey(PreferenceAdbKeyStore(AxeronSettings.getPreferences()), "axeron")
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
            Log.i(tag, "Pair succeed")

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
                Log.w(tag, "Pair failed", exception)
            } else {
                Log.w(tag, "Pair failed")
            }
        }

        getSystemService(NotificationManager::class.java).notify(
            notificationId,
            Notification.Builder(this, notificationChannel)
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
            stopRequestId,
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
            retryRequestId,
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
        val remoteInput = RemoteInput.Builder(remoteInputResultKey).run {
            setLabel("Pairing code")
            build()
        }

        val pendingIntent = PendingIntent.getForegroundService(
            this,
            replyRequestId,
            replyIntent(this, "", -1),
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

    private fun replyNotificationAction(host: String, port: Int): Notification.Action {
        // Ensure pending intent is created
        val action = replyNotificationAction

        PendingIntent.getForegroundService(
            this,
            replyRequestId,
            replyIntent(this, host, port),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return action
    }

    private val searchingNotification by unsafeLazy {
        Notification.Builder(this, notificationChannel)
            .setColor(getColor(R.color.notification))
            .setSmallIcon(R.drawable.ic_axeron)
            .setContentTitle("Searching for pairing service")
            .addAction(stopNotificationAction)
            .build()
    }

    private fun createInputNotification(host: String, port: Int): Notification {
        return Notification.Builder(this, notificationChannel)
            .setColor(getColor(R.color.notification))
            .setContentTitle("Pairing service found")
            .setSmallIcon(R.drawable.ic_axeron)
            .addAction(replyNotificationAction(host, port))
            .build()
    }

    private val workingNotification by unsafeLazy {
        Notification.Builder(this, notificationChannel)
            .setColor(getColor(R.color.notification))
            .setContentTitle("Pairing in progress")
            .setSmallIcon(R.drawable.ic_axeron)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
