package frb.axeron.manager.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AxBladeInjector(private val context: Context? = null) {

    enum class InjectionStatus { IDLE, INJECTING, SUCCESS, FAILURE, GAME_NOT_FOUND }

    suspend fun injectModuleToGame(packageName: String, modulePayload: String): InjectionResult = withContext(Dispatchers.IO) {
        try {
            val lower = modulePayload.lowercase()
            val blocked = listOf("pgrep", "renice", "drop_caches", "setprop debug.hwui", "/proc/", "su -c")
            for (b in blocked) {
                if (lower.contains(b)) {
                    return@withContext InjectionResult.Failure("BLOCKED_FOR_SAFETY: $b")
                }
            }
            try {
                val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", modulePayload))
                val err = p.errorStream.bufferedReader().readText()
                val done = p.waitFor(8, TimeUnit.SECONDS)
                if (!done) {
                    try { p.destroyForcibly() } catch (_: Exception) {}
                    return@withContext InjectionResult.Failure("TIMEOUT")
                }
                if (err.isNotBlank() && err.contains("permission", true)) {
                    return@withContext InjectionResult.Failure(err.trim())
                }
                InjectionResult.Success("OK")
            } catch (e: Exception) {
                InjectionResult.Failure(e.message ?: "EXEC_ERROR")
            }
        } catch (e: Exception) {
            InjectionResult.Failure(e.message ?: "UNKNOWN")
        }
    }
}

sealed class InjectionResult {
    data class Success(val data: String) : InjectionResult()
    data class Failure(val reason: String) : InjectionResult()
}
