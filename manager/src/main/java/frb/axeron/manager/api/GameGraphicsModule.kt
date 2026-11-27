package frb.axeron.manager.api

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings

class GameGraphicsModule(private val appContext: Context) {

    fun enableUltraPerformance(packageName: String): InjectionResult {
        return try {
            val resolver = appContext.contentResolver
            try { Settings.System.putInt(resolver, "peak_refresh_rate", 120) } catch (_: Exception) {}
            try { Settings.System.putInt(resolver, "min_refresh_rate", 120) } catch (_: Exception) {}
            try { Settings.System.putInt(resolver, "user_refresh_rate", 120) } catch (_: Exception) {}
            try { Settings.Global.putInt(resolver, "refresh_rate_mode", 1) } catch (_: Exception) {}
            InjectionResult.Success("120")
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "خطا")
        }
    }

    fun resetToDefault(packageName: String): InjectionResult {
        return try {
            val resolver = appContext.contentResolver
            try { Settings.System.putInt(resolver, "peak_refresh_rate", 60) } catch (_: Exception) {}
            try { Settings.System.putInt(resolver, "min_refresh_rate", 60) } catch (_: Exception) {}
            try { Settings.System.putInt(resolver, "user_refresh_rate", 60) } catch (_: Exception) {}
            InjectionResult.Success("60")
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "خطا")
        }
    }

    fun getCurrentNumber(): Int {
        return try {
            Settings.System.getInt(appContext.contentResolver, "peak_refresh_rate")
        } catch (_: Exception) {
            60
        }
    }

    fun restartGame(packageName: String) {
        try {
            val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            try { am.killBackgroundProcesses(packageName) } catch (_: Exception) {}
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val launch = appContext.packageManager.getLaunchIntentForPackage(packageName)
                    launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (launch != null) appContext.startActivity(launch)
                } catch (_: Exception) {}
            }, 800)
        } catch (_: Exception) {}
    }
}
