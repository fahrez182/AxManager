package com.frb.axmanager.ui.webui.interfaces

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Base64
import android.view.Window
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.frb.engine.client.Axeron
import com.frb.engine.client.PluginService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * Unrooted by FahrezONE
 */
class KsuWebInterface(
    val context: Context,
    private val webView: WebView,
    private val modDir: String
) {

    val PLUGINBIN
        get() = "${modDir}/system/bin"

    @JavascriptInterface
    fun exec(cmd: String): String {
        return runCatching {
            runBlocking(Dispatchers.IO) {
                val result = PluginService.execWithIO(
                    cmd = cmd,
                    useBusybox = false,
                    hideStderr = false
                )
                if (result.err.isNotBlank()) "${result.out}\n${result.err}" else result.out
            }
        }.getOrElse {
            it.toString()
        }
    }


    @JavascriptInterface
    fun exec(cmd: String, callbackFunc: String) {
        exec(cmd, null, callbackFunc)
    }

    private fun processOptions(sb: StringBuilder, options: String?) {
        val opts = if (options == null) JSONObject() else {
            JSONObject(options)
        }

        val cwd = opts.optString("cwd")
        if (!TextUtils.isEmpty(cwd)) {
            sb.append("cd ${cwd};")
        }

        sb.append("[ -d $PLUGINBIN ] && [ -n \"$(ls -A $PLUGINBIN 2>/dev/null)\" ] && export PATH=\$PATH:$PLUGINBIN;")
        opts.optJSONObject("env")?.let { env ->
            env.keys().forEach { key ->
                sb.append("export ${key}=${env.getString(key)};")
            }
        }
    }

    @JavascriptInterface
    fun exec(
        cmd: String,
        options: String?,
        callbackFunc: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val finalCommand = StringBuilder()
            processOptions(finalCommand, options)
            finalCommand.append(cmd)

            val result = PluginService.execWithIO(
                cmd = finalCommand.toString(),
                useBusybox = false,
                hideStderr = false
            )

            val jsCode =
                "javascript: (function() { try { ${callbackFunc}(${result.code}, ${
                    JSONObject.quote(
                        result.out
                    )
                }, ${JSONObject.quote(result.err)}); } catch(e) { console.error(e); } })();"
            webView.post {
                webView.loadUrl(jsCode)
            }
        }
    }

    @JavascriptInterface
    fun spawn(command: String, args: String, options: String?, callbackFunc: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val finalCommand = StringBuilder()

            processOptions(finalCommand, options)

            if (!TextUtils.isEmpty(args)) {
                finalCommand.append(command).append(" ")
                JSONArray(args).let { argsArray ->
                    for (i in 0 until argsArray.length()) {
                        finalCommand.append(argsArray.getString(i))
                        finalCommand.append(" ")
                    }
                }
            } else {
                finalCommand.append(command)
            }

            val emitData = fun(name: String, data: String) {
                val jsCode =
                    "javascript: (function() { try { ${callbackFunc}.${name}.emit('data', ${
                        JSONObject.quote(
                            data
                        )
                    }); } catch(e) { console.error('emitData', e); } })();"
                webView.post {
                    webView.loadUrl(jsCode)
                }
            }

            val future = PluginService.execWithIOFuture(
                cmd = finalCommand.toString(),
                onStdout = { emitData("stdout", it) },
                onStderr = { emitData("stderr", it) },
                useBusybox = false,
                hideStderr = false
            )
            val completableFuture = CompletableFuture.supplyAsync {
                future.get()
            }

            completableFuture.thenAccept { result ->
                val emitExitCode =
                    "javascript: (function() { try { ${callbackFunc}.emit('exit', ${result.code}); } catch(e) { console.error(`emitExit error: \${e}`); } })();"
                webView.post {
                    webView.loadUrl(emitExitCode)
                }

                if (result.code != 0) {
                    val emitErrCode =
                        "javascript: (function() { try { var err = new Error(); err.exitCode = ${result.code}; err.message = ${
                            JSONObject.quote(
                                result.err + "\n"
                            )
                        };${callbackFunc}.emit('error', err); } catch(e) { console.error('emitErr', e); } })();"
                    webView.post {
                        webView.loadUrl(emitErrCode)
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun toast(msg: String) {
        webView.post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun fullScreen(enable: Boolean) {
        if (context is Activity) {
            Handler(Looper.getMainLooper()).post {
                if (enable) {
                    hideSystemUI(context.window)
                } else {
                    showSystemUI(context.window)
                }
            }
        }
    }

    @JavascriptInterface
    fun moduleInfo(): String {
        val pluginInfos = Axeron.getPlugins()
        val currentModuleInfo = JSONObject().also {
            it.put("moduleDir", modDir)
        }
        val moduleId = File(modDir).getName()
        for (i in 0 until pluginInfos.size) {
            val currentInfo = JSONObject(pluginInfos[i])

            if (currentInfo.getString("id") != moduleId) {
                continue
            }

            val keys = currentInfo.keys()
            for (key in keys) {
                currentModuleInfo.put(key, currentInfo.get(key))
            }
            break
        }
        return currentModuleInfo.toString()
    }

    @JavascriptInterface
    fun listSystemPackages(): String {
//        val pm = context.packageManager
        val packages = Axeron.getPackages(0)
        val packageNames = packages.list
            .mapNotNull { pkg ->
                val appInfo = pkg.applicationInfo
                if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                    pkg.packageName
                } else null
            }
            .sorted()
        val jsonArray = JSONArray()
        for (pkgName in packageNames) {
            jsonArray.put(pkgName)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun listUserPackages(): String {
//        val pm = context.packageManager
        val packages = Axeron.getPackages(0)
        val packageNames = packages.list
            .mapNotNull { pkg ->
                val appInfo = pkg.applicationInfo
                if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    pkg.packageName
                } else null
            }
            .sorted()
        val jsonArray = JSONArray()
        for (pkgName in packageNames) {
            jsonArray.put(pkgName)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun listAllPackages(): String {
//        val pm = context.packageManager
        val packages = Axeron.getPackages(0)
        val packageNames = packages.list.map { it.packageName }.sorted()
        val jsonArray = JSONArray()
        for (pkgName in packageNames) {
            jsonArray.put(pkgName)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun getPackagesInfo(packageNamesJson: String): String {
        val pm = context.packageManager
        val packageNames = JSONArray(packageNamesJson)
        val jsonArray = JSONArray()
        for (i in 0 until packageNames.length()) {
            val pkgName = packageNames.getString(i)
            try {
                val pkg = pm.getPackageInfo(pkgName, 0)
                val appInfo = pkg.applicationInfo
                val obj = JSONObject()
                obj.put("packageName", pkg.packageName)
                obj.put("versionName", pkg.versionName ?: "")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    obj.put("versionCode", pkg.longVersionCode)
                } else {
                    @Suppress("DEPRECATION")
                    obj.put("versionCode", pkg.versionCode)
                }
                obj.put("appLabel", if (appInfo != null) pm.getApplicationLabel(appInfo).toString() else "")
                obj.put("isSystem", appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                obj.put("uid", appInfo?.uid ?: JSONObject.NULL)
                jsonArray.put(obj)
            } catch (e: Exception) {
                val obj = JSONObject()
                obj.put("packageName", pkgName)
                obj.put("error", "Package not found or inaccessible")
                jsonArray.put(obj)
            }
        }
        return jsonArray.toString()
    }

    private val packageIconCache = HashMap<String, String>()

    @JavascriptInterface
    fun cacheAllPackageIcons(size: Int) {
        val pm = context.packageManager
        val packages = Axeron.getPackages(0)
        val outputStream = ByteArrayOutputStream()
        for (pkg in packages.list) {
            val pkgName = pkg.packageName
            if (packageIconCache.containsKey(pkgName)) continue
            try {
                val appInfo = pkg.applicationInfo!!
                val drawable = pm.getApplicationIcon(appInfo)
                val bitmap = drawableToBitmap(drawable, size)
                outputStream.reset()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                val iconBase64 = "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
                packageIconCache[pkgName] = iconBase64
            } catch (_: Exception) {
                packageIconCache[pkgName] = ""
            }
        }
    }

    @JavascriptInterface
    fun getPackagesIcons(packageNamesJson: String, size: Int): String {
        val pm = context.packageManager
        val packageNames = JSONArray(packageNamesJson)
        val jsonArray = JSONArray()
        val outputStream = ByteArrayOutputStream()
        for (i in 0 until packageNames.length()) {
            val pkgName = packageNames.getString(i)
            val obj = JSONObject()
            obj.put("packageName", pkgName)
            var iconBase64 = packageIconCache[pkgName]
            if (iconBase64 == null) {
                try {
                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                    val drawable = pm.getApplicationIcon(appInfo)
                    val bitmap = drawableToBitmap(drawable, size)
                    outputStream.reset()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    val byteArray = outputStream.toByteArray()
                    iconBase64 = "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
                } catch (_: Exception) {
                    iconBase64 = ""
                }
                packageIconCache[pkgName] = iconBase64
            }
            obj.put("icon", iconBase64)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}

fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap.width == size && drawable.bitmap.height == size) {
        return drawable.bitmap
    }
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return bitmap
}

fun hideSystemUI(window: Window) =
    WindowInsetsControllerCompat(window, window.decorView).let { controller ->
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

fun showSystemUI(window: Window) =
    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())