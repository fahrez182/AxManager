package com.frb.engine.receiver

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.frb.engine.adb.AdbClient
import com.frb.engine.adb.AdbKey
import com.frb.engine.adb.AdbMdns
import com.frb.engine.adb.PreferenceAdbKeyStore
import com.frb.engine.client.Axeron
import com.frb.engine.core.AxeronProvider
import com.frb.engine.core.AxeronSettings
import com.frb.engine.utils.Starter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


class BootCompleteReceiver : BroadcastReceiver() {

    private var isRunning: Boolean = false

    private var binderReceived: AtomicBoolean = AtomicBoolean(false)

    override fun onReceive(context: Context, intent: Intent) {
        // Hanya handle BOOT_COMPLETED atau LOCKED_BOOT_COMPLETED
        if (intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            intent.action != AxeronProvider.ACTION_BINDER_RECEIVED &&
            intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        if (intent.action == AxeronProvider.ACTION_BINDER_RECEIVED) {
            binderReceived.set(true)
        }

        // Kalau sudah pernah jalan, skip (hindari double trigger)
        if (isRunning) {
            Log.d("BootCompleteReceiver", "Skip duplicate event: ${intent.action}")
            return
        }


        if (Axeron.pingBinder() || binderReceived.get()) {
            Log.d("BootCompleteReceiver", "status: ${Axeron.pingBinder()}")
            Log.d("BootCompleteReceiver", "binderReceived: ${binderReceived.get()}")

            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED &&
            AxeronSettings.getLastLaunchMode() == AxeronSettings.LaunchMethod.ADB) {

            isRunning = true
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                adbStart(context, pending, 0, 5) // attempt=0, maxAttempts=5
            }
        } else {
            Log.w("BootCompleteReceiver", "No support start on boot")
        }

        Log.d("BootCompleteReceiver", "onReceive: ${intent.action}")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun adbStart(
        context: Context,
        pending: PendingResult,
        attempt: Int,
        maxAttempts: Int
    ) {
        val cr = context.contentResolver

        // Force ON adb wifi setiap attempt
        Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
        Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
        Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)

        val adbEnabled = Settings.Global.getInt(cr, "adb_wifi_enabled", 0)
        if (adbEnabled != 1) {
            Log.w("BootCompleteReceiver", "ADB Wi-Fi not Activated, attempt=$attempt")
            if (attempt < maxAttempts) {
                delay(2000L * (attempt + 1)) // exponential backoff
                return adbStart(context, pending, attempt + 1, maxAttempts)
            }
            Log.e("BootCompleteReceiver", "Failed to activate ADB Wi-Fi $maxAttempts attempt")
            return
        }

        Log.d("BootCompleteReceiver", "adbStart, attempt=$attempt")

        val latch = CountDownLatch(1)
        val adbMdns = AdbMdns(context, AdbMdns.TLS_CONNECT) { data ->
            if (data.port <= 0) return@AdbMdns
            try {
                runCatching {
                    val keystore = PreferenceAdbKeyStore(AxeronSettings.getPreferences())
                    val key = AdbKey(keystore, "axeron")
                    val client = AdbClient(data.host, data.port, key)
                    client.connect()
                    client.shellCommand(Starter.internalCommand, null)
                    client.close()
                }
            } catch (_: Exception) {
            }
            latch.countDown()
        }

        adbMdns.start()
        latch.await(3, TimeUnit.SECONDS)
        Log.d("BootCompleteReceiver", "adbFinish")
        adbMdns.stop()

        pending.finish()
    }
}
