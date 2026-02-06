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
import kotlinx.coroutines.runBlocking

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
            startAdb(context) {
                safeFinish(pending)
            }
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
        finish: (AdbStateInfo) -> Unit
    ) = runBlocking(Dispatchers.IO) {
        AdbStarter.startAdbWireless(context, finish)
    }

    fun safeFinish(pending: PendingResult) {
        try {
            pending.finish()
        } catch (e: Exception) {
            Log.e(TAG, "safeFinish failed", e)
        }
    }
}
