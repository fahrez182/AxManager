package com.frb.axmanager.ui.component

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frb.axmanager.R
import com.frb.axmanager.ui.viewmodel.PluginViewModel
import com.frb.axmanager.ui.webui.WebUIActivity
import java.util.Locale

@get:Composable
val Int.scaleDp: Dp
    get() {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val fontScale = configuration.fontScale
        return with(density) { (this@scaleDp * fontScale).dp }
    }

fun Color.blend(other: Color, ratio: Float): Color {
    val inverse = 1f - ratio
    return Color(
        red = red * inverse + other.red * ratio,
        green = green * inverse + other.green * ratio,
        blue = blue * inverse + other.blue * ratio,
        alpha = alpha
    )
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

fun createWebUIShortcut(context: Context, plugin: PluginViewModel.PluginInfo) {
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)

    if (shortcutManager.pinnedShortcuts.any { it.id == plugin.id }) {
        Toast.makeText(
            context, "Shortcut already pinned", Toast.LENGTH_SHORT
        ).show()
        return
    }

    if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
        val shortcut = ShortcutInfo.Builder(context, plugin.id)
            .setShortLabel(plugin.name)
            .setLongLabel(plugin.name)
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(
                Intent(context, WebUIActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("id", plugin.id)
                }
            )
            .build()

        shortcutManager.requestPinShortcut(shortcut, null)
    }
}
