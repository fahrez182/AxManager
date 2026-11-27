package frb.axeron.manager.ui.webui.fgtssl10

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import java.io.ByteArrayInputStream
import java.io.File

class Fgtssl10PathHandler(private val root: File) : Fgtssl10FastRouter.H {
 override fun handle(v: WebView, r: WebResourceRequest): WebResourceResponse? {
  if (!Fgtssl10SecurityShield.allow()) return empty()
  val path = r.url.path ?: return empty()
  if (Fgtssl10SecurityShield.isTraversal(path)) return empty()
  if (path == "/internal/fgt10.js") return js((Fgtssl10VirtualGpu.turboJs() + Fgtssl10SpaceCore.fpsLockJs()).toByteArray())
  if (path == "/internal/display.js") return js(Fgtssl10DisplayDirector.applyJs().toByteArray())
  if (path == "/internal/space.js") return js(Fgtssl10Dynamic84502K.expandJs().toByteArray())
  val f = Fgtssl10SecurityShield.child(root, path) ?: return empty()
  if (!f.exists() || f.length() > Fgtssl10Config.MAX_FILE) return empty()
  Fgtssl10NanoCache.get(f.absolutePath)?.let { return stream(it, f.name) }
  return try {
   val b = if (f.length() < 524288) { val d = f.readBytes(); Fgtssl10NanoCache.put(f.absolutePath, d); Fgtssl10VirtualRamFarm.put("f:" + f.name, d); d } else Fgtssl10SwapFarm.page(f)
   stream(b, f.name)
  } catch (_: Exception) { empty() }
 }
 private fun js(b: ByteArray) = WebResourceResponse("application/javascript", "utf-8", ByteArrayInputStream(b)).apply { responseHeaders = mapOf("Cache-Control" to "max-age=86400", "Content-Security-Policy" to Fgtssl10SecurityShield.csp()) }
 private fun stream(b: ByteArray, n: String) = WebResourceResponse(Fgtssl10SecurityShield.mime(n), null, ByteArrayInputStream(b)).apply { responseHeaders = mapOf("Cache-Control" to "max-age=3600", "Content-Security-Policy" to Fgtssl10SecurityShield.csp()) }
 private fun empty() = WebResourceResponse(null, null, null)
}
