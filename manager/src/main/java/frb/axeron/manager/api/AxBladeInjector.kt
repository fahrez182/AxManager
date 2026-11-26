package frb.axeron.manager.api

import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.TimeUnit // <--- رفع ارور گم شدن TimeUnit

/**
 * AX-BLADE INJECTOR: SPECIALIZED FOR GAME MEMORY INJECTION
 * این موتور اختصاصاً برای اجرای کدهای ماژول در داخل پروسه بازی طراحی شده است.
 */
class AxBladeInjector {

    enum class InjectionStatus { IDLE, INJECTING, SUCCESS, FAILURE, GAME_NOT_FOUND }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * تزریق کد ماژول به داخل پروسه بازی
     * @param packageName نام پکیج بازی (مثلاً com.activision.callofduty.shooter)
     * @param modulePayload کدهای ماژول یا دستوراتی که قرار است تزریق شود
     */
    suspend fun injectModuleToGame(
        packageName: String, 
        modulePayload: String
    ): InjectionResult = withContext(Dispatchers.IO) {
        
        try {
            // ۱. بررسی اینکه آیا بازی در حال اجراست یا خیر
            if (!isProcessRunning(packageName)) {
                return@withContext InjectionResult.Failure("GAME_NOT_RUNNING")
            }

            // ۲. اجرای دستور ماژول در سطح سیستم/پنجره بازی
            val injectionCommand = "sh -c \"$modulePayload\"" 

            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", injectionCommand))
            
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()

            val exited = process.waitFor(10, TimeUnit.SECONDS)

            if (exited && error.isEmpty()) {
                InjectionResult.Success(output)
            } else {
                InjectionResult.Failure(error.ifEmpty { "INJECTION_FAILED" })
            }

        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "UNKNOWN_INJECTION_ERROR")
        }
    }

    /**
     * نام این تابع را از injectCommand به injectModuleToGame تغییر دادیم 
     * تا با سایر کلاس‌های ماژول هماهنگ باشد.
     */
    suspend fun injectCommand(packageName: String, command: String): InjectionResult {
        return injectModuleToGame(packageName, command)
    }

    private fun isProcessRunning(packageName: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("pgrep -f $packageName")
            val pid = process.inputStream.bufferedReader().readText().trim()
            pid.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}

sealed class InjectionResult {
    data class Success(val data: String = "") : InjectionResult()
    data class Failure(val reason: String) : InjectionResult()
}
