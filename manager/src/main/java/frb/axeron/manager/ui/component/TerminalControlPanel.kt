package frb.axeron.manager.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun TerminalControlPanel(
    onKeyPress: (String) -> Unit,
    onHistoryNavigate: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TerminalKey(label = "ESC", onClick = { onKeyPress("\u001b") })
        TerminalKey(label = "TAB", onClick = { onKeyPress("\t") })
        TerminalKey(label = "Ctrl-C", onClick = { onKeyPress("\u0003") })
        TerminalKeyIcon(icon = Icons.AutoMirrored.Filled.KeyboardReturn, label = "Enter", onClick = { onKeyPress("\n") })

        TerminalKeyIcon(icon = Icons.Default.KeyboardArrowUp, onClick = { onKeyPress("\u001b[A") })
        TerminalKeyIcon(icon = Icons.Default.KeyboardArrowDown, onClick = { onKeyPress("\u001b[B") })
        TerminalKeyIcon(icon = Icons.Default.KeyboardArrowLeft, onClick = { onKeyPress("\u001b[D") })
        TerminalKeyIcon(icon = Icons.Default.KeyboardArrowRight, onClick = { onKeyPress("\u001b[C") })

        TerminalKeyIcon(icon = Icons.Default.ArrowUpward, label = "Hist", onClick = { onHistoryNavigate(true) })
        TerminalKeyIcon(icon = Icons.Default.ArrowDownward, label = "Hist", onClick = { onHistoryNavigate(false) })

        TerminalKey(label = "HOME", onClick = { onKeyPress("\u001b[H") })
        TerminalKey(label = "END", onClick = { onKeyPress("\u001b[F") })
    }
}

@Composable
fun TerminalKey(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}

@Composable
fun TerminalKeyIcon(icon: ImageVector, label: String? = null, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                if (label != null) {
                    Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp))
                }
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}
