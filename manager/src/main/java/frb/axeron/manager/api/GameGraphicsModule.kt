package frb.axeron.manager.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameGraphicsModule(private val injector: AxBladeInjector) {

    suspend fun enableUltraPerformance(packageName: String): InjectionResult = withContext(Dispatchers.IO) {
        try {
            // استفاده از تابع injectCommand که در فایل بالا اصلاح کردیم
            val result = injector.injectCommand(packageName, "settings put global min_refresh_rate 120.0")
            
            if (result is InjectionResult.Success) {
                InjectionResult.Success
            } else {
                InjectionResult.Failure("Failed to inject FPS")
            }
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "Error")
        }
    }

    suspend fun resetToDefault(packageName: String): InjectionResult = withContext(Dispatchers.IO) {
        try {
            val res1 = injector.injectCommand(packageName, "wm size reset")
            val res2 = injector.injectCommand(packageName, "wm density reset")
            
            if (res1 is InjectionResult.Success && res2 is InjectionResult.Success) {
                InjectionResult.Success
            } else {
                InjectionResult.Failure("Reset failed")
            }
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "Error")
        }
    }
}
