package frb.axeron.manager.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.OptionTile
import frb.axeron.manager.ui.component.OptionTileGrid
import frb.axeron.manager.ui.component.OptionsSectionHeader
import frb.axeron.manager.ui.component.OrderToggle
import frb.axeron.manager.ui.viewmodel.ProcessManagerViewModel
import frb.axeron.manager.ui.viewmodel.ProcessManagerViewModel.Filter
import frb.axeron.manager.ui.viewmodel.ProcessManagerViewModel.SortBy

private val STATES = listOf("R", "S", "D", "Z", "T")
private val INTERVALS = listOf(1, 2, 3, 5, 10)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProcessOptionsSheet(viewModel: ProcessManagerViewModel, onDismiss: () -> Unit) {
    val filter = viewModel.filter

    AxOptionsSheet(
        title = stringResource(R.string.process_options),
        description = stringResource(R.string.process_options_desc),
        onDismissRequest = onDismiss,
        headerAction = {
            IconButton(
                enabled = filter.isActive,
                onClick = { viewModel.filter = Filter() }
            ) {
                Icon(Icons.Outlined.RestartAlt, contentDescription = stringResource(R.string.process_filter_clear))
            }
        }
    ) {
        OptionsSectionHeader(stringResource(R.string.process_sort))
        OptionTileGrid(items = SortBy.entries, columns = 2) { option, modifier ->
            OptionTile(
                modifier = modifier,
                icon = sortIcon(option),
                label = stringResource(sortLabel(option)),
                subtitle = stringResource(sortHint(option)),
                selected = viewModel.sortBy == option,
                onClick = { viewModel.sortBy = option }
            )
        }
        OrderToggle(ascending = viewModel.ascending, onChange = { viewModel.ascending = it })

        OptionsSectionHeader(stringResource(R.string.process_filter_ids))
        Hint(stringResource(R.string.process_filter_ids_desc))
        NumberField("PID", stringResource(R.string.process_filter_pid_hint), filter.pid) {
            viewModel.filter = filter.copy(pid = it)
        }
        NumberField("PPID", stringResource(R.string.process_filter_ppid_hint), filter.ppid) {
            viewModel.filter = filter.copy(ppid = it)
        }
        NumberField("PGID", stringResource(R.string.process_filter_pgid_hint), filter.pgid) {
            viewModel.filter = filter.copy(pgid = it)
        }
        NumberField("UID", stringResource(R.string.process_filter_uid_hint), filter.uid) {
            viewModel.filter = filter.copy(uid = it)
        }

        OptionsSectionHeader(stringResource(R.string.process_filter_memory))
        Hint(stringResource(R.string.process_filter_memory_desc))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                stringResource(R.string.process_min_rss),
                null,
                filter.minRss?.let { (it / 1024).toInt() },
                Modifier.weight(1f)
            ) { viewModel.filter = filter.copy(minRss = it?.let { mb -> mb * 1024L }) }
            NumberField(
                stringResource(R.string.process_max_rss),
                null,
                filter.maxRss?.let { (it / 1024).toInt() },
                Modifier.weight(1f)
            ) { viewModel.filter = filter.copy(maxRss = it?.let { mb -> mb * 1024L }) }
        }

        OptionsSectionHeader(stringResource(R.string.process_state))
        Hint(stringResource(R.string.process_state_filter_desc))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            STATES.forEach { state ->
                val selected = state in filter.states
                FilterChip(
                    selected = selected,
                    onClick = {
                        viewModel.filter = filter.copy(
                            states = if (selected) filter.states - state else filter.states + state
                        )
                    },
                    label = { Text("$state · ${stateLabel(state)}") }
                )
            }
        }

        OptionsSectionHeader(stringResource(R.string.process_interval))
        Hint(stringResource(R.string.process_interval_desc))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            INTERVALS.forEach { seconds ->
                FilterChip(
                    selected = viewModel.interval == seconds,
                    onClick = { viewModel.interval = seconds },
                    label = { Text(stringResource(R.string.process_interval_seconds, seconds)) }
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

/** Digits only; empty clears the constraint. [hint] sits under the field. */
@Composable
private fun NumberField(
    label: String,
    hint: String?,
    value: Int?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onChange: (Int?) -> Unit
) {
    var text by remember { mutableStateOf(value?.toString().orEmpty()) }
    // Cleared from outside (reset, a row's menu): follow it.
    LaunchedEffect(value) {
        if (text.toIntOrNull() != value) text = value?.toString().orEmpty()
    }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(9)
            text = digits
            onChange(digits.toIntOrNull())
        },
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        shape = AxPanelShape,
        modifier = modifier
    )
}

private fun sortIcon(option: SortBy): ImageVector = when (option) {
    SortBy.PID -> Icons.Outlined.Tag
    SortBy.NAME -> Icons.Outlined.SortByAlpha
    SortBy.RSS -> Icons.Outlined.Memory
    SortBy.VSZ -> Icons.Outlined.Layers
    SortBy.CPU -> Icons.Outlined.Speed
    SortBy.UID -> Icons.Outlined.Person
}

private fun sortLabel(option: SortBy): Int = when (option) {
    SortBy.PID -> R.string.process_sort_pid
    SortBy.NAME -> R.string.process_sort_name
    SortBy.RSS -> R.string.process_sort_rss
    SortBy.VSZ -> R.string.process_sort_vsz
    SortBy.CPU -> R.string.process_sort_cpu
    SortBy.UID -> R.string.process_sort_uid
}

private fun sortHint(option: SortBy): Int = when (option) {
    SortBy.PID -> R.string.process_sort_pid_hint
    SortBy.NAME -> R.string.process_sort_name_hint
    SortBy.RSS -> R.string.process_sort_rss_hint
    SortBy.VSZ -> R.string.process_sort_vsz_hint
    SortBy.CPU -> R.string.process_sort_cpu_hint
    SortBy.UID -> R.string.process_sort_uid_hint
}
