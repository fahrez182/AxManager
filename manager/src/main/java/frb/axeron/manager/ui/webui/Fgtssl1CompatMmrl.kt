package frb.axeron.manager.ui.webui.fgtssl1

import android.content.Context
import android.webkit.JavascriptInterface
import java.io.File

object Fgtssl1CompatMmrl {
 fun bridge(c: Context): Any { val d: File = c.filesDir; return object { @JavascriptInterface fun exec(cmd: String): String = kotlinx.coroutines.runBlocking { Fgtssl1VirtualSu.exec(d, cmd) }; @JavascriptInterface fun version() = Fgtssl1Config.VERSION + "-mmrl"; @JavascriptInterface fun ping() = "pong-mmrl-fgt-50" } }
}
