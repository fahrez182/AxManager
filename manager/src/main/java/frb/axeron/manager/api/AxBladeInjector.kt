package frb.axeron.manager.api

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AxBladeInjector(private val context: Context) {
  suspend fun exec(cmd: String): InjectionResult = withContext(Dispatchers.IO) {
    try {
      val r = Shell.cmd(cmd).exec()
      if (r.isSuccess) {
        InjectionResult.Success(r.out.joinToString("\n").ifEmpty { "OK" })
      } else {
        InjectionResult.Failure("exit=${r.code}: ${(r.out + r.err).joinToString()}")
      }
    } catch (e: Exception) {
      InjectionResult.Failure(e.message ?: "exec failed")
    }
  }
}
