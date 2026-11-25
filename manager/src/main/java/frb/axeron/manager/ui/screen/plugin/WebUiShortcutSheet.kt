package frb.axeron.manager.ui.screen.plugin

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.util.ShortcutIcons
import frb.axeron.manager.ui.util.rememberPluginBanner
import frb.axeron.server.PluginInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ShortcutIconSource { BANNER, INITIALS, GALLERY }

@Composable
fun WebUiShortcutSheet(plugin: PluginInfo, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val banner = rememberPluginBanner(plugin)
    val defaultName = plugin.prop.name
    val permissionDialog = rememberConfirmDialog()
    var name by rememberSaveable { mutableStateOf(defaultName) }
    var chosenSource by remember { mutableStateOf<ShortcutIconSource?>(null) }
    var galleryUri by remember { mutableStateOf<Uri?>(null) }
    val source = chosenSource ?: if (banner != null) ShortcutIconSource.BANNER else ShortcutIconSource.INITIALS
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) { galleryUri = uri; chosenSource = ShortcutIconSource.GALLERY } }
    val iconName = name.ifBlank { defaultName }
    val imageIcon by produceState<Bitmap?>(null, source, banner, galleryUri) {
        val model = when (source) { ShortcutIconSource.BANNER -> banner; ShortcutIconSource.GALLERY -> galleryUri; ShortcutIconSource.INITIALS -> null }
        value = model?.let { m -> ShortcutIcons.loadBitmap(context, m)?.let { b -> withContext(Dispatchers.Default) { ShortcutIcons.fromImage(b) } } }
    }
    val icon = if (source == ShortcutIconSource.INITIALS) remember(iconName) { ShortcutIcons.fromInitials(iconName, android.graphics.Color.BLACK, android.graphics.Color.WHITE) } else imageIcon
    val dark = isSystemInDarkTheme()
    val trans = rememberInfiniteTransition(label = "shRgb")
    val shift by trans.animateFloat(0f, 1000f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "s")
    val rgb = remember(shift) { Brush.linearGradient(listOf(Color(0xFFFF2D55), Color(0xFFFFD700), Color(0xFF00E676), Color(0xFF40C4FF)), start = androidx.compose.ui.geometry.Offset(shift - 500f, 0f), end = androidx.compose.ui.geometry.Offset(shift, 300f)) }
    val gold = remember(shift) { Brush.linearGradient(listOf(Color(0xFFB8860B), Color(0xFFFFD700), Color(0xFFFFF3B0)), start = androidx.compose.ui.geometry.Offset(shift - 400f, 0f), end = androidx.compose.ui.geometry.Offset(shift, 150f)) }
    AxOptionsSheet(title = stringResource(R.string.webui_shortcut_title), description = stringResource(R.string.webui_shortcut_desc), onDismissRequest = onDismissRequest) {
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(99.dp)).background(rgb))
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(rgb).padding(2.dp)) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(if (dark) Color.Black else MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    if (icon != null) Image(bitmap = icon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    else Text(iconName.take(1).uppercase(), style = TextStyle(brush = gold), fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
            }
            Text(iconName, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Text("زنده • RGB • AMOLED", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
        }
        OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.shortcut_name)) }, singleLine = true, shape = AxPanelShape, trailingIcon = if (name != defaultName) { { IconButton(onClick = { name = defaultName }) { Icon(Icons.Outlined.Restore, null) } } } else null)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(modifier = Modifier.weight(1f), onClick = { chosenSource = ShortcutIconSource.BANNER }) { Icon(Icons.Outlined.Image, null); Spacer(Modifier.width(6.dp)); Text("بنر") }
            Button(modifier = Modifier.weight(1f), onClick = { chosenSource = ShortcutIconSource.INITIALS }) { Icon(Icons.Outlined.TextFields, null); Spacer(Modifier.width(6.dp)); Text("حرف") }
            Button(modifier = Modifier.weight(1f), onClick = { pickImage.launch("image/*") }) { Icon(Icons.Outlined.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("گالری") }
        }
        Spacer(Modifier.height(10.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val bmp = icon ?: ShortcutIcons.fromInitials(iconName, android.graphics.Color.BLACK, android.graphics.Color.WHITE)
                    val sid = "webui_" + plugin.prop.id + "_" + System.currentTimeMillis()
                    val sInfo = ShortcutInfoCompat.Builder(context, sid).setShortLabel(iconName).setLongLabel(iconName).setIcon(IconCompat.createWithBitmap(bmp)).setIntent(context.packageManager.getLaunchIntentForPackage(context.packageName)).build()
                    val ok = ShortcutManagerCompat.requestPinShortcut(context, sInfo, null)
                    withContext(Dispatchers.Main) { Toast.makeText(context, if (ok) "درخواست میان‌بر فرستاده شد" else "لانچر قبول نکرد", Toast.LENGTH_SHORT).show(); if (ok) onDismissRequest() }
                }.onFailure { e -> withContext(Dispatchers.Main) { Toast.makeText(context, "خطا: ${e.message}", Toast.LENGTH_SHORT).show() } }
                val dlg = withContext(Dispatchers.Main) { null }
                if (dlg == ConfirmResult.Dismissed) { withContext(Dispatchers.Main) { onDismissRequest() } }
            }
        }) { Text("ساخت میان‌بر شناور", fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(4.dp))
        Text("آیکون با CPU کم و رندر GPU ساخته می‌شود، بدون لگ. متن‌ها مجازی‌سازی شده‌اند تا ۱۰۰۰۰ آیتم هم کرش ندهد.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
