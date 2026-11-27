package frb.axeron.manager.ui.webui.fgtssl10

import android.app.Activity
import android.content.Context
import android.webkit.WebView
import java.io.File

object Fgtssl10MasterLoader {
 fun init(c: Context, wv: WebView, webRoot: File) {
  Fgtssl10Boot.prewarm(c)
  Fgtssl10Engine.tune(wv)
  Fgtssl10VirtualRamFarm.init(c)
  Fgtssl10SwapFarm.init(c)
  if (c is Activity) { Fgtssl10SpaceCore.lock(c); Fgtssl10DisplayDirector.lockUltra(c) }
  wv.addJavascriptInterface(Fgtssl10CompatKsu.bridge(c), "ksu")
  wv.addJavascriptInterface(Fgtssl10CompatMmrl.bridge(c), "mmrl")
  wv.addJavascriptInterface(Fgtssl10CompatAxeron.bridge(c), "axeron")
  Fgtssl10SpaceCore.i("init root=" + webRoot.absolutePath)
 }
 fun onPageFinished(wv: WebView, mode: String = Fgtssl10DisplayDirector.OLD_AMLOD2X) {
  wv.evaluateJavascript(Fgtssl10VirtualCpu.infoJs(), null)
  wv.evaluateJavascript(Fgtssl10VirtualGpu.turboJs(), null)
  wv.evaluateJavascript(Fgtssl10DisplayDirector.applyJs(mode), null)
  wv.evaluateJavascript(Fgtssl10SpaceCore.fpsLockJs(), null)
  wv.evaluateJavascript(Fgtssl10CompatKsu.shimJs(), null)
  wv.evaluateJavascript(Fgtssl10GameFxSpace.hudJs(mode), null)
  wv.evaluateJavascript(Fgtssl10Dynamic84502K.expandJs(), null)
 }
 fun onTrim() { Fgtssl10Engine.trim(); Fgtssl10VirtualRamFarm.trim(); Fgtssl10NanoCache.trim() }
 fun destroy(wv: WebView) { Fgtssl10Engine.destroy(wv); Fgtssl10VirtualRamFarm.clear() }
}
