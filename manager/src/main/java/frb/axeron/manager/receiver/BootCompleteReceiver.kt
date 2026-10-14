package frb.axeron.manager.receiver

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.topjohnwu.superuser.Shell
import frb.axeron.api.Axeron
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.manager.adb.AdbStarter
import frb.axeron.manager.adb.AdbStateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
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

        if (context.packageManager.isSafeMode) {
            Log.d(TAG, "safeMode: ${context.packageManager.isSafeMode}")
            return
        }

        if (Axeron.pingBinder()) {
            Log.d(TAG, "status: ${Axeron.pingBinder()}")
            return
        }

        // goAsync() only defers finishing the broadcast; the work itself must leave the main
        // thread (waiting for Wi-Fi/mDNS or a su shell there risks an ANR at boot).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED &&
            AxeronSettings.getLastLaunchMode() == AxeronSettings.LaunchMethod.ADB
        ) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AdbStarter.startAdbWireless(context.applicationContext)
                } finally {
                    safeFinish(pending)
                }
            }
        } else if (AxeronSettings.getLastLaunchMode() == AxeronSettings.LaunchMethod.ROOT ||
            AxeronSettings.getLastLaunchMode() == AxeronSettings.LaunchMethod.ROOT_AS_SHELL
        ) {
            val pending = goAsync()
            val mode = AxeronSettings.getLastLaunchMode()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    startRoot(mode)
                } finally {
                    safeFinish(pending)
                }
            }
        } else {
            Log.w(TAG, "No support start on boot")
        }

        Log.d(TAG, "onReceive: ${intent.action}")
    }

    private fun startRoot(mode: Int) {
        if (!Shell.getShell().isRoot) {
            Shell.getCachedShell()?.close()
            return
        }

        val command = if (mode == AxeronSettings.LaunchMethod.ROOT_AS_SHELL) {
            "su 2000 -c \"${Starter.internalCommand}\""
        } else {
            Starter.internalCommand
        }

        Shell.cmd(command).exec()
    }

    fun safeFinish(pending: PendingResult) {
        try {
            pending.finish()
        } catch (e: Exception) {
            Log.e(TAG, "safeFinish failed", e)
        }
    }
}
