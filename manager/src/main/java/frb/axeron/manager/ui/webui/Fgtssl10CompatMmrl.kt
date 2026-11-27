package frb.axeron.manager.ui.webui.fgtssl10

import android.content.Context
import android.webkit.JavascriptInterface
import java.io.File

object Fgtssl10CompatMmrl {
 fun bridge(c: Context): Any { val d: File = c.filesDir; return object { @JavascriptInterface fun exec(cmd: String): String = kotlinx.coroutines.runBlocking { Fgtssl10VirtualSu.exec(d, cmd) }; @JavascriptInterface fun version() = Fgtssl10Config.VERSION + "-mmrl"; @JavascriptInterface fun ping() = "pong-mmrl-fgt10" } }
}
