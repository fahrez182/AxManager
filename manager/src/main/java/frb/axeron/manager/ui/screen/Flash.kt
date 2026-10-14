package frb.axeron.manager.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import frb.axeron.manager.ui.component.AxTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.fox2code.androidansi.ktx.parseAsAnsiAnnotatedString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronCommandSession
import frb.axeron.api.AxeronPluginService
import frb.axeron.api.AxeronPluginService.flashPlugin
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.api.utils.AnsiFilter
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.KeyEventBlocker
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.component.resolveDisplayName
import frb.axeron.manager.ui.theme.GREEN
import frb.axeron.manager.ui.theme.ORANGE
import frb.axeron.manager.ui.theme.RED
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel
import frb.axeron.manager.ui.component.TerminalOptionsSheet
import androidx.compose.material.icons.outlined.FormatSize
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import androidx.lifecycle.viewmodel.compose.viewModel
import frb.axeron.server.PluginInstaller
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FlashingStatus {
    IDLE,
    FLASHING,
    SUCCESS,
    FAILED
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Uri.getFileName(context: Context): String {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(this, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex != -1) {
            it.getString(nameIndex)
        } else {
            this.lastPathSegment ?: "unknown.zip"
        }
    } ?: (this.lastPathSegment ?: "unknown.zip")
}

@Parcelize
sealed class FlashIt : Parcelable {
    data class FlashPlugins(val installers: List<PluginInstaller>) : FlashIt()

    data class FlashUninstall(val rootMode: Boolean, val both: Boolean = false) : FlashIt()
}

@ExperimentalMaterial3Api
@Composable
fun InstallDialog(
    confirm: Boolean = false,
    flashIt: FlashIt,
    onConfirm: (FlashIt) -> Unit,
    onDismiss: () -> Unit,
) {
    if (flashIt is FlashIt.FlashPlugins && confirm) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onDismissRequest = onDismiss
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {

                Text(
                    text = stringResource(R.string.ask_install_plugin),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.ask_install_plugin_msg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                val installers = remember {
                    mutableStateListOf<PluginInstaller>().apply {
                        addAll(flashIt.installers)
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    items(installers.size) { index ->
                        val pluginInstaller = installers[index]

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // ICON
                                Icon(
                                    imageVector = Icons.Outlined.Extension,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // TEXT INFO
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = pluginInstaller.uri.resolveDisplayName(LocalContext.current),
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (pluginInstaller.autoEnable)
                                            stringResource(R.string.auto_enable_plugin)
                                        else
                                            stringResource(R.string.manual_enable_plugin),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // SWITCH
                                Switch(
                                    checked = pluginInstaller.autoEnable,
                                    onCheckedChange = { checked ->
                                        installers[index] =
                                            pluginInstaller.copy(autoEnable = checked)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onConfirm(FlashIt.FlashPlugins(installers))
                        }
                    ) {
                        Text(stringResource(R.string.install))
                    }
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun FlashScreen(
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal,
    flashIt: FlashIt,
    finishIntent: Boolean = false
) {
    val pluginViewModel = viewModelGlobal.pluginViewModel
    val context = LocalContext.current
    val activity = context.findActivity()

    val developerOptionsEnabled = AxeronSettings.getEnableDeveloperOptions()

    // The flash job and its output live in the ViewModel so rotation neither loses the log
    // nor leaves the screen stuck on FLASHING.
    val runner: CommandRunViewModel = viewModel()

    // Dialog outcome before the job starts (confirmed / dismissed).
    var requested by rememberSaveable {
        mutableStateOf(FlashingStatus.IDLE)
    }
    val flashing = when (val state = runner.state) {
        CommandRunViewModel.State.Idle -> requested
        CommandRunViewModel.State.Running -> FlashingStatus.FLASHING
        is CommandRunViewModel.State.Finished ->
            if (state.success) FlashingStatus.SUCCESS else FlashingStatus.FAILED
    }

    val view = LocalView.current
    DisposableEffect(flashing) {
        view.keepScreenOn = flashing == FlashingStatus.FLASHING
        onDispose {
            view.keepScreenOn = false
        }
    }

    BackHandler(enabled = flashing == FlashingStatus.FLASHING) {
        // Disable back button if flashing is running
    }

    BackHandler(enabled = flashing != FlashingStatus.FLASHING) {
        navigator.popBackStack()
        if (finishIntent) activity?.finish()
    }

//    var confirmed by rememberSaveable { mutableStateOf(flashIt !is FlashIt.FlashPlugins) }
    var pendingFlashIt by rememberSaveable { mutableStateOf<FlashIt?>(null) }


    if (flashIt is FlashIt.FlashUninstall) {
        pendingFlashIt = flashIt
    }


    InstallDialog(
        confirm = flashing == FlashingStatus.IDLE,
        flashIt = flashIt,
        onConfirm = {
            requested = FlashingStatus.FLASHING
            pendingFlashIt = it
        },
        onDismiss = {
            requested = FlashingStatus.FAILED
            navigator.popBackStack()
            if (finishIntent) activity?.finish()
        }
    )

    val scope = rememberCoroutineScope()

    val errorSaveLog = stringResource(R.string.log_error_code)

    LaunchedEffect(pendingFlashIt) {
        Log.d("FlashScreen", "flashing: $flashing")
        val pending = pendingFlashIt ?: return@LaunchedEffect
        // start() runs once per screen, so recomposition/rotation can't flash twice.
        runner.start { terminal ->
            val result = flashIt(
                pending,
                onStdout = terminal.sink(CommandRunViewModel.Stream.STDOUT),
                onStderr = terminal.sink(CommandRunViewModel.Stream.STDERR)
            )
            if (result.code != 0) {
                terminal.appendLine(
                    CommandRunViewModel.Stream.NOTICE,
                    errorSaveLog.format(result.code, result.err).trimEnd()
                )
            }
            result.code
        }
    }


    val snackBarHost = LocalSnackbarHost.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var showOptions by remember { mutableStateOf(false) }
    if (showOptions) TerminalOptionsSheet(onDismissRequest = { showOptions = false })

    Scaffold(
        topBar = {
            val logSaved = stringResource(R.string.log_saved_to)
            val logFailed = stringResource(R.string.failed_to_save_log)
            TopBar(
                flashing,
                onBack = dropUnlessResumed {
                    pluginViewModel.markNeedRefresh()
                    navigator.popBackStack()
                    if (finishIntent) activity?.finish()
                },
                onOptions = { showOptions = true },
                onSave = {
                    scope.launch {
                        runCatching { runner.saveLog("AxManager_install_log") }
                            .onSuccess { snackBarHost.showSnackbar(logSaved.format(it)) }
                            .onFailure { snackBarHost.showSnackbar(logFailed.format(it.message)) }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            val reigniteLoading = rememberLoadingDialog()
            if (flashIt is FlashIt.FlashPlugins && (flashing == FlashingStatus.SUCCESS)) {
                ExtendedFloatingActionButton(shape = CircleShape, 
                    onClick = {
                        scope.launch {
                            reigniteLoading.withLoading {
                                AxeronPluginService.igniteSuspendService()
                            }
                            navigator.popBackStack()
                            if (finishIntent) activity?.finish()
                        }
                    },
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    text = { Text(text = stringResource(R.string.re_ignite_and_close)) }
                )
            }

            if (flashIt is FlashIt.FlashUninstall && (flashing == FlashingStatus.SUCCESS)) {
                ExtendedFloatingActionButton(shape = CircleShape, 
                    onClick = {
                        scope.launch {
                            reigniteLoading.withLoading {
                                Axeron.newProcess(
                                    AxeronCommandSession.getQuickCmd(
                                        Starter.internalCommand,
                                        true,
                                        false
                                    ),
                                    null,
                                    null
                                )
                            }
                            navigator.popBackStack()
                            if (finishIntent) activity?.finish()
                        }
                    },
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    text = { Text(text = stringResource(R.string.restart_and_close)) }
                )
            }

            if (flashing == FlashingStatus.FAILED) {
                // Close button for modules flashing
                ExtendedFloatingActionButton(shape = CircleShape, 
                    text = { Text(text = stringResource(R.string.close)) },
                    icon = { Icon(Icons.Filled.Close, contentDescription = null) },
                    onClick = {
                        navigator.popBackStack()
                        if (finishIntent) activity?.finish()
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { AxSnackBarHost(hostState = snackBarHost) }
    ) { innerPadding ->
        KeyEventBlocker {
            it.key == Key.VolumeDown || it.key == Key.VolumeUp
        }
        CommandRunOutput(
            runner = runner,
            showStderr = developerOptionsEnabled,
            // Room for the result FAB once the job is done.
            bottomPadding = if (flashing == FlashingStatus.FLASHING) 12.dp else 88.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }

}

suspend fun flashModulesSequentially(
    installers: List<PluginInstaller>,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): AxeronPluginService.FlashResult {
    for (installer in installers) {
        flashPlugin(installer, onStdout, onStderr).apply {
            if (code != 0) {
                return AxeronPluginService.FlashResult(code, err, showReboot)
            }
        }
    }
    return AxeronPluginService.FlashResult(0, "", true)
}

suspend fun flashIt(
    flashIt: FlashIt,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): AxeronPluginService.FlashResult {
    return when (flashIt) {
        is FlashIt.FlashPlugins -> {
            flashModulesSequentially(flashIt.installers, onStdout, onStderr)
        }

        is FlashIt.FlashUninstall -> {
            if (flashIt.both) {
                val resultRoot = AxeronPluginService.resetManagerNative(true, onStdout, onStderr)
                if (resultRoot.code != 0) return resultRoot
                AxeronPluginService.resetManagerNative(false, onStdout, onStderr)
            } else {
                AxeronPluginService.resetManagerNative(flashIt.rootMode, onStdout, onStderr)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    status: FlashingStatus,
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    onOptions: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    AxTopAppBar(
        title = {
            Text(
                when (status) {
                    FlashingStatus.FLASHING -> stringResource(R.string.flashing)
                    FlashingStatus.SUCCESS -> stringResource(R.string.success)
                    FlashingStatus.FAILED -> stringResource(R.string.failed)
                    else -> {
                        stringResource(R.string.idle)
                    }
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = when (status) {
                    FlashingStatus.FLASHING -> ORANGE
                    FlashingStatus.SUCCESS -> GREEN
                    FlashingStatus.FAILED -> RED
                    else -> {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                }
            )
        },
        navigationIcon = {
            IconButton(
                onClick = { if (status != FlashingStatus.FLASHING) onBack() },
                enabled = status != FlashingStatus.FLASHING
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
        actions = {
            IconButton(onClick = onOptions) {
                Icon(
                    Icons.Outlined.FormatSize,
                    contentDescription = stringResource(R.string.terminal_view_options)
                )
            }
            IconButton(
                onClick = { if (status != FlashingStatus.FLASHING) onSave() },
                enabled = status != FlashingStatus.FLASHING
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Localized description"
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}