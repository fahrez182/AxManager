package frb.axeron.manager.ui.screen.plugin

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronPluginService
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.SettingsDivider
import frb.axeron.manager.ui.component.StatusPill
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.server.PluginInfo
import frb.axeron.shared.AxeronApiConstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** What a plugin's scripts actually see, read from the running server. */
private class DevInfo(
    val isRoot: Boolean,
    val uid: Int,
    val selinux: String,
    val versionCode: Long,
    /** The server env: the same values a plugin's scripts get. */
    val env: Map<String, String>,
    val stopWithServer: Boolean
)

private fun loadDevInfo(): DevInfo? = runCatching {
    val server = Axeron.getAxeronInfo().serverInfo
    val env = Axeron.getEnvironment()?.envMap.orEmpty()
    val stop = env["AXERONDIR"]?.let { dir ->
        Axeron.newFileService()
            ?.exists(File(dir, AxeronApiConstant.folder.STOP_PLUGINS_WITH_SERVER).path) == true
    } ?: false
    DevInfo(server.uid == 0, server.uid, server.selinuxContext, server.versionCode, env, stop)
}.getOrNull()

/**
 * Developer mode only, above the plugin list: where plugins live and run, read from the server
 * env so it matches what their scripts see. Collapsed to one summary line by default; values
 * copy on tap. Re-read whenever [plugins] changes (each list refresh).
 */
@Composable
fun PluginDevInfoCard(plugins: List<PluginInfo>) {
    val info by produceState<DevInfo?>(null, plugins) {
        value = withContext(Dispatchers.IO) { loadDevInfo() }
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "devInfoArrow")

    val dir = info?.env?.get("AXERONDIR").orEmpty()
    val mode = when {
        info == null -> "—"
        info!!.isRoot -> "Root"
        else -> "Shell"
    }

    AxCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 12.dp)
        ) {
            IconBadge(imageVector = Icons.Filled.DeveloperMode)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.plugin_dev_info_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusPill(text = mode, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = listOfNotNull(
                        info?.versionCode?.let { "v$it" },
                        dir.ifEmpty { null }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(arrow)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            ) {
                val current = info
                if (current == null) {
                    Text(
                        text = stringResource(R.string.plugin_dev_info_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    return@Column
                }

                SettingsDivider()
                DevSection(stringResource(R.string.plugin_dev_info_server)) {
                    // AXERONVER: what module.prop's axeronPlugin is compared against.
                    DevValue("\$AXERONVER", current.versionCode.toString())
                    DevValue("uid", current.uid.toString())
                    DevValue("SELinux", current.selinux)
                }

                DevSection(stringResource(R.string.plugin_dev_info_paths)) {
                    DevValue("\$AXERONDIR", dir)
                    DevValue("plugins", "$dir/${AxeronApiConstant.folder.PLUGIN}".trimEnd('/'))
                    current.env["AXERONBIN"]?.let { DevValue("\$AXERONBIN", it) }
                    current.env["AXERONXBIN"]?.let { DevValue("\$AXERONXBIN", it) }
                    current.env["TMPDIR"]?.let { DevValue("\$TMPDIR", it) }
                    // post-fs-data.sh / uninstall.sh output, one file per plugin and script.
                    DevValue("logs", "$dir/${AxeronApiConstant.folder.LOG}".trimEnd('/'))
                }

                DevSection(stringResource(R.string.plugin_dev_info_plugins)) {
                    Text(
                        text = stringResource(
                            R.string.plugin_dev_info_counts,
                            plugins.size,
                            plugins.count { it.enabled && !it.remove },
                            plugins.count { it.update }
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    DevValue(
                        stringResource(R.string.stop_plugins_with_server),
                        stringResource(
                            if (current.stopWithServer) R.string.plugin_dev_info_on
                            else R.string.plugin_dev_info_off
                        ),
                        monospace = false,
                        copyable = false
                    )
                    DevValue(
                        stringResource(R.string.plugin_dev_info_marker),
                        "${AxeronPluginService.PLUGIN_MARKER}=<id>"
                    )
                }

                Text(
                    text = stringResource(R.string.plugin_dev_info_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DevSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

/** A label and its value; tapping copies the value. */
@Composable
private fun DevValue(
    label: String,
    value: String,
    monospace: Boolean = true,
    copyable: Boolean = true
) {
    val context = LocalContext.current
    val copied = stringResource(R.string.copied)
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (copyable) Modifier.clickable {
                    // Android 13+ shows its own clipboard confirmation.
                    if (ClipboardUtil.put(context, value) &&
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    ) {
                        Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    }
                } else Modifier
            )
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(0.62f)
        )
    }
}
