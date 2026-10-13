package frb.axeron.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * App-wide top bar: never switches to the tinted "scrolled" color. Instead it sits on a
 * solid-to-transparent gradient so content scrolling underneath fades out softly.
 * Screens let their scroll content pass under it by padding the top inside the scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val background = MaterialTheme.colorScheme.background
    TopAppBar(
        title = title,
        modifier = modifier.background(
            Brush.verticalGradient(
                0f to background,
                0.55f to background.copy(alpha = 0.9f),
                1f to background.copy(alpha = 0f)
            )
        ),
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        scrollBehavior = scrollBehavior
    )
}

/**
 * Pull-to-refresh for lists that scroll under [AxTopAppBar]: the box spans the whole screen,
 * so the indicator is pushed down by [topInset] to appear below the bar instead of behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    topInset: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInset)
            )
        },
        content = content
    )
}

/** Matching fade for the bottom edge, behind the floating nav pill. */
fun bottomBarFadeBrush(background: Color): Brush = Brush.verticalGradient(
    0f to background.copy(alpha = 0f),
    0.5f to background.copy(alpha = 0.7f),
    1f to background.copy(alpha = 0.95f)
)
