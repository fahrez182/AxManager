package frb.axeron.manager.api

import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * AX-TURBO ENGINE: SUPER-ROOT & STEALTH EDITION
 * طراحی شده برای تزریق دستورات سطح بالا و پنهان‌سازی از سیستم‌های امنیتی
 */
class AxTurboEngine {

    enum class EngineStatus { READY, BUSY, ERROR, STEALTH_MODE }

    private var engineStatus = EngineStatus.READY
    private val engineJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + engineJob)
    
    private val securityGuard = SecurityGuard()
    private val executor = CommandExecutor()

    /**
     * اجرای یک ماموریت فوق پیشرفته با قابلیت پنهان‌سازی
     */
    suspend fun executeMission(
        mission: Mission, 
        onComplete: (MissionResult) -> Unit
    ) {
        if (engineStatus == EngineStatus.BUSY) {
            onComplete(MissionResult.Failure("ENGINE_BUSY"))
            return
        }

        engineStatus = EngineStatus.BUSY
        
        scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val commandChain = StringBuilder()

                // ۱. ساخت زنجیره دستورات (Command Chaining) برای سرعت و قدرت
                // ما دستورات را با استفاده از && به هم می‌چسبانیم تا در یک نشست (Session) اجرا شوند
                mission.commands.forEach { cmd ->
                    if (securityGuard.isSafe(cmd)) {
                        if (commandChain.isNotEmpty()) commandChain.append(" && ")
                        commandChain.append(cmd)
                    }
                }

                val finalCommand = commandChain.toString()
                if (finalCommand.isEmpty()) {
                    onComplete(MissionResult.Failure("NO_VALID_COMMANDS"))
                    return@launch
                }

                // ۲. تکنیک Stealth: ایجاد وقفه تصادفی (Jittering) برای جلوگیری از تشخیص الگوی رفتاری
                val stealthDelay = mission.stealthDelay + Random.nextLong(0, 500)
                delay(stealthDelay)

                // ۳. اجرای دستور در سطح Super-Shell
                val result = executor.executeSuperCommand(finalCommand)

                // ۴. مدیریت خروجی و بررسی موفقیت
                if (result.startsWith("ERROR")) {
                    onComplete(MissionResult.Failure(result))
                } else {
                    val duration = System.currentTimeMillis() - startTime
                    // در حالت Stealth، زمان اجرا را در لاگ‌ها مخفی می‌کنیم
                    onComplete(MissionResult.Success("${mission.name} | Latency: ${duration}ms"))
                }

            } catch (e: Exception) {
                onComplete(MissionResult.Failure(e.message ?: "CRITICAL_SYSTEM_FAILURE"))
            } finally {
                engineStatus = EngineStatus.READY
            }
        }
    }

    // --- زیرساخت‌های سطح پایین (Low-Level Infrastructure) ---

    private inner class CommandExecutor {
        /**
         * اجرای دستورات در سطح Shell با شبیه‌سازی محیط سیستم
         */
        suspend fun executeSuperCommand(command: String): String = withContext(Dispatchers.IO) {
            try {
                // استفاده از sh -c برای اجرای زنجیره‌ای و پیچیده
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                
                val output = StringBuilder()
                val error = StringBuilder()

                // خواندن همزمان خروجی و خطا برای جلوگیری از Deadlock در دستورات سنگین
                val outReader = BufferedReader(InputStreamReader(process.inputStream))
                val errReader = BufferedReader(InputStreamReader(process.errorStream))

                // اجرای موازی خواندن برای سرعت حداکثری (Real-time Processing)
                val outJob = launch {
                    var line: String?
                    while (outReader.readLine().also { line = it } != null) {
                        output.append(line).append("\n")
                    }
                }
                val errJob = launch {
                    var line: String?
                    while (errReader.readLine().also { line = it } != null) {
                        error.append(line).append("\n")
                    }
                }

                // انتظار برای اتمام فرآیند با قابلیت Timeout
                val exited = process.waitFor(15, TimeUnit.SECONDS)
                outJob.join()
                errJob.join()

                if (!exited) {
                    process.destroyForcibly()
                    return@withContext "ERROR_TIMEOUT"
                }

                if (error.isNotEmpty() && !output.isNotEmpty()) {
                    "ERROR: ${error.toString().trim()}"
                } else {
                    output.toString().trim()
                }
            } catch (e: Exception) {
                "ERROR: ${e.message}"
            }
        }
    }

    private inner class SecurityGuard {
        // لیست سیاه برای جلوگیری از کرش کردن گوشی (Self-Protection)
        private val blacklist = listOf("reboot", "shutdown", "rm -rf /", "format")
        fun isSafe(cmd: String): Boolean = blacklist.none { cmd.contains(it) }
    }

    fun shutdown() {
        engineJob.cancel()
    }
}

/**
 * مدل‌های داده‌ای برای تعریف ماموریت‌های تزریق
 */
data class Mission(
    val name: String,
    val commands: List<String>,
    val stealthDelay: Long = 100L 
)

sealed class MissionResult {
    data class Success(val details: String) : MissionResult()
    data class Failure(val reason: String) : MissionResult()
}
