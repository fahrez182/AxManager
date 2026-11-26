package frb.axeron.manager.api

import android.content.Context
import android.util.Log

class AutoResolutionManager(private val context: Context, private val injector: AxBladeInjector) {

    private val TAG = "AutoResolutionManager"

    suspend fun applyCustomBoost(dpi: Int): InjectionResult {
        return try {
            val clampedDpi = dpi.coerceIn(220, 420)
            injector.injectCommand("wm density $clampedDpi", "CUSTOM_DPI")
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "Error")
        }
    }

    suspend fun applyProBoost(mode: String): InjectionResult {
        return try {
            val targetDpi = when (mode) {
                "ULTRA_FPS" -> 280
                "HIGH_FPS" -> 320
                else -> 360
            }
            injector.injectCommand("wm density $targetDpi", "PRO_BOOST")
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "Error")
        }
    }

    suspend fun reset(): InjectionResult {
        return try {
            injector.injectCommand("wm density reset", "RESET")
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "Error")
        }
    }
}
