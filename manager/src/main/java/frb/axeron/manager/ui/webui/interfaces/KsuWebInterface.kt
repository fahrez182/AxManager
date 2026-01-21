package frb.axeron.manager.ui.webui.interfaces

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Base64
import android.view.Window
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.createBitmap
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronPluginService
import frb.axeron.server.util.flattenOneLevel
import kotlinx.coroutines.Dispatchers
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
    private val modDir: File
) {

    val pluginBin: String
        get() = "${modDir.absolutePath}/system/bin"

    @JavascriptInterface
    fun exec(cmd: String): String {
        return runCatching {
            runBlocking(Dispatchers.IO) {
                val result = AxeronPluginService.execWithIO(
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

        sb.append($$"export PATH=$$pluginBin:$PATH;")
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
        runBlocking(Dispatchers.IO) {
            val finalCommand = buildString {
                processOptions(this, options)
                append(cmd)
            }
//            val finalCommand = StringBuilder()
//            processOptions(finalCommand, options)
//            finalCommand.append(cmd)

            val result = AxeronPluginService.execWithIO(
                cmd = finalCommand,
                useBusybox = false,
                hideStderr = false
            )

            val jsCode =
                "(function() { try { ${callbackFunc}(${result.code}, ${
                    JSONObject.quote(
                        result.out
                    )
                }, ${JSONObject.quote(result.err)}); } catch(e) { console.error(e); } })();"
            webView.post {
                webView.evaluateJavascript(jsCode, null)
            }
        }
    }

    @JavascriptInterface
    fun spawn(command: String, args: String, options: String?, callbackFunc: String) {
        runBlocking(Dispatchers.IO) {
            val finalCommand = buildString {
                processOptions(this, options)

                if (!TextUtils.isEmpty(args)) {
                    append(command).append(" ")
                    JSONArray(args).let { argsArray ->
                        for (i in 0 until argsArray.length()) {
                            append(argsArray.getString(i))
                            append(" ")
                        }
                    }
                } else {
                    append(command)
                }
            }

//            val finalCommand = StringBuilder()
//            processOptions(finalCommand, options)
//            if (!TextUtils.isEmpty(args)) {
//                finalCommand.append(command).append(" ")
//                JSONArray(args).let { argsArray ->
//                    for (i in 0 until argsArray.length()) {
//                        finalCommand.append(argsArray.getString(i)).append(" ")
//                    }
//                }
//            } else {
//                finalCommand.append(command)
//            }

            val emitData = fun(name: String, data: String) {
                val jsCode =
                    "(function() { try { ${callbackFunc}.${name}.emit('data', ${
                        JSONObject.quote(
                            data
                        )
                    }); } catch(e) { console.error('emitData', e); } })();"
                webView.post {
                    webView.evaluateJavascript(jsCode, null)
                }
            }

            val future = AxeronPluginService.execWithIOFuture(
                cmd = finalCommand,
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
                    "(function() { try { ${callbackFunc}.emit('exit', ${result.code}); } catch(e) { console.error(`emitExit error: \${e}`); } })();"
                webView.post {
                    webView.evaluateJavascript(emitExitCode, null)
                }

                if (result.code != 0) {
                    val emitErrCode =
                        "(function() { try { var err = new Error(); err.exitCode = ${result.code}; err.message = ${
                            JSONObject.quote(
                                result.err + "\n"
                            )
                        };${callbackFunc}.emit('error', err); } catch(e) { console.error('emitErr', e); } })();"
                    webView.post {
                        webView.evaluateJavascript(emitErrCode, null)
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
        var currentModuleInfo: Map<String, Any?> = emptyMap()
        val moduleId = modDir.getName()
        for (i in 0 until pluginInfos.size) {
            val currentInfo = pluginInfos[i]

            if (currentInfo.prop.id != moduleId) {
                continue
            }

            currentModuleInfo = flattenOneLevel(currentInfo.toMap()).toMutableMap().also {
                it["moduleDir"] = modDir
            }
            break
        }
        return GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create()
            .toJson(currentModuleInfo)
    }

    @JavascriptInterface
    fun listPackages(type: String): String {
        val packageNames = Axeron.getPackages(0)
            .filter { packageInfo ->
                val flags = packageInfo.applicationInfo?.flags ?: 0
                when (type.lowercase()) {
                    "system" -> (flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    "user" -> (flags and ApplicationInfo.FLAG_SYSTEM) == 0
                    else -> true
                }
            }
            .map { it.packageName }
            .sorted()

        val jsonArray = JSONArray()
        for (pkgName in packageNames) {
            jsonArray.put(pkgName)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun listSystemPackages(): String {
//        val pm = context.packageManager
        val packages = Axeron.getPackages(0).mapNotNull { pkg ->
            val appInfo = pkg.applicationInfo
            if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                pkg.packageName
            } else null
        }
            .sorted()
        val jsonArray = JSONArray()
        for (pkgName in packages) {
            jsonArray.put(pkgName)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun listUserPackages(): String {
//        val pm = context.packageManager
        val packages = Axeron.getPackages(0)
            .mapNotNull { pkg ->
                val appInfo = pkg.applicationInfo
                if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    pkg.packageName
                } else null
            }
            .sorted()
        val jsonArray = JSONArray()
        for (pkgName in packages) {
            jsonArray.put(pkgName)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun listAllPackages(): String {
//        val pm = context.packageManager
        val packages = Axeron.getPackages(0)
            .map { it.packageName }.sorted()
        val jsonArray = JSONArray()
        for (pkgName in packages) {
            jsonArray.put(pkgName)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun getPackagesInfo(packageNamesJson: String): String {
        val pm = context.packageManager
        val packageNames = JSONArray(packageNamesJson)
        val jsonArray = JSONArray()
        val appMap = Axeron.getPackages(0).associateBy { it.packageName }
        for (i in 0 until packageNames.length()) {
            val pkgName = packageNames.getString(i)
            val appInfo = appMap[pkgName]
            if (appInfo != null) {
                val app = appInfo.applicationInfo
                val obj = JSONObject()
                obj.put("packageName", appInfo.packageName)
                obj.put("versionName", appInfo.versionName ?: "")
                obj.put("versionCode", PackageInfoCompat.getLongVersionCode(appInfo))
                obj.put("appLabel", app?.loadLabel(pm).toString())
                obj.put("isSystem", if (app != null) ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) else JSONObject.NULL)
                obj.put("uid", app?.uid ?: JSONObject.NULL)
                jsonArray.put(obj)
            } else {
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
        for (pkg in packages) {
            val pkgName = pkg.packageName
            if (packageIconCache.containsKey(pkgName)) continue
            try {
                val appInfo = pkg.applicationInfo!!
                val drawable = appInfo.loadIcon(pm)
                val bitmap = drawableToBitmap(drawable, size)
                outputStream.reset()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                val iconBase64 =
                    "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
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
                    val drawable = appInfo.loadIcon(pm)
                    val bitmap = drawableToBitmap(drawable, size)
                    outputStream.reset()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    val byteArray = outputStream.toByteArray()
                    iconBase64 =
                        "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
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
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

fun showSystemUI(window: Window) =
    WindowInsetsControllerCompat(
        window,
        window.decorView
    ).show(WindowInsetsCompat.Type.systemBars())