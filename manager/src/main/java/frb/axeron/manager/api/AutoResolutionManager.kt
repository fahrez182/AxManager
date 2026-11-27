package frb.axeron.manager.api

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AutoResolutionManager(private val context: Context, private val injector: AxBladeInjector) {

    suspend fun applyProBoost(mode: String): InjectionResult = withContext(Dispatchers.IO) {
        try {
            val target = when (mode) {
                "ULTRA_FPS" -> 120
                "HIGH_FPS" -> 90
                else -> 60
            }
            applyRefresh(target)
            injector.injectModuleToGame("safe", "settings put system peak_refresh_rate $target")
            InjectionResult.Success("Applied $mode -> $target")
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "BOOST_ERROR")
        }
    }

    suspend fun applyCustomBoost(userDpi: Int): InjectionResult = withContext(Dispatchers.IO) {
        try {
            val dpi = userDpi.coerceIn(120, 800)
            applyRefresh(120)
            try {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", "wm density $dpi")).waitFor(5, TimeUnit.SECONDS)
            } catch (_: Exception) {}
            injector.injectModuleToGame("safe", "settings put system min_refresh_rate 120")
            InjectionResult.Success("Custom DPI $dpi + 120")
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "CUSTOM_ERROR")
        }
    }

    suspend fun reset(): InjectionResult = withContext(Dispatchers.IO) {
        try {
            applyRefresh(60)
            try {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", "wm density reset")).waitFor(5, TimeUnit.SECONDS)
            } catch (_: Exception) {}
            injector.injectModuleToGame("safe", "settings put system peak_refresh_rate 60")
            InjectionResult.Success("Reset Done")
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "RESET_ERROR")
        }
    }

    private fun applyRefresh(rate: Int) {
        val resolver = context.contentResolver
        try { Settings.System.putInt(resolver, "peak_refresh_rate", rate) } catch (_: Exception) {}
        try { Settings.System.putInt(resolver, "min_refresh_rate", rate) } catch (_: Exception) {}
        try { Settings.System.putInt(resolver, "user_refresh_rate", rate) } catch (_: Exception) {}
    }
}
