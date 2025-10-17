package com.frb.axmanager.ui.screen.plugin

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frb.axmanager.ui.component.AxSnackBarHost
import com.frb.axmanager.ui.component.SearchAppBar
import com.frb.axmanager.ui.component.SettingsItem
import com.frb.axmanager.ui.component.rememberLoadingDialog
import com.frb.axmanager.ui.screen.FlashIt
import com.frb.axmanager.ui.util.LocalSnackbarHost
import com.frb.axmanager.ui.viewmodel.PluginViewModel
import com.frb.axmanager.ui.viewmodel.SettingsViewModel
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.frb.axmanager.ui.webui.WebUIActivity
import com.frb.engine.client.PluginService
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PluginScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val settingsViewModel = viewModelGlobal.settingsViewModel
    val context = LocalContext.current
    val snackBarHost = LocalSnackbarHost.current
    val pluginViewModel = viewModelGlobal.pluginViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit, pluginViewModel.plugins, pluginViewModel.isNeedRefresh) {
        if (pluginViewModel.plugins.isEmpty() || pluginViewModel.isNeedRefresh) {
            pluginViewModel.fetchModuleList()
        }
    }

    val listState = rememberLazyListState()
    var showFab by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (currIndex, currOffset) ->
                val isScrollingDown = currIndex > lastIndex ||
                        (currIndex == lastIndex && currOffset > lastOffset + 4)
                val isScrollingUp = currIndex < lastIndex ||
                        (currIndex == lastIndex && currOffset < lastOffset - 4)

                when {
                    isScrollingDown && showFab -> showFab = false
                    isScrollingUp && !showFab -> showFab = true
                }

                lastIndex = currIndex
                lastOffset = currOffset
            }
    }

    val webUILauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { pluginViewModel.fetchModuleList() }

    var showExtraDialog by remember { mutableStateOf(false) }

    ExtraSettings(
        showExtraDialog,
        settingsViewModel,
        pluginViewModel
    ) {
        showExtraDialog = false
    }

    Scaffold(
        topBar = {
            SearchAppBar(
                title = {
                    Text(
                        text = "Plugin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                searchLabel = "Search Plugins",
                searchText = pluginViewModel.search,
                onSearchTextChange = { pluginViewModel.search = it },
                onClearClick = { pluginViewModel.search = "" },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(top = 0),
                action = {
                    IconButton(
                        onClick = {
                            showExtraDialog = true
                        })
                    {
                        Icon(Icons.Outlined.MoreVert, null)
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            ) {
                val selectZipLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode != RESULT_OK) {
                        return@rememberLauncherForActivityResult
                    }
                    val data = result.data ?: return@rememberLauncherForActivityResult
                    val clipData = data.clipData

                    val uris = mutableListOf<Uri>()
                    if (clipData != null) {
                        for (i in 0 until clipData.itemCount) {
                            clipData.getItemAt(i)?.uri?.let { uris.add(it) }
                        }
                    } else {
                        data.data?.let { uris.add(it) }
                    }

                    if (uris.isEmpty()) return@rememberLauncherForActivityResult

                    pluginViewModel.updateZipUris(uris)

                    navigator.navigate(FlashScreenDestination(FlashIt.FlashPlugins(uris)))
                    pluginViewModel.clearZipUris()
                    pluginViewModel.markNeedRefresh()
                }

                val loadingDialog = rememberLoadingDialog()
                val scope = rememberCoroutineScope()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    AnimatedVisibility(
                        visible = pluginViewModel.isNeedReignite,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    ) {
                        IconButton(
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            onClick = {
                                scope.launch {
                                    val success = loadingDialog.withLoading {
                                        PluginService.igniteSuspendService()
                                    }

                                    if (success) {
                                        pluginViewModel.fetchModuleList()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.LocalFireDepartment, null)
                        }
                    }

                    Spacer(modifier = Modifier.padding(6.dp))

                    FloatingActionButton(
                        onClick = {
                            // Select the zip files to install
                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                setType("application/zip")
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            }
                            selectZipLauncher.launch(intent)
                        }
                    ) {
                        Icon(Icons.Filled.Add, null)
                    }
                }
            }
        },
        snackbarHost = { AxSnackBarHost(hostState = snackBarHost) },
        contentWindowInsets = WindowInsets(top = 0)
    ) { paddingValues ->
        PluginList(
            navigator = navigator,
            settings = settingsViewModel,
            viewModel = pluginViewModel,
            modifier = Modifier.padding(paddingValues),
            onInstallModule = {
                navigator.navigate(FlashScreenDestination(FlashIt.FlashPlugins(listOf(it))))
            },
            onClickModule = { plugin ->
                if (plugin.hasWebUi) {
                    webUILauncher.launch(
                        Intent(context, WebUIActivity::class.java).apply {
                            putExtra("id", plugin.id)
                        }
                    )
                }
            },
            context = context,
            snackBarHost = snackBarHost,
            listState = listState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraSettings(
    showDialog: Boolean,
    settingsViewModel: SettingsViewModel,
    pluginViewModel: PluginViewModel,
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val ascOption = listOf("Ascending", "Descending")
                val sortOption = listOf("Name", "Size", "Enable", "Action", "WebUI")

                SettingsItem(
                    label = "Filter Settings",
                    iconVector = Icons.Outlined.FilterAlt
                ) { enabled, checked ->
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow {
                            ascOption.forEachIndexed { index, label ->
                                SegmentedButton(
                                    colors = SegmentedButtonDefaults.colors().copy(
                                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    icon = {},
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = ascOption.size,
                                        baseShape = SegmentedButtonDefaults.baseShape.copy(
                                            bottomStart = CornerSize(6.dp),
                                            bottomEnd = CornerSize(6.dp),
                                            topStart = CornerSize(6.dp),
                                            topEnd = CornerSize(6.dp)
                                        )
                                    ),
                                    onClick = { pluginViewModel.setSelectedAsc(index) },
                                    selected = index == pluginViewModel.getSelectedAsc,
                                    label = {
                                        Text(
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Normal,
                                            text = label
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.padding(2.dp))

                        SingleChoiceSegmentedButtonRow {
                            sortOption.forEachIndexed { index, label ->
                                SegmentedButton(
                                    colors = SegmentedButtonDefaults.colors().copy(
                                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    icon = {},
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = sortOption.size,
                                        baseShape = SegmentedButtonDefaults.baseShape.copy(
                                            bottomStart = CornerSize(6.dp),
                                            bottomEnd = CornerSize(6.dp),
                                            topStart = CornerSize(6.dp),
                                            topEnd = CornerSize(6.dp)
                                        )
                                    ),
                                    onClick = { pluginViewModel.setSelectedSort(index) },
                                    selected = index == pluginViewModel.getSelectedSort,
                                    label = {
                                        Text(
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Normal,
                                            text = label
                                        )
                                    }
                                )
                            }
                        }
                    }

                }

                SettingsItem(
                    iconVector = Icons.Filled.DeveloperMode,
                    label = "Enable developer options",
                    description = "Show hidden settings and debug info relevant only for developers.",
                    checked = settingsViewModel.isDeveloperModeEnabled,
                    onSwitchChange = {
                        settingsViewModel.setDeveloperOptions(it)
                    }
                )
            }
        }
    }
}


val dummyPlugin = PluginViewModel.PluginInfo(
    id = "dummy",
    name = "Dummy",
    author = "Dummy",
    version = "Dummy",
    versionCode = 0,
    description = "Dummy",
    enabled = true,
    update = false,
    updateInstall = false,
    updateRemove = false,
    updateEnable = false,
    updateDisable = false,
    remove = false,
    updateJson = "false",
    hasWebUi = false,
    hasActionScript = false,
    dirId = "dummy",
    size = 0,
    banner = ""
)

@Preview
@Composable
fun ItemPreview() {
    PluginItem(
        navigator = null,
        settings = viewModel(),
        viewModel = viewModel(),
        plugin = dummyPlugin,
        updateUrl = "https://example.com/update",
        onUninstall = {},
        onRestore = {},
        onCheckChanged = {},
        onUpdate = {},
        onClick = {},
        expanded = false,
        onExpandToggle = {},
    )
}