package frb.axeron.server.shell

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import frb.axeron.api.SystemServiceHelper.getSystemService
import frb.axeron.server.ServerConstants


class BinderRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ServerConstants.REQUEST_BINDER_AXRUNTIME) return
        ShellBinderRequestHandler.handleRequest(context.applicationContext, intent)
    }

    @SuppressLint("BatteryLife")
    fun requestRunBackground(context: Context) {
        try {
            val packageName: String? = context.packageName
            val pm: PowerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent()
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.setData(("package:$packageName").toUri())
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
