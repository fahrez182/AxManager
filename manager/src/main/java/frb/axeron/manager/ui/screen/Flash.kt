package frb.axeron.manager.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronCommandSession
import frb.axeron.api.AxeronPluginService
import frb.axeron.api.AxeronPluginService.flashPlugin
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.AxTopAppBar
import frb.axeron.manager.ui.component.KeyEventBlocker
import frb.axeron.manager.ui.component.TerminalOptionsSheet
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.component.resolveDisplayName
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel.State
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel.Stream
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.server.PluginInfo
import frb.axeron.server.PluginInstaller
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.File

private const val COPY_MAX_LINES = 5_000

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

/** A job for [FlashScreen], the shared output screen for installs, resets and plugin actions. */
@Parcelize
sealed class FlashIt : Parcelable {
    data class FlashPlugins(val installers: List<PluginInstaller>) : FlashIt()

    data class FlashUninstall(val rootMode: Boolean, val both: Boolean = false) : FlashIt()

    /** Runs the plugin's action.sh. */
    data class PluginAction(val plugin: PluginInfo) : FlashIt()
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

    // The job and its output live in the ViewModel so rotation neither loses the log nor
    // runs it twice.
    val runner: CommandRunViewModel = viewModel()
    val state = runner.state
    val running = state == State.Running
    val finished = state is State.Finished

    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    val close: () -> Unit = {
        pluginViewModel.markNeedRefresh()
        navigator.popBackStack()
        if (finishIntent) activity?.finish()
    }

    // The job can't be cancelled; keep the user here until it ends.
    BackHandler(enabled = running) {}
    BackHandler(enabled = !running, onBack = close)

    // Installs wait for the confirm sheet; everything else starts right away.
    var pendingFlashIt by rememberSaveable {
        mutableStateOf(if (flashIt is FlashIt.FlashPlugins) null else flashIt)
    }

    InstallDialog(
        confirm = pendingFlashIt == null && state == State.Idle,
        flashIt = flashIt,
        onConfirm = { pendingFlashIt = it },
        onDismiss = close
    )

    val errorSaveLog = stringResource(R.string.log_error_code)

    LaunchedEffect(pendingFlashIt) {
        val pending = pendingFlashIt ?: return@LaunchedEffect
        // start() runs once per screen, so recomposition/rotation can't run it twice.
        runner.start { terminal ->
            val result = flashIt(
                pending,
                onStdout = terminal.sink(Stream.STDOUT),
                onStderr = terminal.sink(Stream.STDERR)
            )
            if (result.code != 0) {
                terminal.appendLine(
                    Stream.NOTICE,
                    errorSaveLog.format(result.code, result.err).trimEnd()
                )
            }
            result.code
        }
    }

    val subtitle = when (flashIt) {
        is FlashIt.FlashPlugins -> remember(flashIt) {
            flashIt.installers.joinToString { it.uri.resolveDisplayName(context) }
        }

        is FlashIt.FlashUninstall -> stringResource(R.string.uninstall)
        is FlashIt.PluginAction -> flashIt.plugin.prop.name
    }
    val logPrefix = when (flashIt) {
        is FlashIt.FlashPlugins -> "AxManager_install_log"
        is FlashIt.FlashUninstall -> "AxManager_uninstall_log"
        is FlashIt.PluginAction -> "AxManager_action_log"
    }

    val scope = rememberCoroutineScope()
    val snackBarHost = LocalSnackbarHost.current
    val logSaved = stringResource(R.string.log_saved_to)
    val logFailed = stringResource(R.string.failed_to_save_log)
    val copied = stringResource(R.string.copied)
    var showOptions by remember { mutableStateOf(false) }
    if (showOptions) TerminalOptionsSheet(onDismissRequest = { showOptions = false })

    Scaffold(
        topBar = {
            AxTopAppBar(
                title = { CommandRunTitle(runner = runner, subtitle = subtitle) },
                navigationIcon = {
                    IconButton(
                        onClick = dropUnlessResumed(block = close),
                        enabled = !running
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            Icons.Outlined.FormatSize,
                            contentDescription = stringResource(R.string.terminal_view_options)
                        )
                    }
                    IconButton(
                        enabled = finished,
                        onClick = {
                            scope.launch {
                                runCatching { runner.saveLog(logPrefix) }
                                    .onSuccess { snackBarHost.showSnackbar(logSaved.format(it)) }
                                    .onFailure { snackBarHost.showSnackbar(logFailed.format(it.message)) }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = stringResource(R.string.save))
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = finished,
                enter = fadeIn() + expandVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // Android 13+ shows its own clipboard confirmation.
                            // The clipboard is a binder transaction (~1 MB): copy the tail.
                            val log = runner.terminal.text(lastLines = COPY_MAX_LINES)
                            if (ClipboardUtil.put(context, log) &&
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                            ) {
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.activation_copy_log))
                    }
                    FinishButton(
                        flashIt = flashIt,
                        success = (state as? State.Finished)?.success == true,
                        onClose = close,
                        modifier = Modifier.weight(1f)
                    )
                }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

/** Close, or the follow-up a successful job needs (re-ignite plugins, restart the server). */
@Composable
private fun FinishButton(
    flashIt: FlashIt,
    success: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val loading = rememberLoadingDialog()

    fun closeAfter(block: suspend () -> Unit) {
        scope.launch {
            loading.withLoading { block() }
            onClose()
        }
    }

    when {
        success && flashIt is FlashIt.FlashPlugins -> Button(
            modifier = modifier,
            onClick = { closeAfter { AxeronPluginService.igniteSuspendService() } }
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.re_ignite_and_close))
        }

        success && flashIt is FlashIt.FlashUninstall -> Button(
            modifier = modifier,
            onClick = {
                closeAfter {
                    Axeron.newProcess(
                        AxeronCommandSession.getQuickCmd(Starter.internalCommand, true, false),
                        null,
                        null
                    )
                }
            }
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.restart_and_close))
        }

        else -> Button(
            modifier = modifier,
            onClick = dropUnlessResumed(block = onClose)
        ) {
            Text(stringResource(R.string.close))
        }
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

        is FlashIt.PluginAction -> {
            val pluginPath = File(
                PathHelper.getWorkingPath(
                    Axeron.getAxeronInfo().isRoot(),
                    AxeronApiConstant.folder.PARENT_PLUGIN
                ), flashIt.plugin.dirId
            )
            val pluginBin = "${pluginPath.absolutePath}/system/bin"
            val cmd =
                $$"export PATH=$$pluginBin:$PATH; cd \"$$pluginPath\"; sh ./action.sh; RES=$?; cd /; exit $RES"
            // stderr is already streamed into the log; don't repeat it in the error notice.
            val result = AxeronPluginService.execWithIO(
                cmd = cmd,
                onStdout = onStdout,
                onStderr = onStderr,
                hideStderr = false,
                // Anything action.sh leaves running is stopped with the plugin.
                pluginId = flashIt.plugin.dirId
            )
            AxeronPluginService.FlashResult(result.code, "", false)
        }
    }
}
