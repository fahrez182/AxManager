package frb.axeron.manager.ui.webui

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.MarginLayoutParams
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import frb.axeron.api.Axeron
import frb.axeron.api.utils.PathHelper
import frb.axeron.data.AxeronConstant
import frb.axeron.data.PluginInfo
import frb.axeron.manager.ui.viewmodel.AppsViewModel
import frb.axeron.manager.ui.webui.interfaces.AxWebInterface
import frb.axeron.manager.ui.webui.interfaces.KsuWebInterface
import frb.axeron.server.utils.AxWebLoader
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
class WebUIActivity : ComponentActivity() {

    private lateinit var ksuWebInterface: KsuWebInterface
    private lateinit var axWebInterface: AxWebInterface
    private lateinit var plugin: PluginInfo

    private var webView = null as WebView?

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

        plugin = runCatching {
            Axeron.getPluginById(intent.getStringExtra("id") ?: finishAndRemoveTask().let {
                Toast.makeText(this, "Plugin-Id not found", Toast.LENGTH_SHORT).show()
                return
            })
        }.getOrElse {
            Toast.makeText(this, "Plugin not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)
        val enableWebDebugging = prefs.getBoolean("enable_web_debugging", false)

        WebView.setWebContentsDebuggingEnabled(developerOptionsEnabled && enableWebDebugging)

        val pluginDir =
            File(PathHelper.getShellPath(AxeronConstant.folder.PARENT_PLUGIN), plugin.dirId)
        val webRoot = File(pluginDir, "webroot")

        val axWebLoader = AxWebLoader.Builder()
            .addScheme("ax")
            .addDomain("plugin.local").addPathHandler("/", AxPathHandler(webRoot))
            .addDomain("package.icon").addPathHandler("/", AppsViewModel.AppInfo.Handler())
            .addDomain("kernelsu.js").addHandler { view, request ->
                return@addHandler WebResourceResponse(
                    "application/javascript",
                    null,
                    assets.open("js/kernelsu.js")
                )
            }
            .addDomain("axeron.js").addHandler { _, request ->
                Log.d("WebUIActivity", "request: " + request.url.toString())
                return@addHandler WebResourceResponse(
                    "application/javascript",
                    null,
                    assets.open("js/axeron.js")
                )
            }
            .done()
            .addScheme("ksu")
            .addDomain("icon").addPathHandler("/", AppsViewModel.AppInfo.Handler())
            .done()
            .build()

        val webView = WebView(this).apply {
            webView = this
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updateLayoutParams<MarginLayoutParams> {
                    leftMargin = inset.left
                    rightMargin = inset.right
                    topMargin = inset.top
                    bottomMargin = inset.bottom
                }
                return@setOnApplyWindowInsetsListener insets
            }

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false

            axWebInterface = AxWebInterface(this@WebUIActivity, pluginDir)
            addJavascriptInterface(axWebInterface, "Axeron")
            ksuWebInterface = KsuWebInterface(this@WebUIActivity, this, pluginDir)
            addJavascriptInterface(ksuWebInterface, "ksu")

            setWebViewClient(object : WebViewClient() {
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
            })
            clearCache(true)
            loadUrl("ax://plugin.local/index.html")
        }
        setContentView(webView)
    }

    override fun onDestroy() {
        webView?.apply {
            stopLoading()
            removeAllViews()
            destroy()
            webView = null
        }
        super.onDestroy()
    }
}