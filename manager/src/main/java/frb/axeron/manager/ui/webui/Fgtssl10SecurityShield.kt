package frb.axeron.manager.ui.webui.fgtssl10

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

object Fgtssl10SecurityShield {
 private val q = ConcurrentLinkedQueue<Long>()
 fun allow(): Boolean { val n = System.currentTimeMillis(); q.add(n); while (q.isNotEmpty() && n - (q.peek() ?: n) > 1000) q.poll(); return q.size <= Fgtssl10Config.REQ_PER_SEC }
 fun isTraversal(p: String): Boolean { val l = p.lowercase(); return l.contains("..") || l.contains("%2e") || l.contains("%252e") || l.contains("\\") || l.contains("\u0000") }
 fun child(root: File, p: String): File? { return try { val rp = root.canonicalPath; val cp = File(root, p).canonicalPath; if (cp == rp || cp.startsWith(rp + File.separator)) File(cp) else null } catch (_: Exception) { null } }
 fun csp() = "default-src 'self' ax: https://mui.kernelsu.org; script-src 'self' 'unsafe-inline' ax: https://mui.kernelsu.org; style-src 'self' 'unsafe-inline' ax: https://mui.kernelsu.org; img-src * data: blob:; frame-src ax: https://mui.kernelsu.org blob: data:; object-src 'none';"
 fun mime(n: String): String { return when (n.substringAfterLast('.', "").lowercase()) { "html", "htm" -> "text/html"; "css" -> "text/css"; "js", "mjs" -> "application/javascript"; "json" -> "application/json"; "png" -> "image/png"; "jpg", "jpeg" -> "image/jpeg"; "svg" -> "image/svg+xml"; "webp" -> "image/webp"; "wasm" -> "application/wasm"; else -> "text/plain" } }
}
