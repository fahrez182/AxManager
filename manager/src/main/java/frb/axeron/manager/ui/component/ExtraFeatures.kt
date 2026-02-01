package frb.axeron.manager.ui.component

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.OpenableColumns
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

fun createWebUIShortcut(context: Context, plugin: PluginInfo) {
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)

    if (shortcutManager.pinnedShortcuts.any { it.id == plugin.prop.id }) {
        Toast.makeText(
            context, "Shortcut already pinned", Toast.LENGTH_SHORT
        ).show()
        return
    }

    if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
        val shortcut = ShortcutInfo.Builder(context, plugin.prop.id)
            .setShortLabel(plugin.prop.name)
            .setLongLabel(plugin.prop.name)
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(
                Intent(context, WebUIActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("id", plugin.prop.id)
                }
            )
            .build()

        shortcutManager.requestPinShortcut(shortcut, null)
    }
}
