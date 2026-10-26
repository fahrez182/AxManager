package frb.axeron.manager.ui.screen

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.viewmodel.ProcessManagerViewModel.Category
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import frb.axeron.manager.R
import frb.axeron.manager.data.ProcessControl.Outcome
import frb.axeron.manager.data.ProcessControl.Signal
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxCardShape
import frb.axeron.manager.ui.component.AxDropdownDivider
import frb.axeron.manager.ui.component.AxDropdownHeader
import frb.axeron.manager.ui.component.AxDropdownItem
import frb.axeron.manager.ui.component.AxDropdownMenu
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.AxPullToRefreshBox
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.EmptyState
import frb.axeron.manager.ui.component.OptionsSectionHeader
import frb.axeron.manager.ui.component.SearchAppBar
import frb.axeron.manager.ui.component.StatusPill
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.viewmodel.ProcessManagerViewModel
import frb.axeron.manager.ui.viewmodel.ProcessManagerViewModel.Filter
import frb.axeron.manager.ui.viewmodel.ProcessManagerViewModel.Row
import frb.axeron.manager.ui.viewmodel.ProcessManagerViewModel.SortBy
import kotlinx.coroutines.launch

/** Tree indent stops growing past this depth, so deep chains keep room for their text. */
private const val MAX_INDENT_DEPTH = 6


@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ProcessManagerScreen() {
    val viewModel: ProcessManagerViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val confirmDialog = rememberConfirmDialog()
    val listState = rememberLazyListState()

    var live by rememberSaveable { mutableStateOf(true) }
    var showOptions by remember { mutableStateOf(false) }
    var openPid by remember { mutableStateOf<Int?>(null) }

    // Live refresh only while the screen is in front: a background ps every few seconds is waste.
    LifecycleResumeEffect(live) {
        if (live) viewModel.startAutoRefresh() else viewModel.refresh()
        onPauseOrDispose { viewModel.stopAutoRefresh() }
    }

    val confirmTitle = stringResource(R.string.process_signal_confirm)
    val confirmLabel = stringResource(R.string.process_send)
    val cancelLabel = stringResource(R.string.cancel)
    val sentFormat = stringResource(R.string.process_signal_sent)

    /** Sends [signal] to [row] (or its group); TERM, KILL and group sends ask first. */
    fun send(row: Row, signal: Signal, group: Boolean) {
        scope.launch {
            val target = if (group) "PGID ${row.proc.pgid}" else "PID ${row.pid}"
            if (group || signal == Signal.TERM || signal == Signal.KILL) {
                val result = confirmDialog.awaitConfirm(
                    confirmTitle,
                    content = "SIG${signal.name} → $target (${row.title})",
                    confirm = confirmLabel,
                    dismiss = cancelLabel
                )
                if (result != ConfirmResult.Confirmed) return@launch
            }
            val outcome = if (group) viewModel.signalGroup(row.proc.pgid, signal)
            else viewModel.signal(row.pid, signal)
            val text = if (outcome.ok) sentFormat.format("SIG${signal.name}", target) else outcome.text
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    if (showOptions) {
        ProcessOptionsSheet(viewModel = viewModel, onDismiss = { showOptions = false })
    }

    openPid?.let { pid ->
        // Follows the live list; once the process is gone the last row seen stays shown.
        var last by remember(pid) { mutableStateOf<Row?>(null) }
        val row = viewModel.rows.firstOrNull { it.pid == pid }?.also { last = it } ?: last
        if (row != null) {
            ProcessDetailSheet(
                row = row,
                gone = viewModel.rows.none { it.pid == pid },
                viewModel = viewModel,
                onSignal = { signal, group -> send(row, signal, group) },
                onDismiss = { openPid = null }
            )
        } else {
            LaunchedEffect(Unit) { openPid = null }
        }
    }

    Scaffold(
        topBar = {
            SearchAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.process_manager),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                searchLabel = stringResource(R.string.process_search),
                searchText = viewModel.query,
                onSearchTextChange = { viewModel.query = it },
                onClearClick = { viewModel.query = "" },
                action = {
                    IconButton(onClick = { viewModel.treeMode = !viewModel.treeMode }) {
                        Icon(
                            Icons.Outlined.AccountTree,
                            contentDescription = stringResource(R.string.process_tree),
                            tint = if (viewModel.treeMode) MaterialTheme.colorScheme.primary
                            else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { live = !live }) {
                        Icon(
                            if (live) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(
                                if (live) R.string.process_pause else R.string.process_resume
                            )
                        )
                    }
                    IconButton(onClick = { showOptions = true }) {
                        Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.process_options))
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            CategoryTabs(viewModel = viewModel)
            if (viewModel.filter.isActive) {
                ActiveFilterBar(
                    filter = viewModel.filter,
                    onEdit = { showOptions = true },
                    onClear = { viewModel.filter = Filter() }
                )
            }

            val shown = viewModel.shown
            val floatingInset = LocalFloatingBarInset.current

            AxPullToRefreshBox(
                isRefreshing = viewModel.isLoading && viewModel.rows.isNotEmpty(),
                onRefresh = { viewModel.refresh() },
                topInset = 0.dp
            ) {
                if (viewModel.rows.isEmpty() && viewModel.error == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@AxPullToRefreshBox
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = floatingInset + 16.dp
                    )
                ) {
                    viewModel.error?.let { error ->
                        item(key = "error", contentType = "error") { ErrorPanel(error) }
                    }
                    if (viewModel.rows.isNotEmpty()) {
                        item(key = "count", contentType = "count") {
                            Text(
                                text = stringResource(R.string.process_count, shown.size, viewModel.rows.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                    if (shown.isEmpty() && viewModel.rows.isNotEmpty()) {
                        item(key = "empty", contentType = "empty") {
                            EmptyState(
                                icon = Icons.Outlined.SearchOff,
                                title = stringResource(R.string.process_empty),
                                subtitle = stringResource(R.string.process_empty_desc)
                            )
                        }
                    }
                    items(shown, key = { it.pid }, contentType = { "process" }) { row ->
                        ProcessRow(
                            row = row,
                            onClick = { openPid = row.pid },
                            onFilter = { viewModel.filter = it },
                            onSignal = { signal, group -> send(row, signal, group) }
                        )
                    }
                }
            }
        }
    }
}

/** One tab per [Category], as the Settings Editor's source tabs: one selected, each with its count. */
@Composable
private fun CategoryTabs(viewModel: ProcessManagerViewModel) {
    val rowState = rememberLazyListState()
    val selected = viewModel.category
    LaunchedEffect(selected) {
        // Keep the selected tab, and a bit of its left neighbour, on screen.
        rowState.animateScrollToItem((selected.ordinal - 1).coerceAtLeast(0))
    }

    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(Category.entries, key = { it.name }) { category ->
            val isSelected = category == selected
            val container by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "categoryTab"
            )
            val content = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                onClick = { viewModel.category = category },
                shape = CircleShape,
                color = container
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(categoryLabel(category)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = content
                    )
                    if (viewModel.rows.isNotEmpty()) {
                        val n = viewModel.count(category)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = n.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = content.copy(alpha = if (n > 0) 0.7f else 0.4f)
                        )
                    }
                }
            }
        }
    }
}

/** What the filter holds (set from the sheet or a row's menu), with edit and clear. */
@Composable
private fun ActiveFilterBar(filter: Filter, onEdit: () -> Unit, onClear: () -> Unit) {
    val summary = buildList {
        filter.pid?.let { add("PID $it") }
        filter.ppid?.let { add("PPID $it") }
        filter.pgid?.let { add("PGID $it") }
        filter.uid?.let { add("UID $it") }
        filter.minRss?.let { add("RSS ≥ ${formatKib(it)}") }
        filter.maxRss?.let { add("RSS ≤ ${formatKib(it)}") }
        if (filter.states.isNotEmpty()) add("State " + filter.states.sorted().joinToString("/"))
    }.joinToString(" · ")

    Surface(
        onClick = onEdit,
        shape = AxPanelShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.FilterAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.process_filter_clear),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ErrorPanel(text: String) {
    Surface(shape = AxPanelShape, color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProcessRow(
    row: Row,
    onClick: () -> Unit,
    onFilter: (Filter) -> Unit,
    onSignal: (Signal, Boolean) -> Unit
) {
    val proc = row.proc
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }

    Box(Modifier.padding(start = (12 * row.depth.coerceAtMost(MAX_INDENT_DEPTH)).dp)) {
        AxCard(
            modifier = Modifier
                .clip(AxCardShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = true
                    }
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                ProcessIcon(row)
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = row.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        // Memory is what the list sorts by first: the one number read at a glance.
                        Text(
                            text = formatKib(proc.rss),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = proc.args.ifEmpty { proc.name },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    FlowRow(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusPill(stateLabel(proc.state), stateColor(proc.state))
                        if (row.isNew) {
                            StatusPill(stringResource(R.string.process_new), MaterialTheme.colorScheme.tertiary)
                        }
                        MetaTag("PID", proc.pid.toString())
                        MetaTag("PPID", proc.ppid.toString())
                        MetaTag(null, proc.user)
                        proc.cpu?.takeIf { it > 0f }?.let { MetaTag("CPU", "$it%") }
                    }
                }
            }
        }

        val context = LocalContext.current
        val copied = stringResource(R.string.process_copied)
        fun copy(text: String) {
            menu = false
            if (ClipboardUtil.put(context, text)) Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
        }
        fun filter(f: Filter) {
            menu = false
            onFilter(f)
        }
        // A zero-size anchor on the card's right edge: the menu opens there, near the thumb and
        // clear of the icon and name it describes.
        Box(Modifier.align(Alignment.TopEnd)) {
            AxDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                AxDropdownHeader(
                    "${row.title}\nPID ${proc.pid} · ${proc.user} · ${formatKib(proc.rss)} · ${stateLabel(proc.state)}"
                )
                AxDropdownItem(
                    text = stringResource(R.string.process_signal_term),
                    icon = Icons.Outlined.StopCircle,
                    note = stringResource(R.string.process_sig_term),
                    onClick = { menu = false; onSignal(Signal.TERM, false) }
                )
                AxDropdownItem(
                    text = stringResource(R.string.process_signal_kill),
                    icon = Icons.Outlined.StopCircle,
                    destructive = true,
                    note = stringResource(R.string.process_sig_kill),
                    onClick = { menu = false; onSignal(Signal.KILL, false) }
                )
                AxDropdownDivider()
                AxDropdownItem(
                    text = stringResource(R.string.process_show_children),
                    icon = Icons.Outlined.AccountTree,
                    note = stringResource(R.string.process_show_children_note, proc.pid),
                    onClick = { filter(Filter(ppid = proc.pid)) }
                )
                AxDropdownItem(
                    text = stringResource(R.string.process_same_group),
                    icon = Icons.Outlined.FilterAlt,
                    note = stringResource(R.string.process_same_group_note, proc.pgid),
                    onClick = { filter(Filter(pgid = proc.pgid)) }
                )
                AxDropdownItem(
                    text = stringResource(R.string.process_same_uid),
                    icon = Icons.Outlined.FilterAlt,
                    note = stringResource(R.string.process_same_uid_note, proc.user, proc.uid),
                    onClick = { filter(Filter(uid = proc.uid)) }
                )
                AxDropdownDivider()
                AxDropdownItem(
                    text = stringResource(R.string.process_copy_pid),
                    icon = Icons.Outlined.ContentCopy,
                    onClick = { copy(proc.pid.toString()) }
                )
                AxDropdownItem(
                    text = stringResource(R.string.process_copy_cmdline),
                    icon = Icons.Outlined.ContentCopy,
                    enabled = proc.args.isNotEmpty(),
                    onClick = { copy(proc.args) }
                )
            }
        }
    }
}

/** The app's icon for an app process; a kind badge for the rest. */
@Composable
internal fun ProcessIcon(row: Row, size: Dp = 36.dp) {
    val app = row.apps.firstOrNull()
    if (app != null) {
        val context = LocalContext.current
        val request = remember(app.packageName) {
            ImageRequest.Builder(context).data(app.info).crossfade(true).build()
        }
        AsyncImage(
            model = request,
            contentDescription = app.label,
            modifier = Modifier.size(size)
        )
    } else {
        IconBadge(
            imageVector = if (row.isKernel) Icons.Outlined.Memory else Icons.Outlined.Settings,
            tint = if (row.isKernel) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            size = size
        )
    }
}

/** A small "LABEL value" tag; [label] null for a value that names itself (the user). */
@Composable
internal fun MetaTag(label: String?, value: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
            if (label != null) {
                Text(
                    text = "$label ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun categoryIcon(category: Category): ImageVector = when (category) {
    Category.ALL -> Icons.Outlined.Apps
    Category.APPS -> Icons.Outlined.Android
    Category.SYSTEM -> Icons.Outlined.Settings
    Category.KERNEL -> Icons.Outlined.Memory
    Category.AXMANAGER -> Icons.Outlined.Terminal
}

private fun categoryLabel(category: Category): Int = when (category) {
    Category.ALL -> R.string.settings_tab_all
    Category.APPS -> R.string.process_apps_only
    Category.SYSTEM -> R.string.process_category_system
    Category.KERNEL -> R.string.process_category_kernel
    Category.AXMANAGER -> R.string.process_from_server
}

/** `ps` S as a word; anything unknown stays as the letter. */
@Composable
internal fun stateLabel(state: String): String = when (state.take(1)) {
    "R" -> stringResource(R.string.process_state_running)
    "S" -> stringResource(R.string.process_state_sleeping)
    "D" -> stringResource(R.string.process_state_disk)
    "Z" -> stringResource(R.string.process_state_zombie)
    "T", "t" -> stringResource(R.string.process_state_stopped)
    "I" -> stringResource(R.string.process_state_idle)
    else -> state.ifEmpty { "?" }
}

@Composable
internal fun stateColor(state: String): Color = when (state.take(1)) {
    "R" -> MaterialTheme.colorScheme.primary
    "D" -> MaterialTheme.colorScheme.tertiary
    "Z", "T", "t" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.secondary
}

/**
 * The kernel counts process memory in pages (4 KB, or 16 KB on 16K-page devices) and reports
 * it in KB, so KB is the finest unit there is: a process never holds 100 B.
 */
internal fun formatKib(kib: Long): String = when {
    kib >= 1024 * 1024 -> "%.2f GB".format(kib / (1024f * 1024f))
    kib >= 1024 -> "%.1f MB".format(kib / 1024f)
    else -> "$kib KB"
}
