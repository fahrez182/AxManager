package frb.axeron.manager.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoResolutionManager(
    private val context: Context,
    private val injector: AxBladeInjector
) {
    companion object {
        const val PKG_CODM = "com.activision.callofduty.shooter"
        private const val TAG = "AutoResolutionManager"
        
        // بازه امن برای جلوگیری از از کار افتادن رابط کاربری اندروید
        private const val MIN_SAFE_DPI = 220
        private const val MAX_SAFE_DPI = 420
    }

    /**
     * اعمال حالت‌های پیش‌فرض با بررسی لایه‌های امنیتی
     */
    suspend fun applyProBoost(mode: String): InjectionResult = withContext(Dispatchers.IO) {
        try {
            val dpiValue = when (mode) {
                "ULTRA_FPS" -> 260 // بالاترین کارایی
                "HIGH_FPS" -> 300  // تعادل بالا
                "SMOOTH" -> 340    // پایداری گرافیکی
                else -> 300
            }

            return@withContext executeDensityCommand(dpiValue, "Pro Mode ($mode)")
        } catch (e: Exception) {
            Log.e(TAG, "Error in applyProBoost", e)
            return@withContext InjectionResult.Failure("Pro Boost Error: ${e.localizedMessage}")
        }
    }

    /**
     * اعمال مقدار DPI دلخواه با فیلتر امنیتی سخت‌گیرانه
     */
    suspend fun applyCustomBoost(customDpi: Int): InjectionResult = withContext(Dispatchers.IO) {
        try {
            // فیلتر کردن مقادیر خطرناک برای جلوگیری از بریک شدن یا بهم ریختن شدید صفحه
            val safeDpi = when {
                customDpi < MIN_SAFE_DPI -> {
                    Log.w(TAG, "DPI $customDpi is too low! Clamping to $MIN_SAFE_DPI")
                    MIN_SAFE_DPI
                }
                customDpi > MAX_SAFE_DPI -> {
                    Log.w(TAG, "DPI $customDpi is too high! Clamping to $MAX_SAFE_DPI")
                    MAX_SAFE_DPI
                }
                else -> customDpi
            }

            return@withContext executeDensityCommand(safeDpi, "Custom DPI ($safeDpi)")
        } catch (e: Exception) {
            Log.e(TAG, "Error in applyCustomBoost", e)
            return@withContext InjectionResult.Failure("Custom Boost Error: ${e.localizedMessage}")
        }
    }

    /**
     * متد کمکی برای اجرای دستور و بررسی پاسخ موتور تزریق
     */
    private suspend fun executeDensityCommand(dpi: Int, label: String): InjectionResult {
        val command = "wm density $dpi"
        val result = injector.injectCommand(PKG_CODM, command)

        return when (result) {
            is InjectionResult.Success -> {
                Log.i(TAG, "$label successfully applied.")
                InjectionResult.Success("Success: $label Applied!")
            }
            is InjectionResult.Failure -> {
                Log.e(TAG, "Failed to apply $label: ${result.reason}")
                InjectionResult.Failure("Injection Rejected: ${result.reason}")
            }
        }
    }

    /**
     * ریست کردن سیستم به حالت پیش‌فرض کارخانه
     */
    suspend fun reset(): InjectionResult = withContext(Dispatchers.IO) {
        try {
            val command = "wm density reset"
            val result = injector.injectCommand(PKG_CODM, command)

            return@withContext when (result) {
                is InjectionResult.Success -> {
                    InjectionResult.Success("System Restored to Default Successfully")
                }
                is InjectionResult.Failure -> {
                    InjectionResult.Failure("Reset Failed: ${result.reason}")
                }
            }
        } catch (e: Exception) {
            return@withContext InjectionResult.Failure("Reset Exception: ${e.message}")
        }
    }
}
