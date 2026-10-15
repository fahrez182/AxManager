package frb.axeron.manager.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.TerminalOutput
import frb.axeron.manager.ui.component.terminalTextStyle
import frb.axeron.manager.ui.theme.GREEN
import frb.axeron.manager.ui.theme.ORANGE
import frb.axeron.manager.ui.theme.RED
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel.State
import frb.axeron.manager.ui.viewmodel.CommandRunViewModel.Stream
import kotlinx.coroutines.delay
import java.util.Locale

/** Top bar title for a command screen: state, then what is running, duration and exit code. */
@Composable
fun CommandRunTitle(
    runner: CommandRunViewModel,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val state = runner.state

    // Ticks while running; frozen once finished.
    val elapsed by produceState(runner.elapsedMillis(), state) {
        value = runner.elapsedMillis()
        while (state == State.Running) {
            delay(1000)
            value = runner.elapsedMillis()
        }
    }

    val exitCode = (state as? State.Finished)
        ?.let { stringResource(R.string.command_exit_code, it.exitCode) }

    Column(modifier) {
        Text(
            text = stringResource(
                when {
                    state == State.Idle -> R.string.idle
                    state is State.Finished && state.success -> R.string.command_success
                    state is State.Finished -> R.string.command_failed
                    else -> R.string.command_running
                }
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = when {
                state == State.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
                state is State.Finished && state.success -> GREEN
                state is State.Finished -> RED
                else -> ORANGE
            }
        )
        Text(
            text = buildString {
                append(subtitle)
                if (state != State.Idle) append(" · ").append(formatDuration(elapsed))
                if (exitCode != null) append(" · ").append(exitCode)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Output of a [CommandRunViewModel], edge to edge and never wrapped. stderr is shown only when
 * [showStderr] (developer options); app notices are always shown.
 */
@Composable
fun CommandRunOutput(
    runner: CommandRunViewModel,
    modifier: Modifier = Modifier,
    showStderr: Boolean = true,
    bottomPadding: Dp = 16.dp
) {
    val filter = remember(showStderr) {
        if (showStderr) null else { stream: Stream -> stream != Stream.STDERR }
    }
    Box(modifier) {
        TerminalOutput(
            buffer = runner.terminal,
            wrap = false,
            filter = filter,
            color = { it.color() },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
            jumpButtonPadding = PaddingValues(bottom = (bottomPadding - 12.dp).coerceAtLeast(0.dp)),
            modifier = Modifier.fillMaxSize()
        )
        if (runner.terminal.isEmpty && runner.isRunning) {
            Text(
                text = stringResource(R.string.command_waiting_output),
                style = terminalTextStyle(),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
