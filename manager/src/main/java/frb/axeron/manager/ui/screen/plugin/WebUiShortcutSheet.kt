package frb.axeron.manager.ui.screen.plugin

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddToHomeScreen
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

private enum class ShortcutIconSource { BANNER, INITIALS, GALLERY }

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
    val scope = rememberCoroutineScope()
    val banner = rememberPluginBanner(plugin)
    val defaultName = plugin.prop.name

    val permissionDialog = rememberConfirmDialog()
    val permissionTitle = stringResource(R.string.shortcut_permission_title)
    val permissionMessage = stringResource(R.string.shortcut_permission_msg)
    val openSettings = stringResource(R.string.settings)
    val cancel = stringResource(R.string.cancel)

    var name by rememberSaveable { mutableStateOf(defaultName) }
    var chosenSource by remember { mutableStateOf<ShortcutIconSource?>(null) }
    var galleryUri by remember { mutableStateOf<Uri?>(null) }

    // Default: the banner when the plugin has one, otherwise initials.
    val source = chosenSource
        ?: if (banner != null) ShortcutIconSource.BANNER else ShortcutIconSource.INITIALS

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            galleryUri = uri
            chosenSource = ShortcutIconSource.GALLERY
        }
    }

    val initialsBackground = MaterialTheme.colorScheme.primaryContainer.toArgb()
    val initialsForeground = MaterialTheme.colorScheme.onPrimaryContainer.toArgb()
    val iconName = name.ifBlank { defaultName }

    // Image sources reload only when the image changes; initials follow the typed name.
    val imageIcon by produceState<Bitmap?>(null, source, banner, galleryUri) {
        val model = when (source) {
            ShortcutIconSource.BANNER -> banner
            ShortcutIconSource.GALLERY -> galleryUri
            ShortcutIconSource.INITIALS -> null
        }
        value = model?.let { m ->
            ShortcutIcons.loadBitmap(context, m)?.let { bitmap ->
                withContext(Dispatchers.Default) { ShortcutIcons.fromImage(bitmap) }
            }
        }
    }
    val icon = if (source == ShortcutIconSource.INITIALS) {
        remember(iconName, initialsBackground, initialsForeground) {
            ShortcutIcons.fromInitials(iconName, initialsBackground, initialsForeground)
        }
    } else {
        imageIcon
    }

    AxOptionsSheet(
        title = stringResource(R.string.webui_shortcut_title),
        description = stringResource(R.string.webui_shortcut_desc),
        onDismissRequest = onDismissRequest
    ) {
        // Preview as it will roughly look on the launcher.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShortcutIconPreview(icon = icon, size = 72.dp)
            Text(
                modifier = Modifier.widthIn(max = 140.dp),
                text = iconName,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.shortcut_name)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = if (name != defaultName) {
                {
                    IconButton(onClick = { name = defaultName }) {
                        Icon(Icons.Outlined.Restore, contentDescription = null)
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
                    onClick = { chosenSource = ShortcutIconSource.BANNER }
                )

                ShortcutIconSource.INITIALS -> OptionTile(
                    modifier = modifier,
                    icon = Icons.Outlined.TextFields,
                    label = stringResource(R.string.shortcut_icon_initials),
                    subtitle = ShortcutIcons.initialsOf(iconName).ifEmpty { null },
                    selected = source == ShortcutIconSource.INITIALS,
                    onClick = { chosenSource = ShortcutIconSource.INITIALS }
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
                            chosenSource = ShortcutIconSource.GALLERY
                        } else {
                            pickImage.launch("image/*")
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
            enabled = icon != null,
            onClick = {
                val bitmap = icon ?: return@Button
                when (createWebUIShortcut(context, plugin, iconName.trim(), bitmap)) {
                    ShortcutResult.REQUESTED, ShortcutResult.UPDATED -> onDismissRequest()
                    // Keep the sheet open so the user can retry right after granting.
                    ShortcutResult.PERMISSION_DENIED -> scope.launch {
                        val result = permissionDialog.awaitConfirm(
                            title = permissionTitle,
                            content = permissionMessage,
                            confirm = openSettings,
                            dismiss = cancel
                        )
                        if (result == ConfirmResult.Confirmed) {
                            openShortcutPermissionSettings(context)
                        }
                    }

                    ShortcutResult.FAILED -> Unit
                }
            }
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = Icons.Outlined.AddToHomeScreen,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.shortcut_add),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Shows only the center (visible) part of the adaptive layer, masked like a launcher icon. */
@Composable
private fun ShortcutIconPreview(icon: Bitmap?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 30))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            val scale = 1f / ShortcutIcons.VISIBLE_FRACTION
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
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
