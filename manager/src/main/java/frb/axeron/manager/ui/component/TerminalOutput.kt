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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import frb.axeron.manager.ui.util.ClipboardUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    jumpButtonPadding: PaddingValues = PaddingValues(12.dp),
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

    // ---- overscroll ----
    // Past the end, like an editor: the last line can scroll up to the top, so at the very end
    // the line indicator (the top line) shows it and the scroll thumb sits at the bottom.
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var containerHeight by remember { mutableIntStateOf(0) }
    val bottomPaddingPx = with(density) { contentPadding.calculateBottomPadding().toPx() }
    val lineHeightPx = with(density) { style.lineHeight.toPx() }
    // Between the top and bottom padding: where the lines are read.
    val areaPx = (containerHeight - with(density) { contentPadding.calculateTopPadding().toPx() } -
            bottomPaddingPx).coerceAtLeast(0f)
    val overscrollPx = (areaPx - lineHeightPx).coerceAtLeast(0f)
    val listPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection),
        top = contentPadding.calculateTopPadding(),
        end = contentPadding.calculateEndPadding(layoutDirection),
        bottom = contentPadding.calculateBottomPadding() + with(density) { overscrollPx.toDp() }
    )
    val currentArea by rememberUpdatedState(areaPx)
    val currentLineHeight by rememberUpdatedState(lineHeightPx)

    /**
     * The last line at the bottom of the screen, not past it: where following keeps it. One
     * scroll with a negative offset (the list fills the lines above it), not a jump to the very
     * end and back: that drops every line on screen and composes them all again, on each
     * batch of a flood.
     */
    suspend fun scrollToEnd() {
        val info = state.layoutInfo
        val total = info.totalItemsCount
        if (total == 0) return
        // A wrapped last line can be taller than one line; its height is known once it is shown.
        val lastHeight = info.visibleItemsInfo.lastOrNull()
            ?.takeIf { it.index == total - 1 }?.size?.toFloat() ?: currentLineHeight
        state.scrollToItem(total - 1, -(currentArea - lastHeight).coerceAtLeast(0f).roundToInt())
    }

    // ---- follow mode ----
    var follow by remember { mutableStateOf(true) }
    // The last line at the screen bottom or above it, scrolled into the overscroll.
    val atBottom by remember(bottomPaddingPx) {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            last.index == info.totalItemsCount - 1 &&
                    last.offset + last.size <= info.viewportEndOffset - bottomPaddingPx + 8
        }
    }
    val dragged by state.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged, atBottom) {
        if (atBottom) follow = true
        else if (dragged) follow = false
    }
    // The side pill shrinks to its line number while the user scrolls; a tap opens it again.
    var navExpanded by remember { mutableStateOf(true) }
    LaunchedEffect(dragged) { if (dragged) navExpanded = false }
    // Once per published snapshot (throttled by the buffer), not per chunk. Jump instantly:
    // rapid output would keep cancelling an animated scroll.
    // Keyed on the list's own item count, not the buffer's: a new snapshot reaches this before
    // the list recomposes, and scrolling to an item it doesn't have yet lands on its old last
    // item, one batch short of the end, for good once the output stops. The version still
    // counts: an open line can grow (wrap) without adding items.
    LaunchedEffect(state, buffer) {
        snapshotFlow {
            Triple(state.layoutInfo.totalItemsCount, buffer.snapshot.version, follow && TerminalPrefs.autoScroll)
        }.collect { (total, _, following) ->
            if (following && total > 0) scrollToEnd()
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

    // ---- text selection ----
    // Anchored to buffer positions, not to composed lines (a SelectionContainer only knows the
    // lines on screen and breaks when its anchors scroll away): it survives any scroll, and Copy
    // reads every line in between from the buffer, composed or not.
    var selection by remember { mutableStateOf<TextSelection?>(null) }
    val registry = remember { LineRegistry() }
    val drag = remember { SelectionDrag(scope, state, registry) }
    val selectionColors = LocalTextSelectionColors.current
    SideEffect {
        with(density) {
            drag.topEdge = (contentPadding.calculateTopPadding() + 32.dp).toPx()
            drag.bottomInset = (contentPadding.calculateBottomPadding() + 32.dp).toPx()
        }
    }
    // Positions are reused after a clear or screen clear.
    LaunchedEffect(buffer) {
        snapshotFlow { buffer.snapshot.generation }.collect { selection = null }
    }
    BackHandler(enabled = selection != null) { selection = null }

    /** Starts dragging one end of the selection; the other end stays put. */
    fun beginHandleDrag(isStart: Boolean, finger: Offset): Offset? {
        val sel = selection ?: return null
        val mine = if (isStart) sel.start else sel.end
        val other = if (isStart) sel.end else sel.start
        val cursor = registry.cursorAt(mine) ?: return null
        drag.start(cursor.center) { selection = TextSelection(other, it) }
        // Keep the finger's distance to the cursor, so the hit point stays on the line itself.
        return cursor.center - finger
    }

    val context = LocalContext.current
    val copied = stringResource(R.string.copied)
    val truncatedText = stringResource(R.string.terminal_copy_truncated, COPY_LIMIT)
    fun copySelection() {
        val sel = selection ?: return
        val v = view
        val start = sel.start
        val end = sel.end
        var truncated = false
        val text = buildString {
            var first = true
            var i = v.indexOf(start.position)
            while (i < v.count) {
                val position = v.positionAt(i++)
                if (position > end.position) break
                val line = buffer.lineAt(position)?.text?.toAnsiString()?.text ?: continue
                if (!first) append('\n')
                first = false
                sel.rangeIn(position, line.length)?.let { append(line, it.start, it.end) }
                if (length >= COPY_LIMIT) {
                    setLength(COPY_LIMIT)
                    truncated = true
                    break
                }
            }
        }
        if (!ClipboardUtil.put(context, text)) return
        selection = null
        when {
            truncated -> Toast.makeText(context, truncatedText, Toast.LENGTH_LONG).show()
            // Android 13+ shows its own clipboard confirmation.
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
        }
    }

    // Always there once there is output, however short.
    val showScrollbar = view.count > 0
    // The track ends above the side pill, so the thumb at the bottom isn't under it.
    var pillHeight by remember { mutableIntStateOf(0) }

    Box(
        modifier
            .onPlaced { registry.container = it }
            .onSizeChanged { containerHeight = it.height }
    ) {
        LazyColumn(
            state = state,
            contentPadding = listPadding,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (wrap) Modifier
                    else Modifier.scrollable(panState, Orientation.Horizontal)
                )
                // A tap clears the selection; a long press is only there so it isn't a tap.
                .pointerInput(registry) {
                    detectTapGestures(onLongPress = {}, onTap = { selection = null })
                }
                // Long press selects the word under it, dragging on extends it.
                .pointerInput(registry) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val press = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                        val word = registry.anchorAt(press.position)?.let(registry::wordAt)
                            ?: return@awaitEachGesture
                        follow = false
                        selection = word
                        drag.start(press.position) {
                            selection = if (it < word.start) TextSelection(word.end, it)
                            else TextSelection(word.start, it)
                        }
                        // Ours from here, read before the list sees it so it doesn't scroll.
                        while (true) {
                            val change = awaitPointerEvent(PointerEventPass.Initial).changes
                                .firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (!change.pressed) break
                            drag.moveTo(change.position)
                        }
                        drag.stop()
                    }
                }
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
                if (line == null) {
                    // Gone before the next snapshot (clear, screen clear): keep one line's
                    // height. A zero-height item makes a scrolling list compose every
                    // remaining item in one frame to fill the viewport (ANR on big output).
                    BasicText("", style = style)
                    return@items
                }
                // Where this line is laid out, for mapping touches to columns.
                val entry = remember(position) { LineRegistry.Entry() }
                DisposableEffect(position) {
                    registry.lines[position] = entry
                    onDispose { if (registry.lines[position] === entry) registry.lines.remove(position) }
                }
                val mark = highlight?.takeIf { it.line == line.number }
                val currentStart = matches.getOrNull(current)
                    ?.takeIf { it.position == position }?.start ?: -1
                val parsed = remember(line.text) { line.text.toAnsiString() }
                val selected = selection?.rangeIn(position, parsed.length)
                val text = remember(parsed, mark, query, currentStart, selected) {
                    var t = parsed
                    if (mark?.column != null) t = t.withColumnMark(mark.column - 1, columnHighlight)
                    if (query.isNotEmpty()) {
                        t = t.withMatches(
                            query, matchHighlight, currentStart,
                            SpanStyle(background = currentHighlight, color = onCurrentHighlight)
                        )
                    }
                    if (selected != null) t = t.withSelection(selected, selectionColors.backgroundColor)
                    t
                }
                val marked = mark != null || currentStart >= 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (marked) Modifier.background(rowHighlight) else Modifier)
                ) {
                    if (showLineNumbers) {
                        BasicText(
                            text = line.number.toString(),
                            style = gutterStyle,
                            maxLines = 1,
                            modifier = Modifier.width(gutterWidth)
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    BasicText(
                        text = text,
                        style = style.copy(color = color(line.kind)),
                        softWrap = wrap,
                        onTextLayout = { entry.layout = it },
                        modifier = Modifier
                            .weight(1f)
                            .then(if (wrap) Modifier else Modifier.horizontalPan(pan))
                            // After the pan: these coordinates move with the text.
                            .onPlaced { entry.coords = it }
                    )
                }
            }
        }

        if (selection != null) {
            listOf(true, false).forEach { isStart ->
                var grab by remember { mutableStateOf(Offset.Zero) }
                SelectionHandle(
                    isStart = isStart,
                    color = selectionColors.handleColor,
                    container = { registry.container },
                    cursor = {
                        // Read while placing, so the handle follows scrolling and panning.
                        state.layoutInfo
                        pan.offset
                        selection?.let { registry.cursorAt(if (isStart) it.start else it.end) }
                    },
                    onDown = { finger -> beginHandleDrag(isStart, finger)?.let { grab = it } },
                    onMove = { finger -> drag.moveTo(finger + grab) },
                    onUp = { drag.stop() }
                )
            }
        }

        if (showScrollbar) {
            FastScrollbar(
                state = state,
                count = view.count,
                onDragStart = {
                    follow = false
                    navExpanded = false
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(
                        top = contentPadding.calculateTopPadding(),
                        bottom = maxOf(
                            contentPadding.calculateBottomPadding(),
                            jumpButtonPadding.calculateBottomPadding() +
                                    with(density) { pillHeight.toDp() } + 8.dp
                        )
                    )
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
            visible = selection != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = contentPadding.calculateTopPadding())
                .padding(horizontal = 12.dp)
        ) {
            TerminalSelectionBar(
                onCopy = { copySelection() },
                onSelectAll = {
                    val v = view
                    if (v.count > 0) {
                        selection = TextSelection(
                            Anchor(v.positionAt(0), 0),
                            Anchor(v.positionAt(v.count - 1), Int.MAX_VALUE)
                        )
                    }
                },
                onClose = { selection = null }
            )
        }

        // The selection bar takes its place while text is selected.
        AnimatedVisibility(
            visible = searchOpen && selection == null,
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
                .onSizeChanged { pillHeight = it.height }
        ) {
            TerminalNavPill(
                currentLine = currentLine,
                following = follow && TerminalPrefs.autoScroll,
                expanded = navExpanded,
                onExpand = { navExpanded = true },
                canScrollUp = state.canScrollBackward,
                onSearch = { searchOpen = true },
                onTop = {
                    follow = false
                    scope.launch { state.scrollToItem(0) }
                },
                onGoTo = { showGoTo = true },
                onBottom = {
                    follow = true
                    scope.launch { scrollToEnd() }
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

/** Matches kept per search; enough to step through, bounded for million-line output. */
private const val SEARCH_LIMIT = 10_000

/** Longest copy: the clipboard goes over binder, which fails past about 1 MB. */
private const val COPY_LIMIT = 200_000

/** Shown while text is selected: copy / select all / clear. */
@Composable
private fun TerminalSelectionBar(
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onClose: () -> Unit,
) {
    val content = MaterialTheme.colorScheme.onSecondaryContainer
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = content,
        shadowElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(44.dp)
                .padding(horizontal = 2.dp)
        ) {
            TextButton(onClick = onCopy) {
                Icon(Icons.Outlined.ContentCopy, null, tint = content, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.copy), color = content)
            }
            TextButton(onClick = onSelectAll) {
                Icon(Icons.Filled.SelectAll, null, tint = content, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.terminal_select_all), color = content)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
            }
        }
    }
}

/**
 * One end of the selection, hanging under its cursor; dragged to move that end. Not placed
 * while its line is off screen (the selection itself stays).
 */
@Composable
private fun SelectionHandle(
    isStart: Boolean,
    color: Color,
    container: () -> LayoutCoordinates?,
    /** The cursor of this end in container coordinates; read while placing. */
    cursor: () -> Rect?,
    onDown: (finger: Offset) -> Unit,
    onMove: (finger: Offset) -> Unit,
    onUp: () -> Unit,
) {
    val down by rememberUpdatedState(onDown)
    val move by rememberUpdatedState(onMove)
    val up by rememberUpdatedState(onUp)
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // The point sits at the cursor: top-end corner for the start, top-start for the end.
    val shape = if (isStart) RoundedCornerShape(50, 0, 50, 50) else RoundedCornerShape(0, 50, 50, 50)

    fun toContainer(local: Offset): Offset? {
        val box = container()?.takeIf { it.isAttached } ?: return null
        val self = coords?.takeIf { it.isAttached } ?: return null
        return box.localPositionOf(self, local)
    }

    Box(
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(placeable.width, placeable.height) {
                    val at = cursor() ?: return@layout
                    val bounds = container()?.size ?: return@layout
                    if (at.bottom < 0 || at.bottom > bounds.height || at.left < 0 || at.left > bounds.width) {
                        return@layout
                    }
                    val x = if (isStart) at.left - placeable.width else at.left
                    placeable.place(x.roundToInt(), at.bottom.roundToInt())
                }
            }
            .onPlaced { coords = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val first = awaitFirstDown()
                    first.consume()
                    toContainer(first.position)?.let(down)
                    drag(first.id) { change ->
                        change.consume()
                        toContainer(change.position)?.let(move)
                    }
                    up()
                }
            }
            .size(22.dp)
            .background(color, shape)
    )
}

/** A column in a line; [offset] counts the text without ANSI codes, as it is laid out. */
@Immutable
private data class Anchor(val position: Long, val offset: Int) : Comparable<Anchor> {
    override fun compareTo(other: Anchor): Int =
        compareValuesBy(this, other, { it.position }, { it.offset })
}

/** From [fixed] to [moving], in either order: dragging one end past the other just flips it. */
@Immutable
private data class TextSelection(val fixed: Anchor, val moving: Anchor) {
    val start: Anchor get() = minOf(fixed, moving)
    val end: Anchor get() = maxOf(fixed, moving)

    /** The selected part of the line at [position] of [length] characters, if any. */
    fun rangeIn(position: Long, length: Int): TextRange? {
        val s = start
        val e = end
        if (position < s.position || position > e.position) return null
        val from = if (position == s.position) s.offset.coerceIn(0, length) else 0
        val to = if (position == e.position) e.offset.coerceIn(0, length) else length
        return if (from < to) TextRange(from, to) else null
    }
}

/** Where the composed lines are laid out, so a touch point maps to a buffer position and column. */
private class LineRegistry {
    class Entry {
        var coords: LayoutCoordinates? = null
        var layout: TextLayoutResult? = null
    }

    /** Composed lines only, by buffer position. */
    val lines = HashMap<Long, Entry>()
    var container: LayoutCoordinates? = null

    /** The column under [point] (container coordinates) in the nearest composed line. */
    fun anchorAt(point: Offset): Anchor? {
        val box = container?.takeIf { it.isAttached } ?: return null
        var best: Anchor? = null
        var bestDistance = Float.MAX_VALUE
        for ((position, entry) in lines) {
            val coords = entry.coords?.takeIf { it.isAttached } ?: continue
            val layout = entry.layout ?: continue
            val local = coords.localPositionOf(box, point)
            val height = coords.size.height.toFloat()
            val distance = when {
                local.y < 0 -> -local.y
                local.y > height -> local.y - height
                else -> 0f
            }
            if (distance < bestDistance) {
                bestDistance = distance
                val y = local.y.coerceIn(0f, (height - 1).coerceAtLeast(0f))
                best = Anchor(position, layout.getOffsetForPosition(Offset(local.x, y)))
            }
        }
        return best
    }

    /** The cursor at [anchor] in container coordinates, or null when its line isn't composed. */
    fun cursorAt(anchor: Anchor): Rect? {
        val box = container?.takeIf { it.isAttached } ?: return null
        val entry = lines[anchor.position] ?: return null
        val coords = entry.coords?.takeIf { it.isAttached } ?: return null
        val layout = entry.layout ?: return null
        val rect = layout.getCursorRect(anchor.offset.coerceIn(0, layout.layoutInput.text.length))
        return Rect(box.localPositionOf(coords, rect.topLeft), rect.size)
    }

    /** The word at [anchor], or the one character there when it isn't in a word. */
    fun wordAt(anchor: Anchor): TextSelection {
        val layout = lines[anchor.position]?.layout
        val length = layout?.layoutInput?.text?.length ?: 0
        if (layout == null || length == 0) return TextSelection(anchor, anchor)
        val word = layout.getWordBoundary(anchor.offset.coerceIn(0, length - 1))
        val start = word.start
        val end = if (word.end > start) word.end else (start + 1).coerceAtMost(length)
        return TextSelection(Anchor(anchor.position, start), Anchor(anchor.position, end))
    }
}

/**
 * Moves one end of the selection to the finger, scrolling the list while the finger rests near
 * its top or bottom edge, so a selection can run past the screen.
 */
private class SelectionDrag(
    private val scope: CoroutineScope,
    private val state: LazyListState,
    private val registry: LineRegistry,
) {
    /** Scrolling starts above [topEdge] and within [bottomInset] of the bottom, in px. */
    var topEdge = 0f
    var bottomInset = 0f

    private var point: Offset? = null
    private var mover: ((Anchor) -> Unit)? = null
    private var job: Job? = null

    fun start(at: Offset, move: (Anchor) -> Unit) {
        point = at
        mover = move
        job?.cancel()
        job = scope.launch {
            while (true) {
                withFrameNanos { }
                val p = point ?: break
                val height = registry.container?.size?.height ?: break
                val bottomEdge = height - bottomInset
                val over = when {
                    p.y < topEdge -> p.y - topEdge
                    p.y > bottomEdge -> p.y - bottomEdge
                    else -> 0f
                }
                if (over != 0f) {
                    state.scrollBy((over / 2).coerceIn(-MAX_STEP, MAX_STEP))
                    update()
                }
            }
        }
    }

    fun moveTo(at: Offset) {
        point = at
        update()
    }

    fun stop() {
        point = null
        mover = null
        job?.cancel()
        job = null
    }

    private fun update() {
        val p = point ?: return
        registry.anchorAt(p)?.let { mover?.invoke(it) }
    }

    private companion object {
        /** Fastest auto-scroll, px per frame. */
        const val MAX_STEP = 60f
    }
}

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
    expanded: Boolean,
    onExpand: () -> Unit,
    canScrollUp: Boolean,
    onSearch: () -> Unit,
    onTop: () -> Unit,
    onGoTo: () -> Unit,
    onBottom: () -> Unit,
) {
    Surface(
        shape = AxPanelShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 3.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(visible = expanded) {
                Column {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.terminal_search))
                    }
                    IconButton(onClick = onTop, enabled = canScrollUp) {
                        Icon(
                            Icons.Filled.KeyboardDoubleArrowUp,
                            contentDescription = stringResource(R.string.terminal_scroll_top)
                        )
                    }
                }
            }
            // Collapsed, the line number is all that is left and a tap brings the buttons back.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = if (expanded) 0.dp else 4.dp)
                    .clip(AxPanelShape)
                    .clickable(onClick = if (expanded) onGoTo else onExpand)
                    .padding(horizontal = 6.dp, vertical = if (expanded) 2.dp else 6.dp)
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
            AnimatedVisibility(visible = expanded) {
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

private fun AnnotatedString.withSelection(range: TextRange, color: Color): AnnotatedString {
    val source = this
    return buildAnnotatedString {
        append(source)
        addStyle(SpanStyle(background = color), range.start, range.end)
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
