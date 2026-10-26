package frb.axeron.manager.ui.webui.interfaces

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Base64
import android.util.Log
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
import frb.axeron.api.AxeronFileService
import frb.axeron.api.AxeronPluginService
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import frb.axeron.manager.ui.component.createWebUIShortcut
import frb.axeron.manager.ui.util.ShortcutIcons
import frb.axeron.manager.ui.util.loadPluginBannerData
import frb.axeron.manager.ui.webui.WebUIActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "KsuWebInterface"

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

    /**
     * A JavascriptInterface call blocks the page's JS until it returns, so the callback variants
     * return at once and run the command here. Tied to the activity: nothing calls back into a
     * WebView that is gone.
     */
    private val scope: CoroutineScope =
        (context as? LifecycleOwner)?.lifecycleScope ?: CoroutineScope(SupervisorJob())

    /** Commands from the plugin's page are its code: tag them so they stop with the plugin. */
    private val pluginId: String
        get() = modDir.name

    @JavascriptInterface
    fun exec(cmd: String): String {
        return runCatching {
            runBlocking(Dispatchers.IO) {
                val result = AxeronPluginService.execWithIO(
                    cmd = cmd,
                    useBusybox = false,
                    hideStderr = false,
                    pluginId = pluginId
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
        // Built before returning, so malformed options still throw into the caller's JS.
        val finalCommand = buildString {
            processOptions(this, options)
            append(cmd)
        }

        scope.launch(Dispatchers.IO) {
            val result = AxeronPluginService.execWithIO(
                cmd = finalCommand,
                useBusybox = false,
                hideStderr = false,
                pluginId = pluginId
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
        // Built before returning, so malformed args/options still throw into the caller's JS.
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

        scope.launch(Dispatchers.IO) {
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
                hideStderr = false,
                pluginId = pluginId
            )
            val result = runInterruptible { future.get() }

            // 'error' goes first: callers (kernelsu.js included) drop the child object on
            // 'exit', so an 'error' sent after it reached nothing.
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

            val emitExitCode =
                "(function() { try { ${callbackFunc}.emit('exit', ${result.code}); } catch(e) { console.error(`emitExit error: \${e}`); } })();"
            webView.post {
                webView.evaluateJavascript(emitExitCode, null)
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

    /** Closes the WebUI. Not in KSU Next's bridge, but kernelsu.js has exit(). */
    @JavascriptInterface
    fun exit() {
        if (context is Activity) {
            // Each WebUI is its own recents task (WebUIActivity.intentFor): close that too.
            Handler(Looper.getMainLooper()).post { context.finishAndRemoveTask() }
        }
    }

    /** true: the page draws under the system bars and pads itself (insets.css); false: padded for it. */
    @JavascriptInterface
    fun enableInsets(enable: Boolean = true) {
        if (context is WebUIActivity) {
            context.enableInsets(enable)
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

    fun canGoBack(): Boolean {
        return webView.canGoBack()
    }

    fun goBack() {
        webView.post {
            webView.goBack()
        }
    }

    /** Pins the same shortcut as the plugin's WebUI shortcut sheet: banner icon, else initials. */
    @JavascriptInterface
    fun createShortcut(): Boolean {
        return try {
            val plugin = Axeron.getPluginById(pluginId) ?: return false
            scope.launch {
                val banner = withContext(Dispatchers.IO) { loadPluginBannerData(plugin) }
                val icon = banner?.let { ShortcutIcons.loadBitmap(context, it) }
                    ?.let { withContext(Dispatchers.Default) { ShortcutIcons.fromImage(it) } }
                    ?: ShortcutIcons.fromInitials(
                        plugin.prop.name,
                        initialsBackground(),
                        initialsForeground()
                    )
                createWebUIShortcut(context, plugin, plugin.prop.name, icon)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // The sheet takes these from the Compose theme; here the closest system colors.
    private fun initialsBackground(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getColor(android.R.color.system_accent1_100)
        } else 0xFFD3E3FD.toInt()

    private fun initialsForeground(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getColor(android.R.color.system_accent1_900)
        } else 0xFF041E49.toInt()

    /**
     * The installed packages, fetched once per page like KSU Next's app list: pages call
     * getPackagesInfo in a loop, and each call re-reading every package over the binder (and
     * loading every label) froze the page until the list never showed. An empty answer (server
     * not reachable) isn't kept, so the next call tries again.
     */
    @Volatile
    private var packageCache: List<PackageInfo>? = null

    private fun packages(): List<PackageInfo> {
        packageCache?.let { return it }
        return Axeron.getPackages(0).also { if (it.isNotEmpty()) packageCache = it }
    }

    private val labelCache = ConcurrentHashMap<String, String>()

    private fun labelOf(pkg: PackageInfo): String = labelCache.getOrPut(pkg.packageName) {
        runCatching { pkg.applicationInfo?.loadLabel(context.packageManager)?.toString() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: pkg.packageName
    }

    @JavascriptInterface
    fun listPackages(type: String): String {
        val packageNames = packages()
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
    fun getPackagesInfo(packageNamesJson: String): String {
        val packageNames = JSONArray(packageNamesJson)
        val jsonArray = JSONArray()
        val appMap = packages().associateBy { it.packageName }
        for (i in 0 until packageNames.length()) {
            val pkgName = packageNames.getString(i)
            val appInfo = appMap[pkgName]
            if (appInfo != null) {
                val app = appInfo.applicationInfo
                val obj = JSONObject()
                obj.put("packageName", appInfo.packageName)
                obj.put("versionName", appInfo.versionName ?: "")
                obj.put("versionCode", PackageInfoCompat.getLongVersionCode(appInfo))
                obj.put("appLabel", labelOf(appInfo))
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

    private val packageIconCache = ConcurrentHashMap<String, String>()

    /** "" when the package or its icon can't be loaded, so it isn't tried again. */
    private fun iconOf(pkgName: String, size: Int, outputStream: ByteArrayOutputStream): String =
        packageIconCache.getOrPut(pkgName) {
            try {
                val pm = context.packageManager
                val appInfo = packages().firstOrNull { it.packageName == pkgName }?.applicationInfo
                    ?: pm.getApplicationInfo(pkgName, 0)
                val bitmap = drawableToBitmap(appInfo.loadIcon(pm), size)
                outputStream.reset()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } catch (_: Exception) {
                ""
            }
        }

    @JavascriptInterface
    fun cacheAllPackageIcons(size: Int) {
        val outputStream = ByteArrayOutputStream()
        for (pkg in packages()) {
            iconOf(pkg.packageName, size, outputStream)
        }
    }

    @JavascriptInterface
    fun getPackagesIcons(packageNamesJson: String, size: Int): String {
        val packageNames = JSONArray(packageNamesJson)
        val jsonArray = JSONArray()
        val outputStream = ByteArrayOutputStream()
        for (i in 0 until packageNames.length()) {
            val pkgName = packageNames.getString(i)
            val obj = JSONObject()
            obj.put("packageName", pkgName)
            obj.put("icon", iconOf(pkgName, size, outputStream))
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    // ---- Files: as the server (root or shell), the same reach as exec().

    private fun String.quoted() = "'" + replace("'", "'\\''") + "'"

    /** A file command run like exec(), answering only whether it succeeded. */
    private fun execResult(cmd: String): Boolean = runCatching {
        runBlocking(Dispatchers.IO) {
            AxeronPluginService.execWithIO(
                cmd = cmd,
                useBusybox = false,
                hideStderr = false,
                pluginId = pluginId
            ).isSuccess()
        }
    }.getOrDefault(false)

    @JavascriptInterface
    fun listFile(path: String): String {
        return try {
            val files = Axeron.newFileService()
                ?.getDirectories(path)
                ?.mapNotNull { it.name }
                ?: emptyList()
            JSONArray(files).toString()
        } catch (_: Exception) {
            JSONArray().toString()
        }
    }

    @JavascriptInterface
    fun readFile(path: String): String {
        return try {
            Axeron.newFileService()
                ?.setFileInputStream(path)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    /** Streamed through the file service: the server can't read a temp file in our cache dir. */
    @JavascriptInterface
    fun writeFile(path: String, content: String): Boolean {
        return try {
            val session = Axeron.newFileService()?.getStreamSession(path, false, false)
                ?: return false
            session.outputStream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            session.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun removeFile(path: String): Boolean = execResult("rm -rf ${path.quoted()}")

    @JavascriptInterface
    fun moveFile(src: String, dest: String): Boolean =
        execResult("mv ${src.quoted()} ${dest.quoted()}")

    @JavascriptInterface
    fun copyFile(src: String, dest: String): Boolean =
        execResult("cp -a ${src.quoted()} ${dest.quoted()}")

    val fileOutputStream = FileOutputStreamInterface()

    @JavascriptInterface
    fun fileOutputStream(): FileOutputStreamInterface {
        return fileOutputStream
    }

    fun destroy() {
        fileOutputStream.closeAll()
    }
}

/** ksu.fileOutputStream(): files written in chunks, each stream a file service session. */
class FileOutputStreamInterface {
    private class OpenStream(
        val session: AxeronFileService.StreamSession,
        val output: BufferedOutputStream
    )

    private val openStreams = ConcurrentHashMap<String, OpenStream>()

    @JavascriptInterface
    fun open(path: String, append: Boolean): String {
        return try {
            val session = Axeron.newFileService()?.getStreamSession(path, false, append)
                ?: return ""
            val bos = BufferedOutputStream(session.outputStream, 64 * 1024)
            val id = UUID.randomUUID().toString()
            openStreams[id] = OpenStream(session, bos)
            id
        } catch (e: Exception) {
            Log.e(TAG, "open failed", e)
            ""
        }
    }

    @JavascriptInterface
    fun open(path: String): String {
        return open(path, false)
    }

    @JavascriptInterface
    fun writeByte(id: String, b: Int): Boolean {
        return runCatching {
            val bos = openStreams[id]?.output ?: return false
            synchronized(bos) { bos.write(b) }
            true
        }.getOrElse {
            Log.e(TAG, "writeByte failed", it)
            false
        }
    }

    @JavascriptInterface
    fun write(id: String, base64: String): Boolean {
        return runCatching {
            val bos = openStreams[id]?.output ?: return false
            val data = Base64.decode(base64, Base64.NO_WRAP)
            synchronized(bos) { bos.write(data) }
            true
        }.getOrElse {
            Log.e(TAG, "write failed", it)
            false
        }
    }

    @JavascriptInterface
    fun flush(id: String): Boolean {
        return runCatching {
            val bos = openStreams[id]?.output ?: return false
            synchronized(bos) { bos.flush() }
            true
        }.getOrElse {
            Log.e(TAG, "flush failed", it)
            false
        }
    }

    /** Waits for the server to finish the file: true only once it is fully written. */
    @JavascriptInterface
    fun close(id: String): Boolean {
        val stream = openStreams.remove(id) ?: return false
        return runCatching {
            synchronized(stream.output) { stream.output.close() }
            stream.session.isSuccessful
        }.getOrElse {
            Log.e(TAG, "close failed", it)
            false
        }
    }

    fun closeAll() {
        openStreams.forEach { (id, stream) ->
            runCatching {
                synchronized(stream.output) { stream.output.close() }
            }.onFailure {
                Log.e(TAG, "closeAll failed for $id", it)
            }
        }
        openStreams.clear()
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
