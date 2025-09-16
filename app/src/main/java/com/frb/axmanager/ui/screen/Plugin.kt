package com.frb.axmanager.ui.screen

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Wysiwyg
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.frb.axmanager.axApp
import com.frb.axmanager.ui.component.AxSnackBarHost
import com.frb.axmanager.ui.component.ConfirmResult
import com.frb.axmanager.ui.component.ExtraLabel
import com.frb.axmanager.ui.component.ExtraLabelDefaults
import com.frb.axmanager.ui.component.SearchAppBar
import com.frb.axmanager.ui.component.rememberConfirmDialog
import com.frb.axmanager.ui.component.rememberLoadingDialog
import com.frb.axmanager.ui.util.DownloadListener
import com.frb.axmanager.ui.util.LocalSnackbarHost
import com.frb.axmanager.ui.util.download
import com.frb.axmanager.ui.viewmodel.PluginViewModel
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.frb.axmanager.ui.webui.WebUIActivity
import com.frb.engine.client.Axeron
import com.frb.engine.client.PluginService
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ExecutePluginActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PluginScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
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
                windowInsets = WindowInsets(top = 0)
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(
                    animationSpec = tween(200),
                    initialScale = 0.8f
                ) + fadeIn(animationSpec = tween(400)),
                exit = scaleOut(
                    animationSpec = tween(200),
                    targetScale = 0.8f
                ) + fadeOut(animationSpec = tween(400))
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

                ExtendedFloatingActionButton(
                    onClick = {
                        // Select the zip files to install
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        }
                        selectZipLauncher.launch(intent)
                    },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(text = "Install") },
                )
            }
        },
        snackbarHost = { AxSnackBarHost(hostState = snackBarHost) },
        contentWindowInsets = WindowInsets(top = 0)
    ) { paddingValues ->
        PluginList(
            navigator = navigator,
            viewModel = pluginViewModel,
            modifier = Modifier.padding(paddingValues),
            onInstallModule = {
                navigator.navigate(FlashScreenDestination(FlashIt.FlashPlugins(listOf(it))))
            },
            onClickModule = { plugin ->
                if (plugin.hasWebUi) {
                    webUILauncher.launch(
                        Intent(context, WebUIActivity::class.java)
                            .putExtra("plugin", plugin)
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
fun PluginList(
    navigator: DestinationsNavigator,
    viewModel: PluginViewModel,
    modifier: Modifier,
    onInstallModule: (Uri) -> Unit,
    onClickModule: (plugin: PluginViewModel.PluginInfo) -> Unit,
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
        plugin: PluginViewModel.PluginInfo,
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
                        okhttp3.Request.Builder().url(changelogUrl).build()
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
            showToast(fetchChangeLogFailed.format(plugin.name))
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

        showToast(startDownloadingText.format(plugin.name))

        val downloading = downloadingText.format(plugin.name)
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

    suspend fun onModuleUninstall(plugin: PluginViewModel.PluginInfo) {
        val moduleStr = "Uninstall Plugin?"
        val uninstall = "Uninstall"
        val cancel = "Cancel"
        val moduleUninstallConfirm = "Are you sure you want to uninstall module %s?"
        val failedUninstall = "Failed to uninstall: %s"
        val successUninstall = "%s uninstalled"
        val restartService = "Restart Service"

        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = moduleUninstallConfirm.format(plugin.name),
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
            successUninstall.format(plugin.name)
        } else {
            failedUninstall.format(plugin.name)
        }
        if (success) {
            restartService
        } else {
            null
        }
    }

    suspend fun onModuleRestore(plugin: PluginViewModel.PluginInfo) {
        val moduleStr = "Restore Plugin?"
        val restore = "Restore"
        val cancel = "Cancel"
        val moduleRestoreConfirm = "Are you sure you want to restore module %s?"
        "Failed to restore: %s"
        "%s restored"

        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = moduleRestoreConfirm.format(plugin.name),
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
                    bottom = 16.dp
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
                            key1 = plugin.id,
                            initialValue = Triple("", "", "")
                        ) {
                            value = withContext(Dispatchers.IO) {
                                viewModel.checkUpdate(plugin)
                            }
                        }

                        PluginItem(
                            navigator = navigator,
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
                                        snackBarHost.showSnackbar(message.format(plugin.name))
                                    }
                                }
                            },
                            onUpdate = {
                                scope.launch {
                                    onModuleUpdate(
                                        plugin,
                                        updatedModule.third,
                                        updatedModule.first,
                                        "${plugin.name}-${updatedModule.second}.zip"
                                    )
                                    viewModel.fetchModuleList()
                                }
                            },
                            onClick = {
                                onClickModule(it)
                            },
                            expanded = expandedPluginId == plugin.id,
                            onExpandToggle = {
                                expandedPluginId =
                                    if (expandedPluginId == plugin.id) null else plugin.id
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


@Composable
fun PluginItem(
    navigator: DestinationsNavigator?,
    viewModel: PluginViewModel?,
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

    PluginMoreSettings(showExtraSetDialog) {
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
                val amoledMode = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getBoolean("amoled_mode", false)
                val isDynamic = colorScheme.primary != colorScheme.secondary

                val fadeColor = when {
                    amoledMode && isDark -> Color.Black
                    isDynamic -> colorScheme.surface
                    isDark -> Color(0xFF222222)
                    else -> Color.White
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

                var developerOptionsEnabled by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("enable_developer_options", false)
                    )
                }

                LaunchedEffect(Unit) {
                    developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)
                }


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
                                                        viewModel?.fetchModuleList()
                                                    }

                                                }
                                            }

                                        }
                                    )
//                                    when {
//                                        plugin.remove -> {
//                                            ExtraLabel(
//                                                text = "Uninstall",
//                                                style = ExtraLabelDefaults.style.copy(
//                                                    containerColor = MaterialTheme.colorScheme.errorContainer,
//                                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
//                                                )
//                                            )
//                                        }
//                                        plugin.enabled -> {
//                                            ExtraLabel(
//                                                text = "Enable",
//                                                style = ExtraLabelDefaults.style.copy(
//                                                    containerColor = MaterialTheme.colorScheme.errorContainer,
//                                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
//                                                )
//                                            )
//                                        }
//                                        !plugin.enabled -> {
//                                            ExtraLabel(
//                                                text = "Disable",
//                                                style = ExtraLabelDefaults.style.copy(
//                                                    containerColor = MaterialTheme.colorScheme.errorContainer,
//                                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
//                                                )
//                                            )
//                                        }
//                                    }
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
                                fontFamily = MaterialTheme.typography.titleMedium.fontFamily
                            )

                            Text(
                                text = "$moduleVersion: ${plugin.version}",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                            )

                            Text(
                                text = "$moduleAuthor: ${plugin.author}",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                            )

                            if (developerOptionsEnabled) {

                                Text(
                                    text = "$moduleId: ${plugin.id}",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                )

                                Text(
                                    text = "$moduleVersionCode: ${plugin.versionCode}",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                )

                                Text(
                                    text = if (plugin.updateJson.isNotEmpty()) "$moduleUpdateJson: ${plugin.updateJson}" else "$moduleUpdateJson: $moduleUpdateJsonEmpty",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                )
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
                                        viewModel!!.markNeedRefresh()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginMoreSettings(
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    if (showDialog) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("On Development")
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
        viewModel = null,
        plugin = dummyPlugin,
        updateUrl = "https://example.com/update",
        onUninstall = {},
        onRestore = {},
        onCheckChanged = {},
        onUpdate = {},
        onClick = {},
        expanded = false,
        onExpandToggle = {}
    )
}

fun formatSize(size: Long): String {
    if (size == 0L) return "null"
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        size >= gb -> String.format(Locale.getDefault(), "%.2f GB", size.toDouble() / gb)
        size >= mb -> String.format(Locale.getDefault(), "%.2f MB", size.toDouble() / mb)
        size >= kb -> String.format(Locale.getDefault(), "%.2f KB", size.toDouble() / kb)
        else -> "$size B"
    }
}