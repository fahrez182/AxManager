package frb.axeron.manager.ui.screen

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import frb.axeron.manager.data.QuickSettingsStore
import frb.axeron.manager.ui.component.AxDropdownDivider
import frb.axeron.manager.ui.component.AxDropdownHeader
import frb.axeron.manager.ui.component.AxDropdownItem
import frb.axeron.manager.ui.component.AxDropdownMenu
import androidx.compose.material.icons.outlined.Key
import frb.axeron.manager.ui.component.AxCardShape
import frb.axeron.manager.ui.component.ScrollableTextField
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.R
import frb.axeron.manager.data.SettingsRepository.SettingType
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.AxPullToRefreshBox
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.EmptyState
import frb.axeron.manager.ui.component.FastScrollbar
import frb.axeron.manager.ui.component.FloatingBarFab
import frb.axeron.manager.ui.component.OptionTile
import frb.axeron.manager.ui.component.OptionTileGrid
import frb.axeron.manager.ui.component.OptionsSectionHeader
import frb.axeron.manager.ui.component.SearchAppBar
import frb.axeron.manager.ui.component.SectionHeader
import frb.axeron.manager.ui.component.StatusPill
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.viewmodel.SettingsEditorViewModel
import frb.axeron.manager.ui.viewmodel.SettingsEditorViewModel.Entry
import frb.axeron.manager.ui.viewmodel.SettingsEditorViewModel.EnvLock
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import kotlinx.coroutines.launch

/** Lines the value field shows before it scrolls internally. */
private const val VALUE_FIELD_MAX_LINES = 10

/** What the editor sheet is open for. */
@Immutable
private data class EditTarget(
    val type: SettingType,
    val key: String,
    val value: String,
    val isNew: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsEditorScreen(
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal
) {
    val viewModel: SettingsEditorViewModel = viewModel()
    val scope = rememberCoroutineScope()

    // Page 0 is "All" (null); the rest are the individual sources.
    val pages = remember { listOf<SettingType?>(null) + SettingType.entries }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val listStates = remember { pages.map { LazyListState() } }
    val currentType = pages[pagerState.currentPage]

    var showFab by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    LaunchedEffect(pagerState.currentPage) {
        showFab = true
        viewModel.ensureLoaded(pages[pagerState.currentPage])

        // FAB hides while scrolling down the current page and returns on the way up.
        val listState = listStates[pagerState.currentPage]
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (i, o) ->
                val down = i > lastIndex || (i == lastIndex && o > lastOffset + 4)
                val up = i < lastIndex || (i == lastIndex && o < lastOffset - 4)
                if (down) showFab = false else if (up) showFab = true
                lastIndex = i
                lastOffset = o
            }
    }

    editing?.let { target ->
        SettingEditorSheet(
            target = target,
            viewModel = viewModel,
            onDismiss = { editing = null }
        )
    }

    var showQuickSettings by remember { mutableStateOf(false) }
    if (showQuickSettings) {
        QuickSettingsSheet(
            onDismissRequest = { showQuickSettings = false },
            // Keep the list in step with what the sheet just wrote.
            onApplied = { viewModel.refresh(it) }
        )
    }

    // Long-press → "Add to Quick Settings" opens the config sheet for that entry.
    var pinning by remember { mutableStateOf<Entry?>(null) }
    pinning?.let { entry ->
        val added = stringResource(R.string.quick_settings_saved)
        val context = LocalContext.current
        QuickSettingConfigSheet(
            type = entry.type,
            key = entry.key,
            currentValue = entry.value,
            existing = QuickSettingsStore.find(entry.type, entry.key),
            onSave = {
                QuickSettingsStore.save(it)
                pinning = null
                Toast.makeText(context, added, Toast.LENGTH_SHORT).show()
            },
            onDismissRequest = { pinning = null }
        )
    }

    Scaffold(
        topBar = {
            SearchAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_editor),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                searchLabel = stringResource(R.string.settings_search),
                searchText = viewModel.query,
                onSearchTextChange = { viewModel.query = it },
                onClearClick = { viewModel.query = "" },
                action = {
                    IconButton(onClick = { showQuickSettings = true }) {
                        Icon(Icons.Outlined.Bolt, contentDescription = stringResource(R.string.quick_settings))
                    }
                    IconButton(onClick = { viewModel.refresh(currentType) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            // Rendered beside the nav pill by FloatingBarFab.
            FloatingBarFab(visible = showFab) {
                FloatingActionButton(
                    shape = CircleShape,
                    onClick = {
                        editing = EditTarget(currentType ?: SettingType.GLOBAL, "", "", isNew = true)
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.settings_add_title))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            SourceTabs(
                pages = pages,
                selected = pagerState.currentPage,
                // While searching, each tab counts what it would show.
                count = { type -> viewModel.filtered(type).size },
                searching = viewModel.isSearching,
                onSelect = { scope.launch { pagerState.animateScrollToPage(it) } }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { pages[it]?.name ?: "ALL" }
            ) { page ->
                val type = pages[page]
                val sourceEmpty =
                    if (type == null) viewModel.allEntries.isEmpty() else viewModel.entries(type).isEmpty()

                SettingsPage(
                    type = type,
                    shown = viewModel.filtered(type),
                    sourceEmpty = sourceEmpty,
                    query = viewModel.query.trim(),
                    searching = viewModel.isSearching,
                    matched = { viewModel.filtered(it).size },
                    totals = { viewModel.entries(it).size },
                    loading = viewModel.isLoading(type),
                    listState = listStates[page],
                    onRefresh = { viewModel.refresh(type) },
                    onOpen = { editing = EditTarget(it.type, it.key, it.value, isNew = false) },
                    onPin = { pinning = it },
                    onOpenSource = { target ->
                        scope.launch { pagerState.animateScrollToPage(pages.indexOf(target)) }
                    }
                )
            }
        }
    }
}

@Composable
private fun SourceTabs(
    pages: List<SettingType?>,
    selected: Int,
    count: (SettingType?) -> Int,
    searching: Boolean,
    onSelect: (Int) -> Unit
) {
    val rowState = rememberLazyListState()
    LaunchedEffect(selected) {
        // Keep the selected tab, and a bit of its left neighbour, on screen.
        rowState.animateScrollToItem((selected - 1).coerceAtLeast(0))
    }

    LazyRow(
        state = rowState,
        // The search pill above already leaves 8dp; a small top gap keeps the two as one group.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(pages, key = { _, type -> type?.name ?: "ALL" }) { index, type ->
            val isSelected = index == selected
            val container by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "sourceTab"
            )
            val content = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                onClick = { onSelect(index) },
                shape = CircleShape,
                color = container
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = sourceIcon(type),
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(type?.shortStringId ?: R.string.settings_tab_all),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = content
                    )
                    // While searching a 0 is news too (no match there); otherwise it just means
                    // the source hasn't loaded yet.
                    val n = count(type)
                    if (n > 0 || searching) {
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

@Composable
private fun SettingsPage(
    type: SettingType?,
    shown: List<Entry>,
    sourceEmpty: Boolean,
    query: String,
    searching: Boolean,
    matched: (SettingType) -> Int,
    totals: (SettingType) -> Int,
    loading: Boolean,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onOpen: (Entry) -> Unit,
    onPin: (Entry) -> Unit,
    onOpenSource: (SettingType) -> Unit
) {
    val floatingInset = LocalFloatingBarInset.current

    AxPullToRefreshBox(
        isRefreshing = loading && !sourceEmpty,
        onRefresh = onRefresh,
        topInset = 0.dp
    ) {
        if (loading && sourceEmpty) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@AxPullToRefreshBox
        }

        // The "All" page leads with the per-source overview.
        val headerItems = if (type == null) 1 else 0
        val bottomPadding = floatingInset + 16.dp

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding)
        ) {
            if (type == null) {
                item(key = "overview", contentType = "overview") {
                    SourceOverview(
                        searching = searching,
                        matched = matched,
                        totals = totals,
                        onOpenSource = onOpenSource
                    )
                }
            }
            if (shown.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = stringResource(R.string.settings_empty),
                        subtitle = stringResource(R.string.settings_empty_desc)
                    )
                }
            }
            items(shown, key = { it.id }, contentType = { "entry" }) { entry ->
                SettingRow(
                    entry = entry,
                    query = query,
                    showSource = type == null,
                    onClick = { onOpen(entry) },
                    onPin = { onPin(entry) }
                )
            }
        }

        // Follows scrollability, not a row count: search results are often short lists that
        // still overflow the screen.
        if (shown.isNotEmpty() && (listState.canScrollForward || listState.canScrollBackward)) {
            FastScrollbar(
                state = listState,
                count = shown.size + headerItems,
                label = { index ->
                    (shown.getOrNull(index - headerItems) ?: shown.first()).section
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = 4.dp, bottom = bottomPadding)
            )
        }
    }
}

/** Entries per source; while searching, how many of each match. */
@Composable
private fun SourceOverview(
    searching: Boolean,
    matched: (SettingType) -> Int,
    totals: (SettingType) -> Int,
    onOpenSource: (SettingType) -> Unit
) {
    Column(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            SectionHeader(stringResource(R.string.settings_overview))
            Text(
                text = stringResource(R.string.settings_overview_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        OptionTileGrid(items = SettingType.entries, columns = 2) { type, modifier ->
            val total = totals(type)
            OptionTile(
                modifier = modifier,
                icon = sourceIcon(type),
                label = stringResource(type.stringId),
                subtitle = if (searching) {
                    stringResource(R.string.settings_count_matched, matched(type), total)
                } else {
                    stringResource(R.string.settings_count, total)
                },
                selected = false,
                onClick = { onOpenSource(type) }
            )
        }
    }
}

@Composable
private fun SettingRow(
    entry: Entry,
    query: String,
    showSource: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit
) {
    val matchStyle = searchMatchStyle()
    val keyText = remember(entry.id, query, matchStyle) {
        highlightMatches(entry.keyDisplay, query, matchStyle)
    }
    val pinned = QuickSettingsStore.find(entry.type, entry.key) != null
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }

    Box {
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
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    if (pinned) {
                        Icon(
                            Icons.Outlined.Bolt,
                            contentDescription = stringResource(R.string.quick_settings),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 1.dp, end = 4.dp)
                                .size(16.dp)
                        )
                    }
                    Text(
                        text = keyText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (showSource) {
                        Spacer(Modifier.width(8.dp))
                        StatusPill(
                            text = stringResource(entry.type.shortStringId),
                            color = sourceColor(entry.type)
                        )
                    }
                }
                if (entry.value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_empty_value),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    // Cut off by the line limit (seen at layout) or by the preview length.
                    var clipped by remember(entry.id) { mutableStateOf(entry.isPreviewTruncated) }
                    val valueText = remember(entry.id, query, matchStyle) {
                        highlightMatches(entry.valuePreview, query, matchStyle)
                    }
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { if (it.hasVisualOverflow) clipped = true }
                    )
                    if (clipped) {
                        LongValueHint(
                            text = stringResource(
                                R.string.settings_value_truncated,
                                entry.value.lineCount(),
                                entry.value.length
                            )
                        )
                    }
                }
            }
        }
        val context = LocalContext.current
        val keyCopied = stringResource(R.string.key_copied)
        val valueCopied = stringResource(R.string.value_copied)
        AxDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            AxDropdownHeader(entry.key, monospace = true)
            AxDropdownItem(
                text = stringResource(if (pinned) R.string.quick_settings_edit else R.string.quick_settings_add),
                icon = Icons.Outlined.Bolt,
                onClick = {
                    menu = false
                    onPin()
                }
            )
            AxDropdownDivider()
            AxDropdownItem(
                text = stringResource(R.string.settings_copy_key),
                icon = Icons.Outlined.Key,
                onClick = {
                    menu = false
                    if (ClipboardUtil.put(context, entry.key)) {
                        Toast.makeText(context, keyCopied, Toast.LENGTH_SHORT).show()
                    }
                }
            )
            AxDropdownItem(
                text = stringResource(R.string.settings_copy_value),
                icon = Icons.Outlined.ContentCopy,
                enabled = entry.value.isNotEmpty(),
                onClick = {
                    menu = false
                    if (ClipboardUtil.put(context, entry.value)) {
                        Toast.makeText(context, valueCopied, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SettingEditorSheet(
    target: EditTarget,
    viewModel: SettingsEditorViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val confirmDialog = rememberConfirmDialog()

    var type by remember(target) { mutableStateOf(target.type) }
    var key by remember(target) { mutableStateOf(target.key) }
    // An env key is edited as the user's own value, not the merged one; the server's and the
    // system's variables are locked, and their path lists only take additions (EnvLock).
    val isEnv = type == SettingType.AXERON_ENV
    val envLock = if (isEnv) viewModel.envLock(key.trim()) else EnvLock.NONE
    val initialValue = remember(target) {
        if (target.type == SettingType.AXERON_ENV && !target.isNew) viewModel.envEditValue(target.key)
        else target.value
    }
    // TextFieldState: its field exposes the scroll state the long-value indicator needs.
    val valueState = remember(target) { TextFieldState(initialValue) }
    val value = valueState.text.toString()
    var busy by remember { mutableStateOf(false) }

    // The list's search query: highlight it in the key and step through it in the value.
    val query = viewModel.query.trim()
    val matchStyle = searchMatchStyle()
    val valueMatches = remember(value, query) { occurrences(value, query) }
    var currentMatch by remember(target, query) { mutableIntStateOf(-1) }
    val valueFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    /** Selects match [index] in the value field, which highlights it and scrolls it into view. */
    fun selectMatch(index: Int) {
        if (valueMatches.isEmpty()) return
        currentMatch = Math.floorMod(index, valueMatches.size)
        val start = valueMatches[currentMatch]
        valueState.edit { selection = TextRange(start, start + query.length) }
        scope.launch {
            // Selection only shows (and scrolls) in a focused field; keep the keyboard down.
            valueFocus.requestFocus()
            withFrameNanos { }
            keyboard?.hide()
        }
    }

    val modified = value != initialValue
    val canSave = !busy && key.isNotBlank() && (target.isNew || modified) && envLock != EnvLock.LOCKED

    val keyCopied = stringResource(R.string.key_copied)
    val valueCopied = stringResource(R.string.value_copied)
    val saved = stringResource(R.string.settings_saved)
    val removed = stringResource(R.string.settings_removed)
    val saveFailed = stringResource(R.string.failed_to_save_settings)
    val removeFailed = stringResource(R.string.failed_to_remove_settings)
    val removeTitle = stringResource(R.string.ask_remove_settings)
    val removeMessage = stringResource(R.string.ask_remove_settings_msg)
    val removeLabel = stringResource(R.string.remove)
    val cancelLabel = stringResource(R.string.cancel)

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    // Runs a write; the sheet stays open (and undismissable) until it finishes.
    fun write(done: String, failed: String, block: suspend () -> Boolean) {
        scope.launch {
            busy = true
            val ok = block()
            busy = false
            if (ok) {
                toast(done)
                onDismiss()
            } else {
                toast(failed)
            }
        }
    }

    AxOptionsSheet(
        title = stringResource(if (target.isNew) R.string.settings_add_title else R.string.settings_edit_title),
        description = stringResource(type.stringId),
        onDismissRequest = { if (!busy) onDismiss() },
        // An env entry has something to delete only in the user's own env.
        headerAction = if (target.isNew || (isEnv && !viewModel.hasUserEnv(target.key))) null else {
            {
                IconButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            val result = confirmDialog.awaitConfirm(
                                removeTitle,
                                content = removeMessage,
                                confirm = removeLabel,
                                dismiss = cancelLabel
                            )
                            if (result == ConfirmResult.Confirmed) {
                                write(removed, removeFailed) { viewModel.delete(type, key) }
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = removeLabel,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    ) {
        if (target.isNew) {
            OptionsSectionHeader(stringResource(R.string.settings_source))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SettingType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        enabled = !busy,
                        label = { Text(stringResource(option.shortStringId)) },
                        leadingIcon = {
                            Icon(sourceIcon(option), contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text(stringResource(R.string.settings_key)) },
                singleLine = true,
                enabled = !busy,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = AxPanelShape,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // The key of an existing entry is fixed: show it with a copy action.
            Surface(shape = AxPanelShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_key),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = remember(key, query, matchStyle) {
                                highlightMatches(key, query, matchStyle)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = { if (ClipboardUtil.put(context, key)) toast(keyCopied) }) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.settings_copy_key),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Scrolls inside past its visible lines, with an indicator; the hint below says how much.
        val valueScroll = rememberScrollState()
        val scrollsInside = valueScroll.maxValue > 0
        ScrollableTextField(
            state = valueState,
            label = stringResource(R.string.settings_value),
            enabled = !busy && envLock != EnvLock.LOCKED,
            minLines = 3,
            maxLines = VALUE_FIELD_MAX_LINES,
            scrollState = valueScroll,
            trailingIcon = {
                IconButton(
                    enabled = value.isNotEmpty(),
                    onClick = { if (ClipboardUtil.put(context, value)) toast(valueCopied) }
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.settings_copy_value),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            fieldModifier = Modifier.focusRequester(valueFocus),
            modifier = Modifier.fillMaxWidth()
        )
        if (envLock != EnvLock.NONE) {
            Text(
                text = if (envLock == EnvLock.LOCKED) stringResource(R.string.settings_env_locked)
                else stringResource(R.string.settings_env_append, key.trim()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        if (valueMatches.isNotEmpty()) {
            MatchNavigator(
                query = query,
                current = currentMatch,
                total = valueMatches.size,
                enabled = !busy,
                onPrevious = { selectMatch(if (currentMatch < 0) valueMatches.lastIndex else currentMatch - 1) },
                onNext = { selectMatch(currentMatch + 1) }
            )
        }
        if (scrollsInside || (!target.isNew && modified)) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (scrollsInside) {
                    LongValueHint(
                        text = stringResource(
                            R.string.settings_value_scroll_hint,
                            value.lineCount(),
                            value.length
                        )
                    )
                }
                if (!target.isNew && modified) {
                    Text(
                        text = stringResource(R.string.settings_value_modified),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val canReset = !target.isNew && modified
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = !busy,
                onClick = {
                    if (canReset) valueState.setTextAndPlaceCursorAtEnd(initialValue) else onDismiss()
                }
            ) {
                Text(stringResource(if (canReset) R.string.settings_reset_value else R.string.cancel))
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = canSave,
                onClick = { write(saved, saveFailed) { viewModel.put(type, key.trim(), value) } }
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

/** "Match 2 of 5 for “query”" with previous / next, for stepping through the value field. */
@Composable
private fun MatchNavigator(
    query: String,
    current: Int,
    total: Int,
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(shape = AxPanelShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 2.dp)
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (current < 0) {
                    pluralStringResource(R.plurals.settings_value_matches, total, total, query)
                } else {
                    stringResource(R.string.settings_value_match_position, current + 1, total, query)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPrevious, enabled = enabled) {
                Icon(
                    Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.terminal_search_previous),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            IconButton(onClick = onNext, enabled = enabled) {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.terminal_search_next),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

/** "There is more than you see" note for long values, in the list and in the editor. */
@Composable
private fun LongValueHint(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.UnfoldMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun String.lineCount(): Int = count { it == '\n' } + 1

@Composable
private fun searchMatchStyle() = SpanStyle(
    background = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
    fontWeight = FontWeight.Bold
)

/**
 * [display] with each occurrence of [query] (ignoring case) styled. Display text carries
 * zero-width break points after symbols; matching skips them so "a.b" still finds "a.b".
 */
private fun highlightMatches(display: String, query: String, style: SpanStyle): AnnotatedString {
    if (query.isEmpty()) return AnnotatedString(display)
    val toDisplay = IntArray(display.length)
    val plain = buildString {
        display.forEachIndexed { i, c ->
            if (c != ZWSP) {
                toDisplay[length] = i
                append(c)
            }
        }
    }
    var i = plain.indexOf(query, ignoreCase = true)
    if (i < 0) return AnnotatedString(display)
    return buildAnnotatedString {
        append(display)
        while (i >= 0) {
            addStyle(style, toDisplay[i], toDisplay[i + query.length - 1] + 1)
            i = plain.indexOf(query, i + query.length, ignoreCase = true)
        }
    }
}

/** Start of every occurrence of [query] (ignoring case) in [text]. */
private fun occurrences(text: String, query: String): List<Int> {
    if (query.isEmpty()) return emptyList()
    val found = ArrayList<Int>()
    var i = text.indexOf(query, ignoreCase = true)
    while (i >= 0) {
        found.add(i)
        i = text.indexOf(query, i + query.length, ignoreCase = true)
    }
    return found
}

private const val ZWSP = '​'

internal fun sourceIcon(type: SettingType?): ImageVector = when (type) {
    null -> Icons.Outlined.Apps
    SettingType.GLOBAL -> Icons.Outlined.Public
    SettingType.SECURE -> Icons.Outlined.Shield
    SettingType.SYSTEM -> Icons.Outlined.Settings
    SettingType.ANDROID_PROP -> Icons.Outlined.Android
    SettingType.AXERON_ENV -> Icons.Outlined.Terminal
}

@Composable
internal fun sourceColor(type: SettingType): Color = when (type) {
    SettingType.GLOBAL, SettingType.ANDROID_PROP -> MaterialTheme.colorScheme.primary
    SettingType.SECURE, SettingType.AXERON_ENV -> MaterialTheme.colorScheme.tertiary
    SettingType.SYSTEM -> MaterialTheme.colorScheme.secondary
}
