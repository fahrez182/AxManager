package frb.axeron.manager.ui.screen

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.adb.ActivationLog
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.containScroll
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.StatusPill
import frb.axeron.manager.ui.component.axPanel
import frb.axeron.manager.ui.util.ClipboardUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ActivationStatus {
    data object Running : ActivationStatus
    data class Success(val message: String) : ActivationStatus
    data class Failed(val message: String) : ActivationStatus
}

/**
 * Runs one activation attempt and shows its live output in [ActivationOutputSheet] — replaces
 * the old static loading dialog, so a failure shows *where* and *why* (steps, starter/su output,
 * exception chain) instead of only a toast.
 */
@Stable
class ActivationRunner internal constructor(private val scope: CoroutineScope) {
    var visible by mutableStateOf(false)
        private set
    var title by mutableStateOf("")
        private set
    var status by mutableStateOf<ActivationStatus>(ActivationStatus.Running)
        private set

    /** Seconds left before a successful attempt closes the sheet by itself; null when not counting. */
    var autoCloseIn by mutableStateOf<Int?>(null)
        private set

    private var autoCloseJob: Job? = null

    private var attemptJob: Job? = null

    val isRunning: Boolean get() = visible && status is ActivationStatus.Running

    fun run(title: String, block: suspend () -> ActivationStatus) {
        if (isRunning) return
        autoCloseJob?.cancel()
        // One hidden with the sheet may still wait (for a pairing, minutes): it must not
        // answer for this one.
        attemptJob?.cancel()
        autoCloseIn = null
        this.title = title
        ActivationLog.begin(title)
        status = ActivationStatus.Running
        visible = true
        attemptJob = scope.launch {
            status = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                ActivationLog.error("Unexpected error", e)
                ActivationStatus.Failed(e.message ?: e.javaClass.simpleName)
            }
            // Success: leave the log up a moment to read, then close. Failures stay open.
            if (status is ActivationStatus.Success) startAutoClose()
        }
    }

    private fun startAutoClose() {
        autoCloseJob = scope.launch {
            for (seconds in AUTO_CLOSE_SECONDS downTo 1) {
                autoCloseIn = seconds
                delay(1000)
            }
            autoCloseIn = null
            visible = false
        }
    }

    /** Hides the sheet; a still-running attempt keeps going and its log keeps filling. */
    fun dismiss() {
        autoCloseJob?.cancel()
        autoCloseIn = null
        visible = false
    }

    private companion object {
        const val AUTO_CLOSE_SECONDS = 3
    }
}

@Composable
fun rememberActivationRunner(): ActivationRunner {
    val scope = rememberCoroutineScope()
    val runner = remember { ActivationRunner(scope) }
    if (runner.visible) {
        ActivationOutputSheet(runner)
    }
    return runner
}

@Composable
private fun ActivationOutputSheet(runner: ActivationRunner) {
    val lines by ActivationLog.lines.collectAsState()
    val status = runner.status
    val errorCount = lines.count { it.level == ActivationLog.Level.ERROR }

    AxOptionsSheet(
        title = runner.title,
        onDismissRequest = runner::dismiss
    ) {
        ActivationStatusCard(status, errorCount, runner.autoCloseIn)

        ActivationLogPanel(lines = lines, modifier = Modifier.heightIn(max = 320.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CopyLogButton(lines = lines, modifier = Modifier.weight(1f))
            Button(modifier = Modifier.weight(1f), onClick = runner::dismiss) {
                val countdown = runner.autoCloseIn
                Text(
                    if (countdown != null) "${stringResource(R.string.close)} ($countdown)"
                    else stringResource(R.string.close)
                )
            }
        }
    }
}

/** Copies [lines] as plain text. Shared by the activation output sheet and the OTG sheet. */
@Composable
fun CopyLogButton(lines: List<ActivationLog.Line>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val copied = stringResource(R.string.copied)
    FilledTonalButton(
        modifier = modifier,
        onClick = {
            // Android 13+ shows its own clipboard confirmation.
            if (ClipboardUtil.put(context, ActivationLog.asText(lines)) &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.activation_copy_log))
    }
}

@Composable
private fun ActivationStatusCard(status: ActivationStatus, errorCount: Int, autoCloseIn: Int?) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    AxCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (status) {
                ActivationStatus.Running -> IconBadge(imageVector = Icons.Outlined.Terminal)
                is ActivationStatus.Success -> IconBadge(imageVector = Icons.Filled.CheckCircle)
                is ActivationStatus.Failed -> IconBadge(imageVector = Icons.Outlined.ErrorOutline, tint = error)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        when (status) {
                            ActivationStatus.Running -> R.string.activation_running
                            is ActivationStatus.Success -> R.string.activation_success
                            is ActivationStatus.Failed -> R.string.activation_failed
                        }
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val detail = when (status) {
                    ActivationStatus.Running -> null
                    is ActivationStatus.Success -> status.message
                    is ActivationStatus.Failed -> status.message
                }
                if (!detail.isNullOrBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (autoCloseIn != null) {
                    Text(
                        text = stringResource(R.string.activation_auto_close, autoCloseIn),
                        style = MaterialTheme.typography.labelSmall,
                        color = primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                status is ActivationStatus.Running ->
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)

                // Errors on the way (a wireless attempt before pairing) don't undo a success.
                status is ActivationStatus.Success -> StatusPill(text = "OK", color = primary)

                errorCount > 0 -> StatusPill(
                    text = stringResource(R.string.activation_error_count, errorCount),
                    color = error
                )

                else -> StatusPill(text = "OK", color = primary)
            }
        }
    }
}

/**
 * Timestamped, level-coloured log lines (selectable), newest kept in view. Shared by the
 * activation output sheet and the OTG sheet.
 */
@Composable
fun ActivationLogPanel(
    lines: List<ActivationLog.Line>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    val colors = MaterialTheme.colorScheme
    fun colorOf(level: ActivationLog.Level): Color = when (level) {
        ActivationLog.Level.ERROR -> colors.error
        ActivationLog.Level.WARN -> colors.tertiary
        ActivationLog.Level.SUCCESS -> colors.primary
        ActivationLog.Level.OUTPUT -> colors.onSurface
        ActivationLog.Level.INFO -> colors.onSurfaceVariant
    }

    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxWidth()
                .containScroll()
                .axPanel(colors.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(lines) { _, line ->
                Row {
                    Text(
                        text = timeFormat.format(Date(line.time)),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.outline
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (line.level) {
                            ActivationLog.Level.ERROR -> "✗ "
                            ActivationLog.Level.WARN -> "! "
                            ActivationLog.Level.SUCCESS -> "✓ "
                            ActivationLog.Level.OUTPUT -> "│ "
                            ActivationLog.Level.INFO -> "• "
                        } + line.text,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (line.level == ActivationLog.Level.ERROR) FontWeight.SemiBold else null,
                        color = colorOf(line.level)
                    )
                }
            }
        }
    }
}
