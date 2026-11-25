package frb.axeron.manager.ui.screen.plugin

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.AxeronPluginService
import frb.axeron.manager.AxeronApplication.Companion.axeronApp
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxPullToRefreshBox
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.disableWebUIShortcut
import frb.axeron.manager.ui.component.enableWebUIShortcut
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.util.DownloadListener
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.util.download
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel
import frb.axeron.server.PluginInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginList(
    navigator: DestinationsNavigator,
    settings: SettingsViewModel,
    viewModel: PluginViewModel,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onInstallModule: (Uri) -> Unit,
    onClickModule: (plugin: PluginInfo) -> Unit,
    context: Context,
    snackBarHost: SnackbarHostState,
    listState: LazyListState
) {
    val failedEnable = stringResource(R.string.failed_enable_plugin)
    val failedDisable = stringResource(R.string.failed_disable_plugin)

    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog()

    var expandedPluginId by rememberSaveable { mutableStateOf<String?>(null) }

    val updateText = stringResource(R.string.update)
    val changelogText = stringResource(R.string.changelog)
    val downloadingText = stringResource(R.string.downloading_plugin)
    val startDownloadingText = stringResource(R.string.start_downloading_plugin)
    val fetchChangeLogFailed = stringResource(R.string.fetch_changelog_failed)

    suspend fun onModuleUpdate(
        plugin: PluginInfo,
        changelogUrl: String,
        downloadUrl: String,
        fileName: String,
    ) {
        val changelogResult = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                runCatching {
                    axeronApp.okhttpClient.newCall(
                        Request.Builder().url(changelogUrl).build()
                    ).execute().body!!.string()
                }
            }
        }

        val showToast: suspend (String) -> Unit = { msg ->
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    msg,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val changelog = changelogResult.getOrElse {
            showToast(fetchChangeLogFailed.format(it.message))
            return
        }.ifBlank {
            showToast(fetchChangeLogFailed.format(plugin.prop.name))
            return
        }

        val confirmResult = confirmDialog.awaitConfirm(
            title = changelogText,
            content = changelog,
            markdown = true,
            confirm = updateText,
            dragHandle = true,
        )

        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        showToast(startDownloadingText.format(plugin.prop.name))

        val downloading = downloadingText.format(plugin.prop.name)
        withContext(Dispatchers.IO) {
            download(
                context,
                downloadUrl,
                fileName,
                downloading,
                onDownloaded = onInstallModule,
                onDownloading = {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, downloading, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    val askUninstallPlugin = stringResource(R.string.ask_uninstall_plugin)
    val uninstall = stringResource(R.string.uninstall)
    val cancel = stringResource(R.string.cancel)
    val moduleUninstallConfirm = stringResource(R.string.uninstall_plugin_confirmation)
    val successUninstall = stringResource(R.string.plugin_uninstalled)
    val failedUninstall = stringResource(R.string.failed_to_uninstall_plugin)
    val restartService = stringResource(R.string.restart_service)

    suspend fun onModuleUninstall(plugin: PluginInfo) {
        val confirmResult = confirmDialog.awaitConfirm(
            askUninstallPlugin,
            content = moduleUninstallConfirm.format(plugin.prop.name),
            confirm = uninstall,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                AxeronPluginService.uninstallPlugin(plugin.dirId)
            }
        }

        if (success) {
            disableWebUIShortcut(context, plugin.prop.id)
            viewModel.fetchModuleList()
        }
        if (success) {
            successUninstall.format(plugin.prop.name)
        } else {
            failedUninstall.format(plugin.prop.name)
        }
        if (success) {
            restartService
        } else {
            null
        }
    }

    val askRestorePlugin = stringResource(R.string.ask_restore_plugin)
    val restore = stringResource(R.string.restore)
    val moduleRestoreConfirm = stringResource(R.string.restore_plugin_confirmation)

    suspend fun onModuleRestore(plugin: PluginInfo) {
        val confirmResult = confirmDialog.awaitConfirm(
            askRestorePlugin,
            content = moduleRestoreConfirm.format(plugin.prop.name),
            confirm = restore,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                AxeronPluginService.restorePlugin(plugin.dirId)
            }
        }

        if (success) {
            enableWebUIShortcut(context, plugin.prop.id)
            viewModel.fetchModuleList()
        }
    }

    AxPullToRefreshBox(
        modifier = modifier,
        isRefreshing = viewModel.isRefreshing,
        onRefresh = {
            viewModel.fetchModuleList()
        },
        topInset = contentPadding.calculateTopPadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()).nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + LocalFloatingBarInset.current + 16.dp
            ),
        ) {
            if (settings.isDeveloperModeEnabled) {
                item(key = "plugin_dev_info") {
                    PluginDevInfoCard(viewModel.plugins)
                }
            }

            when {
                viewModel.pluginList.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_plugin_installed),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    items(viewModel.pluginList) { plugin ->
                        val scope = rememberCoroutineScope()
                        val updatedModule by produceState(
                            key1 = plugin.prop.id,
                            initialValue = Triple("", "", "")
                        ) {
                            value = withContext(Dispatchers.IO) {
                                viewModel.checkUpdate(plugin)
                            }
                        }

                        PluginItem(
                            navigator = navigator,
                            settings = settings,
                            viewModel = viewModel,
                            plugin = plugin,
                            updateUrl = updatedModule.first,
                            onUninstall = {
                                scope.launch { onModuleUninstall(plugin) }
                            },
                            onRestore = {
                                scope.launch { onModuleRestore(plugin) }
                            },
                            onCheckChanged = {
                                scope.launch {
                                    val success = loadingDialog.withLoading {
                                        withContext(Dispatchers.IO) {
                                            AxeronPluginService.togglePlugin(
                                                plugin.dirId,
                                                !plugin.enabled
                                            )
                                        }
                                    }
                                    if (success) {
                                        viewModel.fetchModuleList()
                                    } else {
                                        val message =
                                            if (plugin.enabled) failedDisable else failedEnable
                                        snackBarHost.showSnackbar(message.format(plugin.prop.name))

    val askUninstallPlugin = stringResource(R.string.ask_uninstall_plugin)
    val uninstall = stringResource(R.string.uninstall)
    val cancel = stringResource(R.string.cancel)
    val moduleUninstallConfirm = stringResource(R.string.uninstall_plugin_confirmation)
    val successUninstall = stringResource(R.string.plugin_uninstalled)
    val failedUninstall = stringResource(R.string.failed_to_uninstall_plugin)
    suspend fun onModuleUninstall(plugin: PluginInfo) {
        val r = confirmDialog.awaitConfirm(askUninstallPlugin, content = moduleUninstallConfirm.format(plugin.prop.name), confirm = uninstall, dismiss = cancel)
        if (r != ConfirmResult.Confirmed) return
        val success = loadingDialog.withLoading { withContext(Dispatchers.IO) { AxeronPluginService.uninstallPlugin(plugin.dirId) } }
        if (success) { disableWebUIShortcut(context, plugin.prop.id); viewModel.fetchModuleList(); Toast.makeText(context, successUninstall.format(plugin.prop.name), Toast.LENGTH_SHORT).show() }
        else Toast.makeText(context, failedUninstall.format(plugin.prop.name), Toast.LENGTH_SHORT).show()
    }
    val askRestorePlugin = stringResource(R.string.ask_restore_plugin)
    val restore = stringResource(R.string.restore)
    val moduleRestoreConfirm = stringResource(R.string.restore_plugin_confirmation)
    suspend fun onModuleRestore(plugin: PluginInfo) {
        val r = confirmDialog.awaitConfirm(askRestorePlugin, content = moduleRestoreConfirm.format(plugin.prop.name), confirm = restore, dismiss = cancel)
        if (r != ConfirmResult.Confirmed) return
        loadingDialog.withLoading { withContext(Dispatchers.IO) { runCatching { AxeronPluginService.uninstallPlugin(plugin.dirId) } } }
        viewModel.fetchModuleList()
    }
    val dark = isSystemInDarkTheme()
    val trans = rememberInfiniteTransition(label = "listRgb")
    val shift by trans.animateFloat(0f, 1000f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "s")
    val rgb = remember(shift) { Brush.linearGradient(listOf(Color(0xFFFF2D55), Color(0xFFFFD700), Color(0xFF00E676), Color(0xFF40C4FF), Color(0xFF7C4DFF)), start = androidx.compose.ui.geometry.Offset(shift - 500f, 0f), end = androidx.compose.ui.geometry.Offset(shift, 300f)) }
    val gold = remember(shift) { Brush.linearGradient(listOf(Color(0xFFB8860B), Color(0xFFFFD700), Color(0xFFFFF3B0)), start = androidx.compose.ui.geometry.Offset(shift - 400f, 0f), end = androidx.compose.ui.geometry.Offset(shift, 150f)) }
    Column(modifier = modifier.fillMaxSize().background(if (dark) Color.Black else MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text("پلاگین‌ها • طلایی زنده", style = TextStyle(brush = gold), fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("${viewModel.pluginList.size} مورد • ${viewModel.pluginEnabledCount} فعال • ${viewModel.pluginUpdateCount} آپدیت", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(6.dp)); Box(Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(99.dp)).background(rgb))
            if (viewModel.isRefreshing) { Spacer(Modifier.height(6.dp)); LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(99.dp))) }
        }
        if (viewModel.pluginList.isEmpty() && !viewModel.isRefreshing) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Text("پلاگینی نیست\nجستجو: ${viewModel.search}", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp) }
        } else {
            LazyColumn(state = listState, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(items = viewModel.pluginList, key = { it.prop.id + "_" + it.prop.versionCode }, contentType = { "plugin_card" }) { plugin ->
                    val isExp = expandedPluginId == plugin.prop.id
                    PluginItem(navigator = navigator, settings = settings, viewModel = viewModel, plugin = plugin, updateUrl = remember(plugin) { try { viewModel.checkUpdate(plugin).first } catch (_: Exception) { "" } }, onUninstall = { scope.launch { onModuleUninstall(it) } }, onRestore = { scope.launch { onModuleRestore(it) } }, onCheckChanged = { scope.launch { viewModel.fetchModuleList(); Toast.makeText(context, if (it) "فعال شد" else "غیرفعال شد", Toast.LENGTH_SHORT).show() } }, onUpdate = { p -> scope.launch { val t = viewModel.checkUpdate(p); if (t.first.isNotEmpty()) onModuleUpdate(p, t.third, t.first, p.prop.id + ".zip") } }, onClick = { onClickModule(it) }, expanded = isExp, onExpandToggle = { expandedPluginId = if (isExp) null else plugin.prop.id })
                                    }
                                    }
                                }
                            },
                            onUpdate = {
                                scope.launch {
                                    onModuleUpdate(
                                        plugin,
                                        updatedModule.third,
                                        updatedModule.first,
                                        "${plugin.prop.name}-${updatedModule.second}.zip"
                                    )
                                    viewModel.fetchModuleList()
                                }
                            },
                            onClick = {
                                onClickModule(plugin)
                            },
                            expanded = expandedPluginId == plugin.prop.id,
                            onExpandToggle = {
                                expandedPluginId =
                                    if (expandedPluginId == plugin.prop.id) null else plugin.prop.id
                            }
                        )
                        Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }
        DownloadListener(context, onInstallModule)
    }
}
