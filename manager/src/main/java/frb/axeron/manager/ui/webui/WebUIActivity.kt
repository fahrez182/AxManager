package frb.axeron.manager.ui.webui

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import frb.axeron.api.Axeron
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.utils.PathHelper
import frb.axeron.data.AxeronConstant
import frb.axeron.data.PluginInfo
import frb.axeron.manager.ui.viewmodel.AppsViewModel
import frb.axeron.manager.ui.webui.interfaces.AxWebInterface
import frb.axeron.manager.ui.webui.interfaces.KsuWebInterface
import frb.axeron.server.utils.AxWebLoader
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
class WebUIActivity : ComponentActivity() {
    private lateinit var insets: Insets
    private var insetsContinuation: CancellableContinuation<Unit>? = null
    private lateinit var plugin: PluginInfo

    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private lateinit var webView: WebView

    fun erudaConsole(context: android.content.Context): String {
        return context.assets.open("js/eruda.min.js").bufferedReader().use { it.readText() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable edge to edge
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                var uris: Array<Uri>? = null
                data?.dataString?.let {
                    uris = arrayOf(it.toUri())
                }
                data?.clipData?.let { clipData ->
                    uris = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                }
                filePathCallback?.onReceiveValue(uris)
                filePathCallback = null
            } else {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            }
        }

        lifecycleScope.launch {
            setupWebView()
        }
    }

    private suspend fun setupWebView() {
        plugin = Axeron.getPluginById(intent.getStringExtra("id") ?: finishAndRemoveTask().let {
            Toast.makeText(this, "Plugin-Id not found", Toast.LENGTH_SHORT).show()
            return
        })

        if (!plugin.hasWebUi || plugin.remove || !plugin.enabled) {
            Toast.makeText(
                this,
                when {
                    plugin.remove -> "Plugin removed"
                    !plugin.enabled -> "Plugin disabled"
                    else -> "Plugin has no web UI"
                },
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        val taskDescription = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityManager.TaskDescription.Builder().setLabel("AxWebUI | ${plugin.prop.name}")
                .build()
        } else {
            @Suppress("DEPRECATION")
            ActivityManager.TaskDescription("AxWebUI | ${plugin.prop.name}")
        }
        setTaskDescription(taskDescription)

        val developerOptionsEnabled = AxeronSettings.getEnableDeveloperOptions()
        val enableWebDebugging = AxeronSettings.getEnableWebDebugging()

        WebView.setWebContentsDebuggingEnabled(developerOptionsEnabled && enableWebDebugging)

        val pluginDir =
            File(PathHelper.getShellPath(AxeronConstant.folder.PARENT_PLUGIN), plugin.dirId)
        val webRoot = File(pluginDir, "webroot")

        insets = Insets(0, 0, 0, 0)

        webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            val density = resources.displayMetrics.density
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
                val inset = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
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
                    insetsContinuation = null
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

        val webViewClient = object : WebViewClient() {
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

            addJavascriptInterface(AxWebInterface(this@WebUIActivity, pluginDir), "Axeron")
            addJavascriptInterface(KsuWebInterface(this@WebUIActivity, this, pluginDir), "ksu")

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@WebUIActivity.filePathCallback = filePathCallback
                    val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
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
                                Toast.makeText(this@WebUIActivity, "Error parsing blob data from console", Toast.LENGTH_LONG).show()
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
    }

    private fun extractMimeTypeAndBase64Data(dataUrl: String): Pair<String, String>? {
        val prefix = "data:"
        if (!dataUrl.startsWith(prefix)) return null
        val commaIndex = dataUrl.indexOf(',')
        if (commaIndex == -1) return null
        val header = dataUrl.substring(prefix.length, commaIndex)
        val data = dataUrl.substring(commaIndex + 1)
        val mimeType = header.substringBefore(';', header).ifEmpty { "application/octet-stream" }
        return Pair(mimeType, data)
    }

    private lateinit var saveFileLauncher: ActivityResultLauncher<Intent>
    private var pendingDownloadData: ByteArray? = null
    private var pendingDownloadSuggestedFilename: String? = null

    private fun saveDataUrlToDownloads(dataUrl: String, mimeTypeFromListener: String) {
        val (mimeType, base64Data) = extractMimeTypeAndBase64Data(dataUrl) ?: run {
            Toast.makeText(this, "Invalid data URL", Toast.LENGTH_SHORT).show()
            return
        }

        val finalMimeType = if (mimeType == "application/octet-stream" && mimeTypeFromListener.isNotBlank()) mimeTypeFromListener else mimeType
        var extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(finalMimeType)
        if (extension != null && !extension.startsWith(".")) {
            extension = ".$extension"
        }
        if (extension.isNullOrEmpty()) {
            extension = ""
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        val formattedDate = sdf.format(Date(System.currentTimeMillis()))
        val fileName = "${plugin.prop.id}_${formattedDate}${extension}"

        try {
            val decodedData = Base64.decode(base64Data, Base64.DEFAULT)

            pendingDownloadData = decodedData
            pendingDownloadSuggestedFilename = fileName

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = finalMimeType
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            saveFileLauncher.launch(intent)

        } catch (e: Exception) {
            Toast.makeText(this, "Error preparing file for saving: ${e.message}", Toast.LENGTH_LONG).show()
            pendingDownloadData = null
            pendingDownloadSuggestedFilename = null
        }
    }

    override fun onDestroy() {
        webView.apply {
            stopLoading()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }
}