package frb.axeron.manager.ui.webui.interfaces

import android.content.Context
import android.text.TextUtils
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.google.gson.Gson
import frb.axeron.api.AxeronPluginService
import org.json.JSONObject
import java.io.File

class AxWebInterface(
    val context: Context,
    val modDir: File
) {
    val PLUGINBIN: String 
        get() = "${modDir}/system/bin"

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

    @JavascriptInterface
    fun exec(command: String, options: String?): String {

        val finalCommand = StringBuilder()
        processOptions(finalCommand, options)
        finalCommand.append(command)

        val result = AxeronPluginService.execWithIO(
            cmd = finalCommand.toString(),
            useBusybox = false,
            hideStderr = false
        )
        return Gson().toJson(result).toString()
    }

    @JavascriptInterface
    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}