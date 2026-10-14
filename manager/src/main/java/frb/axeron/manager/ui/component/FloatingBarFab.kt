package frb.axeron.manager.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * FABs live next to the floating nav pill (rendered by AxActivity), not in each screen's Scaffold,
 * so they always sit on the same baseline as the pill. Screens register theirs with [FloatingBarFab].
 */
@Stable
class FloatingBarFabHost {
    internal class Entry(content: @Composable ColumnScope.() -> Unit, visible: Boolean) {
        var content by mutableStateOf(content)
        var visible by mutableStateOf(visible)
    }

    private val entries = mutableStateListOf<Entry>()

    /** During a navigation transition both screens are composed; the most recent one wins. */
    internal val current: Entry? get() = entries.lastOrNull()

    internal fun add(entry: Entry) = entries.add(entry)
    internal fun remove(entry: Entry) = entries.remove(entry)
}

val LocalFloatingBarFabHost = staticCompositionLocalOf<FloatingBarFabHost?> { null }

/**
 * Shows [content] beside the floating nav pill while this composable is in composition.
 * Items are laid out top to bottom; the last one lines up with the pill, extra ones stack above it.
 * [visible] hides the buttons (e.g. while scrolling) but keeps their slot so the pill does not shift.
 */
@Composable
fun FloatingBarFab(
    visible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val host = LocalFloatingBarFabHost.current ?: return
    val entry = remember { FloatingBarFabHost.Entry(content, visible) }

    SideEffect {
        entry.content = content
        entry.visible = visible
    }

    DisposableEffect(host) {
        host.add(entry)
        onDispose { host.remove(entry) }
    }
}

/**
 * Rendered by AxActivity to the right of the nav pill. Switching pages (FAB appears, disappears
 * or changes) animates the slot's width, so the centered pill glides instead of jumping; content
 * is pinned to the bottom so a taller stack only grows upward.
 */
@Composable
internal fun FloatingBarFabSlot(host: FloatingBarFabHost) {
    AnimatedContent(
        targetState = host.current,
        transitionSpec = {
            (fadeIn(tween(200, delayMillis = 60)) +
                    scaleIn(tween(200, delayMillis = 60), initialScale = 0.85f))
                .togetherWith(fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.85f))
                .using(SizeTransform(clip = false))
        },
        contentAlignment = Alignment.BottomStart,
        label = "floatingBarFab"
    ) { entry ->
        // No entry: emit nothing, so the slot (and its gap) collapses to zero width.
        if (entry != null) FabStack(entry)
    }
}

@Composable
private fun FabStack(entry: FloatingBarFabHost.Entry) {
    // The gap lives inside the slot so it animates away with it. One FAB-sized minimum is
    // reserved so hiding the buttons on scroll never moves the pill.
    Box(
        modifier = Modifier
            .padding(start = 12.dp)
            .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = entry.visible,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = entry.content
            )
        }
    }
}
