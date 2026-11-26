package frb.axeron.manager.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameGraphicsModule(private val injector: AxBladeInjector) {

    /**
     * فعال‌سازی حالت حداکثری (120FPS + MAX Graphics)
     */
    suspend fun enableUltraPerformance(packageName: String): InjectionResult = withContext(Dispatchers.IO) {
        try {
            // ۱. تزریق دستورات گرافیکی برای افزایش فریم‌ریت
            val injectResult = injector.injectCommand(packageName, "settings put global min_refresh_rate 120.0; settings put global peak_refresh_rate 120.0")
            
            if (injectResult is InjectionResult.Success) {
                // ۲. تغییر رزولوشن برای افزایش سرعت پردازش (در صورت نیاز)
                injector.injectCommand(packageName, "wm size reset; wm density reset")
                InjectionResult.Success
            } else {
                InjectionResult.Failure("تزریق فریم‌ریت ناموفق بود.")
            }
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "خطای غیرمنتظره")
        }
    }

    /**
     * بازگشت به حالت عادی (Reset)
     */
    suspend fun resetToDefault(packageName: String): InjectionResult = withContext(Dispatchers.IO) {
        try {
            // بازگشت رزولوشن و تراکم به حالت کارخانه
            val res1 = injector.injectCommand(packageName, "wm size reset")
            val res2 = injector.injectCommand(packageName, "wm density reset")
            // بازگشت نرخ نوسازی به حالت استاندارد (مثلاً ۹۰)
            val res3 = injector.injectCommand(packageName, "settings put global min_refresh_rate 60.0; settings put global peak_refresh_rate 90.0")

            if (res1 is InjectionResult.Success && res2 is InjectionResult.Success && res3 is InjectionResult.Success) {
                InjectionResult.Success
            } else {
                InjectionResult.Failure("برخی تنظیمات بازنگشتند.")
            }
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "خطا در بازگشت")
        }
    }
}
