package frb.axeron.manager.ui.webui.fgtssl10

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

object Fgtssl10VirtualSu {
 private var last = 0L
 suspend fun exec(filesDir: File, cmd: String): String = withContext(Dispatchers.IO) {
  if (cmd.isBlank()) return@withContext ""
  if (Fgtssl10SafeExec.isHardBlocked(cmd)) return@withContext "blocked: fork-bomb denied"
  val now = System.currentTimeMillis(); if (now - last < Fgtssl10Config.RATE_MS) return@withContext "busy: retry"; last = now
  val real = Fgtssl10SafeExec.rewrite(filesDir, cmd)
  if (real == "__VIRT_REBOOT__") return@withContext "virtual-reboot-ok"
  try {
   val root = File(filesDir, "virtual_root").apply { mkdirs() }
   val pb = ProcessBuilder("sh", "-c", real); pb.directory(root)
   pb.environment()["HOME"] = root.absolutePath; pb.environment()["AX_FGT10"] = "1"
   val p = pb.start(); val sb = StringBuilder()
   val t1 = Thread { runCatching { p.inputStream.bufferedReader().forEachLine { if (sb.length < Fgtssl10Config.MAX_OUT) sb.appendLine(it) } } }
   val t2 = Thread { runCatching { p.errorStream.bufferedReader().forEachLine { if (sb.length < Fgtssl10Config.MAX_OUT) sb.appendLine(it) } } }
   t1.start(); t2.start()
   if (!p.waitFor(12, TimeUnit.SECONDS)) { p.destroyForcibly(); return@withContext "timeout" }
   t1.join(1000); t2.join(1000)
   val t = sb.toString().trim(); Fgtssl10SpaceCore.i("exec:" + cmd.take(80)); if (t.isEmpty()) "ok" else t.take(Fgtssl10Config.MAX_OUT)
  } catch (_: Exception) { "error: virtual-su failed" }
 }
}
