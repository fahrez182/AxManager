package frb.axeron.manager.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/**
 * A thin bar over the content showing where [state] is scrolled to and how much there is, for
 * text that scrolls inside its field (nothing shows that on its own). Only drawn while there
 * is something to scroll. [orientation] Horizontal puts it along the bottom edge, for a single
 * line that scrolls sideways.
 */
fun Modifier.scrollIndicator(
    state: ScrollState,
    color: Color,
    orientation: Orientation = Orientation.Vertical,
    inset: Dp = 14.dp,
    edge: Dp = 4.dp
): Modifier = drawWithContent {
    drawContent()
    val max = state.maxValue.toFloat()
    if (max <= 0f) return@drawWithContent
    val viewport = state.viewportSize.toFloat()
    val thickness = 3.dp.toPx()
    val minThumb = 24.dp.toPx()
    val start = inset.toPx()
    // A one-line field can have a track shorter than minThumb: the thumb then fills it.
    if (orientation == Orientation.Vertical) {
        val track = size.height - 2 * start
        if (track <= 0f) return@drawWithContent
        val thumb = (track * viewport / (viewport + max)).coerceIn(minOf(minThumb, track), track)
        val top = start + (track - thumb) * (state.value / max)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - edge.toPx() - thickness, top),
            size = Size(thickness, thumb),
            cornerRadius = CornerRadius(thickness / 2)
        )
    } else {
        val track = size.width - 2 * start
        if (track <= 0f) return@drawWithContent
        val thumb = (track * viewport / (viewport + max)).coerceIn(minOf(minThumb, track), track)
        val left = start + (track - thumb) * (state.value / max)
        drawRoundRect(
            color = color,
            topLeft = Offset(left, size.height - edge.toPx() - thickness),
            size = Size(thumb, thickness),
            cornerRadius = CornerRadius(thickness / 2)
        )
    }
}

/** Takes whatever scroll or fling the content didn't use, so none of it reaches the parents. */
private object ContainedScroll : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = available
    override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
}

/**
 * Keeps a scrolling child's leftover scroll and fling to itself. Without it, scrolling a text
 * field to its end hands the rest to the parent: inside a bottom sheet, the sheet itself moves.
 */
fun Modifier.containScroll(): Modifier = nestedScroll(ContainedScroll)
