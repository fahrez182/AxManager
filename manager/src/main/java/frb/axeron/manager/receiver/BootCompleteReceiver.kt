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
import frb.axeron.manager.ui.viewmodel.ActivateException
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
            try {
                startAdb(context)
            } catch (_: ActivateException) {
            }
            safeFinish(pending)
        } else if (AxeronSettings.getLastLaunchMode() == AxeronSettings.LaunchMethod.ROOT) {
            startRoot()
        } else {
            Log.w(TAG, "No support start on boot")
        }

        Log.d(TAG, "onReceive: ${intent.action}")
    }

    private fun startRoot() {
        if (!Shell.getShell().isRoot) {
            Shell.getCachedShell()?.close()
            return
        }

        Shell.cmd(Starter.internalCommand).exec()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Throws(ActivateException::class)
    private fun startAdb(context: Context) {
        WifiReadyGate(context).await(ActivateException.FailedToConnectToWifi("Failed to connect to Wifi"))

        val cr = context.contentResolver

        Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
        Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
        Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)

        AdbWifiGate(context).await(ActivateException.FailedToAutoActivateAdb("Failed to auto activate ADB"))

        AdbMdns(
            context,
            AdbMdns.TLS_CONNECT
        ) { data ->
            Log.d(TAG, "AdbMdns ${data.host} ${data.port}")
            if (data.port <= 0) throw ActivateException.FailedToGetHostAndPort("Failed to get Host and Port")

            AdbClient(
                data.host,
                data.port,
                AdbKey(
                    PreferenceAdbKeyStore(AxeronSettings.getPreferences()),
                    "axeron"
                )
            ).runCatching {
                Log.d(TAG, "AdbClient running")
                Log.d(TAG, Starter.internalCommand)
                connect()
                shellCommand(Starter.internalCommand, null)
                close()
            }.onSuccess {
                Log.d(TAG, "AdbClient success")
                AxeronSettings.setLastLaunchMode(AxeronSettings.LaunchMethod.ADB)
            }.onFailure {
                throw ActivateException.FailedToConnectToClient("Failed to connect to ADB Client")
            }
        }.runCatching {
            Log.d(TAG, "AdbMdns running")
            start()
        }.onFailure {
            throw ActivateException.FailedToStartAdbMdns("Failed to start ADB mDNS (Multicast DNS)")
        }
    }

    fun safeFinish(pending: PendingResult) {
        try {
            pending.finish()
        } catch (e: Exception) {
            Log.e(TAG, "safeFinish failed", e)
        }
    }
}
