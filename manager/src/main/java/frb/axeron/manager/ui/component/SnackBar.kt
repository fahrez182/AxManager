package frb.axeron.manager.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AxSnackBarHost(
    hostState: SnackbarHostState
) {

    SnackbarHost(hostState = hostState) { data ->
        return@SnackbarHost Snackbar(
            modifier = Modifier.padding(12.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            content = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    data.visuals.message.let {
                        Text(
                            it, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            },
            action = {
                data.visuals.actionLabel?.let {
                    TextButton(onClick = { data.performAction() }) {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        )
    }
}
