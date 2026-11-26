package frb.axeron.manager.api

import android.content.Context
import android.util.Log

class GameGraphicsModule(private val context: Context, private val injector: AxBladeInjector) {

    private val TAG = "GameGraphicsModule"

    suspend fun optimizeGraphics(): InjectionResult {
        Log.i(TAG, "Optimizing game graphics...")
        return try {
            // اعمال تنظیمات گرافیکی بهینه‌سازی شده
            val result = injector.injectCommand("setprop debug.hwui.renderer skiagl", "GRAPHICS_OPT")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Graphics optimization failed: ${e.message}")
            InjectionResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun applyLowGraphicsMode(): InjectionResult {
        Log.i(TAG, "Applying low graphics / performance mode...")
        return try {
            // استفاده از InjectionResult.Success به جای Success خالی
            injector.injectCommand("setprop debug.performance.tuning 1", "LOW_GFX")
            InjectionResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply low graphics: ${e.message}")
            InjectionResult.Failure(e.message ?: "Unknown error")
        }
    }
}
