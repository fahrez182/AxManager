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
import frb.axeron.adb.AdbWifiGate
import frb.axeron.adb.PreferenceAdbKeyStore
import frb.axeron.adb.WifiReadyGate
import frb.axeron.api.Axeron
import frb.axeron.api.core.AxeronSettings
import frb.axeron.server.utils.Starter

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
            startAdb(context, pending)
        } else if (AxeronSettings.getLastLaunchMode() == AxeronSettings.LaunchMethod.ROOT) {
            val pending = goAsync()
            startRoot(pending)
        } else {
            Log.w(TAG, "No support start on boot")
        }

        Log.d(TAG, "onReceive: ${intent.action}")
    }

    private fun startRoot(pending: PendingResult) {
        if (!Shell.getShell().isRoot) {
            Shell.getCachedShell()?.close()
            safeFinish(pending)
            return
        }

        Shell.cmd(Starter.internalCommand).exec()
        safeFinish(pending)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startAdb(
        context: Context,
        pending: PendingResult
    ) {
        WifiReadyGate(
            context,
            onReady = {
                val cr = context.contentResolver

                Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)

                AdbWifiGate(
                    context,
                    onReady = {
                        AdbMdns(context, AdbMdns.TLS_CONNECT) { data ->
                            Log.d(TAG, "AdbMdns ${data.host} ${data.port}")
                            if (data.port <= 0) {
                                safeFinish(pending)
                                return@AdbMdns
                            }

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
                                safeFinish(pending)
                            }.onFailure {
                                Log.e(TAG, "AdbClient failed", it)
                                safeFinish(pending)
                            }

                        }.runCatching {
                            Log.d(TAG, "AdbMdns running")
                            start()
                        }.onFailure {
                            Log.e(TAG, "AdbMdns failed", it)
                            safeFinish(pending)
                        }
                    },
                    onFail = {
                        safeFinish(pending)
                    }
                ).await()
            },
            onFail = {
                safeFinish(pending)
            }
        ).await()
    }

    fun safeFinish(pending: PendingResult) {
        try {
            pending.finish()
        } catch (e: Exception) {
            Log.e(TAG, "safeFinish failed", e)
        }
    }
}
