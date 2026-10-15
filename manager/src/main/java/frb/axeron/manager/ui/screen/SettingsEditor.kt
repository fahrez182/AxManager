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
import androidx.compose.ui.platform.LocalContext
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
                    searching = viewModel.isSearching,
                    matched = { viewModel.filtered(it).size },
                    totals = { viewModel.entries(it).size },
                    loading = viewModel.isLoading(type),
                    listState = listStates[page],
                    onRefresh = { viewModel.refresh(type) },
                    onOpen = { editing = EditTarget(it.type, it.key, it.value, isNew = false) },
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
    searching: Boolean,
    matched: (SettingType) -> Int,
    totals: (SettingType) -> Int,
    loading: Boolean,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onOpen: (Entry) -> Unit,
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
                SettingRow(entry = entry, showSource = type == null, onClick = { onOpen(entry) })
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
    showSource: Boolean,
    onClick: () -> Unit
) {
    AxCard(onClick = onClick) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = entry.keyDisplay,
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
                Text(
                    text = entry.valuePreview,
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
    // TextFieldState: its field exposes the scroll state the long-value indicator needs.
    val valueState = remember(target) { TextFieldState(target.value) }
    val value = valueState.text.toString()
    var busy by remember { mutableStateOf(false) }

    val modified = value != target.value
    val canSave = !busy && key.isNotBlank() && (target.isNew || modified)

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
        headerAction = if (target.isNew) null else {
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
                            text = key,
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

        // The field scrolls inside once the value passes its visible lines; nothing shows that
        // on its own, so draw a scroll indicator and say how much there is.
        val valueScroll = rememberScrollState()
        val scrollsInside = valueScroll.maxValue > 0
        val indicatorColor = MaterialTheme.colorScheme.primary
        Box {
            OutlinedTextField(
                state = valueState,
                label = { Text(stringResource(R.string.settings_value)) },
                enabled = !busy,
                lineLimits = TextFieldLineLimits.MultiLine(3, VALUE_FIELD_MAX_LINES),
                scrollState = valueScroll,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = AxPanelShape,
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
                modifier = Modifier.fillMaxWidth()
            )
            if (scrollsInside) {
                Canvas(
                    Modifier
                        .matchParentSize()
                        .padding(top = 14.dp, bottom = 14.dp, end = 4.dp)
                ) {
                    val viewport = valueScroll.viewportSize.toFloat()
                    val max = valueScroll.maxValue.toFloat()
                    val thumb = (size.height * viewport / (viewport + max)).coerceAtLeast(24.dp.toPx())
                    val top = (size.height - thumb) * (valueScroll.value / max)
                    val width = 3.dp.toPx()
                    drawRoundRect(
                        color = indicatorColor.copy(alpha = 0.6f),
                        topLeft = Offset(size.width - width, top),
                        size = Size(width, thumb),
                        cornerRadius = CornerRadius(width / 2)
                    )
                }
            }
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
                    if (canReset) valueState.setTextAndPlaceCursorAtEnd(target.value) else onDismiss()
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

private fun sourceIcon(type: SettingType?): ImageVector = when (type) {
    null -> Icons.Outlined.Apps
    SettingType.GLOBAL -> Icons.Outlined.Public
    SettingType.SECURE -> Icons.Outlined.Shield
    SettingType.SYSTEM -> Icons.Outlined.Settings
    SettingType.ANDROID_PROP -> Icons.Outlined.Android
    SettingType.AXERON_ENV -> Icons.Outlined.Terminal
}

@Composable
private fun sourceColor(type: SettingType): Color = when (type) {
    SettingType.GLOBAL, SettingType.ANDROID_PROP -> MaterialTheme.colorScheme.primary
    SettingType.SECURE, SettingType.AXERON_ENV -> MaterialTheme.colorScheme.tertiary
    SettingType.SYSTEM -> MaterialTheme.colorScheme.secondary
}
