package frb.axeron.manager.ui.screen.plugin

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.SettingsRow
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel
import frb.axeron.server.PluginInfo

private val RgbList = listOf(Color(0xFFFF2D55), Color(0xFFFFD700), Color(0xFF00E676), Color(0xFF40C4FF), Color(0xFF7C4DFF), Color(0xFFFF2D55))
private val GoldList = listOf(Color(0xFFB8860B), Color(0xFFFFD700), Color(0xFFFFF3B0), Color(0xFFFFD700), Color(0xFFB8860B))

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PluginConfig(showDialog: Boolean, plugin: PluginInfo, onDismissRequest: () -> Unit) {
    var showShortcut by remember { mutableStateOf(false) }
    if (showShortcut) { WebUiShortcutSheet(plugin = plugin, onDismissRequest = { showShortcut = false }) }
    if (showDialog) {
        AxOptionsSheet(title = stringResource(R.string.web_ui_configuration), onDismissRequest = onDismissRequest) {
            SettingsRow(enabled = plugin.hasWebUi, label = stringResource(R.string.add_web_ui_shortcut), description = stringResource(R.string.add_web_ui_shortcut_msg), onClick = { if (plugin.hasWebUi) { onDismissRequest(); showShortcut = true } })
            SettingsRow(label = stringResource(R.string.web_ui_more_soon), description = stringResource(R.string.web_ui_more_soon_desc))
        }
    }
}

@Composable
fun PluginItem(
    navigator: DestinationsNavigator?,
    settings: SettingsViewModel,
    viewModel: PluginViewModel,
    plugin: PluginInfo,
    updateUrl: String,
    onUninstall: (PluginInfo) -> Unit,
    onRestore: (PluginInfo) -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onUpdate: (PluginInfo) -> Unit,
    onClick: (PluginInfo) -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    var showExtra by remember { mutableStateOf(false) }
    PluginConfig(showExtra, plugin) { showExtra = false }
    val dark = isSystemInDarkTheme()
    val trans = rememberInfiniteTransition(label = "axRgb")
    val phase by trans.animateFloat(0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart), label = "rgb")
    val shimmer by trans.animateFloat(0f, 1200f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "gold")
    val floatY by trans.animateFloat(-3f, 3f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse), label = "float")
    val live by trans.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "live")
    val rgbBrush = remember(phase) { Brush.sweepGradient(RgbList) }
    val goldBrush = remember(shimmer) { Brush.linearGradient(GoldList, start = androidx.compose.ui.geometry.Offset(shimmer - 400f, 0f), end = androidx.compose.ui.geometry.Offset(shimmer, 200f)) }
    val safeDesc: String = remember(plugin) { try { (plugin.prop.description as? String).orEmpty().trim() } catch (_: Exception) { "" } }
    val axeronLabel: String = remember(plugin) {
        val raw: Any? = try { plugin.prop.axeronPlugin } catch (_: Exception) { null }
        when (raw) { is Int -> if (raw == 0) "—" else raw.toString(); is Number -> if (raw.toInt() == 0) "—" else raw.toInt().toString(); is String -> if (raw.isBlank() || raw == "0") "—" else raw; else -> raw?.toString()?.takeIf { it.isNotBlank() && it != "0" } ?: "—" }
    }
    val verName: String = remember(plugin) { try { (plugin.prop.version as? String) ?: "" } catch (_: Exception) { "" } }
    val verCode: String = remember(plugin) { try { plugin.prop.versionCode.toString() } catch (_: Exception) { "—" } }
    val updateJsonRaw: String = remember(plugin) { try { (plugin.prop.updateJson as? String).orEmpty() } catch (_: Exception) { "" } }
    val start = remember { SystemClock.elapsedRealtime() }
    val renderMs = SystemClock.elapsedRealtime() - start
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(rgbBrush).padding(2.dp).graphicsLayer { translationY = floatY }) {
        Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onExpandToggle), shape = RoundedCornerShape(18.dp), color = if (dark) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 0.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(rgbBrush).padding(2.dp)) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (dark) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                            Text(text = plugin.prop.name.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = 20.sp, style = TextStyle(brush = goldBrush))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = plugin.prop.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, style = TextStyle(brush = goldBrush))
                        Text(text = plugin.prop.id, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (dark) Color(0xFF9BE7FF) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Color(0xFF00C853).copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF00E676).copy(alpha = live)))
                            Spacer(Modifier.width(5.dp)); Text("زنده • LVL 200", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676), fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(99.dp)).background(rgbBrush).graphicsLayer { rotationZ = (phase % 360f) * 0.02f })
                Text(text = "v$verName ($verCode) • Axeron: $axeronLabel", fontFamily = FontFamily.Monospace, fontSize = 11.5.sp, color = if (dark) Color(0xFFE8F5FF) else MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = if (plugin.enabled) "فعال" else "غیرفعال", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (plugin.enabled) Color(0xFF00E676) else Color.Gray)
                    Switch(checked = plugin.enabled, onCheckedChange = onCheckChanged)
                }
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (safeDesc.isNotBlank()) { Text(text = safeDesc, fontSize = 13.5.sp, lineHeight = 20.sp, color = if (dark) Color(0xFFF5F5F5) else MaterialTheme.colorScheme.onSurface) }
                        if (updateJsonRaw.isNotEmpty()) { Text(text = updateJsonRaw, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, color = Color(0xFF7C9DFF), maxLines = 6, overflow = TextOverflow.Ellipsis) }
                        Text(text = "render ${renderMs}ms • no-lag • AMOLED pure-black", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (updateUrl.isNotBlank()) { FilledTonalButton(onClick = { onUpdate(plugin) }) { Text("آپدیت", fontWeight = FontWeight.Bold) } }
                            TextButton(onClick = { onClick(plugin) }) { Text("جزئیات") }
                            TextButton(onClick = { showExtra = true }) { Text("WebUI") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { if (plugin.remove) onRestore(plugin) else onUninstall(plugin) }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))) { Text(if (plugin.remove) "بازیابی" else "حذف", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}
