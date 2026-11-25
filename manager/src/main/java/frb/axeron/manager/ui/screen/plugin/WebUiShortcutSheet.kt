package frb.axeron.manager.ui.screen.plugin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import frb.axeron.manager.ui.component.AxPanelShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.OptionTile
import frb.axeron.manager.ui.component.OptionTileGrid
import frb.axeron.manager.ui.component.OptionsSectionHeader
import frb.axeron.manager.ui.component.ShortcutResult
import frb.axeron.manager.ui.component.createWebUIShortcut
import frb.axeron.manager.ui.component.openShortcutPermissionSettings
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.util.ShortcutIcons
import frb.axeron.manager.ui.util.rememberPluginBanner
import frb.axeron.server.PluginInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.sin

private enum class ShortcutIconSource { BANNER, INITIALS, GALLERY }

private object WebUiShortcutConfig {
    const val TAG = "WebUiShortcut"
    const val MAX_NAME_LEN = 32
    const val CLICK_THROTTLE_MS = 900L
    const val MAX_BITMAP_SIDE = 1024
    const val MAX_GALLERY_BYTES = 12L * 1024L * 1024L
    const val LIVE_CHANNEL_ID = "axeron_shortcut_live_2026"
    const val LIVE_CHANNEL_NAME = "Axeron Live 2026"
}

private fun safeDefaultName(plugin: PluginInfo): String {
    return try {
        val n = plugin.prop.name.trim()
        if (n.isNotBlank()) n.take(WebUiShortcutConfig.MAX_NAME_LEN)
        else plugin.prop.id.trim().ifBlank { plugin.dirId.trim().ifBlank { "WebUI" } }
    } catch (_: Exception) {
        "WebUI"
    }
}

private fun sanitizeShortcutName(raw: String, fallback: String): String {
    var s = raw.trim().replace(Regex("[\\x00-\\x1F\\n\\r\\t]"), " ")
    s = s.replace(Regex("\\s+"), " ").trim()
    if (s.length > WebUiShortcutConfig.MAX_NAME_LEN) s = s.take(WebUiShortcutConfig.MAX_NAME_LEN).trim()
    if (s.isBlank()) s = fallback.trim().ifBlank { "WebUI" }.take(WebUiShortcutConfig.MAX_NAME_LEN)
    return s
}

private fun queryUriSize(context: Context, uri: Uri): Long {
    return try {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else -1L
        } ?: -1L
    } catch (_: Exception) {
        -1L
    }
}

private fun ensureShortcutLiveChannel(ctx: Context) {
    try {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(WebUiShortcutConfig.LIVE_CHANNEL_ID) != null) return
            val ch = NotificationChannel(
                WebUiShortcutConfig.LIVE_CHANNEL_ID,
                WebUiShortcutConfig.LIVE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live shortcut updates 2026"
                enableLights(true)
                lightColor = 0xFF00E5FF.toInt()
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 80, 50, 80)
            }
            mgr.createNotificationChannel(ch)
        }
    } catch (_: Exception) {
    }
}

private fun postShortcutSystemMessage(ctx: Context, title: String, body: String) {
    try {
        ensureShortcutLiveChannel(ctx)
        if (Build.VERSION.SDK_INT >= 33) {
            val ok = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!ok) return
        }
        val n = NotificationCompat.Builder(ctx, WebUiShortcutConfig.LIVE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title.take(60).ifBlank { "Axeron" })
            .setContentText(body.take(180))
            .setColor(0xFF00E5FF.toInt())
            .setLights(0xFFFF2ED1.toInt(), 400, 400)
            .setVibrate(longArrayOf(0, 80, 50, 80))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify((title + body).hashCode(), n)
    } catch (_: Exception) {
    }
}

@Composable
private fun SandSparkLine(phase: Float) {
    Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
        val w = size.width
        val h = size.height
        for (i in 0 until 20) {
            val speed = 1f + (i % 3) * 0.55f
            val px = ((i * 211.7f + phase * 240f * speed) % (w + 20f)) - 10f
            val py = h * 0.5f + sin(phase * 6.2831f + i * 0.85f) * (h * 0.34f)
            val rad = 1.8f + (i % 3) * 1.3f
            val col = when (i % 5) {
                0 -> Color(0xFF00E5FF)
                1 -> Color(0xFFFF2ED1)
                2 -> Color(0xFFB6FF00)
                3 -> Color(0xFFFFB300)
                else -> Color(0xFF7C4DFF)
            }
            drawCircle(col.copy(alpha = 0.9f), rad, Offset(px, py))
        }
    }
}

/**
 * Creates (or updates) a WebUI home-screen shortcut with a custom name and an icon taken
 * from the plugin banner (center-cropped), the name's initials, or an image the user picks.
 */
@Composable
fun WebUiShortcutSheet(
    plugin: PluginInfo,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext ?: context }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val banner = rememberPluginBanner(plugin)
    val computedDefault = remember(plugin.prop.id, plugin.prop.name) { safeDefaultName(plugin) }
    val defaultName = computedDefault

    val permissionDialog = rememberConfirmDialog()
    val permissionTitle = stringResource(R.string.shortcut_permission_title)
    val permissionMessage = stringResource(R.string.shortcut_permission_msg)
    val openSettings = stringResource(R.string.settings)
    val cancel = stringResource(R.string.cancel)

    var name by rememberSaveable(plugin.prop.id) { mutableStateOf(defaultName) }
    var chosenOrdinal by rememberSaveable(plugin.prop.id) { mutableStateOf<Int?>(null) }
    var galleryUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var lastClickAt by rememberSaveable { mutableStateOf(0L) }

    LaunchedEffect(defaultName) {
        if (name.isBlank()) name = defaultName
        runCatching { ensureShortcutLiveChannel(appContext) }
    }

    val chosenSource: ShortcutIconSource? = remember(chosenOrdinal) {
        chosenOrdinal?.let { ShortcutIconSource.entries.getOrNull(it) }
    }
    fun setSource(s: ShortcutIconSource?) {
        chosenOrdinal = s?.ordinal
    }

    // Default: the banner when the plugin has one, otherwise initials.
    val source = chosenSource
        ?: if (banner != null) ShortcutIconSource.BANNER else ShortcutIconSource.INITIALS

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val sz = queryUriSize(appContext, uri)
                if (sz > WebUiShortcutConfig.MAX_GALLERY_BYTES) {
                    Toast.makeText(appContext, "Image too large", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }
                galleryUri = uri
                setSource(ShortcutIconSource.GALLERY)
                runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            }.onFailure { Log.e(WebUiShortcutConfig.TAG, "pick failed", it) }
        }
    }

    val initialsBackground = MaterialTheme.colorScheme.primaryContainer.toArgb()
    val initialsForeground = MaterialTheme.colorScheme.onPrimaryContainer.toArgb()
    val rawName = name.ifBlank { defaultName }
    val iconName = remember(rawName, defaultName) { sanitizeShortcutName(rawName, defaultName) }

    // Image sources reload only when the image changes; initials follow the typed name.
    val imageIcon by produceState<Bitmap?>(null, source, banner, galleryUri, appContext) {
        val model = when (source) {
            ShortcutIconSource.BANNER -> banner
            ShortcutIconSource.GALLERY -> galleryUri
            ShortcutIconSource.INITIALS -> null
        }
        value = model?.let { m ->
            runCatching {
                val loaded = ShortcutIcons.loadBitmap(appContext, m) ?: return@runCatching null
                withContext(Dispatchers.Default) {
                    val w = loaded.width
                    val h = loaded.height
                    val maxSide = maxOf(w, h)
                    val bmp = if (maxSide > WebUiShortcutConfig.MAX_BITMAP_SIDE) {
                        val ratio = WebUiShortcutConfig.MAX_BITMAP_SIDE.toFloat() / maxSide.toFloat()
                        val nw = (w * ratio).toInt().coerceAtLeast(1)
                        val nh = (h * ratio).toInt().coerceAtLeast(1)
                        runCatching { Bitmap.createScaledBitmap(loaded, nw, nh, true) }.getOrNull() ?: loaded
                    } else loaded
                    ShortcutIcons.fromImage(bmp)
                }
            }.onFailure { Log.e(WebUiShortcutConfig.TAG, "icon load failed", it) }
                .getOrNull()
        }
    }
    val icon = if (source == ShortcutIconSource.INITIALS) {
        remember(iconName, initialsBackground, initialsForeground) {
            runCatching { ShortcutIcons.fromInitials(iconName, initialsBackground, initialsForeground) }
                .onFailure { Log.e(WebUiShortcutConfig.TAG, "initials failed", it) }
                .getOrNull()
        }
    } else {
        imageIcon
    }

    val trans = rememberInfiniteTransition(label = "shortcut2026")
    val shift by trans.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    val spin by trans.animateFloat(0f, 360f, infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), label = "spin")
    val liveBrush = remember(shift) {
        Brush.linearGradient(
            listOf(Color(0xFF00F0FF), Color(0xFFFF2ED1), Color(0xFFB6FF00), Color(0xFFFFB300), Color(0xFF7C4DFF), Color(0xFF00F0FF)),
            start = Offset(shift * 620f, 0f),
            end = Offset(shift * 620f + 480f, 300f)
        )
    }
    val ringBrush = remember {
        Brush.sweepGradient(listOf(Color(0xFF00F0FF), Color(0xFFFF2ED1), Color(0xFFB6FF00), Color(0xFFFFB300), Color(0xFF00F0FF)))
    }

    AxOptionsSheet(
        title = stringResource(R.string.webui_shortcut_title),
        description = stringResource(R.string.webui_shortcut_desc),
        onDismissRequest = onDismissRequest
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(AxPanelShape)
                .background(Color(0xFF080A12))
                .border(1.4.dp, liveBrush, AxPanelShape)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LIVE • 2026 • 4K RGB • ${iconName.uppercase(Locale.US).take(18)}",
                    style = TextStyle(brush = liveBrush, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.7.sp),
                    textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                SandSparkLine(phase = shift)
            }
        }

        // Preview as it will roughly look on the launcher.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(84.dp).graphicsLayer { rotationZ = spin }
                        .border(3.dp, ringBrush, CircleShape)
                ) {}
                ShortcutIconPreview(icon = icon, size = 72.dp)
            }
            Text(
                modifier = Modifier.widthIn(max = 180.dp),
                text = iconName,
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 0.4.sp, brush = liveBrush),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${iconName.length}/${WebUiShortcutConfig.MAX_NAME_LEN} • ${source.name.lowercase(Locale.US)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChange = { v ->
                name = v.take(WebUiShortcutConfig.MAX_NAME_LEN + 8)
            },
            label = { Text(stringResource(R.string.shortcut_name)) },
            supportingText = {
                val left = WebUiShortcutConfig.MAX_NAME_LEN - iconName.length
                Text(if (left >= 0) "$left left" else "too long", fontFamily = FontFamily.SansSerif)
            },
            isError = iconName.isBlank() || name.length > WebUiShortcutConfig.MAX_NAME_LEN + 4,
            singleLine = true,
            shape = AxPanelShape,
            trailingIcon = if (name != defaultName) {
                {
                    IconButton(onClick = {
                        name = defaultName
                        runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    }) {
                        Icon(Icons.Outlined.Restore, contentDescription = defaultName)
                    }
                }
            } else null
        )

        OptionsSectionHeader(stringResource(R.string.shortcut_icon))
        OptionTileGrid(items = ShortcutIconSource.entries) { option, modifier ->
            when (option) {
                ShortcutIconSource.BANNER -> OptionTile(
                    modifier = modifier,
                    icon = Icons.Outlined.Image,
                    label = stringResource(R.string.shortcut_icon_banner),
                    subtitle = if (banner == null) stringResource(R.string.shortcut_icon_no_banner) else null,
                    selected = source == ShortcutIconSource.BANNER,
                    enabled = banner != null,
                    onClick = {
                        setSource(ShortcutIconSource.BANNER)
                        runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    }
                )

                ShortcutIconSource.INITIALS -> OptionTile(
                    modifier = modifier,
                    icon = Icons.Outlined.TextFields,
                    label = stringResource(R.string.shortcut_icon_initials),
                    subtitle = runCatching { ShortcutIcons.initialsOf(iconName) }.getOrNull()?.ifEmpty { null },
                    selected = source == ShortcutIconSource.INITIALS,
                    onClick = {
                        setSource(ShortcutIconSource.INITIALS)
                        runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    }
                )

                ShortcutIconSource.GALLERY -> OptionTile(
                    modifier = modifier,
                    icon = Icons.Outlined.PhotoLibrary,
                    label = stringResource(R.string.shortcut_icon_gallery),
                    subtitle = if (galleryUri == null) stringResource(R.string.shortcut_icon_pick) else null,
                    selected = source == ShortcutIconSource.GALLERY,
                    onClick = {
                        // First tap (or tapping the active tile) opens the picker; a previously
                        // picked image is simply reselected.
                        if (galleryUri != null && source != ShortcutIconSource.GALLERY) {
                            setSource(ShortcutIconSource.GALLERY)
                        } else {
                            runCatching { pickImage.launch("image/*") }
                                .onFailure { Log.e(WebUiShortcutConfig.TAG, "picker launch failed", it) }
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = CircleShape,
            enabled = icon != null && !creating && iconName.isNotBlank(),
            onClick = {
                val now = SystemClock.uptimeMillis()
                if (now - lastClickAt < WebUiShortcutConfig.CLICK_THROTTLE_MS) return@Button
                lastClickAt = now
                if (creating) return@Button
                val bitmap = icon ?: return@Button
                val finalName = sanitizeShortcutName(iconName, defaultName)
                if (finalName.isBlank()) {
                    Toast.makeText(appContext, "Name is empty", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                creating = true
                runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                val res = runCatching { createWebUIShortcut(appContext, plugin, finalName.trim(), bitmap) }
                    .onFailure { Log.e(WebUiShortcutConfig.TAG, "create failed", it) }
                    .getOrDefault(ShortcutResult.FAILED)
                when (res) {
                    ShortcutResult.REQUESTED, ShortcutResult.UPDATED -> {
                        runCatching { Toast.makeText(appContext, finalName, Toast.LENGTH_SHORT).show() }
                        runCatching { postShortcutSystemMessage(appContext, finalName, "Shortcut added • 2026 Live") }
                        creating = false
                        currentOnDismiss()
                    }
                    // Keep the sheet open so the user can retry right after granting.
                    ShortcutResult.PERMISSION_DENIED -> {
                        creating = false
                        scope.launch {
                            val result = runCatching {
                                permissionDialog.awaitConfirm(
                                    title = permissionTitle,
                                    content = permissionMessage,
                                    confirm = openSettings,
                                    dismiss = cancel
                                )
                            }.getOrDefault(ConfirmResult.Cancelled)
                            if (result == ConfirmResult.Confirmed) {
                                runCatching { openShortcutPermissionSettings(appContext) }
                                    .onFailure { Log.e(WebUiShortcutConfig.TAG, "open settings failed", it) }
                            }
                        }
                    }

                    ShortcutResult.FAILED -> {
                        creating = false
                        runCatching { Toast.makeText(appContext, "Failed: $finalName", Toast.LENGTH_SHORT).show() }
                        runCatching { postShortcutSystemMessage(appContext, finalName, "Shortcut failed") }
                    }
                }
            }
        ) {
            if (creating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.AutoMirrored.Outlined.AddToHomeScreen,
                    contentDescription = iconName
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.shortcut_add),
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/** Shows only the center (visible) part of the adaptive layer, masked like a launcher icon. */
@Composable
private fun ShortcutIconPreview(icon: Bitmap?, size: Dp) {
    val fraction = try {
        val f = ShortcutIcons.VISIBLE_FRACTION
        if (f <= 0f || f > 1f) 0.72f else f
    } catch (_: Exception) {
        0.72f
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(AxPanelShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            val scale = 1f / fraction
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = "shortcut preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}
