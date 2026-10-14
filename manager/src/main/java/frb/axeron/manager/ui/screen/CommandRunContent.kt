package frb.axeron.manager.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.StatusPill
import frb.axeron.manager.ui.component.TerminalOutput
import frb.axeron.manager.ui.component.axPanel
import frb.axeron.manager.ui.component.terminalTextStyle
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel.State
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel.Stream
import kotlinx.coroutines.delay
import java.util.Locale

/** Status header for a command screen: state, what is running, duration and exit code. */
@Composable
fun CommandRunStatusCard(
    runner: CommandRunViewModel,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val state = runner.state
    val error = MaterialTheme.colorScheme.error
    val primary = MaterialTheme.colorScheme.primary

    // Ticks while running; frozen once finished.
    val elapsed by produceState(runner.elapsedMillis(), state) {
        value = runner.elapsedMillis()
        while (state == State.Running) {
            delay(1000)
            value = runner.elapsedMillis()
        }
    }

    AxCard(modifier) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                state is State.Finished && state.success ->
                    IconBadge(imageVector = Icons.Filled.CheckCircle)

                state is State.Finished ->
                    IconBadge(imageVector = Icons.Outlined.ErrorOutline, tint = error)

                else -> IconBadge(imageVector = Icons.Outlined.Terminal)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        when {
                            state is State.Finished && state.success -> R.string.command_success
                            state is State.Finished -> R.string.command_failed
                            else -> R.string.command_running
                        }
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (state == State.Idle) subtitle
                    else "$subtitle · ${formatDuration(elapsed)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            when (state) {
                is State.Finished -> StatusPill(
                    text = stringResource(R.string.command_exit_code, state.exitCode),
                    color = if (state.success) primary else error
                )

                else -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

/**
 * Output panel for a [CommandRunViewModel]. stderr is shown only when [showStderr] (developer
 * options); app notices are always shown.
 */
@Composable
fun CommandRunOutput(
    runner: CommandRunViewModel,
    modifier: Modifier = Modifier,
    wrap: Boolean = true,
    showStderr: Boolean = true,
    bottomPadding: androidx.compose.ui.unit.Dp = 12.dp
) {
    val filter = remember(showStderr) {
        if (showStderr) null else { stream: Stream -> stream != Stream.STDERR }
    }
    Box(modifier.axPanel(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        TerminalOutput(
            buffer = runner.terminal,
            wrap = wrap,
            filter = filter,
            color = { it.color() },
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = bottomPadding),
            jumpButtonPadding = PaddingValues(bottom = (bottomPadding - 12.dp).coerceAtLeast(0.dp)),
            modifier = Modifier.fillMaxSize()
        )
        if (runner.terminal.isEmpty && runner.isRunning) {
            Text(
                text = stringResource(R.string.command_waiting_output),
                style = terminalTextStyle(),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun Stream.color(): Color = when (this) {
    Stream.STDOUT -> MaterialTheme.colorScheme.onSurfaceVariant
    Stream.STDERR -> MaterialTheme.colorScheme.error
    Stream.NOTICE -> MaterialTheme.colorScheme.primary
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    return when {
        seconds < 60 -> String.format(Locale.ROOT, "%.1fs", ms / 1000f)
        seconds < 3600 -> String.format(Locale.ROOT, "%dm %02ds", seconds / 60, seconds % 60)
        else -> String.format(Locale.ROOT, "%dh %02dm", seconds / 3600, (seconds % 3600) / 60)
    }
}
