package frb.axeron.manager.ui.webui.interfaces

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import frb.axeron.api.AxeronPluginService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File

class AxWebInterface(
    val context: Context,
    private val webView: WebView,
    val modDir: File
) {
    val PLUGINBIN: String
        get() = "${modDir}/system/bin"

    /**
     * A JavascriptInterface call blocks the page's JS until it returns, so the callback variant
     * returns at once and runs the command here. Tied to the activity: nothing calls back into a
     * WebView that is gone.
     */
    private val scope: CoroutineScope =
        (context as? LifecycleOwner)?.lifecycleScope ?: CoroutineScope(SupervisorJob())

    private fun processOptions(sb: StringBuilder, options: String?) {
        val opts = if (options == null) JSONObject() else {
            JSONObject(options)
        }

        val cwd = opts.optString("cwd")
        if (!TextUtils.isEmpty(cwd)) {
            sb.append("cd ${cwd};")
        }

        sb.append("export PATH=$PLUGINBIN:\$PATH;")
        opts.optJSONObject("env")?.let { env ->
            env.keys().forEach { key ->
                sb.append("export ${key}=${env.getString(key)};")
            }
        }
    }

    private fun buildCommand(command: String, options: String?): String =
        StringBuilder().also {
            processOptions(it, options)
            it.append(command)
        }.toString()

    private suspend fun run(command: String) = AxeronPluginService.execWithIO(
        cmd = command,
        useBusybox = false,
        hideStderr = false,
        // Commands from the plugin's page are its code: they stop with the plugin.
        pluginId = modDir.name
    )

    /** Blocks the page's JS until the command ends; prefer the callback variant. */
    @JavascriptInterface
    fun exec(command: String, options: String?): String {
        val finalCommand = buildCommand(command, options)
        return runBlocking(Dispatchers.IO) {
            Gson().toJson(run(finalCommand)).toString()
        }
    }

    /** Returns at once; calls `window[callbackFunc](errno, stdout, stderr)` when the command ends. */
    @JavascriptInterface
    fun exec(command: String, options: String?, callbackFunc: String) {
        // Built before returning, so malformed options still throw into the caller's JS.
        val finalCommand = buildCommand(command, options)

        scope.launch(Dispatchers.IO) {
            val result = run(finalCommand)
            val jsCode =
                "(function() { try { ${callbackFunc}(${result.code}, ${
                    JSONObject.quote(result.out)
                }, ${JSONObject.quote(result.err)}); } catch(e) { console.error(e); } })();"
            webView.post {
                webView.evaluateJavascript(jsCode, null)
            }
        }
    }

    @JavascriptInterface
    fun toast(msg: String) {
        // JavascriptInterface calls arrive on a binder thread without a Looper.
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
