package frb.axeron.manager.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import frb.axeron.api.AxeronPluginService
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.utils.AnsiFilter
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.KeyEventBlocker
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.server.PluginInfo
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Destination<RootGraph>
@Composable
fun ExecutePluginActionScreen(
    navigator: DestinationsNavigator,
    plugin: PluginInfo
) {
    val developerOptionsEnabled = AxeronSettings.getEnableDeveloperOptions()

    var isActionRunning by rememberSaveable { mutableStateOf(true) }

    val view = LocalView.current
    DisposableEffect(isActionRunning) {
        view.keepScreenOn = isActionRunning
        onDispose {
            view.keepScreenOn = false
        }
    }

    BackHandler(enabled = isActionRunning) {
        // Disable back button if action is running
    }

    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    val logContent = rememberSaveable { StringBuilder() }

    LaunchedEffect(Unit) {
        if (text.isNotEmpty()) {
            return@LaunchedEffect
        }
        launch(Dispatchers.IO) {
            val pluginPath =
                File(
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
                onStdout = {
                    if (AnsiFilter.isScreenControl(it)) { // clear command
                        launch(Dispatchers.Main) {
                            text = AnsiFilter.stripAnsi(it) + "\n"
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            text += "$it\n"
                        }
                    }
                    logContent.append(it).append("\n")
                },
                onStderr = {
                    logContent.append(it).append("\n")
                }
            )
        }
        isActionRunning = false
    }

    val snackBarHost = LocalSnackbarHost.current
    val scrollState = rememberScrollState()

    val logSaved = stringResource(R.string.log_saved_to)
    val logFailed = stringResource(R.string.failed_to_save_log)

    Scaffold(
        topBar = {
            TopBar(
                isActionRunning = isActionRunning,
                onBack = dropUnlessResumed {
                    navigator.popBackStack()
                },
                onSave = {
                    if (!isActionRunning) {
                        scope.launch {
                            val format =
                                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                            val date = format.format(Date())

                            val baseDir = PathHelper.getPath(AxeronApiConstant.folder.PARENT_LOG)
                            if (!baseDir.exists()) {
                                baseDir.mkdirs()
                            }

                            val file = File(baseDir, "AxManager_action_log_${date}.log")

                            try {
                                val fos = Axeron.newFileService()
                                    .getStreamSession(file.absolutePath, true, false).outputStream
                                fos.write("$logContent\n".toByteArray())
                                fos.flush()

                                snackBarHost.showSnackbar(logSaved.format(file.absolutePath))
                            } catch (e: Exception) {
                                snackBarHost.showSnackbar(logFailed.format(e.message))
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isActionRunning) {
                ExtendedFloatingActionButton(
                    text = { Text(text = stringResource(R.string.close)) },
                    icon = { Icon(Icons.Filled.Close, contentDescription = null) },
                    onClick = {
                        navigator.popBackStack()
                    }
                )
            }
        },
        snackbarHost = { AxSnackBarHost(snackBarHost) }
    ) { innerPadding ->
        KeyEventBlocker {
            it.key == Key.VolumeDown || it.key == Key.VolumeUp
        }
        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(innerPadding)
                .verticalScroll(scrollState),
        ) {
            LaunchedEffect(text) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }

            text = if (developerOptionsEnabled) logContent.toString() else text
            BasicText(
                modifier = Modifier.padding(8.dp),
                text = text.parseAsAnsiAnnotatedString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = MaterialTheme.typography.bodyMedium.fontSize, // samain dengan fontSize
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                ),
                softWrap = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(isActionRunning: Boolean, onBack: () -> Unit = {}, onSave: () -> Unit = {}) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.action),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                enabled = !isActionRunning
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
        actions = {
            IconButton(
                onClick = onSave,
                enabled = !isActionRunning
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = null,
                )
            }
        },
    )
}