package frb.axeron.manager.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import frb.axeron.api.Axeron
import frb.axeron.server.PluginInfo
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A plugin's banner as a Coil model: the URL as-is, or the image bytes read from the plugin's
 * folder through the Axeron file service. Null when there is no banner or it can't be read.
 * Blocking (binder + file IO); call it off the main thread.
 */
fun loadPluginBannerData(plugin: PluginInfo): Any? {
    val banner = plugin.prop.banner
    if (banner.isEmpty()) return null
    if (banner.startsWith("http://", ignoreCase = true) ||
        banner.startsWith("https://", ignoreCase = true)
    ) return banner

    return try {
        val pluginDir = File(
            PathHelper.getWorkingPath(
                Axeron.getAxeronInfo().isRoot(),
                AxeronApiConstant.folder.PARENT_PLUGIN
            ),
            plugin.prop.id
        )
        Axeron.newFileService()
            ?.setFileInputStream(File(pluginDir, banner).absolutePath)
            ?.use { it.readBytes() }
    } catch (_: Exception) {
        null
    }
}

/** [loadPluginBannerData] loaded on the IO dispatcher; null until ready. */
@Composable
fun rememberPluginBanner(plugin: PluginInfo): Any? {
    val banner by produceState<Any?>(null, plugin.prop.id, plugin.prop.banner) {
        value = withContext(Dispatchers.IO) { loadPluginBannerData(plugin) }
    }
    return banner
}
