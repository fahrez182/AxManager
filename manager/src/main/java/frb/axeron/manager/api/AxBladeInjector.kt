package frb.axeron.manager.api

import android.content.Context
import android.util.Log

class AutoResolutionManager(private val context: Context, private val injector: AxBladeInjector) {

    private val TAG = "AutoResolutionManager"

    suspend fun applyCustomBoost(dpi: Int): InjectionResult {
        Log.i(TAG, "Applying custom DPI boost: $dpi")
        return try {
            // ایمن‌سازی مقادیر DPI بین 220 و 420
            val clampedDpi = dpi.coerceIn(220, 420)
            val command = "wm density $clampedDpi"
            
            // استفاده از دستور تزریق
            val result = injector.injectCommand(command, "DPI_BOOST")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply custom boost: ${e.message}")
            InjectionResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun applyProBoost(mode: String): InjectionResult {
        Log.i(TAG, "Applying Pro Boost mode: $mode")
        return try {
            val targetDpi = when (mode) {
                "ULTRA_FPS" -> 280
                "HIGH_FPS" -> 320
                else -> 360
            }
            val command = "wm density $targetDpi"
            injector.injectCommand(command, "PRO_BOOST_$mode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply pro boost: ${e.message}")
            InjectionResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun reset(): InjectionResult {
        Log.i(TAG, "Resetting display density to default")
        return try {
            injector.injectCommand("wm density reset", "RESET_DENSITY")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset: ${e.message}")
            InjectionResult.Failure(e.message ?: "Unknown error")
        }
    }
}
