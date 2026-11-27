package frb.axeron.manager.ui.webui.fgtssl1

import android.content.Context
import android.webkit.JavascriptInterface
import java.io.File

object Fgtssl1CompatAxeron {
 fun bridge(c: Context): Any { val d: File = c.filesDir; return object { @JavascriptInterface fun exec(cmd: String): String = kotlinx.coroutines.runBlocking { Fgtssl1VirtualSu.exec(d, cmd) }; @JavascriptInterface fun info(): String = "{\"engine\":\"FGTSSL1-SPACE-50PCT\",\"vcpu\":27,\"vram\":36864,\"banks\":40,\"swaps\":26,\"fps\":240}"; @JavascriptInterface fun ping() = "pong-axeron-fgt-50" } }
}
