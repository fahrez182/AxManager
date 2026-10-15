package frb.axeron.manager.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.SolidColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fox2code.androidansi.ktx.parseAsAnsiAnnotatedString
import frb.axeron.manager.R
import frb.axeron.manager.ui.util.TerminalBuffer
import frb.axeron.manager.ui.util.TerminalPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Default text style for command output, scaled by [TerminalPrefs.textScale]. */
@Composable
fun terminalTextStyle(scale: Float = TerminalPrefs.textScale): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp * scale,
    lineHeight = 16.sp * scale,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)

/**
 * Renders a [TerminalBuffer]: line numbers, follow-the-bottom, a fast-scroll thumb for huge
 * output and a side pill (top / go to line[:column] / bottom).
 *
 * Index based: only the visible lines are read from the buffer, so a million spooled lines
 * scroll like twenty. View options (line numbers, text scale) come from [TerminalPrefs].
 *
 * @param wrap soft-wrap long lines; when false the text pans horizontally while the line
 * numbers stay put.
 * @param filter hides lines by kind (e.g. stderr outside developer mode). Remember it.
 * @param color per-kind base colour; ANSI colours in the text still win.
 */
@Composable
fun <K : Any> TerminalOutput(
    buffer: TerminalBuffer<K>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    wrap: Boolean = true,
    filter: ((K) -> Boolean)? = null,
    color: @Composable (K) -> Color = { MaterialTheme.colorScheme.onSurfaceVariant },
    jumpButtonPadding: PaddingValues = PaddingValues(0.dp),
    state: LazyListState = rememberLazyListState(),
) {
    val showLineNumbers = TerminalPrefs.lineNumbers
    val style = terminalTextStyle()

    val index = remember(buffer, filter) { filter?.let { FilterIndex(it) } }
    val view by remember(buffer, index) {
        derivedStateOf {
            val snap = buffer.snapshot
            index?.view(snap, buffer) ?: LineView(snap.size, snap.head, null, 0)
        }
    }
    val version = buffer.snapshot.version

    // ---- follow mode ----
    var follow by remember { mutableStateOf(true) }
    val atBottom by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            last.index == info.totalItemsCount - 1 &&
                    last.offset + last.size <= info.viewportEndOffset + 8
        }
    }
    val dragged by state.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged, atBottom) {
        if (atBottom) follow = true
        else if (dragged) follow = false
    }
    // Once per published snapshot (throttled by the buffer), not per chunk. Jump instantly:
    // rapid output would keep cancelling an animated scroll.
    LaunchedEffect(state, buffer) {
        snapshotFlow { view to follow }.collect { (shown, following) ->
            if (following && shown.count > 0) state.scrollToItem(shown.count - 1, Int.MAX_VALUE)
        }
    }

    // ---- horizontal panning (no-wrap) ----
    val pan = remember { HorizontalPan() }
    LaunchedEffect(wrap) { pan.reset() }
    LaunchedEffect(buffer) {
        snapshotFlow { buffer.snapshot.isEmpty }.collect { if (it) pan.reset() }
    }
    val panState = rememberScrollableState { delta ->
        val old = pan.offset
        pan.scrollTo(old - delta)
        old - pan.offset
    }

    // ---- gutter ----
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val charWidth = remember(style, density) { measurer.measure("0", style).size.width }
    val digits = maxOf(3, buffer.snapshot.tail.toString().length)
    val gutterWidth = with(density) { (charWidth * digits).toDp() }
    val gutterStyle = style.copy(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        textAlign = TextAlign.End
    )

    // ---- go to line ----
    var highlight by remember { mutableStateOf<Mark?>(null) }
    LaunchedEffect(highlight) {
        if (highlight != null) {
            delay(2500)
            highlight = null
        }
    }
    val rowHighlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val columnHighlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    var showGoTo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ---- find in output ----
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf(emptyList<TerminalBuffer.Match>()) }
    var current by remember { mutableIntStateOf(-1) }
    val matchHighlight = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
    val currentHighlight = MaterialTheme.colorScheme.primary
    val onCurrentHighlight = MaterialTheme.colorScheme.onPrimary

    /** Scrolls [match] into view with some context above, panning to it when not wrapping. */
    suspend fun reveal(match: TerminalBuffer.Match) {
        val v = view
        if (v.count == 0) return
        follow = false
        state.scrollToItem((v.indexOf(match.position) - 3).coerceAtLeast(0))
        if (!wrap) {
            withFrameNanos { } // let the line measure its width first
            pan.scrollTo(match.start * charWidth - pan.viewport / 3f)
        }
    }

    // Re-runs as output streams in; collectLatest drops a search that new output made stale.
    LaunchedEffect(buffer, filter, query) {
        if (query.isEmpty()) {
            matches = emptyList()
            current = -1
            return@LaunchedEffect
        }
        var fresh = true
        snapshotFlow { buffer.snapshot.version }.collectLatest {
            delay(if (fresh) 200 else 400) // typing / live-output debounce
            val found = withContext(Dispatchers.Default) {
                val job = coroutineContext
                buffer.search(query, filter ?: { true }, limit = SEARCH_LIMIT, stop = { !job.isActive })
            }
            matches = found
            if (fresh) {
                // Start from the first match on screen or below it, like find-in-page.
                val top = view.let { v ->
                    if (v.count == 0) 0L else v.positionAt(state.firstVisibleItemIndex.coerceIn(0, v.count - 1))
                }
                current = found.indexOfFirst { it.position >= top }.let { if (it < 0) 0 else it }
                found.getOrNull(current)?.let { reveal(it) }
                fresh = false
            } else {
                current = current.coerceAtMost(found.lastIndex)
            }
        }
    }

    fun step(delta: Int) {
        if (matches.isEmpty()) return
        current = Math.floorMod(current + delta, matches.size)
        val target = matches[current]
        scope.launch { reveal(target) }
    }

    fun closeSearch() {
        searchOpen = false
        query = ""
    }
    BackHandler(enabled = searchOpen) { closeSearch() }

    val canScroll by remember {
        derivedStateOf { state.canScrollBackward || state.canScrollForward }
    }
    val showScrollbar = canScroll && view.count > SCROLLBAR_MIN_LINES

    Box(modifier) {
        SelectionContainer(Modifier.fillMaxSize()) {
            LazyColumn(
                state = state,
                contentPadding = contentPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (wrap) Modifier
                        else Modifier.scrollable(panState, Orientation.Horizontal)
                    )
            ) {
                val shown = view
                items(
                    count = shown.count,
                    key = { shown.positionAt(it) },
                    contentType = { 0 }
                ) { i ->
                    val position = shown.positionAt(i)
                    // Re-read on every publish: an open line (progress bar) changes in place.
                    val line = remember(position, version) { buffer.lineAt(position) }
                        ?: return@items
                    val mark = highlight?.takeIf { it.line == line.number }
                    val currentStart = matches.getOrNull(current)
                        ?.takeIf { it.position == position }?.start ?: -1
                    val parsed = remember(line.text) { line.text.toAnsiString() }
                    val text = remember(parsed, mark, query, currentStart) {
                        var t = parsed
                        if (mark?.column != null) t = t.withColumnMark(mark.column - 1, columnHighlight)
                        if (query.isNotEmpty()) {
                            t = t.withMatches(
                                query, matchHighlight, currentStart,
                                SpanStyle(background = currentHighlight, color = onCurrentHighlight)
                            )
                        }
                        t
                    }
                    val marked = mark != null || currentStart >= 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (marked) Modifier.background(rowHighlight) else Modifier)
                    ) {
                        if (showLineNumbers) {
                            DisableSelection {
                                BasicText(
                                    text = line.number.toString(),
                                    style = gutterStyle,
                                    maxLines = 1,
                                    modifier = Modifier.width(gutterWidth)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                        }
                        BasicText(
                            text = text,
                            style = style.copy(color = color(line.kind)),
                            softWrap = wrap,
                            modifier = Modifier
                                .weight(1f)
                                .then(if (wrap) Modifier else Modifier.horizontalPan(pan))
                        )
                    }
                }
            }
        }

        if (showScrollbar) {
            FastScrollbar(
                state = state,
                count = view.count,
                onDragStart = { follow = false },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(contentPadding.let {
                        PaddingValues(top = it.calculateTopPadding(), bottom = it.calculateBottomPadding())
                    })
            )
        }

        val currentLine by remember {
            derivedStateOf {
                val v = view
                if (v.count == 0) null
                else v.positionAt(state.firstVisibleItemIndex.coerceIn(0, v.count - 1)) + 1
            }
        }
        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = contentPadding.calculateTopPadding())
                .padding(horizontal = 12.dp)
        ) {
            TerminalSearchBar(
                query = query,
                onQueryChange = { query = it },
                current = current,
                total = matches.size,
                capped = matches.size >= SEARCH_LIMIT,
                onPrevious = { step(-1) },
                onNext = { step(1) },
                onClose = { closeSearch() }
            )
        }

        // Shown whenever there is output: search is useful even when it all fits.
        AnimatedVisibility(
            visible = view.count > 0,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(jumpButtonPadding)
                .padding(12.dp)
                // Stay clear of the scrollbar thumb.
                .padding(end = if (showScrollbar) 14.dp else 0.dp)
        ) {
            TerminalNavPill(
                currentLine = currentLine,
                following = follow,
                canScrollUp = state.canScrollBackward,
                onSearch = { searchOpen = true },
                onTop = {
                    follow = false
                    scope.launch { state.scrollToItem(0) }
                },
                onGoTo = { showGoTo = true },
                onBottom = {
                    follow = true
                    scope.launch { state.scrollToItem((view.count - 1).coerceAtLeast(0), Int.MAX_VALUE) }
                }
            )
        }
    }

    if (showGoTo && view.count > 0) {
        val v = view
        GoToLineDialog(
            first = v.positionAt(0) + 1,
            last = v.positionAt(v.count - 1) + 1,
            onDismiss = { showGoTo = false },
            onGo = { target, column ->
                showGoTo = false
                val i = v.indexOf(target - 1)
                follow = false
                scope.launch {
                    // A few lines of context above the target.
                    state.scrollToItem((i - 3).coerceAtLeast(0))
                    highlight = Mark(v.positionAt(i) + 1, column)
                    if (!wrap) {
                        withFrameNanos { } // let the target line measure its width first
                        val x = column?.let { (it - 1) * charWidth - pan.viewport / 3f } ?: 0f
                        pan.scrollTo(x)
                    }
                }
            }
        )
    }
}

private const val SCROLLBAR_MIN_LINES = 200

/** Matches kept per search; enough to step through, bounded for million-line output. */
private const val SEARCH_LIMIT = 10_000

/** Find bar: query, "current/total" and previous / next / close. */
@Composable
private fun TerminalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    current: Int,
    total: Int,
    capped: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val content = MaterialTheme.colorScheme.onSecondaryContainer
    val textStyle = MaterialTheme.typography.bodyMedium.copy(color = content)

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(44.dp)
                .padding(start = 14.dp, end = 2.dp)
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = content.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.terminal_search_hint),
                                style = textStyle,
                                color = content.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }
                        inner()
                    }
                }
            )
            if (query.isNotEmpty()) {
                Text(
                    text = if (total == 0) "0/0"
                    else "${current + 1}/$total${if (capped) "+" else ""}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (total == 0) MaterialTheme.colorScheme.error else content,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            IconButton(onClick = onPrevious, enabled = total > 0, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.terminal_search_previous)
                )
            }
            IconButton(onClick = onNext, enabled = total > 0, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.terminal_search_next)
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
            }
        }
    }
}

/** Which buffer positions are on screen, in order. */
@Immutable
private class LineView(
    val count: Int,
    private val head: Long,
    private val positions: LongArray?,
    private val start: Int,
) {
    fun positionAt(i: Int): Long = if (positions == null) head + i else positions[start + i]

    /** Index of the first shown line at or after [position], clamped to the view. */
    fun indexOf(position: Long): Int {
        if (count == 0) return 0
        if (positions == null) return (position - head).coerceIn(0, count - 1L).toInt()
        var lo = 0
        var hi = count - 1
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (positions[start + mid] < position) lo = mid + 1 else hi = mid
        }
        return lo
    }
}

/**
 * Positions of the lines passing a filter, extended incrementally per snapshot: only new lines
 * are scanned, so filtering stays cheap with a million lines. Rebuilt when positions were
 * dropped or reused (snapshot generation).
 */
private class FilterIndex<K : Any>(private val filter: (K) -> Boolean) {
    private var positions = LongArray(1024)
    private var size = 0
    private var start = 0
    private var scanned = 0L
    private var generation = -1

    fun view(snap: TerminalBuffer.Snapshot, buffer: TerminalBuffer<K>): LineView {
        if (generation != snap.generation) {
            // New array: views handed out earlier keep reading the old one.
            positions = LongArray(1024)
            size = 0
            start = 0
            scanned = snap.head
            generation = snap.generation
        }
        if (scanned < snap.head) scanned = snap.head
        if (scanned < snap.tail) {
            buffer.forEachKind(scanned, snap.tail) { position, kind ->
                if (filter(kind)) {
                    if (size == positions.size) positions = positions.copyOf(size * 2)
                    positions[size++] = position
                }
            }
            scanned = snap.tail
        }
        while (start < size && positions[start] < snap.head) start++
        return LineView(size - start, snap.head, positions, start)
    }
}

@Composable
private fun TerminalNavPill(
    currentLine: Long?,
    following: Boolean,
    canScrollUp: Boolean,
    onSearch: () -> Unit,
    onTop: () -> Unit,
    onGoTo: () -> Unit,
    onBottom: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 3.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.terminal_search))
            }
            IconButton(onClick = onTop, enabled = canScrollUp) {
                Icon(
                    Icons.Filled.KeyboardDoubleArrowUp,
                    contentDescription = stringResource(R.string.terminal_scroll_top)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onGoTo)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.terminal_line_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = currentLine?.toString() ?: "–",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            IconButton(onClick = onBottom) {
                Icon(
                    Icons.Filled.KeyboardDoubleArrowDown,
                    contentDescription = stringResource(R.string.terminal_scroll_bottom),
                    // Highlighted while new output is not being followed.
                    tint = if (following) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private val GOTO_REGEX = Regex("""^(\d+)(?:\s*[:.,\s]\s*(\d+))?$""")

@Composable
private fun GoToLineDialog(
    first: Long,
    last: Long,
    onDismiss: () -> Unit,
    onGo: (line: Long, column: Int?) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val parsed = remember(input) {
        GOTO_REGEX.matchEntire(input.trim())?.let { m ->
            val line = m.groupValues[1].toLongOrNull() ?: return@let null
            val column = m.groupValues[2].toIntOrNull()?.takeIf { it > 0 }
            line to column
        }
    }
    val submit = { parsed?.let { onGo(it.first, it.second) } }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.terminal_goto_title)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                placeholder = { Text("100:20") },
                isError = input.isNotBlank() && parsed == null,
                supportingText = { Text(stringResource(R.string.terminal_goto_range, first, last)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                modifier = Modifier.focusRequester(focus)
            )
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = parsed != null) {
                Text(stringResource(R.string.terminal_goto))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private data class Mark(val line: Long, val column: Int?)

/**
 * Shared horizontal offset for every line. Lines read [offset] only while placing, so panning
 * re-places the visible lines without recomposing or re-measuring the list.
 */
@Stable
private class HorizontalPan {
    var offset by mutableFloatStateOf(0f)
        private set

    /** Widest line measured so far; plain fields, written during layout. */
    var contentWidth = 0
    var viewport = 0

    fun scrollTo(x: Float) {
        offset = x.coerceIn(0f, (contentWidth - viewport).coerceAtLeast(0).toFloat())
    }

    fun reset() {
        contentWidth = 0
        offset = 0f
    }
}

private fun Modifier.horizontalPan(pan: HorizontalPan) = clipToBounds().layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
    if (placeable.width > pan.contentWidth) pan.contentWidth = placeable.width
    pan.viewport = width
    layout(width, placeable.height) {
        placeable.placeRelative(-pan.offset.roundToInt(), 0)
    }
}

private const val ESC = '\u001B'

/** Most lines carry no ANSI codes; skip the parser for those. */
private fun String.toAnsiString(): AnnotatedString =
    if (indexOf(ESC) < 0) AnnotatedString(this) else parseAsAnsiAnnotatedString()

/**
 * Marks every occurrence of [query] (ignoring case) with [color]; the one starting at
 * [currentStart] gets [currentStyle] instead.
 */
private fun AnnotatedString.withMatches(
    query: String,
    color: Color,
    currentStart: Int,
    currentStyle: SpanStyle
): AnnotatedString {
    var i = text.indexOf(query, ignoreCase = true)
    if (i < 0) return this
    val source = this
    return buildAnnotatedString {
        append(source)
        while (i >= 0) {
            val style = if (i == currentStart) currentStyle else SpanStyle(background = color)
            addStyle(style, i, i + query.length)
            i = source.text.indexOf(query, i + query.length, ignoreCase = true)
        }
    }
}

private fun AnnotatedString.withColumnMark(index: Int, color: Color): AnnotatedString {
    if (index !in indices) return this
    val source = this
    return buildAnnotatedString {
        append(source)
        addStyle(SpanStyle(background = color), index, index + 1)
    }
}
