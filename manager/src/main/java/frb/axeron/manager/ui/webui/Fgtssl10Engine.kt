package frb.axeron.manager.ui.webui.fgtssl10

import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import java.io.File

object Fgtssl10Engine {
    fun tune(wv: WebView) {
        val s = wv.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.allowFileAccessFromFileURLs = false
        s.allowUniversalAccessFromFileURLs = false
        s.loadsImagesAutomatically = true
        runCatching { s.offscreenPreRaster = true }
        runCatching { File(wv.context.cacheDir, "fgt10").mkdirs() }
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        runCatching { wv.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true) }
        wv.overScrollMode = View.OVER_SCROLL_NEVER
    }
    fun trim() { runCatching { System.gc() } }
    fun destroy(wv: WebView) { runCatching { wv.stopLoading(); wv.loadUrl("about:blank"); wv.clearHistory(); wv.removeAllViews(); wv.destroy() } }
}
