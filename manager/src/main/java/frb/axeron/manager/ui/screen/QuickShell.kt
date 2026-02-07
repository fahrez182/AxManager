package frb.axeron.manager.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DoNotTouch
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fox2code.androidansi.ktx.parseAsAnsiAnnotatedString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.Axeron
import frb.axeron.api.utils.AnsiFilter
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.CheckBoxText
import frb.axeron.manager.ui.component.KeyEventBlocker
import frb.axeron.manager.ui.component.SettingsItem
import frb.axeron.manager.ui.component.SettingsItemExpanded
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.util.PrefsEnumHelper
import frb.axeron.manager.ui.viewmodel.QuickShellViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun QuickShellScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val viewModel: QuickShellViewModel = viewModel()
    val running = viewModel.isRunning

    val listState = rememberLazyListState()
    val logs = remember { mutableStateListOf<QuickShellViewModel.Output>() }

    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    // FAB visibility
    var fabVisible by remember { mutableStateOf(true) }

    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(listState, viewModel.isRunning) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (!viewModel.isRunning) {  // hanya update FAB kalau sedang tidak running
                    if (index > previousIndex || (index == previousIndex && offset > previousScrollOffset)) {
                        fabVisible = false
                    } else if (index < previousIndex || offset < previousScrollOffset) {
                        fabVisible = true
                    }
                }
                previousIndex = index
                previousScrollOffset = offset
            }
    }

    val scope = rememberCoroutineScope()
    val snackBarHost = LocalSnackbarHost.current

    var showExtraDialog by remember { mutableStateOf(false) }

    ExtraSettings(
        showExtraDialog,
        viewModel,
    ) {
        showExtraDialog = false
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.quick_shell),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.stop()
                        },
                        enabled = running,
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                    }
                    IconButton(
                        onClick = {
                            viewModel.clear()
                        },
                        enabled = logs.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.ClearAll, contentDescription = null)
                    }
                    IconButton(
                        onClick = {
                            showExtraDialog = true
                        }
                    ) {
                        Icon(Icons.Outlined.MoreVert, null)
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible && logs.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            ) {
                val context = LocalContext.current
                FloatingActionButton(onClick = {
                    scope.launch {
                        saveLogsToDownload(context, logs, snackBarHost)
                    }
                }) {
                    Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save))
                }
            }
        },
        snackbarHost = { AxSnackBarHost(hostState = snackBarHost) }
    ) { paddingValues ->
        val context = LocalContext.current
        val focusManager = LocalFocusManager.current
        var keyboardVisible by remember { mutableStateOf(false) }
        val keyboardController = LocalSoftwareKeyboardController.current

        KeyEventBlocker {
            val prefs = PrefsEnumHelper<QuickShellViewModel.KeyEventType>("block_")
            when (it.key) {
                Key.VolumeUp -> prefs.loadState(
                    context,
                    QuickShellViewModel.KeyEventType.VOLUME_UP,
                    true
                )

                Key.VolumeDown -> prefs.loadState(
                    context,
                    QuickShellViewModel.KeyEventType.VOLUME_DOWN,
                    true
                )

                else -> false
            }
        }

        KeyboardVisibilityListener(
            onKeyboardState = { visible ->
                keyboardVisible = visible
                if (!visible) {
                    focusManager.clearFocus()
                }
            }
        )

        Box(
            Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {


            LaunchedEffect(viewModel.clear) {
                logs.clear()
            }

            LaunchedEffect(logs.size) {
                if (logs.isNotEmpty()) {
                    listState.animateScrollToItem(logs.lastIndex)
                }
            }

            // collect flow
            LaunchedEffect(viewModel.output) {
                viewModel.output.collect { line ->
                    val raw = line.output

                    if (line.type != QuickShellViewModel.OutputType.TYPE_SPACE && raw.isBlank()) return@collect
                    // ===== DETECT SCREEN MODE =====

                    // ===== SCREEN MODE (top, watch, htop, etc) =====
                    if (AnsiFilter.isScreenControl(raw)) {
                        val clean = AnsiFilter.stripAnsi(raw)

                        if (clean.isEmpty()) return@collect
                        if (logs.isEmpty()) {
                            logs.add(
                                QuickShellViewModel.Output(
                                    type = line.type,
                                    output = clean,
                                    completed = false
                                )
                            )
                        } else {
                            val last = logs.last()
                            logs[logs.lastIndex] = last.copy(output = clean)
                        }
                        return@collect
                    }


                    // selain stdout/stderr → selalu item baru
                    if (line.type != QuickShellViewModel.OutputType.TYPE_STDOUT && line.type != QuickShellViewModel.OutputType.TYPE_STDERR) {
                        logs.add(line.copy(completed = true))
                        return@collect
                    }

                    val hasNewline =
                        raw.contains('\n')

                    val hasCarriageReturn =
                        raw.contains('\r') && !raw.contains('\n')

                    val clean = raw.trimEnd('\n', '\r')

                    val last = logs.lastOrNull()

                    when {

                        /* ===============================
                           CASE 1: CARRIAGE RETURN (\r)
                           overwrite baris terakhir
                           =============================== */
                        hasCarriageReturn && last != null &&
                                !last.completed &&
                                last.type == line.type -> {

                            val i = logs.lastIndex
                            logs[i] = last.copy(
                                output = clean,
                                completed = false
                            )
                        }

                        /* ===============================
                           CASE 2: LANJUT BARIS SEBELUMNYA
                           =============================== */
                        last != null &&
                                !last.completed &&
                                last.type == line.type -> {

                            val i = logs.lastIndex
                            logs[i] = last.copy(
                                output = last.output + clean,
                                completed = hasNewline
                            )
                        }

                        /* ===============================
                           CASE 3: BARIS BARU
                           =============================== */
                        else -> {
                            logs.add(
                                QuickShellViewModel.Output(
                                    type = line.type,
                                    output = clean,
                                    completed = hasNewline
                                )
                            )
                        }
                    }
                }
            }


            val hScroll = rememberScrollState()

            val context = LocalContext.current

            SelectionContainer(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .horizontalScroll(hScroll)
                ) {
                    LazyColumn(
                        state = listState,
                    ) {
                        item {
                            Spacer(modifier = Modifier.size(70.dp))
                        }
                        items(logs) { line ->
                            if (!PrefsEnumHelper<QuickShellViewModel.OutputType>("output_")
                                    .loadState(context, line.type, true)
                            ) return@items
                            BasicText(
                                text = line.output.parseAsAnsiAnnotatedString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    lineHeight = MaterialTheme.typography.labelSmall.fontSize, // samain dengan fontSize
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                ),
                                softWrap = false,
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            @SuppressLint("ConfigurationScreenWidthHeight")
            val screen = LocalConfiguration.current.screenHeightDp
            val paddingHeight = (screen * 0.52).dp


            ElevatedCard(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.elevatedCardColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
//                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .run {
                        if (keyboardVisible) {
                            padding(bottom = paddingHeight)
                        } else {
                            padding(bottom = 0.dp)
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    TextField(
                        value = viewModel.commandText,
                        onValueChange = {
                            viewModel.setCommand(it)
                        },
                        label = {
                            Text(viewModel.execMode)
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = MaterialTheme.typography.bodyLarge.fontSize,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            ),
                            fontFamily = FontFamily.Monospace
                        ),
                        maxLines = if (keyboardVisible) Int.MAX_VALUE else 1,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,   // garis saat fokus
                            unfocusedIndicatorColor = Color.Transparent, // garis saat tidak fokus
                            disabledIndicatorColor = Color.Transparent,   // garis saat disabled
                            focusedContainerColor = Color.Transparent,   // ⬅ ini penting
                            unfocusedContainerColor = Color.Transparent, // ⬅ ini juga
                            disabledContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    easing = LinearOutSlowInEasing
                                )
                            )
                    )

                    IconButton(
                        onClick = {
                            viewModel.runShell()
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_exec),
                            contentDescription = stringResource(R.string.exec),
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun KeyboardVisibilityListener(
    onKeyboardState: (visible: Boolean) -> Unit
) {
    val insets = WindowInsets.ime
    val imeHeight = insets.getBottom(LocalDensity.current)

    val imeVisible = imeHeight > 0
    val prevImeVisible = remember { mutableStateOf(imeVisible) }

    LaunchedEffect(imeVisible) {
        if (imeVisible != prevImeVisible.value) {
            onKeyboardState(imeVisible)
            prevImeVisible.value = imeVisible
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraSettings(
    showDialog: Boolean,
    quickShellViewModel: QuickShellViewModel,
    onDismissRequest: () -> Unit
) {
    if (showDialog) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onDismissRequest = onDismissRequest
        ) {
            val context: Context = LocalContext.current
            val outputOption = QuickShellViewModel.OutputType.entries
            val keyEventOption = QuickShellViewModel.KeyEventType.entries

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val checkedOutputStates = remember {
                        mutableStateMapOf<QuickShellViewModel.OutputType, Boolean>().apply {
                            outputOption.forEach {
                                put(
                                    it,
                                    PrefsEnumHelper<QuickShellViewModel.OutputType>("output_")
                                        .loadState(context, it, true)
                                )
                            }
                        }
                    }

                    SettingsItemExpanded(
                        label = stringResource(R.string.output_filter),
                        description = stringResource(R.string.filter_output_desc),
                        iconVector = Icons.Outlined.Output
                    ) { _, expanded ->
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                outputOption.forEach { type ->
                                    val isChecked = checkedOutputStates[type] ?: false
                                    CheckBoxText(
                                        label = stringResource(type.labelId),
                                        checked = isChecked
                                    ) {
                                        checkedOutputStates[type] = it
                                        PrefsEnumHelper<QuickShellViewModel.OutputType>("output_")
                                            .saveState(context, type, it)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val checkedSaveStates = remember {
                        mutableStateMapOf<QuickShellViewModel.OutputType, Boolean>().apply {
                            outputOption.forEach {
                                put(
                                    it,
                                    PrefsEnumHelper<QuickShellViewModel.OutputType>("save_")
                                        .loadState(context, it, true)
                                )
                            }
                        }
                    }

                    SettingsItemExpanded(
                        label = stringResource(R.string.save_log_filter),
                        description = stringResource(R.string.filter_save_log_desc),
                        iconVector = Icons.Outlined.Save,
                    ) { _, expanded ->
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                outputOption.forEach { type ->
                                    val isChecked = checkedSaveStates[type] ?: false
                                    CheckBoxText(
                                        label = stringResource(type.labelId),
                                        checked = isChecked
                                    ) {
                                        checkedSaveStates[type] = it
                                        PrefsEnumHelper<QuickShellViewModel.OutputType>("save_")
                                            .saveState(context, type, it)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val blockedKeyEventStates = remember {
                        mutableStateMapOf<QuickShellViewModel.KeyEventType, Boolean>().apply {
                            keyEventOption.forEach {
                                put(
                                    it,
                                    PrefsEnumHelper<QuickShellViewModel.KeyEventType>("block_")
                                        .loadState(context, it, true)
                                )
                            }
                        }
                    }

                    SettingsItemExpanded(
                        label = stringResource(R.string.block_key_event),
                        description = stringResource(R.string.block_key_event_desc),
                        iconVector = Icons.Outlined.DoNotTouch,
                    ) { _, expanded ->
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                keyEventOption.forEach { type ->
                                    val isChecked = blockedKeyEventStates[type] ?: false
                                    CheckBoxText(
                                        label = stringResource(type.labelId),
                                        checked = isChecked
                                    ) {
                                        blockedKeyEventStates[type] = it
                                        PrefsEnumHelper<QuickShellViewModel.KeyEventType>("block_")
                                            .saveState(context, type, it)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsItem(
                        iconVector = Icons.Filled.Security,
                        label = stringResource(R.string.shell_restriction),
                        description = stringResource(R.string.shell_restriction_desc),
                        checked = quickShellViewModel.isShellRestrictionEnabled,
                        onSwitchChange = { quickShellViewModel.setShellRestriction(it) }
                    )
                }

                item {
                    SettingsItem(
                        iconVector = Icons.Outlined.Bolt,
                        label = stringResource(R.string.compat_mode),
                        description = stringResource(R.string.compat_mode_desc),
                        checked = quickShellViewModel.isCompatModeEnabled,
                        onSwitchChange = { quickShellViewModel.setCompatMode(it) }
                    )
                }
            }
        }
    }
}


suspend fun saveLogsToDownload(
    context: Context,
    logs: List<QuickShellViewModel.Output>,
    snackbar: SnackbarHostState
) {
    val logSaved = context.getString(R.string.log_saved_to)
    val logFailed = context.getString(R.string.failed_to_save_log)
    if (logs.isEmpty()) return
    val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    val date = format.format(Date())

    val baseDir = PathHelper.getPath(AxeronApiConstant.folder.PARENT_LOG)
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }

    val file = File(baseDir, "QuickShell_log_${date}.log")

    try {
        val fos =
            Axeron.newFileService().getStreamSession(file.absolutePath, true, false).outputStream
        logs.forEach { line ->
            if (!PrefsEnumHelper<QuickShellViewModel.OutputType>("save_")
                    .loadState(context, line.type, true)
            ) return@forEach
            fos.write("${line.output}\n".toByteArray())
        }
        fos.flush()

        snackbar.showSnackbar(logSaved.format(file.absolutePath))
    } catch (e: Exception) {
        snackbar.showSnackbar(logFailed.format(e.message))
    }
}



