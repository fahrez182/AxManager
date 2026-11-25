package frb.axeron.manager.ui.screen.plugin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.AxeronPluginService
import frb.axeron.manager.AxeronApplication.Companion.axeronApp
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxPullToRefreshBox
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.disableWebUIShortcut
import frb.axeron.manager.ui.component.enableWebUIShortcut
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.util.DownloadListener
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.util.download
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel
import frb.axeron.server.PluginInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin

private object PluginListConfig {
    const val TAG = "PluginList"
    const val ACTION_THROTTLE_MS = 700L
    const val CLICK_THROTTLE_MS = 450L
    const val EXPAND_THROTTLE_MS = 150L
    const val REFRESH_THROTTLE_MS = 1500L
    const val DOWNLOAD_TOAST_THROTTLE_MS = 4000L
    const val UPDATE_CACHE_TTL_MS = 5L * 60L * 1000L
    const val MAX_CHANGELOG_CHARS = 24000
    const val MAX_CHANGELOG_MARKDOWN_CHARS = 16000
    const val MAX_URL_LEN = 2048
    const val MAX_FILE_NAME_LEN = 80
    const val MAX_VERSION_LEN = 40
    const val MAX_NAME_LEN = 80
    const val LIVE_CHANNEL_ID = "axeron_live_2026"
    const val LIVE_CHANNEL_NAME = "Axeron Live 2026"
}

private val ILLEGAL_FILE_CHARS = Regex("[\\\\/:*?\"<>|]")
private val MULTI_UNDERSCORE = Regex("_{2,}")

private object PluginUpdateCache {
    private data class Entry(val value: Triple<String, String, String>, val time: Long)
    private val map = LinkedHashMap<String, Entry>(64)

    @Synchronized
    fun get(id: String): Triple<String, String, String>? {
        if (id.isBlank()) return null
        val e = map[id] ?: return null
        if (SystemClock.uptimeMillis() - e.time > PluginListConfig.UPDATE_CACHE_TTL_MS) {
            map.remove(id)
            return null
        }
        return e.value
    }

    @Synchronized
    fun put(id: String, value: Triple<String, String, String>) {
        if (id.isBlank()) return
        if (map.size > 120) {
            val it = map.entries.iterator()
            var c = 0
            while (it.hasNext() && c < 20) { it.next(); it.remove(); c++ }
        }
        map[id] = Entry(value, SystemClock.uptimeMillis())
    }
}

private fun safeFormat(template: String, vararg args: Any?): String {
    if (template.isEmpty()) return ""
    return try {
        template.format(*args)
    } catch (_: Exception) {
        template
    }
}

private fun isBlockedHost(urlLower: String): Boolean {
    return urlLower.contains("localhost") ||
        urlLower.contains("127.") ||
        urlLower.contains("0.0.0.0") ||
        urlLower.contains("[::1]") ||
        urlLower.contains("169.254.169.254") ||
        urlLower.contains("2130706433")
}

private fun isSafeHttpUrl(url: String): Boolean {
    val t = url.trim()
    if (t.isEmpty() || t.length > PluginListConfig.MAX_URL_LEN) return false
    if (t.contains(' ') || t.contains('\n') || t.contains('\r') || t.contains('\t')) return false
    val low = t.lowercase(Locale.US)
    if (!(low.startsWith("https://") || low.startsWith("http://"))) return false
    if (low.contains("file:") || low.contains("javascript:") || low.contains("data:")) return false
    if (isBlockedHost(low)) return false
    val after = t.substringAfter("://", "")
    if (after.isBlank() || !after.contains('.')) return false
    if (after.contains('\\')) return false
    return true
}

private fun sanitizeFileName(raw: String, fallbackId: String): String {
    var s = raw.trim().ifBlank { "$fallbackId.zip" }
    s = s.filter { it.code >= 32 }
    s = ILLEGAL_FILE_CHARS.replace(s, "_").trim()
    s = MULTI_UNDERSCORE.replace(s, "_")
    if (s.length > PluginListConfig.MAX_FILE_NAME_LEN) {
        val isZip = s.endsWith(".zip", ignoreCase = true)
        s = s.take(PluginListConfig.MAX_FILE_NAME_LEN)
        if (isZip && !s.endsWith(".zip", ignoreCase = true)) {
            s = s.dropLast(4) + ".zip"
        }
    }
    if (!s.endsWith(".zip", ignoreCase = true)) s += ".zip"
    if (s == ".zip") s = "${fallbackId.ifBlank { "plugin" }}.zip"
    return s
}

private fun sanitizeVersion(raw: String): String {
    var s = raw.trim().filter { it.code >= 32 }
    s = ILLEGAL_FILE_CHARS.replace(s, "_")
    s = s.replace(" ", "_").replace("\n", "_").replace("\r", "_").replace("\t", "_")
    s = MULTI_UNDERSCORE.replace(s, "_")
    if (s.length > PluginListConfig.MAX_VERSION_LEN) s = s.take(PluginListConfig.MAX_VERSION_LEN)
    if (s.isBlank()) s = "update"
    return s
}

private fun safePluginName(plugin: PluginInfo): String {
    return try {
        val n = plugin.prop.name.trim()
        if (n.isNotBlank()) n.take(PluginListConfig.MAX_NAME_LEN)
        else plugin.prop.id.ifBlank { plugin.dirId.ifBlank { "plugin" } }
    } catch (_: Exception) {
        "plugin"
    }
}

private fun safePluginId(plugin: PluginInfo): String {
    return try {
        plugin.prop.id.ifBlank { plugin.dirId.ifBlank { "plugin" } }
    } catch (_: Exception) {
        "plugin"
    }
}

private fun safeDirId(plugin: PluginInfo): String {
    return try {
        plugin.dirId.trim()
    } catch (_: Exception) {
        ""
    }
}

private fun truncateChangelog(raw: String): String {
    val t = raw.trim()
    if (t.length <= PluginListConfig.MAX_CHANGELOG_CHARS) return t
    return t.take(PluginListConfig.MAX_CHANGELOG_CHARS) + "\n…"
}

private fun shouldUseMarkdown(changelog: String): Boolean {
    return changelog.length <= PluginListConfig.MAX_CHANGELOG_MARKDOWN_CHARS
}

private fun ensureLiveChannel(ctx: Context) {
    try {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(PluginListConfig.LIVE_CHANNEL_ID) != null) return
            val ch = NotificationChannel(
                PluginListConfig.LIVE_CHANNEL_ID,
                PluginListConfig.LIVE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live RGB updates 2026"
                enableLights(true)
                lightColor = 0xFFFF00FF.toInt()
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 90, 60, 90)
            }
            mgr.createNotificationChannel(ch)
        }
    } catch (_: Exception) {
    }
}

private fun postLiveSystemMessage(ctx: Context, title: String, body: String, id: Int) {
    try {
        ensureLiveChannel(ctx)
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ActivityCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val notif = NotificationCompat.Builder(ctx, PluginListConfig.LIVE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title.take(60).ifBlank { "Axeron Live" })
            .setContentText(body.take(180))
            .setColor(0xFF00E5FF.toInt())
            .setLights(0xFFFF00FF.toInt(), 400, 400)
            .setVibrate(longArrayOf(0, 90, 60, 90))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, notif)
    } catch (_: Exception) {
    }
}

@Composable
private fun UltraSandField(phase: Float) {
    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
        val w = size.width
        val h = size.height
        for (i in 0 until 22) {
            val speed = 1f + (i % 3) * 0.6f
            val px = ((i * 197.3f + phase * 220f * speed) % (w + 20f)) - 10f
            val py = h * 0.5f + sin(phase * 6.2831f + i * 0.9f) * (h * 0.32f)
            val rad = 2f + (i % 3) * 1.4f
            val col = when (i % 5) {
                0 -> Color(0xFF00E5FF)
                1 -> Color(0xFFFF2ED1)
                2 -> Color(0xFFB6FF00)
                3 -> Color(0xFFFFB300)
                else -> Color(0xFF7C4DFF)
            }
            drawCircle(color = col.copy(alpha = 0.85f), radius = rad, center = Offset(px, py))
        }
    }
}

@Composable
private fun UltraLiveBanner2026(count: Int, isRefreshing: Boolean) {
    val trans = rememberInfiniteTransition(label = "ultra2026")
    val shift by trans.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift"
    )
    val spin by trans.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val shimmer by trans.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmer"
    )
    val liveBrush = remember(shift) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF00F0FF),
                Color(0xFFFF2ED1),
                Color(0xFFB6FF00),
                Color(0xFFFFB300),
                Color(0xFF7C4DFF),
                Color(0xFF00F0FF)
            ),
            start = Offset(shift * 700f, 0f),
            end = Offset(shift * 700f + 520f, 320f)
        )
    }
    val ringBrush = remember {
        Brush.sweepGradient(
            listOf(
                Color(0xFF00F0FF),
                Color(0xFFFF2ED1),
                Color(0xFFB6FF00),
                Color(0xFFFFB300),
                Color(0xFF00F0FF)
            )
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF080A12))
            .border(1.5.dp, liveBrush, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer { rotationZ = spin }
                        .border(3.dp, ringBrush, CircleShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(liveBrush)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "AXERON • LIVE 2026 • 4K RGB",
                        style = TextStyle(
                            brush = liveBrush,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 0.6.sp
                        ),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isRefreshing) "در حال تازه‌سازی زنده…" else "$count پلاگین • 120Hz AMOLED • بهینه CPU/GPU",
                        color = Color.White.copy(alpha = 0.72f + shimmer * 0.22f),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.3.sp,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            UltraSandField(phase = shift)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginList(
    navigator: DestinationsNavigator,
    settings: SettingsViewModel,
    viewModel: PluginViewModel,
    modifier: Modifier,
    /** Scaffold padding; applied inside the list so it can scroll under the top bar. */
    contentPadding: PaddingValues,
    onInstallModule: (Uri) -> Unit,
    onClickModule: (plugin: PluginInfo) -> Unit,
    context: Context,
    snackBarHost: SnackbarHostState,
    listState: LazyListState
) {
    val failedEnable = stringResource(R.string.failed_enable_plugin)
    val failedDisable = stringResource(R.string.failed_disable_plugin)

    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog()

    var expandedPluginId by rememberSaveable { mutableStateOf<String?>(null) }

    val updateText = stringResource(R.string.update)
    val changelogText = stringResource(R.string.changelog)
    val downloadingText = stringResource(R.string.downloading_plugin)
    val startDownloadingText = stringResource(R.string.start_downloading_plugin)
    val fetchChangeLogFailed = stringResource(R.string.fetch_changelog_failed)

    val appContext = remember(context) { context.applicationContext ?: context }
    val haptic = LocalHapticFeedback.current
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)
    val floatingInset = LocalFloatingBarInset.current

    val scope = rememberCoroutineScope()
    val currentOnInstall by rememberUpdatedState(onInstallModule)
    val currentOnClick by rememberUpdatedState(onClickModule)

    val lastAction = remember { ConcurrentHashMap<String, Long>() }
    val lastDownloadToast = remember { ConcurrentHashMap<String, Long>() }
    val busyMap = remember { ConcurrentHashMap<String, Boolean>() }

    LaunchedEffect(Unit) {
        runCatching { ensureLiveChannel(appContext) }
    }

    fun canPass(key: String, windowMs: Long = PluginListConfig.ACTION_THROTTLE_MS): Boolean {
        val now = SystemClock.uptimeMillis()
        val last = lastAction[key] ?: 0L
        if (now - last < windowMs) return false
        lastAction[key] = now
        return true
    }

    fun tryBusy(key: String): Boolean {
        if (busyMap[key] == true) return false
        busyMap[key] = true
        return true
    }

    fun freeBusy(key: String) {
        busyMap[key] = false
    }

    suspend fun pingLive(title: String, body: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                postLiveSystemMessage(appContext, title, body, (title + body).hashCode())
            }
        }
    }

    suspend fun onModuleUpdate(
        plugin: PluginInfo,
        changelogUrl: String,
        downloadUrl: String,
        fileName: String,
    ) {
        val pluginId = safePluginId(plugin)
        val pluginName = safePluginName(plugin)
        val safeChangelogUrl = changelogUrl.trim()
        val safeDownloadUrl = downloadUrl.trim()
        val safeFileName = sanitizeFileName(fileName, pluginId)

        if (!isSafeHttpUrl(safeChangelogUrl) || !isSafeHttpUrl(safeDownloadUrl)) {
            Log.e(PluginListConfig.TAG, "blocked unsafe update url for $pluginId")
            withContext(Dispatchers.Main) {
                runCatching {
                    Toast.makeText(appContext, safeFormat(fetchChangeLogFailed, pluginName), Toast.LENGTH_SHORT).show()
                }
            }
            pingLive(pluginName, safeFormat(fetchChangeLogFailed, pluginName))
            return
        }

        if (!tryBusy("update_$pluginId")) return
        try {
            val changelogResult = loadingDialog.withLoading {
                withContext(Dispatchers.IO) {
                    runCatching {
                        axeronApp.okhttpClient.newCall(
                            Request.Builder().url(safeChangelogUrl).build()
                        ).execute().use { resp ->
                            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                            val raw = resp.body?.string().orEmpty()
                            truncateChangelog(raw)
                        }
                    }
                }
            }

            val showToast: suspend (String) -> Unit = { msg ->
                withContext(Dispatchers.Main) {
                    runCatching {
                        Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
                    }.onFailure { Log.e(PluginListConfig.TAG, "toast failed", it) }
                }
            }

            val changelog = changelogResult.getOrElse {
                Log.e(PluginListConfig.TAG, "changelog fetch failed for $pluginId", it)
                showToast(safeFormat(fetchChangeLogFailed, it.message ?: pluginName))
                pingLive(pluginName, safeFormat(fetchChangeLogFailed, pluginName))
                return
            }.ifBlank {
                showToast(safeFormat(fetchChangeLogFailed, pluginName))
                return
            }

            // changelog is not empty, show it and wait for confirm
            val useMd = shouldUseMarkdown(changelog)
            val confirmResult = runCatching {
                confirmDialog.awaitConfirm(
                    title = changelogText,
                    content = changelog,
                    markdown = useMd,
                    confirm = updateText,
                    dragHandle = true,
                )
}.getOrDefault(ConfirmResult.Dismissed)

if (confirmResult != ConfirmResult.Confirmed) {
                return
            }

            showToast(safeFormat(startDownloadingText, pluginName))
            pingLive(pluginName, safeFormat(startDownloadingText, pluginName))

            val downloading = safeFormat(downloadingText, pluginName)
            withContext(Dispatchers.IO) {
                runCatching {
                    download(
                        appContext,
                        safeDownloadUrl,
                        safeFileName,
                        downloading,
                        onDownloaded = { uri ->
                            runCatching { currentOnInstall(uri) }
                                .onFailure { Log.e(PluginListConfig.TAG, "onInstall failed", it) }
                            runCatching {
                                postLiveSystemMessage(
                                    appContext, pluginName, "واردات کامل شد • آماده نصب",
                                    ("done_$pluginId").hashCode()
                                )
                            }
                        },
                        onDownloading = {
                            val now = SystemClock.uptimeMillis()
                            val last = lastDownloadToast[pluginId] ?: 0L
                            if (now - last < PluginListConfig.DOWNLOAD_TOAST_THROTTLE_MS) return@download
                            lastDownloadToast[pluginId] = now
                            launch(Dispatchers.Main) {
                                runCatching {
                                    Toast.makeText(appContext, downloading, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }.onFailure {
                    Log.e(PluginListConfig.TAG, "download failed for $pluginId", it)
                    launch(Dispatchers.Main) {
                        runCatching { snackBarHost.showSnackbar(safeFormat(fetchChangeLogFailed, pluginName)) }
                    }
                    runCatching {
                        postLiveSystemMessage(
                            appContext, pluginName,
                            safeFormat(fetchChangeLogFailed, pluginName),
                            ("fail_$pluginId").hashCode()
                        )
                    }
                }
            }
        } finally {
            freeBusy("update_$pluginId")
        }
    }

    val askUninstallPlugin = stringResource(R.string.ask_uninstall_plugin)
    val uninstall = stringResource(R.string.uninstall)
    val cancel = stringResource(R.string.cancel)
    val moduleUninstallConfirm = stringResource(R.string.uninstall_plugin_confirmation)
    val successUninstall = stringResource(R.string.plugin_uninstalled)
    val failedUninstall = stringResource(R.string.failed_to_uninstall_plugin)
    val restartService = stringResource(R.string.restart_service)

    suspend fun onModuleUninstall(plugin: PluginInfo) {
        val pluginId = safePluginId(plugin)
        val pluginName = safePluginName(plugin)
        val dirId = safeDirId(plugin)
        if (dirId.isBlank()) {
            withContext(Dispatchers.Main) {
                runCatching { snackBarHost.showSnackbar(safeFormat(failedUninstall, pluginName)) }
            }
            return
        }
        if (!tryBusy("uninstall_$pluginId")) return
        try {
            val confirmResult = runCatching {
                confirmDialog.awaitConfirm(
                    askUninstallPlugin,
                    content = safeFormat(moduleUninstallConfirm, pluginName),
                    confirm = uninstall,
                    dismiss = cancel
                )
            }.getOrDefault(ConfirmResult.Cancelled)
            if (confirmResult != ConfirmResult.Confirmed) {
                return
            }

            val success = loadingDialog.withLoading {
                withContext(Dispatchers.IO) {
                    runCatching { AxeronPluginService.uninstallPlugin(dirId) }
                        .onFailure { Log.e(PluginListConfig.TAG, "uninstall failed $dirId", it) }
                        .getOrDefault(false)
                }
            }

            if (success) {
                // Its WebUI shortcut would point at nothing once re-ignited; retire it now.
                runCatching { disableWebUIShortcut(appContext, plugin.prop.id) }
                    .onFailure { Log.e(PluginListConfig.TAG, "disable shortcut failed", it) }
                runCatching { viewModel.fetchModuleList() }
                    .onFailure { Log.e(PluginListConfig.TAG, "fetch after uninstall failed", it) }
            }
            val uninstallMsg = if (success) {
                safeFormat(successUninstall, pluginName)
            } else {
                safeFormat(failedUninstall, pluginName)
            }
            val restartHint: String? = if (success) {
                restartService
            } else {
                null
            }
            withContext(Dispatchers.Main) {
                runCatching { Toast.makeText(appContext, uninstallMsg, Toast.LENGTH_SHORT).show() }
                if (restartHint != null) {
                    runCatching { snackBarHost.showSnackbar("$uninstallMsg • $restartHint") }
                } else {
                    runCatching { snackBarHost.showSnackbar(uninstallMsg) }
                }
            }
            pingLive(pluginName, uninstallMsg)
        } finally {
            freeBusy("uninstall_$pluginId")
        }
    }

    val askRestorePlugin = stringResource(R.string.ask_restore_plugin)
    val restore = stringResource(R.string.restore)
    val moduleRestoreConfirm = stringResource(R.string.restore_plugin_confirmation)

    suspend fun onModuleRestore(plugin: PluginInfo) {
        val pluginId = safePluginId(plugin)
        val pluginName = safePluginName(plugin)
        val dirId = safeDirId(plugin)
        if (dirId.isBlank()) return
        if (!tryBusy("restore_$pluginId")) return
        try {
            val confirmResult = runCatching {
                confirmDialog.awaitConfirm(
                    askRestorePlugin,
                    content = safeFormat(moduleRestoreConfirm, pluginName),
                    confirm = restore,
                    dismiss = cancel
                )
            }.getOrDefault(ConfirmResult.Cancelled)
            if (confirmResult != ConfirmResult.Confirmed) {
                return
            }

            val success = loadingDialog.withLoading {
                withContext(Dispatchers.IO) {
                    runCatching { AxeronPluginService.restorePlugin(dirId) }
                        .onFailure { Log.e(PluginListConfig.TAG, "restore failed $dirId", it) }
                        .getOrDefault(false)
                }
            }

            if (success) {
                runCatching { enableWebUIShortcut(appContext, plugin.prop.id) }
                    .onFailure { Log.e(PluginListConfig.TAG, "enable shortcut failed", it) }
                runCatching { viewModel.fetchModuleList() }
                    .onFailure { Log.e(PluginListConfig.TAG, "fetch after restore failed", it) }
                pingLive(pluginName, restore)
            } else {
                withContext(Dispatchers.Main) {
                    runCatching { snackBarHost.showSnackbar(safeFormat(failedEnable, pluginName)) }
                }
            }
        } finally {
            freeBusy("restore_$pluginId")
        }
    }

    val liveCount = try { viewModel.pluginList.size } catch (_: Exception) { 0 }

    AxPullToRefreshBox(
        modifier = modifier,
        isRefreshing = viewModel.isRefreshing,
        onRefresh = {
            if (!canPass("__refresh__", PluginListConfig.REFRESH_THROTTLE_MS)) return@AxPullToRefreshBox
            runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            runCatching { viewModel.fetchModuleList() }
                .onFailure { Log.e(PluginListConfig.TAG, "refresh failed", it) }
        },
        topInset = contentPadding.calculateTopPadding()
    ) {

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // Scroll under the fading top bar and clear the floating bar (FABs sit inside it).
            contentPadding = PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + floatingInset + 16.dp
            ),
        ) {
            item(key = "ultra_rgb_2026_live") {
                UltraLiveBanner2026(count = liveCount, isRefreshing = viewModel.isRefreshing)
            }
            // First item, so it sits right under the search pill and scrolls with the list.
            if (settings.isDeveloperModeEnabled) {
                item(key = "plugin_dev_info") {
                    val snap = remember(viewModel.plugins) {
                        try {
                            viewModel.plugins.toList()
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                    PluginDevInfoCard(snap)
                }
            }

            when {
                viewModel.pluginList.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_plugin_installed),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }

                else -> {
                    items(viewModel.pluginList, key = { safePluginId(it) + "@" + safeDirId(it) }) { plugin ->
                        val pluginId = safePluginId(plugin)
                        val updatedModule by produceState(
                            key1 = pluginId,
                            initialValue = Triple("", "", "")
                        ) {
                            val cached = PluginUpdateCache.get(pluginId)
                            if (cached != null) {
                                value = cached
                            } else {
                                value = withContext(Dispatchers.IO) {
                                    val fresh = runCatching { viewModel.checkUpdate(plugin) }
                                        .onFailure { Log.e(PluginListConfig.TAG, "checkUpdate failed $pluginId", it) }
                                        .getOrDefault(Triple("", "", ""))
                                    PluginUpdateCache.put(pluginId, fresh)
                                    fresh
                                }
                            }
                        }

                        val rawUpdateUrl = updatedModule.first.trim()
                        val safeUpdateUrl = if (isSafeHttpUrl(rawUpdateUrl)) rawUpdateUrl else ""

                        PluginItem(
                            navigator = navigator,
                            settings = settings,
                            viewModel = viewModel,
                            plugin = plugin,
                            updateUrl = safeUpdateUrl,
                            onUninstall = {
                                if (!canPass("uninstall_$pluginId")) return@PluginItem
                                runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                scope.launch {
                                    runCatching { onModuleUninstall(plugin) }
                                        .onFailure { Log.e(PluginListConfig.TAG, "onUninstall crash", it) }
                                }
                            },
                            onRestore = {
                                if (!canPass("restore_$pluginId")) return@PluginItem
                                runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                scope.launch {
                                    runCatching { onModuleRestore(plugin) }
                                        .onFailure { Log.e(PluginListConfig.TAG, "onRestore crash", it) }
                                }
                            },
                            onCheckChanged = {
                                if (!canPass("toggle_$pluginId")) return@PluginItem
                                if (!tryBusy("toggle_$pluginId")) return@PluginItem
                                runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                scope.launch {
                                    try {
                                        val success = loadingDialog.withLoading {
                                            withContext(Dispatchers.IO) {
                                                val dId = safeDirId(plugin)
                                                if (dId.isBlank()) false else runCatching {
                                                    AxeronPluginService.togglePlugin(
                                                        dId,
                                                        !plugin.enabled
                                                    )
                                                }.onFailure {
                                                    Log.e(PluginListConfig.TAG, "toggle failed $dId", it)
                                                }.getOrDefault(false)
                                            }
                                        }

                                        if (success) {
                                            runCatching { viewModel.fetchModuleList() }
                                        } else {
                                            val message =
                                                if (plugin.enabled) failedDisable else failedEnable
                                            runCatching {
                                                snackBarHost.showSnackbar(safeFormat(message, safePluginName(plugin)))
                                            }
                                        }
                                    } finally {
                                        freeBusy("toggle_$pluginId")
                                    }
                                }
                            },
                            onUpdate = {
                                if (!canPass("update_$pluginId")) return@PluginItem
                                runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                scope.launch {
                                    runCatching {
                                        val dl = updatedModule.first.trim()
                                        val cl = updatedModule.third.trim()
                                        if (!isSafeHttpUrl(dl) || !isSafeHttpUrl(cl)) {
                                            Log.e(PluginListConfig.TAG, "blocked unsafe onUpdate $pluginId")
                                            snackBarHost.showSnackbar(safeFormat(fetchChangeLogFailed, safePluginName(plugin)))
                                            return@launch
                                        }
                                        val safeSecond = sanitizeVersion(updatedModule.second)
                                        val rawName = "${safePluginName(plugin)}-$safeSecond.zip"
                                        onModuleUpdate(
                                            plugin,
                                            cl,
                                            dl,
                                            sanitizeFileName(rawName, pluginId)
                                        )
                                        runCatching { viewModel.fetchModuleList() }
                                    }.onFailure { Log.e(PluginListConfig.TAG, "onUpdate crash", it) }
                                }
                            },
                            onClick = {
                                if (!canPass("click_$pluginId", PluginListConfig.CLICK_THROTTLE_MS)) return@PluginItem
                                runCatching { currentOnClick(plugin) }
                                    .onFailure { Log.e(PluginListConfig.TAG, "onClick crash", it) }
                            },
                            expanded = expandedPluginId == plugin.prop.id,
                            onExpandToggle = {
                                if (!canPass("expand_$pluginId", PluginListConfig.EXPAND_THROTTLE_MS)) return@PluginItem
                                expandedPluginId =
                                    if (expandedPluginId == plugin.prop.id) null else plugin.prop.id
                            }
                        )

                        Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }

        DownloadListener(appContext, { uri -> currentOnInstall(uri) })
    }
}
