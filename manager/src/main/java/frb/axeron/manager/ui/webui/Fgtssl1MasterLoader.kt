package frb.axeron.manager.ui.webui.fgtssl1

import android.app.Activity
import android.content.Context
import android.webkit.WebView
import java.io.File

object Fgtssl1MasterLoader {
 fun init(c: Context, wv: WebView, webRoot: File) {
  Fgtssl1Boot.prewarm(c)
  Fgtssl1Engine.tune(wv)
  Fgtssl1VirtualRamFarm.init(c)
  Fgtssl1SwapFarm.init(c)
  if (c is Activity) { Fgtssl1SpaceCore.lock(c); Fgtssl1DisplayDirector.lockUltra(c) }
  wv.addJavascriptInterface(Fgtssl1CompatKsu.bridge(c), "ksu")
  wv.addJavascriptInterface(Fgtssl1CompatMmrl.bridge(c), "mmrl")
  wv.addJavascriptInterface(Fgtssl1CompatAxeron.bridge(c), "axeron")
  Fgtssl1SpaceCore.i("init root=" + webRoot.absolutePath)
 }
 fun onPageFinished(wv: WebView, mode: String = Fgtssl1DisplayDirector.FUSION) {
  wv.evaluateJavascript(Fgtssl1VirtualCpu.infoJs(), null)
  wv.evaluateJavascript(Fgtssl1VirtualGpu.turboJs(), null)
  wv.evaluateJavascript(Fgtssl1DisplayDirector.applyJs(mode), null)
  wv.evaluateJavascript(Fgtssl1SpaceCore.fpsLockJs(), null)
  wv.evaluateJavascript(Fgtssl1CompatKsu.shimJs(), null)
  wv.evaluateJavascript(Fgtssl1GameFxSpace.hudJs(mode), null)
  wv.evaluateJavascript(Fgtssl1Dynamic4502K.expandJs(), null)
 }
 fun onTrim() { Fgtssl1Engine.trim(); Fgtssl1VirtualRamFarm.trim(); Fgtssl1NanoCache.trim() }
 fun destroy(wv: WebView) { Fgtssl1Engine.destroy(wv); Fgtssl1VirtualRamFarm.clear() }
}
