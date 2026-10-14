package frb.axeron.manager.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * The label and icon a user gave a plugin's WebUI shortcut. A pinned shortcut's icon can't be
 * read back from ShortcutManager, so it is kept here (plain files, so the separate WebUI
 * process sees the same data) and WebUIActivity uses it as its recents identity.
 */
object WebUiShortcutStore {

    private fun dir(context: Context) = File(context.filesDir, "webui_shortcuts")

    private fun fileName(pluginId: String) = pluginId.replace(Regex("[^A-Za-z0-9._-]"), "_")

    fun save(context: Context, pluginId: String, label: String, taskIcon: Bitmap) {
        runCatching {
            val dir = dir(context).apply { mkdirs() }
            val name = fileName(pluginId)
            File(dir, "$name.label").writeText(label)
            File(dir, "$name.png").outputStream().use {
                taskIcon.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    fun label(context: Context, pluginId: String): String? =
        runCatching { File(dir(context), "${fileName(pluginId)}.label").readText() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    fun icon(context: Context, pluginId: String): Bitmap? =
        File(dir(context), "${fileName(pluginId)}.png")
            .takeIf { it.exists() }
            ?.let { BitmapFactory.decodeFile(it.absolutePath) }
}
