package frb.axeron.manager.ui.component

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Process
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import frb.axeron.manager.R
import frb.axeron.manager.receiver.ShortcutPinnedReceiver
import frb.axeron.manager.ui.util.ShortcutIcons
import frb.axeron.manager.ui.util.WebUiShortcutStore
import frb.axeron.manager.ui.webui.WebUIActivity
import frb.axeron.server.PluginInfo
import java.util.Locale

@get:Composable
val Int.scaleDp: Dp
    get() {
        val configuration = LocalConfiguration.current
        val fontScale = configuration.fontScale
        return (this@scaleDp * fontScale).dp
    }

fun Uri.resolveDisplayName(context: Context): String =
    context.contentResolver.query(
        this,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null, null, null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else "unknown.zip"
    } ?: "unknown.zip"

@Composable
fun UseLifecycle(
    onResume: () -> Unit = {},
    onPause: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_PAUSE -> onPause()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}


fun formatSize(size: Long): String {
    if (size == 0L) return "null"
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        size >= gb -> String.format(Locale.getDefault(), "%.2f GB", size.toDouble() / gb)
        size >= mb -> String.format(Locale.getDefault(), "%.2f MB", size.toDouble() / mb)
        size >= kb -> String.format(Locale.getDefault(), "%.2f KB", size.toDouble() / kb)
        else -> "$size B"
    }
}

enum class ShortcutResult { REQUESTED, UPDATED, PERMISSION_DENIED, FAILED }

/**
 * Apps can't unpin a home-screen shortcut, only disable it: the launcher greys it out and shows
 * this message when tapped, so the user knows to remove it. Used once a plugin is uninstalled.
 */
fun disableWebUIShortcut(context: Context, pluginId: String) {
    try {
        context.getSystemService(ShortcutManager::class.java)?.disableShortcuts(
            listOf(pluginId),
            context.getString(R.string.shortcut_plugin_uninstalled)
        )
    } catch (_: Exception) {
        // No such shortcut, or the launcher refused; nothing else to do.
    }
}

/** Undoes [disableWebUIShortcut], e.g. when an uninstall is restored. */
fun enableWebUIShortcut(context: Context, pluginId: String) {
    try {
        context.getSystemService(ShortcutManager::class.java)?.enableShortcuts(listOf(pluginId))
    } catch (_: Exception) {
        // No such shortcut; nothing to re-enable.
    }
}

/** HyperOS/MIUI's own "Home screen shortcuts" app-op; pin requests are dropped silently while denied. */
private const val MIUI_OP_INSTALL_SHORTCUT = 10017

private fun isMiuiShortcutPermissionDenied(context: Context): Boolean = try {
    // Hidden int-op overload (hidden APIs are exempted at startup). Throws on ROMs
    // without this op, which simply means there is no such gate.
    val appOps = context.getSystemService(AppOpsManager::class.java)
    val mode = AppOpsManager::class.java
        .getMethod(
            "checkOpNoThrow",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            String::class.java
        )
        .invoke(appOps, MIUI_OP_INSTALL_SHORTCUT, Process.myUid(), context.packageName) as Int
    mode == AppOpsManager.MODE_IGNORED || mode == AppOpsManager.MODE_ERRORED
} catch (_: Throwable) {
    false
}

/** Opens the HyperOS/MIUI per-app permission page, or the standard app info page elsewhere. */
fun openShortcutPermissionSettings(context: Context) {
    val miuiEditor = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
        setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.permissions.PermissionsEditorActivity"
        )
        putExtra("extra_pkgname", context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(miuiEditor)
    } catch (_: Exception) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/**
 * Pins a home-screen shortcut that opens [plugin]'s WebUI, with a custom [label] and an
 * adaptive [icon] layer (see ShortcutIcons). An already pinned shortcut is updated in place.
 * Failures get a toast here; [ShortcutResult.PERMISSION_DENIED] is left to the caller to explain.
 */
fun createWebUIShortcut(
    context: Context,
    plugin: PluginInfo,
    label: String,
    icon: Bitmap
): ShortcutResult {
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported) {
        Toast.makeText(context, R.string.shortcut_not_supported, Toast.LENGTH_SHORT).show()
        return ShortcutResult.FAILED
    }

    val shortcut = ShortcutInfo.Builder(context, plugin.prop.id)
        .setShortLabel(label)
        .setLongLabel(label)
        .setIcon(Icon.createWithAdaptiveBitmap(icon))
        .setIntent(WebUIActivity.intentFor(context, plugin.prop.id))
        .build()

    // WebUIActivity shows this label/icon in recents, whichever way it's opened.
    WebUiShortcutStore.save(context, plugin.prop.id, label, ShortcutIcons.toTaskIcon(icon))

    if (shortcutManager.pinnedShortcuts.any { it.id == plugin.prop.id }) {
        // It may have been disabled by an earlier uninstall of the same plugin id.
        enableWebUIShortcut(context, plugin.prop.id)
        shortcutManager.updateShortcuts(listOf(shortcut))
        Toast.makeText(context, R.string.shortcut_updated, Toast.LENGTH_SHORT).show()
        return ShortcutResult.UPDATED
    }

    if (isMiuiShortcutPermissionDenied(context)) {
        return ShortcutResult.PERMISSION_DENIED
    }

    val onPinned = ShortcutPinnedReceiver.callback(context, plugin.prop.id, label)
    return if (shortcutManager.requestPinShortcut(shortcut, onPinned)) {
        ShortcutResult.REQUESTED
    } else {
        Toast.makeText(context, R.string.shortcut_failed, Toast.LENGTH_SHORT).show()
        ShortcutResult.FAILED
    }
}
