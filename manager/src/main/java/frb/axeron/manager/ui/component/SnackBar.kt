package frb.axeron.manager.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import frb.axeron.manager.ui.util.LocalFloatingBarInset

/**
 * The app's snackbar host. It sits above the floating nav pill (the activity draws that pill
 * over every screen, so Scaffold's own system-bar inset alone left snackbars under it).
 */
@Composable
fun AxSnackBarHost(
    hostState: SnackbarHostState
) {
    SnackbarHost(
        hostState = hostState,
        modifier = Modifier.padding(bottom = LocalFloatingBarInset.current)
    ) { data -> AxSnackbar(data) }
}

/**
 * High-contrast card: icon badge, message, optional action and dismiss, and a thin line along
 * the bottom that runs down for as long as the snackbar stays up.
 */
@Composable
private fun AxSnackbar(data: SnackbarData) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.inversePrimary
    // How long the host keeps it up (Material's own timings); indefinite has no countdown.
    val durationMillis = when (data.visuals.duration) {
        SnackbarDuration.Short -> 4_000
        SnackbarDuration.Long -> 10_000
        SnackbarDuration.Indefinite -> null
    }
    val left = remember(data) { Animatable(1f) }
    if (durationMillis != null) {
        LaunchedEffect(data) { left.animateTo(0f, tween(durationMillis, easing = LinearEasing)) }
    }

    Surface(
        shape = AxCardShape,
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)
            ) {
                Surface(shape = CircleShape, color = accent.copy(alpha = 0.18f), modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    modifier = Modifier.weight(1f)
                )
                data.visuals.actionLabel?.let { label ->
                    TextButton(
                        onClick = { data.performAction() },
                        colors = ButtonDefaults.textButtonColors(contentColor = accent)
                    ) {
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
                if (data.visuals.withDismissAction) {
                    IconButton(onClick = { data.dismiss() }) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                } else if (data.visuals.actionLabel == null) {
                    Spacer(Modifier.width(8.dp))
                }
            }
            if (durationMillis != null) {
                LinearProgressIndicator(
                    progress = { left.value },
                    color = accent,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                )
            }
        }
    }
}
