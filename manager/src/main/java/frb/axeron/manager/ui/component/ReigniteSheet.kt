package frb.axeron.manager.ui.component

import android.os.SystemClock
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import frb.axeron.api.AxeronPluginService
import frb.axeron.manager.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Runs a step-by-step action (an ignite, a server shutdown or restart) in a sheet that shows,
 * line by line, what it is doing, with Cancel while it runs.
 */
interface ReigniteSheetHandle {
    /**
     * Opens the sheet and runs the igniter; true when it succeeded. Returns once it is done; the
     * sheet stays open on its log until the user closes it.
     */
    suspend fun reignite(): Boolean

    /** As [reignite], for any action: [block] writes the log and says when Cancel may be used. */
    suspend fun run(title: String, block: suspend (ActionLog) -> Boolean): Boolean
}

/** What an action run in the sheet writes to; callable from any thread. */
interface ActionLog {
    /** Output text; each line of it becomes a log line. */
    fun out(text: String)
    fun err(text: String)

    /**
     * Whether Cancel may be used now: off for a step that must not be cut short (a server being
     * replaced, which a stop halfway would leave with none). On at the start of a run.
     */
    fun setCancellable(cancellable: Boolean)
}

/** One output line, [atMillis] after the run started. */
private class LogLine(val atMillis: Long, val text: String, val error: Boolean)

private enum class RunState { RUNNING, SUCCEEDED, FAILED }

private class ReigniteSheetHandleImpl(
    private val visible: MutableState<Boolean>,
    private val title: MutableState<String>,
    private val igniteTitle: () -> String,
    private val state: MutableState<RunState>,
    private val cancellable: MutableState<Boolean>,
    private val startedAt: MutableState<Long>,
    private val lines: SnapshotStateList<LogLine>,
) : ReigniteSheetHandle, ActionLog {

    /** The action running now, for [cancel]. */
    @Volatile
    private var current: Job? = null

    override suspend fun reignite(): Boolean = run(igniteTitle()) { log ->
        AxeronPluginService.igniteSuspendService(onStdout = log::out, onStderr = log::err)
    }

    override suspend fun run(title: String, block: suspend (ActionLog) -> Boolean): Boolean {
        withContext(Dispatchers.Main) {
            lines.clear()
            this@ReigniteSheetHandleImpl.title.value = title
            startedAt.value = SystemClock.elapsedRealtime()
            cancellable.value = true
            state.value = RunState.RUNNING
            visible.value = true
        }
        // A supervisor: a failed or cancelled run ends here, not in the caller's coroutine.
        val ok = supervisorScope {
            val job = async(Dispatchers.IO) { block(this@ReigniteSheetHandleImpl) }
            current = job
            try {
                job.await()
            } catch (e: CancellationException) {
                ensureActive() // the caller itself was cancelled: let it go
                add("! Cancelled", error = true)
                false
            } catch (e: Throwable) {
                add(e.message ?: e.javaClass.simpleName, error = true)
                false
            } finally {
                current = null
            }
        }
        withContext(Dispatchers.Main) {
            state.value = if (ok) RunState.SUCCEEDED else RunState.FAILED
        }
        return ok
    }

    /** Stops the running action (execProcessSafe kills its process); [run] returns false. */
    fun cancel() {
        if (cancellable.value) current?.cancel()
    }

    override fun out(text: String) = add(text, error = false)

    override fun err(text: String) = add(text, error = true)

    override fun setCancellable(cancellable: Boolean) {
        mainHandler.post { this.cancellable.value = cancellable }
    }

    // Callbacks come from the readers' threads; the list is changed on the main thread only.
    private fun add(chunk: String, error: Boolean) {
        val at = SystemClock.elapsedRealtime() - startedAt.value
        val texts = chunk.trimEnd('\n').split('\n')
        mainHandler.post { texts.forEach { lines.add(LogLine(at, it, error)) } }
    }

    private companion object {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    }
}

@Composable
fun rememberReigniteSheet(): ReigniteSheetHandle {
    val visible = remember { mutableStateOf(false) }
    val title = remember { mutableStateOf("") }
    val state = remember { mutableStateOf(RunState.RUNNING) }
    val cancellable = remember { mutableStateOf(true) }
    val startedAt = remember { mutableStateOf(0L) }
    val lines = remember { mutableStateListOf<LogLine>() }
    val igniteTitle = stringResource(R.string.ignite)
    val currentIgniteTitle by rememberUpdatedState(igniteTitle)
    val handle = remember {
        ReigniteSheetHandleImpl(
            visible, title, { currentIgniteTitle }, state, cancellable, startedAt, lines
        )
    }

    if (visible.value) {
        ReigniteSheet(
            title = title.value,
            state = state.value,
            cancellable = cancellable.value,
            startedAt = startedAt.value,
            lines = lines,
            onCancel = handle::cancel,
            onClose = { visible.value = false },
        )
    }

    return handle
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReigniteSheet(
    title: String,
    state: RunState,
    cancellable: Boolean,
    startedAt: Long,
    lines: List<LogLine>,
    onCancel: () -> Unit,
    onClose: () -> Unit,
) {
    val running = state == RunState.RUNNING
    // Set on Cancel until this run ends, so it is pressed once.
    var cancelling by remember(startedAt) { mutableStateOf(false) }
    // Not dismissable while the igniter runs: closing would hide it, not stop it. The lambda is
    // a key of the sheet's state, so it must stay the same object: a new one on every
    // recomposition makes a new state, and the sheet restarts its show animation each time.
    val runningNow by rememberUpdatedState(running)
    val confirmValueChange = remember { { value: SheetValue -> value != SheetValue.Hidden || !runningNow } }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = confirmValueChange
    )

    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    ModalBottomSheet(
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        dragHandle = null,
        onDismissRequest = { if (!running) onClose() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: what is happening and for how long.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                    when (state) {
                        RunState.RUNNING -> CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        RunState.SUCCEEDED -> Icon(
                            Icons.Filled.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        RunState.FAILED -> Icon(
                            Icons.Filled.Error, null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = when (state) {
                        RunState.RUNNING -> "$title…"
                        RunState.SUCCEEDED -> stringResource(R.string.success)
                        RunState.FAILED -> stringResource(R.string.failed)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                ElapsedTime(startedAt = startedAt, running = running)
            }

            // The log, one step per line as the igniter announces it before doing it: how long each
            // step took (until the next line), and the current one's time running live.
            Surface(
                shape = AxPanelShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .containScroll()
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 360.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    itemsIndexed(lines) { index, line ->
                        val current = index == lines.lastIndex
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (current) {
                                // Still going (until the next line, or the run ends).
                                StepTime(
                                    since = startedAt + line.atMillis,
                                    running = running,
                                    modifier = Modifier.width(56.dp)
                                )
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
                                    if (running) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(10.dp),
                                            strokeWidth = 1.5.dp
                                        )
                                    }
                                }
                            } else {
                                StepDuration(
                                    millis = lines[index + 1].atMillis - line.atMillis,
                                    modifier = Modifier.width(56.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                softWrap = false,
                                color = if (line.error) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Cancel while the action runs (its process is killed: plugins it hadn't reached stay
            // as they were), unless the step now must not be cut short; Close once it is done.
            if (running) {
                OutlinedButton(
                    onClick = {
                        cancelling = true
                        onCancel()
                    },
                    enabled = !cancelling && cancellable,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.close), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * The run's total time, ticking while [running]. Its own composable, so the ticks recompose this
 * text alone and not the sheet around it.
 */
@Composable
private fun ElapsedTime(startedAt: Long, running: Boolean) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(running) {
        while (running) {
            now = SystemClock.elapsedRealtime()
            delay(100)
        }
        now = SystemClock.elapsedRealtime()
    }
    Text(
        text = seconds(now - startedAt),
        style = MaterialTheme.typography.labelLarge,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * The current step's time, ticking from [since] while [running]; frozen once the run ends.
 * Its own composable, so the ticks recompose this text alone.
 */
@Composable
private fun StepTime(since: Long, running: Boolean, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(running, since) {
        while (running) {
            now = SystemClock.elapsedRealtime()
            delay(100)
        }
        now = SystemClock.elapsedRealtime()
    }
    StepDuration(now - since, modifier)
}

/** How long a step took; a slow one stands out. */
@Composable
private fun StepDuration(millis: Long, modifier: Modifier = Modifier) {
    val slow = millis >= SLOW_STEP_MS
    Text(
        text = seconds(millis),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = if (slow) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
        fontWeight = if (slow) FontWeight.Bold else null,
        modifier = modifier
    )
}

/** A step this long stands out. */
private const val SLOW_STEP_MS = 3_000L

private fun seconds(millis: Long) = String.format(Locale.ROOT, "%.1fs", millis.coerceAtLeast(0) / 1000.0)
