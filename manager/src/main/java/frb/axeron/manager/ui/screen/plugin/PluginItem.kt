package frb.axeron.manager.ui.screen.plugin

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import frb.axeron.manager.ui.screen.FlashIt
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.Axeron
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxCardShape
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.ExtraLabel
import frb.axeron.manager.ui.component.ExtraLabelDefaults
import frb.axeron.manager.ui.component.SettingsItem
import frb.axeron.manager.ui.component.SettingsItemType
import frb.axeron.manager.ui.component.SettingsRow
import frb.axeron.manager.ui.component.formatSize
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.component.rememberReigniteSheet
import frb.axeron.manager.ui.util.rememberPluginBanner
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel
import frb.axeron.server.PluginInfo
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.launch
import java.io.File

// [ارتقا-سطح۲۰۰] قرارداد داخلی بدون تغییر API عمومی
private const val TAG_ITEM = "PluginItem"
private object PluginItemConfig {
    const val ACTION_THROTTLE_MS = 800L
    const val EXPAND_THROTTLE_MS = 200L
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginConfig(
    showDialog: Boolean,
    plugin: PluginInfo,
    onDismissRequest: () -> Unit
) {
    // [حفظ‌شده + ارتقا] با کلید id تا با عوض‌شدن پلاگین ریست شود و با چرخش نپرد
    var showShortcutSheet by rememberSaveable(plugin.prop.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    if (showShortcutSheet) {
        WebUiShortcutSheet(
            plugin = plugin,
            onDismissRequest = { showShortcutSheet = false }
        )
    }

    if (showDialog) {
        AxOptionsSheet(
            title = stringResource(R.string.web_ui_configuration),
            onDismissRequest = onDismissRequest
        ) {
            AxCard {
                SettingsRow(
                    enabled = plugin.hasWebUi,
                    iconVector = Icons.Outlined.Home,
                    label = stringResource(R.string.add_web_ui_shortcut),
                    description = stringResource(R.string.add_web_ui_shortcut_msg),
                    onClick = {
                        if (plugin.hasWebUi) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            // Swap this sheet for the shortcut editor.
                            onDismissRequest()
                            showShortcutSheet = true
                        }
                    }
                )
            }

            // Reserved for upcoming WebUI options.
            AxCard {
                SettingsRow(
                    label = stringResource(R.string.web_ui_more_soon),
                    description = stringResource(R.string.web_ui_more_soon_desc)
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
    plugin: PluginInfo,
    updateUrl: String,
    onUninstall: (PluginInfo) -> Unit,
    onRestore: (PluginInfo) -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onUpdate: (PluginInfo) -> Unit,
    onClick: (PluginInfo) -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
) {
    // [حفظ‌شده + ارتقا] هویت پایدار به‌ازای هر پلاگین
    var showExtraSetDialog by rememberSaveable(plugin.prop.id) { mutableStateOf(false) }

    // [ارتقا-سطح۲۰۰] هوست‌شده در بالای آیتم تا با ریکامپوزیشن داخلی گم نشود
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val confirmDialog = rememberConfirmDialog()
    val reigniteSheet = rememberReigniteSheet()
    var confirmBusy by rememberSaveable { mutableStateOf(false) }
    var lastActionMs by remember { mutableLongStateOf(0L) }
    var lastExpandMs by remember { mutableLongStateOf(0L) }

    fun canDoAction(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionMs < PluginItemConfig.ACTION_THROTTLE_MS) return false
        lastActionMs = now
        return true
    }
    fun canExpand(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - lastExpandMs < PluginItemConfig.EXPAND_THROTTLE_MS) return false
        lastExpandMs = now
        return true
    }
    fun devLog(msg: String) {
        if (settings.isDeveloperModeEnabled) Log.d(TAG_ITEM, msg)
    }

    PluginConfig(
        showExtraSetDialog,
        plugin
    ) {
        showExtraSetDialog = false
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AxCardShape)
            .combinedClickable(
                onClick = {
                    if (!canExpand()) return@combinedClickable
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExpandToggle()
                },
                role = Role.Button
            ),
        shape = AxCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // [حفظ‌شده + ارتقا] خوانش کش‌شده + هوک همیشه در یک ترتیب
            val useBanner = remember(context) {
                runCatching {
                    context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .getBoolean("use_banner", true)
                }.getOrDefault(true)
            }
            // URL or file bytes, read off the main thread; shared with the shortcut sheet.
            val bannerRaw = rememberPluginBanner(plugin)
            val banner = if (useBanner) bannerRaw else null
            val bannerModel = remember(banner) {
                banner?.let {
                    runCatching {
                        ImageRequest.Builder(appContext)
                            .data(it)
                            .crossfade(true)
                            .allowHardware(false)
                            .build()
                    }.getOrNull() ?: it
                }
            }

            if (banner != null) {
                val colorScheme = MaterialTheme.colorScheme
                val fadeColor = colorScheme.surfaceVariant
                val alpha = 0.5f

                Box(
                    modifier = Modifier
                        .matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = bannerModel ?: banner,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.18f
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        fadeColor.copy(alpha = 0.0f),
                                        fadeColor.copy(alpha = alpha)
                                    )
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
                        val pluginVersion = stringResource(R.string.plugin_version)
                        val pluginAuthor = stringResource(R.string.plugin_author)
                        val pluginId = stringResource(R.string.plugin_id)
                        val pluginVersionCode = stringResource(R.string.plugin_version_code)
                        val pluginAxeronSupport = stringResource(R.string.plugin_axeron_support)
                        val pluginUpdateJson = stringResource(R.string.plugin_update_json)
                        val pluginUpdateJsonEmpty = stringResource(R.string.plugin_update_json_empty)

                        // [ارتقا] نام امن برای جلوگیری از خط خالی یا کرش لایه‌اوت
                        val safeName = plugin.prop.name.ifBlank { plugin.prop.id.ifBlank { "—" } }
                        val safeAuthor = plugin.prop.author.ifBlank { "—" }
                        val safeDesc = plugin.prop.description.orEmpty()

                        Column(
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ExtraLabel(
                                    iconVector = Icons.Outlined.Tune,
                                    text = runCatching { formatSize(plugin.size) }.getOrDefault("—"),
                                    style = ExtraLabelDefaults.style.copy(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    onClick = {
                                        if (!canDoAction()) return@ExtraLabel
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showExtraSetDialog = true
                                    }
                                )
                                if (plugin.update) {
                                    val title = stringResource(R.string.what_is_ignite)
                                    val content = stringResource(R.string.what_is_ignite_msg)
                                    val confirm = stringResource(R.string.understand)
                                    val neutral = stringResource(R.string.re_ignite_now)
                                    ExtraLabel(
                                        text = stringResource(R.string.ignite) + when {
                                            plugin.updateInstall -> " → ${stringResource(R.string.install)}"
                                            plugin.updateRemove -> " → ${stringResource(R.string.uninstall)}"
                                            plugin.updateDisable -> " → ${stringResource(R.string.disable)}"
                                            plugin.updateEnable -> " → ${stringResource(R.string.enable)}"
                                            else -> ""
                                        },
                                        style = ExtraLabelDefaults.style.copy(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        onClick = {
                                            if (!canDoAction() || confirmBusy) return@ExtraLabel
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            scope.launch {
                                                confirmBusy = true
                                                try {
                                                    val result = confirmDialog.awaitConfirm(
                                                        title,
                                                        content = content,
                                                        confirm = confirm,
                                                        neutral = neutral
                                                    )
                                                    if (result == ConfirmResult.Neutral) {
                                                        val success = runCatching { reigniteSheet.reignite() }
                                                            .onFailure { Log.e(TAG_ITEM, "reignite failed", it) }
                                                            .getOrDefault(false)
                                                        if (success) {
                                                            runCatching { viewModel.fetchModuleList() }
                                                                .onFailure { Log.e(TAG_ITEM, "fetch failed", it) }
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e(TAG_ITEM, "confirm failed", e)
                                                } finally {
                                                    confirmBusy = false
                                                }
                                            }
                                        }
                                    )
                                }

                                if (updateUrl.isNotEmpty() && !plugin.remove && !plugin.update) {
                                    ExtraLabel(
                                        text = stringResource(R.string.update),
                                        style = ExtraLabelDefaults.style.copy(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                                if (plugin.enabled && !plugin.remove && !plugin.update) {
                                    if (plugin.hasWebUi) {
                                        ExtraLabel(
                                            text = stringResource(R.string.web_ui),
                                            style = ExtraLabelDefaults.style.copy(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                    if (plugin.hasActionScript) {
                                        ExtraLabel(
                                            text = stringResource(R.string.action),
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
                                text = safeName,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "$pluginVersion: ${plugin.prop.version.ifBlank { "—" }}",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "$pluginAuthor: $safeAuthor",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            AnimatedVisibility(
                                visible = settings.isDeveloperModeEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Text(
                                        text = "$pluginId: ${plugin.prop.id.ifBlank { "—" }}",
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "$pluginVersionCode: ${plugin.prop.versionCode}",
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = "$pluginAxeronSupport: ${plugin.prop.axeronPlugin.ifBlank { "—" }}",
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = if (plugin.prop.updateJson.isNotEmpty()) "$pluginUpdateJson: ${plugin.prop.updateJson}" else "$pluginUpdateJson: $pluginUpdateJsonEmpty",
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
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
                                onCheckedChange = { checked ->
                                    if (!canDoAction()) return@Switch
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    runCatching { onCheckChanged(checked) }
                                        .onFailure { Log.e(TAG_ITEM, "toggle failed", it) }
                                },
                                interactionSource = if (!plugin.hasWebUi) interactionSource else null
                            )
                        }
                    }
                    val safeDesc = (plugin.prop.description ?: "").toString().trim()

                    Spacer(modifier = Modifier.height(12.dp))

                    if (safeDesc.isNotBlank()) {
                        Text(
                            text = safeDesc,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                            fontWeight = MaterialTheme.typography.bodySmall.fontWeight,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            // [ارتقا] در حالت باز کامل نشان بده، در حالت جمع ۴ خط
                            maxLines = if (expanded) Int.MAX_VALUE else 4
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                    }

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
                                        if (!canDoAction()) return@FilledTonalButton
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        try {
                                            if (navigator == null) {
                                                devLog("navigator null, skip action")
                                                return@FilledTonalButton
                                            }
                                            navigator.navigate(
                                                FlashScreenDestination(FlashIt.PluginAction(plugin))
                                            )
                                            runCatching { viewModel.markNeedRefresh() }
                                        } catch (e: Exception) {
                                            Log.e(TAG_ITEM, "navigate action failed", e)
                                        }
                                    },
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Terminal,
                                        contentDescription = stringResource(R.string.action)
                                    )
                                    if (!plugin.hasWebUi && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            text = stringResource(R.string.action),
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
                                        if (!canDoAction()) return@FilledTonalButton
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        runCatching { onClick(plugin) }
                                            .onFailure { Log.e(TAG_ITEM, "open webui failed", it) }
                                    },
                                    interactionSource = interactionSource,
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Filled.Web,
                                        contentDescription = stringResource(R.string.web_ui)
                                    )
                                    if (!plugin.hasActionScript && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = stringResource(R.string.open)
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
                                        if (!canDoAction()) return@Button
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        runCatching { onUpdate(plugin) }
                                            .onFailure { Log.e(TAG_ITEM, "update failed", it) }
                                    },
                                    shape = ButtonDefaults.textShape,
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = stringResource(R.string.update)
                                    )
                                    if (!plugin.hasActionScript || !plugin.hasWebUi) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = stringResource(R.string.update)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(0.1f, true))
                            }

                            if (plugin.remove) {
                                FilledTonalButton(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    onClick = {
                                        if (!canDoAction()) return@FilledTonalButton
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        runCatching { onRestore(plugin) }
                                            .onFailure { Log.e(TAG_ITEM, "restore failed", it) }
                                    },
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Restore,
                                        contentDescription = stringResource(R.string.restore)
                                    )
                                    if (!plugin.hasActionScript && !plugin.hasWebUi && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = stringResource(R.string.restore)
                                        )
                                    }
                                }
                            } else {
                                FilledTonalButton(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    enabled = true,
                                    onClick = {
                                        if (!canDoAction()) return@FilledTonalButton
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        runCatching { onUninstall(plugin) }
                                            .onFailure { Log.e(TAG_ITEM, "uninstall failed", it) }
                                    },
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(R.string.uninstall)
                                    )
                                    if (!plugin.hasActionScript && !plugin.hasWebUi && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = stringResource(R.string.uninstall)
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

@Preview(showBackground = true, name = "PluginItem light")
@Composable
private fun PluginItemPreviewLight() {
    MaterialTheme {
        PluginConfig(showDialog = false, plugin = PluginInfo()) {}
    }
}
