package frb.axeron.manager.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.ui.util.TerminalPrefs
import kotlin.math.roundToInt

private const val SCALE_STEP = 0.1f

/** View options for command output (shared by every output screen, applied live). */
@Composable
fun TerminalOptionsCard(modifier: Modifier = Modifier) {
    AxCard(modifier) {
        SettingsRow(
            label = stringResource(R.string.terminal_line_numbers),
            description = stringResource(R.string.terminal_line_numbers_desc),
            iconVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
            checked = TerminalPrefs.lineNumbers,
            onSwitchChange = { TerminalPrefs.lineNumbers = it }
        )
        SettingsDivider()
        val scale = TerminalPrefs.textScale
        SettingsRow(
            label = stringResource(R.string.terminal_text_size),
            description = "${(scale * 100).roundToInt()}%",
            iconVector = Icons.Outlined.FormatSize,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 52.dp, end = 4.dp, bottom = 4.dp)
            ) {
                IconButton(
                    onClick = { TerminalPrefs.textScale = scale.snap() - SCALE_STEP },
                    enabled = scale > TerminalPrefs.MIN_SCALE
                ) { Icon(Icons.Outlined.TextDecrease, contentDescription = null) }
                Slider(
                    value = scale,
                    onValueChange = { TerminalPrefs.textScale = it.snap() },
                    valueRange = TerminalPrefs.MIN_SCALE..TerminalPrefs.MAX_SCALE,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { TerminalPrefs.textScale = scale.snap() + SCALE_STEP },
                    enabled = scale < TerminalPrefs.MAX_SCALE
                ) { Icon(Icons.Outlined.TextIncrease, contentDescription = null) }
            }
        }
    }
}

/** [TerminalOptionsCard] in a bottom sheet, for screens without their own options sheet. */
@Composable
fun TerminalOptionsSheet(onDismissRequest: () -> Unit) {
    AxOptionsSheet(
        title = stringResource(R.string.terminal_view_options),
        onDismissRequest = onDismissRequest
    ) {
        TerminalOptionsCard()
    }
}

private fun Float.snap(): Float = (this / SCALE_STEP).roundToInt() * SCALE_STEP
