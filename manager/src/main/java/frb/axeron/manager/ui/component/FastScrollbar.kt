package frb.axeron.manager.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ThumbHeight = 44.dp
private val TrackWidth = 22.dp
private val BubbleSize = 52.dp

/**
 * Draggable thumb on the right edge of a list; maps the touch position straight to an item.
 *
 * @param label text for the item under the thumb (e.g. the first letter of a sorted list),
 * shown in a bubble beside the thumb while dragging. Null shows no bubble.
 */
@Composable
fun FastScrollbar(
    state: LazyListState,
    count: Int,
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit = {},
    label: ((index: Int) -> String)? = null,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbPx = with(density) { ThumbHeight.toPx() }
    val bubblePx = with(density) { BubbleSize.toPx() }
    var trackPx by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val currentCount by rememberUpdatedState(count)
    val currentOnDragStart by rememberUpdatedState(onDragStart)

    val scrollFraction by remember(count) {
        derivedStateOf {
            val visible = state.layoutInfo.visibleItemsInfo.size
            val max = (count - visible).coerceAtLeast(1)
            (state.firstVisibleItemIndex.toFloat() / max).coerceIn(0f, 1f)
        }
    }
    val fraction = if (dragging) dragFraction else scrollFraction
    val active = dragging || state.isScrollInProgress
    val alpha by animateFloatAsState(if (active) 0.9f else 0.35f, label = "thumbAlpha")
    val width by animateDpAsState(if (dragging) 8.dp else 4.dp, label = "thumbWidth")
    val thumbColor = MaterialTheme.colorScheme.primary
    val thumbTop = fraction * (trackPx - thumbPx).coerceAtLeast(0f)

    // No pointer input on the outer box: touches beside the track reach the list below.
    Box(modifier.fillMaxHeight()) {
        if (label != null && count > 0) {
            val index = (dragFraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
            AnimatedVisibility(
                visible = dragging,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(
                            -TrackWidth.roundToPx() - 4.dp.roundToPx(),
                            (thumbTop + thumbPx / 2 - bubblePx / 2).roundToInt()
                                .coerceIn(0, (trackPx - bubblePx).roundToInt().coerceAtLeast(0))
                        )
                    }
            ) {
                Surface(
                    modifier = Modifier.size(BubbleSize),
                    shape = AxScrollbarBubbleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 3.dp,
                    shadowElevation = 3.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label(index),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(TrackWidth)
                .onSizeChanged { trackPx = it.height }
                // Not keyed on count: streaming output changes it every batch, and a restart
                // cancels the drag in progress.
                .pointerInput(Unit) {
                    fun jump(y: Float) {
                        val range = (trackPx - thumbPx).coerceAtLeast(1f)
                        dragFraction = ((y - thumbPx / 2) / range).coerceIn(0f, 1f)
                        val target = (dragFraction * (currentCount - 1)).roundToInt().coerceAtLeast(0)
                        scope.launch { state.scrollToItem(target) }
                    }
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragging = true
                            currentOnDragStart()
                            jump(it.y)
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false }
                    ) { change, _ ->
                        change.consume()
                        jump(change.position.y)
                    }
                }
        ) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, thumbTop.roundToInt()) }
                    .padding(end = 4.dp)
                    .width(width)
                    .height(ThumbHeight)
                    .clip(CircleShape)
                    .background(thumbColor.copy(alpha = alpha))
            )
        }
    }
}
