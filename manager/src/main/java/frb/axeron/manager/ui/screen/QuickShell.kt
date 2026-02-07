package frb.axeron.manager.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DoNotTouch
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import frb.axeron.manager.ui.component.TerminalControlPanel
import frb.axeron.manager.ui.component.TerminalInputView
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
    val isAdvancedMode = viewModel.isAdvancedMode

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
                if (!viewModel.isRunning) {
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
            AnimatedVisibility(
                visible = !isAdvancedMode,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.quick_shell),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleAdvancedMode() }) {
                            Icon(
                                if (isAdvancedMode) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                                contentDescription = stringResource(R.string.advanced_mode),
                                tint = if (isAdvancedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.stop() }, enabled = running) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                        }
                        IconButton(onClick = { viewModel.clear() }, enabled = logs.isNotEmpty()) {
                            Icon(Icons.Filled.ClearAll, contentDescription = null)
                        }
                        IconButton(onClick = { showExtraDialog = true }) {
                            Icon(Icons.Outlined.MoreVert, null)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isAdvancedMode && fabVisible && logs.isNotEmpty(),
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
                Key.VolumeUp -> prefs.loadState(context, QuickShellViewModel.KeyEventType.VOLUME_UP, true)
                Key.VolumeDown -> prefs.loadState(context, QuickShellViewModel.KeyEventType.VOLUME_DOWN, true)
                else -> false
            }
        }

        KeyboardVisibilityListener { visible ->
            keyboardVisible = visible
            if (!visible && !isAdvancedMode) {
                focusManager.clearFocus()
            }
        }

        Box(
            Modifier
                .padding(if (isAdvancedMode) PaddingValues(0.dp) else paddingValues)
                .fillMaxSize()
        ) {
            LaunchedEffect(viewModel.clear) { logs.clear() }

            LaunchedEffect(logs.size) {
                if (logs.isNotEmpty() && !isAdvancedMode) {
                    listState.animateScrollToItem(logs.lastIndex)
                }
            }

            LaunchedEffect(isAdvancedMode) {
                if (isAdvancedMode) {
                    snackBarHost.showSnackbar("Advanced Mode: ${viewModel.adbStatus}")
                }
            }

            // collect flow for standard logs
            LaunchedEffect(viewModel.output) {
                viewModel.output.collect { line ->
                    if (isAdvancedMode) return@collect

                    val raw = line.output
                    if (line.type != QuickShellViewModel.OutputType.TYPE_SPACE && raw.isBlank()) return@collect

                    if (AnsiFilter.isScreenControl(raw)) {
                        val clean = AnsiFilter.stripAnsi(raw)
                        if (clean.isEmpty()) return@collect
                        if (logs.isEmpty()) {
                            logs.add(QuickShellViewModel.Output(type = line.type, output = clean, completed = false))
                        } else {
                            val last = logs.last()
                            logs[logs.lastIndex] = last.copy(output = clean)
                        }
                        return@collect
                    }

                    if (line.type != QuickShellViewModel.OutputType.TYPE_STDOUT && line.type != QuickShellViewModel.OutputType.TYPE_STDERR) {
                        logs.add(line.copy(completed = true))
                        return@collect
                    }

                    val hasNewline = raw.contains('\n')
                    val hasCarriageReturn = raw.contains('\r') && !raw.contains('\n')
                    val clean = raw.trimEnd('\n', '\r')
                    val last = logs.lastOrNull()

                    when {
                        hasCarriageReturn && last != null && !last.completed && last.type == line.type -> {
                            logs[logs.lastIndex] = last.copy(output = clean, completed = false)
                        }
                        last != null && !last.completed && last.type == line.type -> {
                            logs[logs.lastIndex] = last.copy(output = last.output + clean, completed = hasNewline)
                        }
                        else -> {
                            logs.add(QuickShellViewModel.Output(type = line.type, output = clean, completed = hasNewline))
                        }
                    }
                }
            }

            if (isAdvancedMode) {
                AdvancedTerminalView(viewModel)
            } else {
                StandardLogView(logs, listState)
            }

            // Bottom controls for Standard Mode
            AnimatedVisibility(
                visible = !isAdvancedMode,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                StandardBottomControls(viewModel, keyboardVisible)
            }
        }
    }
}

@Composable
fun StandardLogView(logs: List<QuickShellViewModel.Output>, listState: androidx.compose.foundation.lazy.LazyListState) {
    val hScroll = rememberScrollState()
    val context = LocalContext.current

    SelectionContainer(modifier = Modifier.padding(horizontal = 24.dp)) {
        Box(modifier = Modifier.horizontalScroll(hScroll)) {
            LazyColumn(state = listState) {
                item { Spacer(modifier = Modifier.size(70.dp)) }
                items(logs) { line ->
                    if (!PrefsEnumHelper<QuickShellViewModel.OutputType>("output_").loadState(context, line.type, true)) return@items
                    BasicText(
                        text = line.output.parseAsAnsiAnnotatedString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            lineHeight = MaterialTheme.typography.labelSmall.fontSize,
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
                item { Spacer(modifier = Modifier.size(22.dp)) }
            }
        }
    }
}

@Composable
fun StandardBottomControls(viewModel: QuickShellViewModel, keyboardVisible: Boolean) {
    @SuppressLint("ConfigurationScreenWidthHeight")
    val screen = LocalConfiguration.current.screenHeightDp
    val paddingHeight = (screen * 0.52).dp
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(Modifier.padding(horizontal = 16.dp)) {
        ElevatedCard(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.elevatedCardColors().copy(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier
                .fillMaxWidth()
                .run { if (keyboardVisible) padding(bottom = paddingHeight) else padding(bottom = 0.dp) }
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = viewModel.commandText,
                    onValueChange = { viewModel.setCommand(it) },
                    label = { Text(viewModel.execMode) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = if (keyboardVisible) Int.MAX_VALUE else 1,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                )
                IconButton(
                    onClick = {
                        viewModel.runShell()
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 4.dp)
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
        Spacer(modifier = Modifier.size(16.dp))
    }
}

@Composable
fun AdvancedTerminalView(viewModel: QuickShellViewModel) {
    val emulator = viewModel.terminalEmulator
    val lines = emulator.outputLines
    val cursorRow = emulator.cursorRow
    val cursorCol = emulator.cursorCol

    var terminalInputView by remember { mutableStateOf<TerminalInputView?>(null) }
    val scrollState = rememberScrollState()

    LaunchedEffect(terminalInputView) {
        terminalInputView?.requestTerminalFocus()
    }

    // Auto-scroll to bottom when content changes
    LaunchedEffect(emulator.revision, cursorRow) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ADB: ${viewModel.adbStatus}",
                color = if (viewModel.adbStatus == "Connected") Color.Green else Color.Red,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = { viewModel.toggleAdvancedMode() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Filled.Terminal, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    terminalInputView?.requestTerminalFocus()
                }
        ) {
            val density = LocalDensity.current
            val charWidth = with(density) { 7.2.sp.toDp() } // Approximate for 12sp monospace
            val charHeight = with(density) { 14.sp.toDp() }

            val cols = (maxWidth / charWidth).toInt().coerceAtLeast(10)
            val rows = (maxHeight / charHeight).toInt().coerceAtLeast(10)

            LaunchedEffect(cols, rows) {
                viewModel.terminalEmulator.resize(rows, cols)
            }

            // Real input view instead of hidden TextField
            AndroidView(
                factory = { ctx ->
                    TerminalInputView(ctx).apply {
                        onTextInput = { text -> viewModel.sendInput(text) }
                        onActionKey = { keyCode ->
                            when (keyCode) {
                                android.view.KeyEvent.KEYCODE_ENTER -> viewModel.sendInput("\n")
                                android.view.KeyEvent.KEYCODE_DEL -> viewModel.sendRaw(byteArrayOf(0x7f))
                                android.view.KeyEvent.KEYCODE_FORWARD_DEL -> viewModel.sendInput("\u001b[3~")
                                android.view.KeyEvent.KEYCODE_TAB -> viewModel.sendInput("\t")
                                android.view.KeyEvent.KEYCODE_DPAD_UP -> viewModel.sendInput("\u001b[A")
                                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> viewModel.sendInput("\u001b[B")
                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> viewModel.sendInput("\u001b[D")
                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> viewModel.sendInput("\u001b[C")
                                android.view.KeyEvent.KEYCODE_MOVE_HOME -> viewModel.sendInput("\u001b[H")
                                android.view.KeyEvent.KEYCODE_MOVE_END -> viewModel.sendInput("\u001b[F")
                                android.view.KeyEvent.KEYCODE_PAGE_UP -> viewModel.sendInput("\u001b[5~")
                                android.view.KeyEvent.KEYCODE_PAGE_DOWN -> viewModel.sendInput("\u001b[6~")
                                android.view.KeyEvent.KEYCODE_ESCAPE -> viewModel.sendInput("\u001b")
                            }
                        }
                        terminalInputView = this
                    }
                },
                update = {
                    // Focus is handled by LaunchedEffect and click listener
                },
                modifier = Modifier.size(1.dp).alpha(0f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(4.dp)
            ) {
                lines.forEachIndexed { index, line ->
                    TerminalLine(
                        line = line,
                        isCursorLine = index == cursorRow,
                        cursorCol = cursorCol
                    )
                }
            }
        }

        // Control Panel
        TerminalControlPanel(
            onKeyPress = { key ->
                viewModel.sendSpecialKey(key)
            },
            onHistoryNavigate = { up ->
                // History navigate via special keys or handled in ViewModel
            },
            isCtrlPressed = viewModel.isCtrlPressed,
            isAltPressed = viewModel.isAltPressed,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        )
    }
}

@Composable
fun TerminalLine(line: AnnotatedString, isCursorLine: Boolean, cursorCol: Int) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box {
        BasicText(
            text = line,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE5E5E5),
                fontSize = 12.sp,
                lineHeight = 14.sp
            ),
            onTextLayout = { textLayoutResult = it }
        )
        if (isCursorLine) {
            val cursorOffset = if (textLayoutResult != null) {
                if (cursorCol in 0 until line.length) {
                    textLayoutResult?.getHorizontalPosition(cursorCol, true) ?: 0f
                } else {
                    textLayoutResult?.getLineRight(0) ?: 0f
                }
            } else 0f

            val density = LocalDensity.current
            BlinkingCursor(
                offset = with(density) { cursorOffset.toDp() },
                height = with(density) { 14.sp.toDp() }
            )
        }
    }
}

@Composable
fun BlinkingCursor(offset: Dp, height: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        Modifier
            .padding(start = offset)
            .width(8.dp)
            .height(height)
            .background(Color.White.copy(alpha = alpha))
    )
}

@Composable
fun KeyboardVisibilityListener(onKeyboardState: (visible: Boolean) -> Unit) {
    val insets = WindowInsets.ime
    val density = LocalDensity.current
    val imeHeight = insets.getBottom(density)
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
fun ExtraSettings(showDialog: Boolean, quickShellViewModel: QuickShellViewModel, onDismissRequest: () -> Unit) {
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
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val checkedOutputStates = remember {
                        mutableStateMapOf<QuickShellViewModel.OutputType, Boolean>().apply {
                            outputOption.forEach {
                                put(it, PrefsEnumHelper<QuickShellViewModel.OutputType>("output_").loadState(context, it, true))
                            }
                        }
                    }
                    SettingsItemExpanded(
                        label = stringResource(R.string.output_filter),
                        description = stringResource(R.string.filter_output_desc),
                        iconVector = Icons.Outlined.Output
                    ) { _, expanded ->
                        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                outputOption.forEach { type ->
                                    val isChecked = checkedOutputStates[type] ?: false
                                    CheckBoxText(label = stringResource(type.labelId), checked = isChecked) {
                                        checkedOutputStates[type] = it
                                        PrefsEnumHelper<QuickShellViewModel.OutputType>("output_").saveState(context, type, it)
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
                                put(it, PrefsEnumHelper<QuickShellViewModel.OutputType>("save_").loadState(context, it, true))
                            }
                        }
                    }
                    SettingsItemExpanded(
                        label = stringResource(R.string.save_log_filter),
                        description = stringResource(R.string.filter_save_log_desc),
                        iconVector = Icons.Outlined.Save,
                    ) { _, expanded ->
                        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                outputOption.forEach { type ->
                                    val isChecked = checkedSaveStates[type] ?: false
                                    CheckBoxText(label = stringResource(type.labelId), checked = isChecked) {
                                        checkedSaveStates[type] = it
                                        PrefsEnumHelper<QuickShellViewModel.OutputType>("save_").saveState(context, type, it)
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
                                put(it, PrefsEnumHelper<QuickShellViewModel.KeyEventType>("block_").loadState(context, it, true))
                            }
                        }
                    }
                    SettingsItemExpanded(
                        label = stringResource(R.string.block_key_event),
                        description = stringResource(R.string.block_key_event_desc),
                        iconVector = Icons.Outlined.DoNotTouch,
                    ) { _, expanded ->
                        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                keyEventOption.forEach { type ->
                                    val isChecked = blockedKeyEventStates[type] ?: false
                                    CheckBoxText(label = stringResource(type.labelId), checked = isChecked) {
                                        blockedKeyEventStates[type] = it
                                        PrefsEnumHelper<QuickShellViewModel.KeyEventType>("block_").saveState(context, type, it)
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

suspend fun saveLogsToDownload(context: Context, logs: List<QuickShellViewModel.Output>, snackbar: SnackbarHostState) {
    val logSaved = context.getString(R.string.log_saved_to)
    val logFailed = context.getString(R.string.failed_to_save_log)
    if (logs.isEmpty()) return
    val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    val date = format.format(Date())
    val baseDir = PathHelper.getPath(AxeronApiConstant.folder.PARENT_LOG)
    if (!baseDir.exists()) baseDir.mkdirs()
    val file = File(baseDir, "QuickShell_log_${date}.log")
    try {
        val fos = Axeron.newFileService().getStreamSession(file.absolutePath, true, false).outputStream
        logs.forEach { line ->
            if (!PrefsEnumHelper<QuickShellViewModel.OutputType>("save_").loadState(context, line.type, true)) return@forEach
            fos.write("${line.output}\n".toByteArray())
        }
        fos.flush()
        snackbar.showSnackbar(logSaved.format(file.absolutePath))
    } catch (e: Exception) {
        snackbar.showSnackbar(logFailed.format(e.message))
    }
}
