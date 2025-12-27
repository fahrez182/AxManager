package frb.axeron.manager.receiver

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.topjohnwu.superuser.Shell
import frb.axeron.adb.AdbClient
import frb.axeron.adb.AdbKey
import frb.axeron.adb.AdbMdns
import frb.axeron.adb.PreferenceAdbKeyStore
import frb.axeron.api.Axeron
import frb.axeron.api.core.AxeronSettings
import frb.axeron.server.utils.Starter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BootCompleteReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "BootCompleteReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Hanya handle BOOT_COMPLETED
        val isBootAction =
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED

        if (!isBootAction || !AxeronSettings.getStartOnBoot()) {
            return
        }


        if (Axeron.pingBinder()) {
            Log.d(TAG, "status: ${Axeron.pingBinder()}")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED &&
            AxeronSettings.getLastLaunchMode() == AxeronSettings.LaunchMethod.ADB
        ) {
            val pending = goAsync()
            startAdb(context, pending, 0, 5) // attempt=0, maxAttempts=5
        } else if (AxeronSettings.getLastLaunchMode() == AxeronSettings.LaunchMethod.ROOT) {
            startRoot()
        } else {
            Log.w(TAG, "No support start on boot")
        }

        Log.d(TAG, "onReceive: ${intent.action}")
    }

    private fun startRoot() {
        if (!Shell.getShell().isRoot) {
            //NotificationHelper.notify(context, AppConstants.NOTIFICATION_ID_STATUS, AppConstants.NOTIFICATION_CHANNEL_STATUS, R.string.notification_service_start_no_root)
            Shell.getCachedShell()?.close()
            return
        }

        Shell.cmd(Starter.internalCommand).exec()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startAdb(
        context: Context,
        pending: PendingResult,
        attempt: Int,
        maxAttempts: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val cr = context.contentResolver

            // Force ON adb wifi setiap attempt
            Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)

            val adbWifiEnabled = Settings.Global.getInt(cr, "adb_wifi_enabled", 0)
            if (adbWifiEnabled != 1) {
                Log.w(TAG, "ADB Wi-Fi not Activated, attempt=$attempt")
                if (attempt < maxAttempts) {
                    delay(2000L * (attempt + 1)) // exponential backoff
                    return@launch startAdb(context, pending, attempt + 1, maxAttempts)
                }
            }

            AdbMdns(context, AdbMdns.TLS_CONNECT) { data ->
                Log.d(TAG, "AdbMdns ${data.host} ${data.port}")

                AdbClient(
                    data.host,
                    data.port,
                    AdbKey(PreferenceAdbKeyStore(AxeronSettings.getPreferences()), "axeron")
                ).runCatching {
                    Log.d(TAG, "AdbClient running")
                    connect()
                    shellCommand(Starter.internalCommand, null)
                    close()
                }.onSuccess {
                    Log.d(TAG, "AdbClient success")
                    AxeronSettings.setLastLaunchMode(AxeronSettings.LaunchMethod.ADB)
                    pending.finish()
                }.onFailure {
                    if (attempt < maxAttempts) {
                        Log.w(TAG, "AdbClient failed, attempt=$attempt")
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(2000L * (attempt + 1))
                            startAdb(context, pending, attempt + 1, maxAttempts)
                        }
                    } else {
                        Log.e(TAG, "AdbClient failed", it)
                    }
                }

            }.runCatching {
                Log.d(TAG, "AdbMdns running")
                start()
            }.onFailure {
                Log.e(TAG, "AdbMdns failed", it)
            }
        }
    }
}
