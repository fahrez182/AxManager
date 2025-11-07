package com.frb.axmanager.ui.screen.plugin

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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frb.axmanager.axApp
import com.frb.axmanager.ui.component.ConfirmResult
import com.frb.axmanager.ui.component.rememberConfirmDialog
import com.frb.axmanager.ui.component.rememberLoadingDialog
import com.frb.axmanager.ui.util.DownloadListener
import com.frb.axmanager.ui.util.download
import com.frb.axmanager.ui.viewmodel.PluginViewModel
import com.frb.axmanager.ui.viewmodel.SettingsViewModel
import com.frb.engine.client.PluginService
import com.frb.engine.data.PluginInfo
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
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
    onInstallModule: (Uri) -> Unit,
    onClickModule: (plugin: PluginInfo) -> Unit,
    context: Context,
    snackBarHost: SnackbarHostState,
    listState: LazyListState
) {
    val failedEnable = "Failed to enable plugin: %s"
    val failedDisable = "Failed to disable plugin: %s"

    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog()

    var expandedPluginId by rememberSaveable { mutableStateOf<String?>(null) }

    suspend fun onModuleUpdate(
        plugin: PluginInfo,
        changelogUrl: String,
        downloadUrl: String,
        fileName: String,
    ) {
        val updateText = "Update"
        val changelogText = "Changelog"
        val downloadingText = "Downloading module: %s"
        val startDownloadingText = "Start downloading: %s"
        val fetchChangeLogFailed = "Fetch changelog failed: %s"

        val changelogResult = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                runCatching {
                    axApp.okhttpClient.newCall(
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

        // changelog is not empty, show it and wait for confirm
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

    suspend fun onModuleUninstall(plugin: PluginInfo) {
        val moduleStr = "Uninstall Plugin?"
        val uninstall = "Uninstall"
        val cancel = "Cancel"
        val moduleUninstallConfirm = "Are you sure you want to uninstall module %s?"
        val failedUninstall = "Failed to uninstall: %s"
        val successUninstall = "%s uninstalled"
        val restartService = "Restart Service"

        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = moduleUninstallConfirm.format(plugin.prop.name),
            confirm = uninstall,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                PluginService.uninstallPlugin(plugin.dirId)
            }
        }

        if (success) {
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

    suspend fun onModuleRestore(plugin: PluginInfo) {
        val moduleStr = "Restore Plugin?"
        val restore = "Restore"
        val cancel = "Cancel"
        val moduleRestoreConfirm = "Are you sure you want to restore module %s?"
        "Failed to restore: %s"
        "%s restored"

        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = moduleRestoreConfirm.format(plugin.prop.name),
            confirm = restore,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                PluginService.restorePlugin(plugin.dirId)
            }
        }

        if (success) {
            viewModel.fetchModuleList()
        }
    }

    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = viewModel.isRefreshing,
        onRefresh = {
            viewModel.fetchModuleList()
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()).nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = remember {
                PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 120.dp
                )
            },

            ) {
            when {
                viewModel.pluginList.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No plugin installed",
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
                                            PluginService.togglePlugin(
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