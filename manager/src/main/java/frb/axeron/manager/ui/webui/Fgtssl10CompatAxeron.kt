package frb.axeron.manager.ui.webui.fgtssl10

import android.content.Context
import android.webkit.JavascriptInterface
import java.io.File

object Fgtssl10CompatAxeron {
 fun bridge(c: Context): Any { val d: File = c.filesDir; return object { @JavascriptInterface fun exec(cmd: String): String = kotlinx.coroutines.runBlocking { Fgtssl10VirtualSu.exec(d, cmd) }; @JavascriptInterface fun info(): String = "{\"engine\":\"FGTSSL10-SPACE-2000PCT\",\"vcpu\":540,\"vram\":737280,\"banks\":400,\"swaps\":263,\"fps\":240}"; @JavascriptInterface fun ping() = "pong-axeron-fgt10" } }
}
