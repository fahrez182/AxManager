package frb.axeron.manager.ui.webui.fgtssl10

import android.content.Context
import android.webkit.JavascriptInterface
import java.io.File

object Fgtssl10CompatKsu {
 fun bridge(c: Context): Any { val d: File = c.filesDir; val app = c.applicationContext; return object { @JavascriptInterface fun exec(cmd: String): String = kotlinx.coroutines.runBlocking { Fgtssl10VirtualSu.exec(d, cmd) }; @JavascriptInterface fun toast(m: String) { runCatching { android.widget.Toast.makeText(app, m.take(200), android.widget.Toast.LENGTH_SHORT).show() } }; @JavascriptInterface fun version() = Fgtssl10Config.VERSION; @JavascriptInterface fun isVirtualRoot() = true; @JavascriptInterface fun ping() = "pong-fgt10-space-2000" } }
 fun shimJs() = "(function(){try{if(!window.ksu)window.ksu={};if(!window.mmrl)window.mmrl=window.ksu;if(!window.axeron)window.axeron=window.ksu;window.ksu.version='FGTSSL10-SPACE-2000PCT';window.ksu.isVirtualRoot=true;window.ksu.fps240=true;window.ksu.space=true;window.ksu.banks400=true;window.ksu.swaps263=true;if(!window.ksu.execAsync)window.ksu.execAsync=function(c){return Promise.resolve(window.ksu.exec(c));};document.dispatchEvent(new Event('fgt10-ready'));}catch(e){}})();"
}
