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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DoNotTouch
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import frb.axeron.manager.ui.component.AxTopAppBar
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
import frb.axeron.manager.data.AxshLauncher
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.component.FloatingBarFab
import frb.axeron.manager.ui.component.TerminalOutput
import frb.axeron.manager.ui.component.TerminalOptionsCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import frb.axeron.manager.ui.util.TerminalBuffer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.derivedStateOf
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.CheckBoxText
import frb.axeron.manager.ui.component.KeyEventBlocker
import frb.axeron.manager.ui.component.SettingsItem
import frb.axeron.manager.ui.component.SettingsItemExpanded
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.util.PrefsEnumHelper
import frb.axeron.manager.ui.viewmodel.QuickShellViewModel
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import frb.axeron.manager.data.CommandBookmarks
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Checkbox
import androidx.compose.ui.text.style.TextAlign
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.OptionsSectionHeader
import frb.axeron.manager.ui.component.SettingsRow
import androidx.compose.foundation.clickable
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.scaleDp
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
    val hasLogs by remember { derivedStateOf { !viewModel.terminal.isEmpty } }

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
            AxTopAppBar(
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
                        enabled = hasLogs
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
            // Rendered beside the nav pill; only registered once there is a log to save.
            if (hasLogs) {
                val context = LocalContext.current
                FloatingBarFab(visible = fabVisible) {
                    FloatingActionButton(shape = CircleShape, onClick = {
                        scope.launch {
                            saveLogsToDownload(context, viewModel.terminal, snackBarHost)
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save))
                    }
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


            // Visible kinds come from the output filter; re-read when the settings sheet closes.
            val visibleTypes = remember(showExtraDialog) {
                val prefs = PrefsEnumHelper<QuickShellViewModel.OutputType>("output_")
                QuickShellViewModel.OutputType.entries
                    .filter { prefs.loadState(context, it, true) }
                    .toSet()
            }
            val filter = remember(visibleTypes) {
                if (visibleTypes.size == QuickShellViewModel.OutputType.entries.size) null
                else { type: QuickShellViewModel.OutputType -> type in visibleTypes }
            }
            val floatingInset = LocalFloatingBarInset.current

            TerminalOutput(
                buffer = viewModel.terminal,
                state = listState,
                wrap = false,
                filter = filter,
                color = { it.color() },
                // Top clears the command card; bottom clears the floating bar.
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 70.dp,
                    bottom = 22.dp + floatingInset
                ),
                jumpButtonPadding = PaddingValues(bottom = 12.dp + floatingInset),
                modifier = Modifier.fillMaxSize()
            )

            @SuppressLint("ConfigurationScreenWidthHeight")
            val screen = LocalConfiguration.current.screenHeightDp
            val paddingHeight = (screen * 0.52).dp


            val savedText = stringResource(R.string.quickshell_bookmark_saved)
            var showBookmarks by remember { mutableStateOf(false) }
            if (showBookmarks) {
                BookmarksSheet(
                    onInsert = { command ->
                        showBookmarks = false
                        viewModel.insertCommand(command)
                    },
                    onRun = { command ->
                        showBookmarks = false
                        viewModel.setCommand(command)
                        viewModel.runShell()
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    onDismissRequest = { showBookmarks = false }
                )
            }

            // Like a terminal: "#" when the server runs as root, "$" as shell.
            val prompt = remember {
                val root = runCatching { Axeron.pingBinder() && Axeron.getAxeronInfo().isRoot() }
                    .getOrDefault(false)
                if (root) "#" else "$"
            }
            CommandInput(
                state = viewModel.commandInput,
                prompt = prompt,
                expanded = keyboardVisible,
                onRun = {
                    viewModel.runShell()
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
                onSaveBookmark = { text ->
                    CommandBookmarks.add(text)
                    Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
                },
                onOpenBookmarks = { showBookmarks = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (keyboardVisible) paddingHeight else 0.dp)
            )

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


/**
 * QuickShell's settings: snippets, which output is shown and which is saved (one table),
 * blocked keys, terminal view options and shell behaviour.
 */
@Composable
fun ExtraSettings(
    showDialog: Boolean,
    quickShellViewModel: QuickShellViewModel,
    onDismissRequest: () -> Unit
) {
    if (!showDialog) return
    val context: Context = LocalContext.current

    var showSnippets by remember { mutableStateOf(false) }
    var addingSnippet by remember { mutableStateOf(false) }
    if (showSnippets) {
        BookmarksSheet(
            onInsert = { command ->
                showSnippets = false
                quickShellViewModel.insertCommand(command)
                onDismissRequest()
            },
            onRun = { command ->
                showSnippets = false
                quickShellViewModel.setCommand(command)
                quickShellViewModel.runShell()
                onDismissRequest()
            },
            onDismissRequest = { showSnippets = false }
        )
    }
    if (addingSnippet) {
        AddSnippetSheet(onDismissRequest = { addingSnippet = false })
    }

    AxOptionsSheet(
        title = stringResource(R.string.quickshell_settings),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- snippets ----
            AxCard {
                SettingsRow(
                    iconVector = Icons.Outlined.BookmarkBorder,
                    label = stringResource(R.string.quickshell_bookmarks),
                    description = stringResource(R.string.quickshell_snippets_count, CommandBookmarks.items.size),
                    onClick = { showSnippets = true }
                )
                SettingsRow(
                    iconVector = Icons.Outlined.Add,
                    label = stringResource(R.string.quickshell_add_snippet),
                    description = stringResource(R.string.quickshell_add_snippet_desc),
                    onClick = { addingSnippet = true },
                    showDivider = true
                )
            }

            // ---- output: shown on screen / kept in saved logs, per kind ----
            OptionsSectionHeader(stringResource(R.string.quickshell_output_section))
            OutputFilterTable(context)

            // ---- keys ----
            OptionsSectionHeader(stringResource(R.string.block_key_event))
            AxCard {
                val prefs = remember { PrefsEnumHelper<QuickShellViewModel.KeyEventType>("block_") }
                val blocked = remember {
                    mutableStateMapOf<QuickShellViewModel.KeyEventType, Boolean>().apply {
                        QuickShellViewModel.KeyEventType.entries.forEach { put(it, prefs.loadState(context, it, true)) }
                    }
                }
                // Checkboxes side by side: a short list of keys, each simply blocked or not.
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    QuickShellViewModel.KeyEventType.entries.forEach { type ->
                        val checked = blocked[type] == true
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            // No clip: a rounded shape here cuts off the checkbox's own highlight.
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    blocked[type] = !checked
                                    prefs.saveState(context, type, !checked)
                                }
                                .padding(end = 8.dp)
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = stringResource(type.labelId),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // ---- terminal view ----
            OptionsSectionHeader(stringResource(R.string.terminal_view_options))
            TerminalOptionsCard()

            // ---- shell ----
            OptionsSectionHeader(stringResource(R.string.quickshell_shell_section))
            AxCard {
                SettingsRow(
                    iconVector = Icons.Filled.Security,
                    label = stringResource(R.string.shell_restriction),
                    description = stringResource(R.string.shell_restriction_desc),
                    checked = quickShellViewModel.isShellRestrictionEnabled,
                    onSwitchChange = { quickShellViewModel.setShellRestriction(it) }
                )
                SettingsRow(
                    iconVector = Icons.Outlined.Bolt,
                    label = stringResource(R.string.compat_mode),
                    description = stringResource(R.string.compat_mode_desc),
                    checked = quickShellViewModel.isCompatModeEnabled,
                    onSwitchChange = { quickShellViewModel.setCompatMode(it) },
                    showDivider = true
                )
            }
            RunInAnyTerminalCard()
        }
    }
}

/**
 * How to reach this shell from another terminal: the /sdcard/axeron/axsh launcher, run with sh or
 * installed once into Termux. Each copy first makes sure the launcher matches this install.
 */
@Composable
private fun RunInAnyTerminalCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.copied)
    val failed = stringResource(R.string.run_in_any_terminal_failed, AxshLauncher.PATH)
    val binary = remember { AxshLauncher.binaryPath(context) }

    fun copy(command: String) {
        scope.launch {
            // The launcher may still carry the path of an older install: rewrite it first.
            val ready = withContext(Dispatchers.IO) { AxshLauncher.ensure(context) }
            if (!ready) {
                Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            // Android 13+ shows its own clipboard confirmation.
            if (ClipboardUtil.put(context, command) && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AxCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(imageVector = Icons.Filled.Terminal, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.run_in_any_terminal),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.run_in_any_terminal_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = AxPanelShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column {
                    CommandLine(
                        label = stringResource(R.string.run_in_any_terminal_run),
                        command = AxshLauncher.RUN_COMMAND,
                        onCopy = ::copy
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                    CommandLine(
                        label = stringResource(R.string.run_in_any_terminal_install),
                        command = AxshLauncher.TERMUX_INSTALL_COMMAND,
                        onCopy = ::copy
                    )
                }
            }

            // Keeping axsh ready is per app, next to its Axeron permission.
            Text(
                text = stringResource(R.string.run_in_any_terminal_keep_alive_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // The raw binary, for whoever wants it; it changes with every update.
            SelectionContainer {
                Text(
                    text = binary,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/** One command in the inner card: what it is for, the command itself, tap to copy. */
@Composable
private fun CommandLine(label: String, command: String, onCopy: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy(command) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = command,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Outlined.ContentCopy,
            contentDescription = stringResource(R.string.copy),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * One row per output kind, two checkboxes: shown on screen, and kept when the log is saved.
 * Same preferences as the two filters it replaces ("output_", "save_"), so choices carry over.
 */
@Composable
private fun OutputFilterTable(context: Context) {
    val showPrefs = remember { PrefsEnumHelper<QuickShellViewModel.OutputType>("output_") }
    val savePrefs = remember { PrefsEnumHelper<QuickShellViewModel.OutputType>("save_") }
    val types = QuickShellViewModel.OutputType.entries
    val shown = remember {
        mutableStateMapOf<QuickShellViewModel.OutputType, Boolean>().apply {
            types.forEach { put(it, showPrefs.loadState(context, it, true)) }
        }
    }
    val saved = remember {
        mutableStateMapOf<QuickShellViewModel.OutputType, Boolean>().apply {
            types.forEach { put(it, savePrefs.loadState(context, it, true)) }
        }
    }
    val column = 60.dp

    AxCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.quickshell_output_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            listOf(R.string.quickshell_show, R.string.quickshell_save_column).forEach {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(column)
                )
            }
        }
        // Compact: no 48dp touch reserve per checkbox; each cell is its own tap target instead.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            types.forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                ) {
                    Text(
                        text = stringResource(type.labelId),
                        style = MaterialTheme.typography.bodySmall,
                        color = type.color(),
                        modifier = Modifier.weight(1f)
                    )
                    CompactCheckCell(checked = shown[type] == true, width = column) {
                        shown[type] = it
                        showPrefs.saveState(context, type, it)
                    }
                    CompactCheckCell(checked = saved[type] == true, width = column) {
                        saved[type] = it
                        savePrefs.saveState(context, type, it)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}


/** A checkbox in a fixed-width table cell; the whole cell toggles it. */
@Composable
private fun CompactCheckCell(checked: Boolean, width: Dp, onChange: (Boolean) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(width)
            .clip(AxPanelShape)
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun QuickShellViewModel.OutputType.color(): Color = when (this) {
    QuickShellViewModel.OutputType.TYPE_COMMAND -> MaterialTheme.colorScheme.primary
    QuickShellViewModel.OutputType.TYPE_STDIN -> MaterialTheme.colorScheme.tertiary
    QuickShellViewModel.OutputType.TYPE_STDERR,
    QuickShellViewModel.OutputType.TYPE_THROW -> MaterialTheme.colorScheme.error
    QuickShellViewModel.OutputType.TYPE_START,
    QuickShellViewModel.OutputType.TYPE_EXIT -> MaterialTheme.colorScheme.outline
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

suspend fun saveLogsToDownload(
    context: Context,
    terminal: TerminalBuffer<QuickShellViewModel.OutputType>,
    snackbar: SnackbarHostState
) {
    val logSaved = context.getString(R.string.log_saved_to)
    val logFailed = context.getString(R.string.failed_to_save_log)
    if (terminal.isEmpty) return
    val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    val date = format.format(Date())

    val baseDir = PathHelper.getPath(AxeronApiConstant.folder.PARENT_LOG)
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }

    val file = File(baseDir, "QuickShell_log_${date}.log")

    try {
        val prefs = PrefsEnumHelper<QuickShellViewModel.OutputType>("save_")
        val saved = QuickShellViewModel.OutputType.entries
            .filter { prefs.loadState(context, it, true) }
            .toSet()
        // Streamed line by line off the main thread: unlimited output can exceed the heap.
        withContext(Dispatchers.IO) {
            val session = checkNotNull(
                checkNotNull(Axeron.newFileService()) { "Server isn't running" }
                    .getStreamSession(file.absolutePath, true, false)
            ) { "Can't write ${file.absolutePath}" }
            session.outputStream.use { terminal.writeTo(it) { type -> type in saved } }
        }

        snackbar.showSnackbar(logSaved.format(file.absolutePath))
    } catch (e: Exception) {
        snackbar.showSnackbar(logFailed.format(e.message))
    }
}



