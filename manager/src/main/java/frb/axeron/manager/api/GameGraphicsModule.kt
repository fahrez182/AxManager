package frb.axeron.manager.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameGraphicsModule(private val injector: AxBladeInjector) {

    /**
     * فعال‌سازی ۱۲۰ فریم و گرافیک MAX
     */
    suspend fun enableUltraPerformance(packageName: String): InjectionResult = withContext(Dispatchers.IO) {
        try {
            val result = injector.injectCommand(
                packageName, 
                "settings put global min_refresh_rate 120.0; settings put global peak_refresh_rate 120.0"
            )
            
            // بررسی خروجی به صورت امن
            return@withContext when (result) {
                is InjectionResult.Success -> {
                    // اعمال دستور دوم برای رزولوشن
                    injector.injectCommand(packageName, "wm size reset")
                    InjectionResult.Success("")
                }
                is InjectionResult.Failure -> {
                    InjectionResult.Failure(result.reason)
                }
            }
        } catch (e: Exception) {
            return@withContext InjectionResult.Failure(e.message ?: "Unknown Error")
        }
    }

    /**
     * بازگشت به حالت پیش‌فرض (Reset)
     */
    suspend fun resetToDefault(packageName: String): InjectionResult = withContext(Dispatchers.IO) {
        try {
            val res1 = injector.injectCommand(packageName, "wm size reset")
            val res2 = injector.injectCommand(packageName, "wm density reset")
            
            return@withContext if (res1 is InjectionResult.Success && res2 is InjectionResult.Success) {
                InjectionResult.Success("")
            } else {
                InjectionResult.Failure("Reset failed to apply")
            }
        } catch (e: Exception) {
            return@withContext InjectionResult.Failure(e.message ?: "Unknown Error")
        }
    }
}
