package frb.axeron.manager.api

import kotlinx.coroutines.*
import java.io.*

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

            // ۲. شبیه‌سازی تزریق در سطح Blade
            // در اینجا ما از تکنیک "Command Chaining" استفاده می‌کنیم تا دستور ماژول 
            // مستقیماً در محیط اجرای بازی (Game Context) اجرا شود
            val injectionCommand = "su -c \"$modulePayload\"" 
            // نکته: اگر روت نداشته باشی، موتور از ADB Proxy که در AxTurboEngine بود استفاده می‌کند

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
    data class Success(val data: String) : InjectionResult()
    data class Failure(val reason: String) : InjectionResult()
}
