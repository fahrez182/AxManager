package frb.axeron.manager.ui.webui

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.MimeTypeMap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import frb.axeron.api.Axeron
import frb.axeron.api.core.AxeronSettings
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.disableWebUIShortcut
import frb.axeron.manager.ui.util.WebUiShortcutStore
import frb.axeron.manager.ui.util.awaitAxeronBinder
import frb.axeron.manager.ui.viewmodel.AppsViewModel
import frb.axeron.manager.ui.webui.interfaces.AxWebInterface
import frb.axeron.manager.ui.webui.interfaces.KsuWebInterface
import frb.axeron.server.PluginInfo
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
class WebUIActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ID = "id"
        private const val PLUGIN_POLL_INTERVAL_MS = 2000L

        /**
         * The one way to open a plugin's WebUI (in-app and home-screen shortcuts alike).
         * documentLaunchMode matches tasks by intent data, not extras, so the data Uri gives
         * every plugin its own recents task instead of all WebUIs sharing one.
         */
        fun intentFor(context: Context, pluginId: String): Intent =
            Intent(context, WebUIActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.fromParts("axwebui", pluginId, null))
                .putExtra(EXTRA_ID, pluginId)
    }

    private lateinit var webView: WebView
    private lateinit var insets: Insets
    private lateinit var plugin: PluginInfo

    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private lateinit var saveFileLauncher: ActivityResultLauncher<Intent>
    private var pendingDownloadData: ByteArray? = null
    private var pendingDownloadSuggestedFilename: String? = null

    private var insetsContinuation: CancellableContinuation<Unit>? = null

    /** ksu.enableInsets(): true draws under the system bars (the default), false keeps clear of them. */
    private var insetsEnabled = true

    private lateinit var ksuInterface: KsuWebInterface

    fun enableInsets(enable: Boolean) = runOnUiThread {
        if (insetsEnabled == enable) return@runOnUiThread
        insetsEnabled = enable
        if (::webView.isInitialized) ViewCompat.requestApplyInsets(webView)
    }

    fun erudaConsole(context: android.content.Context): String {
        return context.assets.open("js/eruda.min.js").bufferedReader().use { it.readText() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        // Recents identity first, before any binder wait, so the task never shows as "AxManager".
        intent.getStringExtra(EXTRA_ID)?.let { applyTaskIdentity(it, pluginName = null) }

        fileChooserLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data
                    var uris: Array<Uri>? = null
                    data?.dataString?.let {
                        uris = arrayOf(it.toUri())
                    }
                    data?.clipData?.let { clipData ->
                        uris = Array(clipData.itemCount) { i ->
                            clipData.getItemAt(i).uri
                        }
                    }
                    filePathCallback?.onReceiveValue(uris)
                } else {
                    filePathCallback?.onReceiveValue(null)
                }
                filePathCallback = null
            }

        saveFileLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        contentResolver.openOutputStream(uri)?.use { out ->
                            pendingDownloadData?.let { out.write(it) }
                        }
                    }
                }
                pendingDownloadData = null
                pendingDownloadSuggestedFilename = null
            }

        lifecycleScope.launch {
            setupWebView()
        }
    }

    private suspend fun setupWebView() {
        val pluginId = intent.getStringExtra(EXTRA_ID) ?: run {
            Toast.makeText(this, "Plugin-Id not found", Toast.LENGTH_SHORT).show()
            finishAndRemoveTask()
            return
        }

        // Server down: say so, but leave the shortcut alone — the plugin may be fine.
        if (!awaitAxeronBinder()) {
            close(R.string.webui_service_not_running, pluginId)
            return
        }

        // Same checks the live watcher uses; a missing plugin also retires its shortcut.
        val lookup = withContext(Dispatchers.IO) { fetchPlugin(pluginId) }
        val found = (lookup as? PluginLookup.Found)?.plugin
        val reason = if (lookup == PluginLookup.ServerDown) {
            R.string.webui_service_not_running
        } else {
            blockReason(found)
        }
        if (reason != null || found == null) {
            close(reason ?: R.string.shortcut_plugin_uninstalled, pluginId)
            return
        }
        plugin = found

        // A shortcut's custom label was applied in onCreate; otherwise fall back to the plugin name.
        applyTaskIdentity(pluginId, plugin.prop.name)

        val developerOptionsEnabled = AxeronSettings.getEnableDeveloperOptions()
        val enableWebDebugging = AxeronSettings.getEnableWebDebugging()

        WebView.setWebContentsDebuggingEnabled(developerOptionsEnabled && enableWebDebugging)

        val pluginDir =
            File(PathHelper.getWorkingPath(Axeron.getAxeronInfo().isRoot(),AxeronApiConstant.folder.PARENT_PLUGIN), plugin.dirId)
        val webRoot = File(pluginDir, "webroot")

        insets = Insets(0, 0, 0, 0)

        webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            val density = resources.displayMetrics.density
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
                val inset = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                // Margins, not padding: a WebView draws its content under its own padding.
                (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                    val margin = if (insetsEnabled) androidx.core.graphics.Insets.NONE else inset
                    params.setMargins(margin.left, margin.top, margin.right, margin.bottom)
                    view.layoutParams = params
                }
                insets = Insets(
                    top = (inset.top / density).toInt(),
                    bottom = (inset.bottom / density).toInt(),
                    left = (inset.left / density).toInt(),
                    right = (inset.right / density).toInt()
                )
                insetsContinuation?.resumeWith(Result.success(Unit))
                insetsContinuation = null
                WindowInsetsCompat.CONSUMED
            }
        }

        setContentView(webView)

        if (insets == Insets(0, 0, 0, 0)) {
            suspendCancellableCoroutine { cont ->
                insetsContinuation = cont
                cont.invokeOnCancellation {
                    if (insetsContinuation === cont) {
                        insetsContinuation = null
                    }
                }
            }
        }

        val axPathHandler = AxPathHandler(webRoot) { insets }
        val iconHandler = AppsViewModel.AppInfo.Handler()

        val axWebLoader = AxWebLoader.Builder()
            .addScheme("ax")
            .addDomain("plugin.local").addPathHandler("/", axPathHandler).done()
            .addDomain("package.icon").addPathHandler("/", iconHandler).done()
            .addDomain("kernelsu.js").addHandler { _, _, _ ->
                return@addHandler WebResourceResponse(
                    "application/javascript",
                    null,
                    assets.open("js/kernelsu.js")
                )
            }.done()
            .addDomain("axeron.js").addHandler { _, _, request ->
                Log.d("WebUIActivity", "request: " + request.url.toString())
                return@addHandler WebResourceResponse(
                    "application/javascript",
                    null,
                    assets.open("js/axeron.js")
                )
            }.done()
            .addScheme("ksu")
            .addDomain("icon").addPathHandler("/", iconHandler).done()
            .addScheme("https")
            .addDomain("mui.kernelsu.org").addPathHandler("/", axPathHandler).done()
            .build()

        ksuInterface = KsuWebInterface(this@WebUIActivity, webView, pluginDir)

        // Back walks the page's own history first; only enabled while there is some.
        val backCallback = onBackPressedDispatcher.addCallback(this, enabled = false) {
            ksuInterface.goBack()
        }

        val webViewClient = object : WebViewClient() {
            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                backCallback.isEnabled = ksuInterface.canGoBack()
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                Log.d("WebUIActivity", request.url.toString())
                return axWebLoader.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (developerOptionsEnabled && enableWebDebugging) {
                    view?.evaluateJavascript(
                        erudaConsole(this@WebUIActivity),
                        null
                    )
                    view?.evaluateJavascript("eruda.init();", null)
                }
            }
        }

        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false

            addJavascriptInterface(AxWebInterface(this@WebUIActivity, this, pluginDir), "Axeron")
            addJavascriptInterface(ksuInterface, "ksu")

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@WebUIActivity.filePathCallback = filePathCallback
                    val intent = fileChooserParams?.createIntent()
                        ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                    if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    try {
                        fileChooserLauncher.launch(intent)
                    } catch (_: Exception) {
                        this@WebUIActivity.filePathCallback?.onReceiveValue(null)
                        this@WebUIActivity.filePathCallback = null
                        return false
                    }
                    return true
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        val message = it.message()
                        val prefix = "ksuBlobData:"
                        if (message.startsWith(prefix)) {
                            val jsonData = message.substring(prefix.length)
                            try {
                                val json = JSONObject(jsonData)
                                val dataUrl = json.getString("dataUrl")
                                val mimeType = json.getString("mimeType")
                                saveDataUrlToDownloads(dataUrl, mimeType)
                                return true
                            } catch (_: org.json.JSONException) {
                                Toast.makeText(
                                    this@WebUIActivity,
                                    "Error parsing blob data from console",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    return super.onConsoleMessage(consoleMessage)
                }
            }

            setWebViewClient(webViewClient)

            setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                if (url.startsWith("blob:")) {
                    val escContentDisposition = contentDisposition?.replace("'", "\\'") ?: ""
                    val escMimeType = mimeType?.replace("'", "\\'") ?: ""
                    val script = """
                        javascript:(function() {
                            fetch('$url')
                                .then(response => response.blob())
                                .then(blob => {
                                    const reader = new FileReader();
                                    reader.onloadend = function() {
                                        const payload = {
                                            dataUrl: reader.result,
                                            contentDisposition: '${escContentDisposition}',
                                            mimeType: '${escMimeType}'
                                        };
                                        console.log('ksuBlobData:' + JSON.stringify(payload));
                                    };
                                    reader.readAsDataURL(blob);
                                });
                        })();
                    """.trimIndent()
                    webView.evaluateJavascript(script, null)
                } else if (url.startsWith("data:")) {
                    saveDataUrlToDownloads(url, mimeType)
                } else {
                    Toast.makeText(
                        this@WebUIActivity,
                        "Cannot download from this URL type",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            clearCache(true)
            loadUrl("ax://plugin.local/index.html")
        }

        watchPluginState(pluginId)
    }

    /**
     * Keeps the open WebUI in step with the plugin: re-checks right away whenever the screen
     * comes back (e.g. after disabling it in the manager) and periodically while visible,
     * since the server has no change events. Closes as soon as the plugin can't be shown.
     */
    private fun watchPluginState(pluginId: String) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val latest = withContext(Dispatchers.IO) { fetchPlugin(pluginId) }
                    val reason = when (latest) {
                        PluginLookup.ServerDown -> R.string.webui_service_not_running
                        is PluginLookup.Found -> blockReason(latest.plugin)
                        PluginLookup.Missing -> blockReason(null)
                    }
                    if (reason != null) {
                        close(reason, pluginId)
                        return@repeatOnLifecycle
                    }
                    delay(PLUGIN_POLL_INTERVAL_MS)
                }
            }
        }
    }

    private sealed interface PluginLookup {
        data object ServerDown : PluginLookup
        data object Missing : PluginLookup
        data class Found(val plugin: PluginInfo) : PluginLookup
    }

    /**
     * getPluginById() also answers null when the binder died mid-call, so a null only means
     * "uninstalled" while the server is still reachable.
     */
    private fun fetchPlugin(pluginId: String): PluginLookup {
        if (!Axeron.pingBinder()) return PluginLookup.ServerDown
        val found = try {
            Axeron.getPluginById(pluginId)
        } catch (_: Exception) {
            return PluginLookup.ServerDown
        }
        return when {
            found != null -> PluginLookup.Found(found)
            Axeron.pingBinder() -> PluginLookup.Missing
            else -> PluginLookup.ServerDown
        }
    }

    /** Why [plugin]'s WebUI can't be shown right now (null = uninstalled), or null if it can. */
    private fun blockReason(plugin: PluginInfo?): Int? = when {
        plugin == null -> R.string.shortcut_plugin_uninstalled
        plugin.remove -> R.string.webui_plugin_removed
        !plugin.enabled -> R.string.webui_plugin_disabled
        !plugin.hasWebUi -> R.string.webui_plugin_no_webui
        else -> null
    }

    private fun close(@StringRes reason: Int, pluginId: String) {
        // Only a plugin that is truly gone retires its shortcut; disabled/removed-pending can come back.
        if (reason == R.string.shortcut_plugin_uninstalled) disableWebUIShortcut(this, pluginId)
        Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
        finishAndRemoveTask()
    }

    /**
     * Recents label/icon: what the user gave the home-screen shortcut, else the plugin name.
     * The deprecated constructor is the only one that still takes a Bitmap icon.
     */
    @Suppress("DEPRECATION")
    private fun applyTaskIdentity(pluginId: String, pluginName: String?) {
        val label = WebUiShortcutStore.label(this, pluginId) ?: pluginName ?: return
        setTaskDescription(
            ActivityManager.TaskDescription(label, WebUiShortcutStore.icon(this, pluginId))
        )
    }

    private fun extractMimeTypeAndBase64Data(dataUrl: String): Pair<String, String>? {
        if (!dataUrl.startsWith("data:")) return null
        val commaIndex = dataUrl.indexOf(',')
        if (commaIndex == -1) return null

        val header = dataUrl.substring(5, commaIndex)
        val data = dataUrl.substring(commaIndex + 1)
        val mimeType = header.substringBefore(';').ifEmpty {
            "application/octet-stream"
        }

        return mimeType to data
    }

    private fun saveDataUrlToDownloads(
        dataUrl: String,
        mimeTypeFromListener: String
    ) {
        val extracted = extractMimeTypeAndBase64Data(dataUrl) ?: run {
            Toast.makeText(this, "Invalid data URL", Toast.LENGTH_SHORT).show()
            return
        }

        val (mimeFromData, base64Data) = extracted

        val finalMimeType =
            if (mimeFromData == "application/octet-stream" && mimeTypeFromListener.isNotBlank())
                mimeTypeFromListener
            else
                mimeFromData

        var extension =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(finalMimeType)

        if (!extension.isNullOrEmpty() && !extension.startsWith(".")) {
            extension = ".$extension"
        }
        if (extension.isNullOrEmpty()) {
            extension = ""
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        val fileName = "${plugin.dirId}_${sdf.format(Date())}${extension ?: ""}"

        try {
            pendingDownloadData = Base64.decode(base64Data, Base64.DEFAULT)
            pendingDownloadSuggestedFilename = fileName

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = finalMimeType
                putExtra(Intent.EXTRA_TITLE, fileName)
            }

            saveFileLauncher.launch(intent)

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error preparing file: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            pendingDownloadData = null
            pendingDownloadSuggestedFilename = null
        }
    }

    override fun onDestroy() {
        // setupWebView() finishes early (disabled/removed plugin, missing id, no WebUI)
        // before the WebView exists, e.g. when opened from a home-screen shortcut.
        if (::webView.isInitialized) {
            webView.apply {
                stopLoading()
                removeAllViews()
                destroy()
            }
        }
        if (::ksuInterface.isInitialized) {
            // Closing a stream waits for the server to finish the file: not on the main thread.
            Thread { ksuInterface.destroy() }.start()
        }
        super.onDestroy()
    }
}