package frb.axeron.manager.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun TerminalControlPanel(
    onKeyPress: (String) -> Unit,
    onHistoryNavigate: (Boolean) -> Unit,
    isCtrlPressed: Boolean = false,
    isAltPressed: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Row 1: ESC, /, -, HOME, (up), END, PGUP
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TerminalKey("ESC") { onKeyPress("\u001b") }
            TerminalKey("/") { onKeyPress("/") }
            TerminalKey("-") { onKeyPress("-") }
            TerminalKey("HOME") { onKeyPress("\u001b[H") }
            TerminalKeyIcon(Icons.Default.KeyboardArrowUp) { onKeyPress("\u001b[A") }
            TerminalKey("END") { onKeyPress("\u001b[F") }
            TerminalKey("PGUP") { onKeyPress("\u001b[5~") }
        }

        // Row 2: TAB, CTRL, ALT, (left), (down), (right), PGDN
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TerminalKeyIcon(Icons.AutoMirrored.Filled.KeyboardTab) { onKeyPress("\t") }
            TerminalKey("CTRL", isPressed = isCtrlPressed) { onKeyPress("CTRL") }
            TerminalKey("ALT", isPressed = isAltPressed) { onKeyPress("ALT") }
            TerminalKeyIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft) { onKeyPress("\u001b[D") }
            TerminalKeyIcon(Icons.Default.KeyboardArrowDown) { onKeyPress("\u001b[B") }
            TerminalKeyIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight) { onKeyPress("\u001b[C") }
            TerminalKey("PGDN") { onKeyPress("\u001b[6~") }
        }
    }
}

@Composable
fun TerminalKey(label: String, isPressed: Boolean = false, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = 44.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TerminalKeyIcon(icon: ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
    }
}
