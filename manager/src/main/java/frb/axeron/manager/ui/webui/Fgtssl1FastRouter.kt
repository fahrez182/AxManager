package frb.axeron.manager.ui.webui.fgtssl1

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

class Fgtssl1FastRouter(routes: List<Fgtssl1Route>) {
 private val idx: Map<String, List<Fgtssl1Route>> = routes.groupBy { it.scheme + "://" + it.host }
 @Volatile private var lastKey: String? = null
 @Volatile private var lastList: List<Fgtssl1Route>? = null
 fun handle(v: WebView, r: WebResourceRequest): WebResourceResponse? {
  val u = r.url ?: return null
  val key = u.scheme + "://" + u.host
  var l = lastList
  if (key != lastKey || l == null) { l = idx[key] ?: return null; lastKey = key; lastList = l }
  val p = u.path ?: "/"
  for (x in l) if (x.prefix.isNullOrEmpty() || p.startsWith(x.prefix)) return x.h.handle(v, r)
  return null
 }
 data class Fgtssl1Route(val scheme: String, val host: String, val prefix: String?, val h: H)
 fun interface H { fun handle(v: WebView, r: WebResourceRequest): WebResourceResponse? }
}
