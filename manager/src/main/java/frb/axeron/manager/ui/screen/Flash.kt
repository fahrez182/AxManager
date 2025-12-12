package frb.axeron.manager.ui.screen

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import frb.axeron.api.AxeronPluginService.flashPlugin
import frb.axeron.api.core.Engine.Companion.application
import frb.axeron.api.utils.PathHelper
import frb.axeron.data.AxeronConstant
import frb.axeron.manager.BuildConfig
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.KeyEventBlocker
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.theme.GREEN
import frb.axeron.manager.ui.theme.ORANGE
import frb.axeron.manager.ui.theme.RED
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FlashingStatus {
    FLASHING,
    SUCCESS,
    FAILED
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Uri.getFileName(context: Context): String {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(this, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex != -1) {
            it.getString(nameIndex)
        } else {
            this.lastPathSegment ?: "unknown.zip"
        }
    } ?: (this.lastPathSegment ?: "unknown.zip")
}

@Parcelize
sealed class FlashIt : Parcelable {
    data class FlashPlugins(val uris: List<Uri>) : FlashIt()

    data object FlashUninstall : FlashIt()
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

    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)

    var flashing by rememberSaveable {
        mutableStateOf(FlashingStatus.FLASHING)
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

    val confirmDialog = rememberConfirmDialog()
    var confirmed by rememberSaveable { mutableStateOf(flashIt !is FlashIt.FlashPlugins) }
    var pendingFlashIt by rememberSaveable { mutableStateOf<FlashIt?>(null) }

    LaunchedEffect(key1 = flashIt) {
        if (flashIt is FlashIt.FlashPlugins && !confirmed) {
            val uris = flashIt.uris
            val moduleNames =
                uris.mapIndexed { index, uri -> "\n${index + 1}. ${uri.getFileName(context)}" }
                    .joinToString("")
            val confirmContent =
                String.format("The following modules will be installed: %s", moduleNames)
            val confirmTitle = "Install Plugin?"
            val result = confirmDialog.awaitConfirm(
                title = confirmTitle,
                content = confirmContent,
                markdown = true,
                confirm = "Install",
                dismiss = "Cancel"
            )
            if (result == ConfirmResult.Confirmed) {
                confirmed = true
                pendingFlashIt = flashIt
            } else {
                // User cancelled, go back
                navigator.popBackStack()
                if (finishIntent) activity?.finish()
            }
        } else {
            confirmed = true
            pendingFlashIt = flashIt
        }
    }

    var tempText: String
    val logContent = rememberSaveable { StringBuilder() }
    //Is text is a log?
    var text by rememberSaveable { mutableStateOf("") }
    var hasFlashed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(confirmed, pendingFlashIt) {
        if (!confirmed || pendingFlashIt == null || text.isNotEmpty() || hasFlashed) return@LaunchedEffect
        hasFlashed = true
        withContext(Dispatchers.IO) {
            flashIt(
                pendingFlashIt!!,
                onStdout = {
                    tempText = "$it\n"
                    if (tempText.startsWith("[H[J")) { // clear command
                        text = tempText.substring(6)
                    } else {
                        text += tempText
                    }
                    logContent.append(it).append("\n")
                },
                onStderr = {
                    logContent.append(it).append("\n")
                }).apply {
                if (code != 0) {
                    text += "Error code: $code.\n $err Please save and check the log.\n"
                }
                if (showReboot) {
                    text += "\n\n\n"
                }
                flashing = if (code == 0) FlashingStatus.SUCCESS else FlashingStatus.FAILED
            }
        }
    }

    val scope = rememberCoroutineScope()
    val snackBarHost = LocalSnackbarHost.current
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
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

                        val baseDir = PathHelper.getPath(AxeronConstant.folder.PARENT_LOG)
                        if (!baseDir.exists()) {
                            baseDir.mkdirs()
                        }

                        val file = File(baseDir, "AxManager_install_log_${date}.log")

                        try {
                            val fos = Axeron.newFileService().getStreamSession(file.absolutePath, true, false).outputStream
                            fos.write("$logContent\n".toByteArray())
                            fos.flush()

                            snackBarHost.showSnackbar("Log saved to ${file.absolutePath}")
                        } catch (e: Exception) {
                            snackBarHost.showSnackbar("Failed to save logs: ${e.message}")
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            val reigniteLoading = rememberLoadingDialog()
            if (flashIt is FlashIt.FlashPlugins && (flashing == FlashingStatus.SUCCESS)) {
                // Reboot button for modules flashing
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
                    text = { Text(text = "Restart & Close") }
                )
            }

            if (flashIt is FlashIt.FlashPlugins && (flashing == FlashingStatus.FAILED)) {
                // Close button for modules flashing
                ExtendedFloatingActionButton(
                    text = { Text(text = "Close") },
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
                softWrap = true,   // MATIIN WRAP
            )
//            Text(
//                modifier = Modifier.padding(8.dp),
//                text = if (developerOptionsEnabled) logContent.toString() else text,
//                fontSize = MaterialTheme.typography.bodySmall.fontSize,
//                fontFamily = FontFamily.Monospace,
//                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
//            )
        }
    }

}

suspend fun flashModulesSequentially(
    uris: List<Uri>,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): AxeronPluginService.FlashResult {
    for (uri in uris) {
        flashPlugin(uri, onStdout, onStderr).apply {
            if (code != 0) {
                return AxeronPluginService.FlashResult(code, err, showReboot)
            }
        }
    }
    return AxeronPluginService.FlashResult(0, "", true)
}

suspend fun uninstallPermanently(
    onStdout: (String) -> Unit, onStderr: (String) -> Unit
): AxeronPluginService.FlashResult {
    val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val cmd = ". functions.sh; uninstall_axmanager \"${prefs.getBoolean("enable_developer_options", false)}\" \"${BuildConfig.APPLICATION_ID}\"; exit 0"
    val result = AxeronPluginService.execWithIO(cmd, onStdout, onStderr, standAlone = true, useSetsid = true)
    return AxeronPluginService.FlashResult(result)
}

suspend fun flashIt(
    flashIt: FlashIt,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): AxeronPluginService.FlashResult {
    return when (flashIt) {
        is FlashIt.FlashPlugins -> {
            flashModulesSequentially(flashIt.uris, onStdout, onStderr)
        }
        is FlashIt.FlashUninstall -> uninstallPermanently(onStdout, onStderr)
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
                    FlashingStatus.FLASHING -> "Flashing"
                    FlashingStatus.SUCCESS -> "Success"
                    FlashingStatus.FAILED -> "Failed"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = when (status) {
                    FlashingStatus.FLASHING -> ORANGE
                    FlashingStatus.SUCCESS -> GREEN
                    FlashingStatus.FAILED -> RED
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