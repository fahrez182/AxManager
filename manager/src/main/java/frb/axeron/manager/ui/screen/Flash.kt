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
import androidx.compose.material3.TopAppBar
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
import frb.axeron.manager.BuildConfig
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.KeyEventBlocker
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.component.resolveDisplayName
import frb.axeron.manager.ui.theme.GREEN
import frb.axeron.manager.ui.theme.ORANGE
import frb.axeron.manager.ui.theme.RED
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
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

    data object FlashUninstall : FlashIt()
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

    var flashing by rememberSaveable {
        mutableStateOf(FlashingStatus.IDLE)
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
        flashing == FlashingStatus.FLASHING
        pendingFlashIt = flashIt
    }


    InstallDialog(
        confirm = flashing == FlashingStatus.IDLE,
        flashIt = flashIt,
        onConfirm = {
            flashing = FlashingStatus.FLASHING
            pendingFlashIt = it
        },
        onDismiss = {
            flashing = FlashingStatus.FAILED
            navigator.popBackStack()
            if (finishIntent) activity?.finish()
        }
    )

    val scope = rememberCoroutineScope()
    val logContent = rememberSaveable { StringBuilder() }
    //Is text is a log?
    var text by rememberSaveable { mutableStateOf("") }
    var hasFlashed by rememberSaveable { mutableStateOf(false) }

    val errorSaveLog = stringResource(R.string.log_error_code)

    LaunchedEffect(pendingFlashIt) {
        Log.d("FlashScreen", "flashing: $flashing")
        if (pendingFlashIt == null || text.isNotEmpty() || hasFlashed) return@LaunchedEffect
        hasFlashed = true
        // No need for an external 'scope' when inside LaunchedEffect
        launch(Dispatchers.IO) {
            val result = flashIt(
                pendingFlashIt!!,
                onStdout = {
                    logContent.append(it).append("\n")
                    if (AnsiFilter.isScreenControl(it)) { // clear command
                        // Switch to the main thread to update state
                        launch(Dispatchers.Main) {
                            text = AnsiFilter.stripAnsi(it) + "\n"
                        }
                    } else {
                        // Switch to the main thread to update state
                        launch(Dispatchers.Main) {
                            text += "$it\n"
                        }
                    }
                },
                onStderr = {
                    logContent.append(it).append("\n")
                })

            // After the background task is done, switch to the main thread to update the final state
            withContext(Dispatchers.Main) {
                var finalLogText = ""
                if (result.code != 0) {
                    finalLogText += errorSaveLog.format(result.code, result.err)
                }
                if (result.showReboot) {
                    finalLogText += "\n\n\n"
                }
                if (finalLogText.isNotEmpty()) {
                    text += finalLogText
                }
                flashing = if (result.code == 0) FlashingStatus.SUCCESS else FlashingStatus.FAILED
            }
        }
    }


    val snackBarHost = LocalSnackbarHost.current
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

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
                onSave = {
                    scope.launch {
                        val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                        val date = format.format(Date())

                        val baseDir = PathHelper.getPath(AxeronApiConstant.folder.PARENT_LOG)
                        if (!baseDir.exists()) {
                            baseDir.mkdirs()
                        }

                        val file = File(baseDir, "AxManager_install_log_${date}.log")

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
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            val reigniteLoading = rememberLoadingDialog()
            if (flashIt is FlashIt.FlashPlugins && (flashing == FlashingStatus.SUCCESS)) {
                ExtendedFloatingActionButton(
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
                ExtendedFloatingActionButton(
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
                ExtendedFloatingActionButton(
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
        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
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

suspend fun uninstallPermanently(
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): AxeronPluginService.FlashResult {
    val cmd = """
        . functions.sh; uninstall_axmanager "${AxeronSettings.getEnableDeveloperOptions()}" "${BuildConfig.APPLICATION_ID}"; exit 0
    """.trimIndent()
    val result =
        AxeronPluginService.execWithIO(cmd, onStdout, onStderr, standAlone = true, useSetsid = true)
    return AxeronPluginService.FlashResult(result)
}

suspend fun resetManager(
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): AxeronPluginService.FlashResult {
    val cmd = """
        . functions.sh; reset_manager "${AxeronSettings.getEnableDeveloperOptions()}"; exit 0
    """.trimIndent()
    val result =
        AxeronPluginService.execWithIO(cmd, onStdout, onStderr, standAlone = true, useSetsid = true)
    return AxeronPluginService.FlashResult(result)
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

        is FlashIt.FlashUninstall -> resetManager(onStdout, onStderr)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    status: FlashingStatus,
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
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