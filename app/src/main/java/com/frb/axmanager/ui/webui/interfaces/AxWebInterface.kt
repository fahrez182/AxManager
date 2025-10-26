package com.frb.axmanager.ui.webui.interfaces

import android.content.Context
import android.text.TextUtils
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.frb.engine.client.PluginService
import com.google.gson.Gson
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

        sb.append("[ -d $PLUGINBIN ] && [ -n \"$(ls -A $PLUGINBIN 2>/dev/null)\" ] && export PATH=$PLUGINBIN:\$PATH;")
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

        val result = PluginService.execWithIO(
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