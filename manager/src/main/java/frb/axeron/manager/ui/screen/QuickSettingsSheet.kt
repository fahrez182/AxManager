package frb.axeron.manager.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import frb.axeron.manager.R
import frb.axeron.manager.data.QuickSetting
import frb.axeron.manager.data.QuickSetting.Control
import frb.axeron.manager.data.QuickSettingsStore
import frb.axeron.manager.data.SettingsAccess
import frb.axeron.manager.data.SettingsRepository.SettingType
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.ScrollableTextField
import androidx.compose.foundation.text.input.TextFieldState
import frb.axeron.manager.ui.component.AxDropdownDivider
import frb.axeron.manager.ui.component.AxDropdownHeader
import frb.axeron.manager.ui.component.AxDropdownItem
import frb.axeron.manager.ui.component.AxDropdownMenu
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.EmptyState
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.OptionTile
import frb.axeron.manager.ui.component.OptionTileGrid
import frb.axeron.manager.ui.component.OptionsSectionHeader
import frb.axeron.manager.ui.component.StatusPill
import frb.axeron.manager.ui.viewmodel.QuickSettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/**
 * The saved quick settings, each with its control: a switch, option chips or a slider.
 * [onApplied] runs after a successful write, e.g. to refresh a list showing that setting.
 */
@Composable
fun QuickSettingsSheet(
    onDismissRequest: () -> Unit,
    onApplied: (SettingType) -> Unit = {}
) {
    val viewModel: QuickSettingsViewModel = viewModel()
    val items = QuickSettingsStore.items
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val applyFailed = stringResource(R.string.quick_settings_apply_failed)

    // Re-read whenever the sheet opens or the saved list changes.
    LaunchedEffect(items) { viewModel.refresh() }
    // Without the server only settings the app's own grants cover can be written.
    val writable = remember(items) { items.associate { it.id to SettingsAccess.canWrite(context, it.type) } }

    var editing by remember { mutableStateOf<QuickSetting?>(null) }
    editing?.let { item ->
        QuickSettingConfigSheet(
            type = item.type,
            key = item.key,
            currentValue = viewModel.values[item.id],
            existing = item,
            onSave = {
                QuickSettingsStore.save(it)
                editing = null
            },
            onDismissRequest = { editing = null }
        )
    }

    AxOptionsSheet(
        title = stringResource(R.string.quick_settings),
        description = stringResource(R.string.quick_settings_desc),
        onDismissRequest = onDismissRequest,
        scrollable = false
    ) {
        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Bolt,
                title = stringResource(R.string.quick_settings_empty),
                subtitle = stringResource(R.string.quick_settings_empty_desc)
            )
            return@AxOptionsSheet
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                QuickSettingCard(
                    modifier = Modifier.animateItem(),
                    item = item,
                    canMoveUp = index > 0,
                    canMoveDown = index < items.lastIndex,
                    onMove = { QuickSettingsStore.move(item, it) },
                    value = viewModel.values[item.id],
                    loaded = viewModel.values.containsKey(item.id),
                    writing = viewModel.writing[item.id] == true,
                    writable = writable[item.id] ?: true,
                    onApply = { value ->
                        scope.launch {
                            if (viewModel.apply(item, value)) onApplied(item.type)
                            else Toast.makeText(context, applyFailed.format(item.label), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEdit = { editing = item },
                    onRemove = { QuickSettingsStore.remove(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickSettingCard(
    modifier: Modifier,
    item: QuickSetting,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (delta: Int) -> Unit,
    value: String?,
    loaded: Boolean,
    writing: Boolean,
    writable: Boolean,
    onApply: (String) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val control = item.control
    val canApply = loaded && !writing && writable
    AxCard(modifier) {
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(imageVector = control.icon(), size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(
                            text = stringResource(item.type.shortStringId),
                            color = sourceColor(item.type)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = item.key,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (writing || !loaded) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
                if (control is Control.Toggle) {
                    Switch(
                        checked = value == control.on,
                        enabled = canApply,
                        onCheckedChange = { onApply(if (it) control.on else control.off) }
                    )
                }
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = null)
                    }
                    AxDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        AxDropdownHeader(item.label)
                        AxDropdownItem(
                            text = stringResource(R.string.quick_settings_move_up),
                            icon = Icons.Outlined.ArrowUpward,
                            enabled = canMoveUp,
                            onClick = {
                                menu = false
                                onMove(-1)
                            }
                        )
                        AxDropdownItem(
                            text = stringResource(R.string.quick_settings_move_down),
                            icon = Icons.Outlined.ArrowDownward,
                            enabled = canMoveDown,
                            onClick = {
                                menu = false
                                onMove(1)
                            }
                        )
                        AxDropdownDivider()
                        AxDropdownItem(
                            text = stringResource(R.string.quick_settings_edit),
                            icon = Icons.Outlined.Edit,
                            onClick = {
                                menu = false
                                onEdit()
                            }
                        )
                        AxDropdownItem(
                            text = stringResource(R.string.quick_settings_remove),
                            icon = Icons.Outlined.Delete,
                            destructive = true,
                            onClick = {
                                menu = false
                                onRemove()
                            }
                        )
                    }
                }
            }

            when (control) {
                is Control.Toggle -> Unit

                Control.Custom -> CustomValueControl(
                    value = value,
                    enabled = canApply,
                    onApply = onApply
                )

                is Control.Choice -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    control.options.forEach { option ->
                        FilterChip(
                            selected = value == option.value,
                            onClick = { if (value != option.value) onApply(option.value) },
                            enabled = canApply,
                            label = { Text(option.displayLabel) }
                        )
                    }
                }

                is Control.Slider -> SliderControl(
                    control = control,
                    value = value,
                    enabled = canApply,
                    onApply = onApply
                )
            }

            // Say what the setting holds when the control can't show it.
            val matched = when (control) {
                Control.Custom -> true
                is Control.Toggle -> value == control.on || value == control.off
                is Control.Choice -> control.options.any { it.value == value }
                is Control.Slider -> value?.toDoubleOrNull() != null
            }
            if (!writable) {
                Text(
                    text = stringResource(
                        if (item.type == SettingType.ANDROID_PROP || item.type == SettingType.AXERON_ENV)
                            R.string.quick_settings_needs_server
                        else R.string.quick_settings_needs_permission
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (loaded && !matched) {
                Text(
                    text = stringResource(
                        R.string.quick_settings_current,
                        value ?: stringResource(R.string.quick_settings_unset)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Slider plus -/+ for exact steps; writes once on release, not on every frame of a drag. */
@Composable
private fun SliderControl(
    control: Control.Slider,
    value: String?,
    enabled: Boolean,
    onApply: (String) -> Unit
) {
    val current = value?.toDoubleOrNull()?.let { control.snap(it) } ?: control.min
    var dragging by remember { mutableStateOf<Double?>(null) }
    val shown = dragging ?: current

    Column {
        Text(
            text = control.format(shown),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                enabled = enabled && shown > control.min,
                onClick = { onApply(control.format(shown - control.step)) }
            ) { Icon(Icons.Outlined.Remove, contentDescription = null) }
            // Continuous track snapped by hand: tick marks for 0.001 steps would be a solid bar.
            Slider(
                value = shown.toFloat(),
                onValueChange = { dragging = control.snap(it.toDouble()) },
                onValueChangeFinished = {
                    dragging?.let { onApply(control.format(it)) }
                    dragging = null
                },
                valueRange = control.min.toFloat()..control.max.toFloat(),
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                enabled = enabled && shown < control.max,
                onClick = { onApply(control.format(shown + control.step)) }
            ) { Icon(Icons.Outlined.Add, contentDescription = null) }
        }
        Row(Modifier.padding(horizontal = 12.dp)) {
            Text(control.format(control.min), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(control.format(control.max), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Free-form value: the current one in a field, applied with the check button. */
@Composable
private fun CustomValueControl(
    value: String?,
    enabled: Boolean,
    onApply: (String) -> Unit
) {
    // Follows the setting until the user starts typing.
    val state = remember(value) { TextFieldState(value.orEmpty()) }
    val draft = state.text.toString()
    val changed = draft != value.orEmpty()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        // Shared long-text field: scroll bar past 4 lines, and its scrolling stays in it.
        ScrollableTextField(
            state = state,
            enabled = enabled,
            minLines = 1,
            maxLines = 4,
            placeholder = stringResource(R.string.quick_settings_unset),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            enabled = enabled && changed,
            onClick = { onApply(draft) }
        ) {
            Icon(Icons.Outlined.Check, contentDescription = stringResource(R.string.save))
        }
    }
}

private enum class Mode(val labelId: Int, val icon: ImageVector) {
    CUSTOM(R.string.quick_settings_mode_custom, Icons.Outlined.BookmarkBorder),
    TOGGLE(R.string.quick_settings_mode_toggle, Icons.Outlined.ToggleOn),
    CHOICE(R.string.quick_settings_mode_choice, Icons.AutoMirrored.Outlined.List),
    SLIDER(R.string.quick_settings_mode_slider, Icons.Outlined.Tune)
}

private fun Control.icon(): ImageVector = when (this) {
    Control.Custom -> Mode.CUSTOM.icon
    is Control.Toggle -> Mode.TOGGLE.icon
    is Control.Choice -> Mode.CHOICE.icon
    is Control.Slider -> Mode.SLIDER.icon
}

private class OptionDraft(label: String, value: String) {
    var label by mutableStateOf(label)
    var value by mutableStateOf(value)
}

/**
 * Adds a setting to Quick Settings, or edits one ([existing]). Defaults come from the
 * setting's [currentValue]: 0/1 and true/false become a switch, numbers a slider.
 */
@Composable
fun QuickSettingConfigSheet(
    type: SettingType,
    key: String,
    currentValue: String?,
    existing: QuickSetting?,
    onSave: (QuickSetting) -> Unit,
    onDismissRequest: () -> Unit
) {
    val current = currentValue.orEmpty()
    val control = existing?.control
    val number = current.toDoubleOrNull()

    var label by remember { mutableStateOf(existing?.label ?: key) }
    var mode by remember {
        mutableStateOf(
            when (control) {
                Control.Custom -> Mode.CUSTOM
                is Control.Toggle -> Mode.TOGGLE
                is Control.Choice -> Mode.CHOICE
                is Control.Slider -> Mode.SLIDER
                null -> when {
                    current in listOf("0", "1", "true", "false") -> Mode.TOGGLE
                    number != null -> Mode.SLIDER
                    else -> Mode.CUSTOM
                }
            }
        )
    }

    // Switch
    val boolLike = current == "true" || current == "false"
    var onValue by remember {
        mutableStateOf((control as? Control.Toggle)?.on ?: if (boolLike) "true" else "1")
    }
    var offValue by remember {
        mutableStateOf((control as? Control.Toggle)?.off ?: if (boolLike) "false" else "0")
    }

    // Options
    val options = remember {
        mutableStateListOf<OptionDraft>().apply {
            val saved = (control as? Control.Choice)?.options
            if (saved != null) saved.forEach { add(OptionDraft(it.label, it.value)) }
            else {
                add(OptionDraft("", current))
                add(OptionDraft("", ""))
            }
        }
    }

    // Slider
    val slider = control as? Control.Slider
    val currentIsDecimal = number != null && number != Math.floor(number)
    var decimal by remember { mutableStateOf(slider?.decimal ?: currentIsDecimal) }
    var minText by remember { mutableStateOf(slider?.min?.plain() ?: "0") }
    var maxText by remember {
        mutableStateOf(
            slider?.max?.plain() ?: when {
                number == null -> "100"
                currentIsDecimal -> maxOf(1.0, number * 2).plain()
                else -> maxOf(100.0, number * 2).plain()
            }
        )
    }
    var stepText by remember { mutableStateOf(slider?.step?.plain() ?: if (currentIsDecimal) "0.1" else "1") }

    val errorToggle = stringResource(R.string.quick_settings_error_toggle)
    val errorOptions = stringResource(R.string.quick_settings_error_options)
    val errorNumbers = stringResource(R.string.quick_settings_error_numbers)
    val errorInteger = stringResource(R.string.quick_settings_error_integer)
    val errorRange = stringResource(R.string.quick_settings_error_range)

    // The built control, or why it can't be built yet.
    val result: Pair<Control?, String?> = when (mode) {
        Mode.CUSTOM -> Control.Custom to null

        Mode.TOGGLE ->
            if (onValue.isBlank() || offValue.isBlank() || onValue == offValue) null to errorToggle
            else Control.Toggle(onValue.trim(), offValue.trim()) to null

        Mode.CHOICE -> {
            val filled = options.filter { it.value.isNotBlank() }
            val values = filled.map { it.value.trim() }
            if (filled.size < 2 || values.distinct().size != values.size) null to errorOptions
            else Control.Choice(filled.map { QuickSetting.Option(it.label.trim(), it.value.trim()) }) to null
        }

        Mode.SLIDER -> {
            val min = minText.toNumber()
            val max = maxText.toNumber()
            val step = stepText.toNumber()
            when {
                min == null || max == null || step == null -> null to errorNumbers
                !decimal && listOf(min, max, step).any { it != Math.floor(it) } -> null to errorInteger
                min >= max || step <= 0 || step > max - min -> null to errorRange
                else -> Control.Slider(decimal, min, max, step) to null
            }
        }
    }

    AxOptionsSheet(
        title = stringResource(if (existing == null) R.string.quick_settings_add else R.string.quick_settings_edit),
        description = key,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.quick_settings_label)) },
                singleLine = true,
                shape = AxPanelShape,
                modifier = Modifier.fillMaxWidth()
            )
            if (currentValue != null) {
                Text(
                    text = stringResource(R.string.quick_settings_current, currentValue),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            OptionsSectionHeader(stringResource(R.string.quick_settings_mode))
            OptionTileGrid(items = Mode.entries, columns = 2) { option, modifier ->
                OptionTile(
                    modifier = modifier,
                    icon = option.icon,
                    label = stringResource(option.labelId),
                    selected = mode == option,
                    onClick = { mode = option }
                )
            }

            when (mode) {
                Mode.CUSTOM -> Text(
                    text = stringResource(R.string.quick_settings_mode_custom_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Mode.TOGGLE -> {
                    ValueField(stringResource(R.string.quick_settings_on_value), onValue) { onValue = it }
                    ValueField(stringResource(R.string.quick_settings_off_value), offValue) { offValue = it }
                }

                Mode.CHOICE -> {
                    options.forEachIndexed { index, option ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = option.label,
                                onValueChange = { option.label = it },
                                label = { Text(stringResource(R.string.quick_settings_option_label)) },
                                singleLine = true,
                                shape = AxPanelShape,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = option.value,
                                onValueChange = { option.value = it },
                                label = { Text(stringResource(R.string.quick_settings_option_value)) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                shape = AxPanelShape,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                enabled = options.size > 2,
                                onClick = { options.removeAt(index) }
                            ) { Icon(Icons.Outlined.Close, contentDescription = null) }
                        }
                    }
                    TextButton(onClick = { options.add(OptionDraft("", "")) }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.quick_settings_add_option))
                    }
                }

                Mode.SLIDER -> {
                    OptionsSectionHeader(stringResource(R.string.quick_settings_number_type))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !decimal,
                            onClick = { decimal = false },
                            label = { Text(stringResource(R.string.quick_settings_integer)) }
                        )
                        FilterChip(
                            selected = decimal,
                            onClick = { decimal = true },
                            label = { Text(stringResource(R.string.quick_settings_decimal)) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(stringResource(R.string.quick_settings_min), minText, Modifier.weight(1f)) { minText = it }
                        NumberField(stringResource(R.string.quick_settings_max), maxText, Modifier.weight(1f)) { maxText = it }
                        NumberField(stringResource(R.string.quick_settings_step), stepText, Modifier.weight(1f)) { stepText = it }
                    }
                }
            }

            result.second?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = result.first != null,
                    onClick = {
                        result.first?.let { built ->
                            onSave(QuickSetting(type, key, label.trim().ifEmpty { key }, built))
                        }
                    }
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

@Composable
private fun ValueField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        shape = AxPanelShape,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = value.toNumber() == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        shape = AxPanelShape,
        modifier = modifier
    )
}

/** Accepts a decimal comma too ("0,001"). */
private fun String.toNumber(): Double? = trim().replace(',', '.').toDoubleOrNull()

/** 1.0 → "1", 0.001 → "0.001" (no exponent). */
private fun Double.plain(): String =
    if (this == Math.floor(this) && kotlin.math.abs(this) < 1e15) roundToLong().toString()
    else java.math.BigDecimal(toString()).stripTrailingZeros().toPlainString()
