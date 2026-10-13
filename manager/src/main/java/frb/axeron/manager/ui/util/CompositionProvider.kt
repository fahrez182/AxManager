package frb.axeron.manager.ui.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> {
    error("CompositionLocal LocalSnackbarController not present")
}

/**
 * Height the floating bottom bar (nav pill plus any FABs beside it) occupies above the system
 * navigation bar, 0 when hidden. Screens add it to their bottom content padding.
 */
val LocalFloatingBarInset = compositionLocalOf<Dp> { 0.dp }
