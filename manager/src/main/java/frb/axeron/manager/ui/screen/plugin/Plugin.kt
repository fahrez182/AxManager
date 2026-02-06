package frb.axeron.manager.ui.screen.plugin

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.AxeronPluginService
import frb.axeron.api.AxeronPluginService.ensureManageExternalStorageAllowed
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.SearchAppBar
import frb.axeron.manager.ui.component.SettingsItem
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.screen.FlashIt
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.manager.ui.webui.WebUIActivity
import frb.axeron.server.PluginInfo
import frb.axeron.server.PluginInstaller
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

    ExtraFilterSettings(
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
                        text = stringResource(R.string.plugin),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                searchLabel = stringResource(R.string.search_label_plugin),
                searchText = pluginViewModel.search,
                onSearchTextChange = { pluginViewModel.search = it },
                onClearClick = { pluginViewModel.search = "" },
                scrollBehavior = scrollBehavior,
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

                    val installers = mutableListOf<PluginInstaller>()
                    if (clipData != null) {
                        for (i in 0 until clipData.itemCount) {
                            clipData.getItemAt(i)?.uri?.let {
                                installers.add(PluginInstaller(it))
                            }
                        }
                    } else {
                        data.data?.let {
                            installers.add(PluginInstaller(it))
                        }
                    }

                    if (installers.isEmpty()) return@rememberLauncherForActivityResult

                    pluginViewModel.updateZipUris(installers)

                    navigator.navigate(FlashScreenDestination(FlashIt.FlashPlugins(installers)))
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
                                        AxeronPluginService.igniteSuspendService()
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

                    val permissionDenied = stringResource(R.string.permission_denied)
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                setType("application/zip")
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                ensureManageExternalStorageAllowed(context) {
                                    if (it) {
                                        selectZipLauncher.launch(intent)
                                    } else {
                                        Toast.makeText(context, permissionDenied, Toast.LENGTH_LONG)
                                            .show()
                                    }
                                }
                            } else {
                                selectZipLauncher.launch(intent)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, null)
                    }
                }
            }
        },
        snackbarHost = { AxSnackBarHost(hostState = snackBarHost) }
    ) { paddingValues ->
        PluginList(
            navigator = navigator,
            settings = settingsViewModel,
            viewModel = pluginViewModel,
            modifier = Modifier.padding(paddingValues),
            onInstallModule = {
                navigator.navigate(
                    FlashScreenDestination(
                        FlashIt.FlashPlugins(
                            listOf(
                                PluginInstaller(it)
                            )
                        )
                    )
                )
            },
            onClickModule = { plugin ->
                if (plugin.hasWebUi) {
                    webUILauncher.launch(
                        Intent(context, WebUIActivity::class.java).apply {
                            putExtra("id", plugin.prop.id)
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
fun ExtraFilterSettings(
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
                val ascOption = listOf(R.string.ascending, R.string.descending)
                val sortOption = listOf(
                    R.string.name, R.string.size,
                    R.string.enable, R.string.action, R.string.web_ui
                )

                SettingsItem(
                    label = stringResource(R.string.filter_settings),
                    iconVector = Icons.Outlined.FilterAlt
                ) { enabled, checked ->
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow {
                            ascOption.forEachIndexed { index, labelId ->
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
                                            text = stringResource(labelId)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.padding(2.dp))

                        SingleChoiceSegmentedButtonRow {
                            sortOption.forEachIndexed { index, labelId ->
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
                                            text = stringResource(labelId)
                                        )
                                    }
                                )
                            }
                        }
                    }

                }

                SettingsItem(
                    iconVector = Icons.Filled.DeveloperMode,
                    label = stringResource(R.string.enable_developer_mode),
                    description = stringResource(R.string.enable_developer_mode_msg),
                    checked = settingsViewModel.isDeveloperModeEnabled,
                    onSwitchChange = {
                        settingsViewModel.setDeveloperOptions(it)
                    }
                )
            }
        }
    }
}


val dummyPlugin = PluginInfo()

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