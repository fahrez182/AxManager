package com.frb.axmanager.ui.screen.plugin

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Wysiwyg
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.frb.axmanager.ui.component.ConfirmResult
import com.frb.axmanager.ui.component.ExtraLabel
import com.frb.axmanager.ui.component.ExtraLabelDefaults
import com.frb.axmanager.ui.component.SettingsItem
import com.frb.axmanager.ui.component.createWebUIShortcut
import com.frb.axmanager.ui.component.formatSize
import com.frb.axmanager.ui.component.rememberConfirmDialog
import com.frb.axmanager.ui.component.rememberLoadingDialog
import com.frb.axmanager.ui.viewmodel.PluginViewModel
import com.frb.axmanager.ui.viewmodel.SettingsViewModel
import com.frb.engine.client.Axeron
import com.frb.engine.client.PluginService
import com.ramcosta.composedestinations.generated.destinations.ExecutePluginActionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginConfig(
    showDialog: Boolean,
    plugin: PluginViewModel.PluginInfo,
    onDismissRequest: () -> Unit
) {
    if (showDialog) {
        ModalBottomSheet(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onDismissRequest = onDismissRequest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val context = LocalContext.current

                SettingsItem(
                    iconVector = Icons.Outlined.Home,
                    label = "Add Shortcut",
                    description = "Add WebUI shortcut of this plugin to your homescreen",
                    onClick = {
                        createWebUIShortcut(
                            context = context,
                            plugin = plugin
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun PluginItem(
    navigator: DestinationsNavigator?,
    settings: SettingsViewModel,
    viewModel: PluginViewModel,
    plugin: PluginViewModel.PluginInfo,
    updateUrl: String,
    onUninstall: (PluginViewModel.PluginInfo) -> Unit,
    onRestore: (PluginViewModel.PluginInfo) -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onUpdate: (PluginViewModel.PluginInfo) -> Unit,
    onClick: (PluginViewModel.PluginInfo) -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
) {
    var showExtraSetDialog by remember { mutableStateOf(false) }

    PluginConfig(
        showExtraSetDialog,
        plugin
    ) {
        showExtraSetDialog = false
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onExpandToggle,
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val useBanner = prefs.getBoolean("use_banner", true)

            if (useBanner && plugin.banner.isNotEmpty()) {
                val isDark = isSystemInDarkTheme()
                val colorScheme = MaterialTheme.colorScheme
                val context = LocalContext.current
                val fadeColor = when {
                    isDark -> colorScheme.surfaceVariant
                    else -> colorScheme.surfaceVariant
                }

                Box(
                    modifier = Modifier
                        .matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (plugin.banner.startsWith("https", true) || plugin.banner.startsWith(
                            "http",
                            true
                        )
                    ) {
                        AsyncImage(
                            model = plugin.banner,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.18f
                        )
                    } else {
                        val bannerData = remember(plugin.banner) {
                            try {
                                val file =
                                    Axeron.newFileService()
                                        .setFileInputStream("/data/local/tmp/AxManager/plugins/${plugin.id}/${plugin.banner}")
                                file.use {
                                    it.readBytes()
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bannerData != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(bannerData)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.18f
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        fadeColor.copy(alpha = 0.0f),
                                        fadeColor.copy(alpha = 0.65f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                }
            }

            Column {
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .padding(22.dp, 18.dp, 22.dp, 12.dp)
                )
                {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val confirmDialog = rememberConfirmDialog()
                        val reigniteLoading = rememberLoadingDialog()
                        val scope = rememberCoroutineScope()

                        val moduleVersion = "Version"
                        val moduleAuthor = "Author"
                        val moduleId = "ID"
                        val moduleVersionCode = "VersionCode"
                        val moduleUpdateJson = "UpdateJson"
                        val moduleUpdateJsonEmpty = "Empty"

                        Column(
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ExtraLabel(
                                    iconVector = Icons.Outlined.Tune,
                                    text = formatSize(plugin.size),
                                    style = ExtraLabelDefaults.style.copy(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    onClick = {
                                        showExtraSetDialog = true
                                    }
                                )
                                if (plugin.update) {
                                    ExtraLabel(
                                        text = "Ignite" + when {
                                            plugin.updateInstall -> " → Install"
                                            plugin.updateRemove -> " → Uninstall"
                                            plugin.updateDisable -> " → Disable"
                                            plugin.updateEnable -> " → Enable"
                                            else -> ""
                                        },
                                        style = ExtraLabelDefaults.style.copy(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        onClick = {
                                            scope.launch {
                                                val result = confirmDialog.awaitConfirm(
                                                    "What is Ignite?",
                                                    content = "Ignite was a replace method of reboot in AxManager, you can ignite on Home > Reignite",
                                                    confirm = "I understand",
                                                    neutral = "Re-ignite now"
                                                )
                                                if (result == ConfirmResult.Neutral) {
                                                    val success = reigniteLoading.withLoading {
                                                        PluginService.igniteSuspendService()
                                                    }

                                                    if (success) {
                                                        viewModel.fetchModuleList()
                                                    }

                                                }
                                            }

                                        }
                                    )
                                }

                                if (updateUrl.isNotEmpty() && !plugin.remove && !plugin.update) {
                                    ExtraLabel(
                                        text = "Update",
                                        style = ExtraLabelDefaults.style.copy(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                                if (plugin.enabled && !plugin.remove && !plugin.update) {
                                    if (plugin.hasWebUi) {
                                        ExtraLabel(
                                            text = "WebUI",
                                            style = ExtraLabelDefaults.style.copy(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                    if (plugin.hasActionScript) {
                                        ExtraLabel(
                                            text = "Action",
                                            style = ExtraLabelDefaults.style.copy(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = plugin.name,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                            )

                            Text(
                                text = "$moduleVersion: ${plugin.version}",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "$moduleAuthor: ${plugin.author}",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            AnimatedVisibility(
                                visible = settings.isDeveloperModeEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Text(
                                        text = "$moduleId: ${plugin.id}",
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = "$moduleVersionCode: ${plugin.versionCode}",
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = if (plugin.updateJson.isNotEmpty()) "$moduleUpdateJson: ${plugin.updateJson}" else "$moduleUpdateJson: $moduleUpdateJsonEmpty",
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                            }

                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Switch(
                                enabled = (if (plugin.enabled) !plugin.updateDisable else !plugin.updateEnable) && !plugin.remove && !plugin.updateInstall,
                                checked = plugin.enabled && !plugin.remove,
                                onCheckedChange = onCheckChanged,
                                interactionSource = if (!plugin.hasWebUi) interactionSource else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = plugin.description,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        fontWeight = MaterialTheme.typography.bodySmall.fontWeight,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (expanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn() + expandVertically(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (plugin.hasActionScript && !plugin.updateInstall) {
                                FilledTonalButton(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    enabled = !plugin.remove && plugin.enabled && !plugin.update,
                                    onClick = {
                                        navigator!!.navigate(
                                            ExecutePluginActionScreenDestination(
                                                plugin
                                            )
                                        )
                                        viewModel.markNeedRefresh()
                                    },
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Terminal,
                                        contentDescription = null
                                    )
                                    if (!plugin.hasWebUi && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            text = "Action",
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(0.1f, true))
                            }

                            if (plugin.hasWebUi && !plugin.updateInstall) {
                                FilledTonalButton(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    enabled = !plugin.remove && plugin.enabled && !plugin.update,
                                    onClick = {
                                        onClick(plugin)
                                    },
                                    interactionSource = interactionSource,
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.AutoMirrored.Outlined.Wysiwyg,
                                        contentDescription = null
                                    )
                                    if (!plugin.hasActionScript && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = "Open"
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f, true))

                            if (updateUrl.isNotEmpty() && !plugin.remove && !plugin.updateInstall) {
                                Button(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    enabled = !plugin.update,
                                    onClick = {
                                        onUpdate(plugin)
                                    },
                                    shape = ButtonDefaults.textShape,
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null
                                    )
                                    if (!plugin.hasActionScript || !plugin.hasWebUi) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = "Update"
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(0.1f, true))
                            }

                            if (plugin.remove) {
                                FilledTonalButton(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    onClick = {
                                        onRestore(plugin)
                                    },
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Restore,
                                        contentDescription = null
                                    )
                                    if (!plugin.hasActionScript && !plugin.hasWebUi && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = "Restore"
                                        )
                                    }
                                }
                            } else {
                                FilledTonalButton(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    enabled = true,
                                    onClick = {
                                        onUninstall(plugin)
                                    },
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null
                                    )
                                    if (!plugin.hasActionScript && !plugin.hasWebUi && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = "Uninstall"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}