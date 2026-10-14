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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.OptionTile
import frb.axeron.manager.ui.component.OptionTileGrid
import frb.axeron.manager.ui.component.OptionsSectionHeader
import frb.axeron.manager.ui.component.OrderToggle
import frb.axeron.manager.ui.component.SearchAppBar
import frb.axeron.manager.ui.component.SettingsRow
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.screen.FlashIt
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.component.FloatingBarFab
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
                        // A dot marks that the list is sorted differently from the default.
                        BadgedBox(
                            badge = { if (!pluginViewModel.isFilterDefault) Badge() }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = stringResource(R.string.filter_settings)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Launcher and dialogs stay in this screen's composition; only the buttons are
            // rendered beside the nav pill by FloatingBarFab.
            run {
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

                // Re-ignite stacks above the add button when needed.
                FloatingBarFab(visible = showFab) {
                    AnimatedVisibility(
                        visible = pluginViewModel.isNeedReignite,
                        // Grows in place above the add button; a vertical slide would pass over it.
                        enter = fadeIn() + scaleIn(initialScale = 0.6f),
                        exit = fadeOut() + scaleOut(targetScale = 0.6f),
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

                    val permissionDenied = stringResource(R.string.permission_denied)
                    FloatingActionButton(shape = CircleShape, 
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
            modifier = Modifier,
            contentPadding = paddingValues,
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
                        WebUIActivity.intentFor(context, plugin.prop.id)
                    )
                }
            },
            context = context,
            snackBarHost = snackBarHost,
            listState = listState
        )
    }
}

@Composable
fun ExtraFilterSettings(
    showDialog: Boolean,
    settingsViewModel: SettingsViewModel,
    pluginViewModel: PluginViewModel,
    onDismissRequest: () -> Unit
) {
    if (!showDialog) return

    AxOptionsSheet(
        title = stringResource(R.string.filter_settings),
        description = stringResource(R.string.plugin_filter_desc),
        onDismissRequest = onDismissRequest,
        headerAction = {
            TextButton(
                enabled = !pluginViewModel.isFilterDefault,
                onClick = pluginViewModel::resetFilter
            ) {
                Text(stringResource(R.string.reset))
            }
        }
    ) {
        // Index order matches PluginViewModel's sort codes.
        OptionsSectionHeader(stringResource(R.string.sort_by))
        OptionTileGrid(
            items = listOf(
                Icons.Outlined.SortByAlpha to R.string.name,
                Icons.Outlined.SdStorage to R.string.size,
                Icons.Outlined.ToggleOn to R.string.enable,
                Icons.Outlined.Terminal to R.string.action,
                Icons.Outlined.Web to R.string.web_ui
            ).withIndex().toList()
        ) { (index, option), modifier ->
            OptionTile(
                modifier = modifier,
                icon = option.first,
                label = stringResource(option.second),
                selected = pluginViewModel.getSelectedSort == index,
                onClick = { pluginViewModel.setSelectedSort(index) }
            )
        }

        OptionsSectionHeader(stringResource(R.string.sort_order))
        OrderToggle(
            ascending = pluginViewModel.getSelectedAsc == 0,
            onChange = { ascending -> pluginViewModel.setSelectedAsc(if (ascending) 0 else 1) }
        )

        OptionsSectionHeader(stringResource(R.string.settings_section_more))
        AxCard {
            SettingsRow(
                iconVector = Icons.Filled.DeveloperMode,
                label = stringResource(R.string.enable_developer_mode),
                description = stringResource(R.string.enable_developer_mode_msg),
                checked = settingsViewModel.isDeveloperModeEnabled,
                onSwitchChange = { settingsViewModel.setDeveloperOptions(it) }
            )
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