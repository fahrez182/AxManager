package frb.axeron.manager.ui.screen

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.runtime.remember
import frb.axeron.manager.ui.component.TerminalOptionsSheet
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronPluginService
import frb.axeron.api.core.AxeronSettings
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.AxTopAppBar
import frb.axeron.manager.ui.component.KeyEventBlocker
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel.Stream
import frb.axeron.server.PluginInfo
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.launch
import java.io.File

private const val COPY_MAX_LINES = 5_000

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ExecutePluginActionScreen(
    navigator: DestinationsNavigator,
    plugin: PluginInfo
) {
    val runner: CommandRunViewModel = viewModel()
    val running = runner.isRunning
    val finished = runner.state is CommandRunViewModel.State.Finished
    val developerOptionsEnabled = AxeronSettings.getEnableDeveloperOptions()

    var wrap by rememberSaveable { mutableStateOf(true) }
    var showOptions by remember { mutableStateOf(false) }
    if (showOptions) TerminalOptionsSheet(onDismissRequest = { showOptions = false })

    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    // The action can't be cancelled; keep the user here until it ends.
    BackHandler(enabled = !finished) {}

    LaunchedEffect(Unit) {
        runner.start { terminal ->
            val pluginPath = File(
                PathHelper.getWorkingPath(
                    Axeron.getAxeronInfo().isRoot(),
                    AxeronApiConstant.folder.PARENT_PLUGIN
                ), plugin.dirId
            )
            val pluginBin = "${pluginPath.absolutePath}/system/bin"
            val cmd =
                $$"export PATH=$$pluginBin:$PATH; cd \"$$pluginPath\"; sh ./action.sh; RES=$?; cd /; exit $RES"
            AxeronPluginService.execWithIO(
                cmd = cmd,
                onStdout = terminal.sink(Stream.STDOUT),
                onStderr = terminal.sink(Stream.STDERR),
                hideStderr = false
            ).code
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackBarHost = LocalSnackbarHost.current
    val logSaved = stringResource(R.string.log_saved_to)
    val logFailed = stringResource(R.string.failed_to_save_log)
    val copied = stringResource(R.string.copied)

    Scaffold(
        topBar = {
            AxTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.action),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = dropUnlessResumed { navigator.popBackStack() },
                        enabled = finished
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            Icons.Outlined.FormatSize,
                            contentDescription = stringResource(R.string.terminal_view_options)
                        )
                    }
                    IconToggleButton(checked = wrap, onCheckedChange = { wrap = it }) {
                        Icon(
                            Icons.AutoMirrored.Filled.WrapText,
                            contentDescription = stringResource(R.string.command_word_wrap)
                        )
                    }
                    IconButton(
                        enabled = finished,
                        onClick = {
                            scope.launch {
                                runCatching { runner.saveLog("AxManager_action_log") }
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
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = dropUnlessResumed { navigator.popBackStack() }
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        },
        snackbarHost = { AxSnackBarHost(snackBarHost) }
    ) { innerPadding ->
        KeyEventBlocker {
            it.key == Key.VolumeDown || it.key == Key.VolumeUp
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = if (finished) 0.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandRunStatusCard(runner = runner, subtitle = plugin.prop.name)
            CommandRunOutput(
                runner = runner,
                wrap = wrap,
                showStderr = developerOptionsEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
