package frb.axeron.manager.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.data.ProcessControl
import frb.axeron.manager.data.ProcessControl.Outcome
import frb.axeron.manager.data.ProcessControl.Signal
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.OptionsSectionHeader
import frb.axeron.manager.ui.component.StatusPill
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.viewmodel.ProcessManagerViewModel
import frb.axeron.manager.ui.viewmodel.ProcessManagerViewModel.Row
import kotlinx.coroutines.launch

/** Rows a folded section shows before "show all". */
private const val PREVIEW_ROWS = 8

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProcessDetailSheet(
    row: Row,
    gone: Boolean,
    viewModel: ProcessManagerViewModel,
    onSignal: (Signal, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val proc = row.proc
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    var detail by remember(proc.pid) { mutableStateOf<Map<String, Outcome>?>(null) }
    var group by remember { mutableStateOf(false) }

    LaunchedEffect(proc.pid, reload) {
        detail = viewModel.detail(proc.pid)
    }
    val reads = detail
    val status = remember(reads) {
        reads?.get("status")?.takeIf { it.ok }?.let { ProcessControl.parseStatus(it.text) }.orEmpty()
    }
    val parent = viewModel.rows.firstOrNull { it.pid == proc.ppid }

    AxOptionsSheet(
        title = row.title,
        description = row.apps.firstOrNull()?.packageName ?: proc.name,
        onDismissRequest = onDismiss,
        headerAction = {
            IconButton(onClick = { reload++ }) {
                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.process_reload))
            }
        }
    ) {
        // Who it is at a glance: icon, state, and the tags the list row shows.
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProcessIcon(row, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusPill(stateLabel(proc.state), stateColor(proc.state))
                if (gone) StatusPill(stringResource(R.string.process_gone), MaterialTheme.colorScheme.error)
                if (row.fromServer) StatusPill(stringResource(R.string.process_from_server), MaterialTheme.colorScheme.tertiary)
                MetaTag("PID", proc.pid.toString())
                MetaTag(null, proc.user)
            }
        }
        Text(
            text = stateDescription(proc.state),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        OptionsSectionHeader(stringResource(R.string.process_section_usage))
        StatGrid(
            listOfNotNull(
                Stat(stringResource(R.string.process_stat_rss), formatKib(proc.rss), stringResource(R.string.process_stat_rss_hint)),
                Stat(stringResource(R.string.process_stat_vsz), formatKib(proc.vsz), stringResource(R.string.process_stat_vsz_hint)),
                status["VmSwap"]?.let { kbValue(it) }?.let {
                    Stat(stringResource(R.string.process_stat_swap), formatKib(it), stringResource(R.string.process_stat_swap_hint))
                },
                status["Threads"]?.let {
                    Stat(stringResource(R.string.process_stat_threads), it, stringResource(R.string.process_stat_threads_hint))
                },
                proc.cpu?.let {
                    Stat(stringResource(R.string.process_stat_cpu), "$it%", stringResource(R.string.process_stat_cpu_hint))
                },
                proc.time?.let {
                    Stat(stringResource(R.string.process_stat_cpu_time), it, stringResource(R.string.process_stat_cpu_time_hint))
                },
                proc.elapsed?.let {
                    Stat(stringResource(R.string.process_stat_uptime), it, stringResource(R.string.process_stat_uptime_hint))
                },
                reads?.get("oom_score_adj")?.takeIf { it.ok }?.text?.trim()?.toIntOrNull()?.let {
                    Stat(stringResource(R.string.process_stat_oom), it.toString(), oomDescription(it))
                },
            )
        )

        OptionsSectionHeader(stringResource(R.string.process_section_identity))
        InfoGroup {
            InfoRow("PID", proc.pid.toString(), stringResource(R.string.process_info_pid))
            InfoRow(
                "PPID",
                proc.ppid.toString() + (parent?.let { " · ${it.title}" } ?: ""),
                stringResource(R.string.process_info_ppid)
            )
            InfoRow("PGID", proc.pgid.toString(), stringResource(R.string.process_info_pgid))
            InfoRow("User / UID", "${proc.user} · ${proc.uid}", stringResource(R.string.process_info_uid))
            row.apps.forEach { app ->
                InfoRow(stringResource(R.string.process_info_app), "${app.label}\n${app.packageName}", null)
            }
            proc.label?.let { InfoRow("SELinux", it, stringResource(R.string.process_info_selinux)) }
            proc.policy?.let { InfoRow(stringResource(R.string.process_info_policy), policyLabel(it), stringResource(R.string.process_info_policy_desc)) }
            proc.nice?.let {
                InfoRow("Nice / PRI", "$it / ${proc.priority ?: "?"}", stringResource(R.string.process_info_nice))
            }
            reads?.get("exe")?.let { InfoRow(stringResource(R.string.process_info_exe), it.text.trim(), null, error = !it.ok) }
            reads?.get("cwd")?.let { InfoRow(stringResource(R.string.process_info_cwd), it.text.trim(), null, error = !it.ok) }
        }

        OptionsSectionHeader(stringResource(R.string.process_section_cmdline))
        val args = reads?.get("cmdline")?.takeIf { it.ok }?.text?.lines()?.filter { it.isNotEmpty() }
            ?: proc.args.split(' ').filter { it.isNotEmpty() }
        InfoGroup {
            if (args.isEmpty()) {
                InfoRow(null, "—", stringResource(R.string.process_cmdline_empty))
            }
            args.forEachIndexed { i, arg -> InfoRow("argv[$i]", arg, null) }
        }

        OptionsSectionHeader(stringResource(R.string.process_signals))
        Text(
            text = stringResource(R.string.process_signals_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        InfoGroup {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.process_whole_group, proc.pgid),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.process_whole_group_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = group, onCheckedChange = { group = it })
            }
            Signal.entries.forEach { signal ->
                RowDivider()
                SignalRow(signal) { onSignal(signal, group) }
            }
        }

        if (row.apps.isNotEmpty()) {
            OptionsSectionHeader(stringResource(R.string.process_section_app))
            val stopped = stringResource(R.string.process_force_stopped)
            row.apps.forEach { app ->
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            val outcome = viewModel.forceStop(app, proc.uid)
                            val text = if (outcome.ok) stopped.format(app.label) else outcome.error ?: "?"
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Outlined.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.process_force_stop, app.label))
                }
            }
            Text(
                text = stringResource(R.string.process_force_stop_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        OptionsSectionHeader(stringResource(R.string.process_section_internals))
        if (reads == null) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            reads["environ"]?.let { read ->
                val vars = remember(read) { if (read.ok) ProcessControl.parseEnviron(read.text) else emptyList() }
                FoldSection(
                    title = stringResource(R.string.process_env),
                    description = stringResource(R.string.process_env_desc),
                    count = vars.size,
                    read = read
                ) { limit ->
                    vars.take(limit).forEachIndexed { i, (name, value) ->
                        if (i > 0) RowDivider()
                        InfoRow(name, value.ifEmpty { "—" }, null, monoLabel = true)
                    }
                }
            }
            reads["fd"]?.let { read ->
                val fds = remember(read) { if (read.ok) ProcessControl.parseFds(read.text) else emptyList() }
                FoldSection(
                    title = stringResource(R.string.process_fds),
                    description = stringResource(R.string.process_fds_desc) + "\n" + fdSummary(fds),
                    count = fds.size,
                    read = read
                ) { limit ->
                    fds.take(limit).forEachIndexed { i, fd ->
                        if (i > 0) RowDivider()
                        InfoRow("fd ${fd.fd}", fd.target, fdKind(fd.target), monoLabel = true)
                    }
                }
            }
            reads["limits"]?.let { read ->
                val limits = remember(read) { if (read.ok) ProcessControl.parseLimits(read.text) else emptyList() }
                FoldSection(
                    title = stringResource(R.string.process_limits),
                    description = stringResource(R.string.process_limits_desc),
                    count = limits.size,
                    read = read
                ) { limit ->
                    limits.take(limit).forEachIndexed { i, l ->
                        if (i > 0) RowDivider()
                        InfoRow(
                            l.name,
                            if (l.soft == l.hard) l.soft else "${l.soft} / ${l.hard}",
                            if (l.soft == l.hard) l.unit.ifEmpty { null }
                            else stringResource(R.string.process_limits_soft_hard) + l.unit.let { if (it.isEmpty()) "" else " · $it" }
                        )
                    }
                }
            }
            reads["cgroup"]?.let { read ->
                val groups = remember(read) { if (read.ok) ProcessControl.parseCgroup(read.text) else emptyList() }
                FoldSection(
                    title = "cgroup",
                    description = stringResource(R.string.process_cgroup_desc),
                    count = groups.size,
                    read = read
                ) { limit ->
                    groups.take(limit).forEachIndexed { i, (controller, path) ->
                        if (i > 0) RowDivider()
                        InfoRow(controller, path.ifEmpty { "/" }, null, monoLabel = true)
                    }
                }
            }
            reads["status"]?.let { read ->
                val entries = status.entries.toList()
                FoldSection(
                    title = "status",
                    description = stringResource(R.string.process_status_desc),
                    count = entries.size,
                    read = read
                ) { limit ->
                    entries.take(limit).forEachIndexed { i, (key, value) ->
                        if (i > 0) RowDivider()
                        InfoRow(key, value.replace(WHITESPACE, " "), null, monoLabel = true)
                    }
                }
            }
        }
    }
}

private val WHITESPACE = Regex("\\s+")

private class Stat(val label: String, val value: String, val hint: String)

/** Two tiles a row; a row's tiles share one height. */
@Composable
private fun StatGrid(stats: List<Stat>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { stat ->
                    Surface(
                        shape = AxPanelShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stat.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stat.value,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stat.hint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** A panel of [InfoRow]s; selection works across all of them. */
@Composable
private fun InfoGroup(content: @Composable () -> Unit) {
    Surface(shape = AxPanelShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        SelectionContainer {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { content() }
        }
    }
}

/**
 * Label above its value, the value in monospace so ids and paths read exactly, and an optional
 * line saying what it means. Stacked rather than side by side: long values (paths, SELinux
 * contexts, package names) get the whole width instead of wrapping in a narrow column.
 */
@Composable
private fun InfoRow(
    label: String?,
    value: String,
    description: String?,
    error: Boolean = false,
    monoLabel: Boolean = false
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (monoLabel) FontFamily.Monospace else null,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
}

@Composable
private fun SignalRow(signal: Signal, onClick: () -> Unit) {
    val strong = signal == Signal.KILL || signal == Signal.STOP
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SIG${signal.name}",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (strong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(92.dp)
            )
            Text(
                text = signalDescription(signal),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * A `/proc` read under a title that says what it is. Folded to [PREVIEW_ROWS] rows; a refused
 * read shows the kernel's own message instead of rows.
 */
@Composable
private fun FoldSection(
    title: String,
    description: String,
    count: Int,
    read: Outcome,
    rows: @Composable (limit: Int) -> Unit
) {
    val context = LocalContext.current
    val copied = stringResource(R.string.process_copied)
    var open by remember(title) { mutableStateOf(false) }
    var all by remember(title) { mutableStateOf(false) }

    Surface(shape = AxPanelShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column {
            Surface(onClick = { open = !open }, color = Color.Transparent) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(8.dp))
                            if (read.ok) {
                                MetaTag(null, count.toString())
                            } else {
                                Icon(
                                    Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            if (ClipboardUtil.put(context, read.text)) {
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Icon(
                        if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            AnimatedVisibility(open) {
                Column {
                    RowDivider()
                    if (!read.ok) {
                        InfoRow(null, read.text.trim().ifEmpty { "?" }, null, error = true)
                    } else {
                        SelectionContainer {
                            Column { rows(if (all) Int.MAX_VALUE else PREVIEW_ROWS) }
                        }
                        if (count > PREVIEW_ROWS) {
                            Surface(onClick = { all = !all }, color = Color.Transparent) {
                                Text(
                                    text = if (all) stringResource(R.string.process_show_less)
                                    else stringResource(R.string.process_show_all, count),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** `VmSwap: 1234 kB` → 1234. */
private fun kbValue(text: String): Long? = text.substringBefore(' ').toLongOrNull()

@Composable
private fun stateDescription(state: String): String = when (state.take(1)) {
    "R" -> stringResource(R.string.process_state_running_desc)
    "S" -> stringResource(R.string.process_state_sleeping_desc)
    "D" -> stringResource(R.string.process_state_disk_desc)
    "Z" -> stringResource(R.string.process_state_zombie_desc)
    "T", "t" -> stringResource(R.string.process_state_stopped_desc)
    "I" -> stringResource(R.string.process_state_idle_desc)
    else -> ""
}

/** Android's oom_score_adj bands (ProcessList): lower survives longer when memory runs out. */
@Composable
private fun oomDescription(adj: Int): String = stringResource(
    when {
        adj <= -900 -> R.string.process_oom_never
        adj < 0 -> R.string.process_oom_system
        adj == 0 -> R.string.process_oom_foreground
        adj < 200 -> R.string.process_oom_visible
        adj < 300 -> R.string.process_oom_perceptible
        adj < 900 -> R.string.process_oom_service
        else -> R.string.process_oom_cached
    }
)

/** Android's scheduling policy names, as toybox's PCY prints them. */
@Composable
private fun policyLabel(policy: String): String = when (policy) {
    "fg" -> "fg · " + stringResource(R.string.process_policy_fg)
    "bg" -> "bg · " + stringResource(R.string.process_policy_bg)
    "ta" -> "ta · " + stringResource(R.string.process_policy_ta)
    "rs" -> "rs · " + stringResource(R.string.process_policy_rs)
    "aa", "as" -> "$policy · " + stringResource(R.string.process_policy_audio)
    "rt" -> "rt · " + stringResource(R.string.process_policy_rt)
    else -> policy
}

@Composable
private fun signalDescription(signal: Signal): String = stringResource(
    when (signal) {
        Signal.TERM -> R.string.process_sig_term
        Signal.KILL -> R.string.process_sig_kill
        Signal.INT -> R.string.process_sig_int
        Signal.HUP -> R.string.process_sig_hup
        Signal.QUIT -> R.string.process_sig_quit
        Signal.STOP -> R.string.process_sig_stop
        Signal.CONT -> R.string.process_sig_cont
        Signal.USR1, Signal.USR2 -> R.string.process_sig_usr
    }
)

private fun fdKind(target: String): String? = when {
    target.startsWith("socket:") -> "socket"
    target.startsWith("pipe:") -> "pipe"
    target.startsWith("anon_inode:") -> target.removePrefix("anon_inode:")
    target.startsWith("/dev/") -> "device"
    target.startsWith("/") -> null
    else -> null
}

@Composable
private fun fdSummary(fds: List<ProcessControl.Fd>): String {
    if (fds.isEmpty()) return ""
    val files = fds.count { it.target.startsWith("/") && !it.target.startsWith("/dev/") }
    val sockets = fds.count { it.target.startsWith("socket:") }
    val pipes = fds.count { it.target.startsWith("pipe:") }
    return stringResource(R.string.process_fds_summary, files, sockets, pipes, fds.size - files - sockets - pipes)
}
