package frb.axeron.manager.ui.webui.fgtssl1

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import java.io.ByteArrayInputStream
import java.io.File

class Fgtssl1PathHandler(private val root: File) : Fgtssl1FastRouter.H {
 override fun handle(v: WebView, r: WebResourceRequest): WebResourceResponse? {
  if (!Fgtssl1SecurityShield.allow()) return empty()
  val path = r.url.path ?: return empty()
  if (Fgtssl1SecurityShield.isTraversal(path)) return empty()
  if (path == "/internal/fgtssl1.js") return js((Fgtssl1VirtualGpu.turboJs() + Fgtssl1SpaceCore.fpsLockJs() + Fgtssl1DisplayDirector.applyJs()).toByteArray())
  if (path == "/internal/display.js") return js(Fgtssl1DisplayDirector.applyJs().toByteArray())
  if (path == "/internal/space.js") return js(Fgtssl1Dynamic4502K.expandJs().toByteArray())
  val f = Fgtssl1SecurityShield.child(root, path) ?: return empty()
  if (!f.exists() || f.length() > Fgtssl1Config.MAX_FILE) return empty()
  Fgtssl1NanoCache.get(f.absolutePath)?.let { return stream(it, f.name) }
  return try {
   val b = if (f.length() < 524288) { val d = f.readBytes(); Fgtssl1NanoCache.put(f.absolutePath, d); Fgtssl1VirtualRamFarm.put("f:" + f.name, d); d } else Fgtssl1SwapFarm.page(f)
   stream(b, f.name)
  } catch (_: Exception) { empty() }
 }
 private fun js(b: ByteArray) = WebResourceResponse("application/javascript", "utf-8", ByteArrayInputStream(b)).apply { responseHeaders = mapOf("Cache-Control" to "max-age=86400", "Content-Security-Policy" to Fgtssl1SecurityShield.csp()) }
 private fun stream(b: ByteArray, n: String) = WebResourceResponse(Fgtssl1SecurityShield.mime(n), null, ByteArrayInputStream(b)).apply { responseHeaders = mapOf("Cache-Control" to "max-age=3600", "Content-Security-Policy" to Fgtssl1SecurityShield.csp()) }
 private fun empty() = WebResourceResponse(null, null, null)
}
