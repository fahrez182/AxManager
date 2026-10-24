package frb.axeron.manager.ui.screen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import frb.axeron.manager.R
import frb.axeron.manager.adb.OtgActivator
import frb.axeron.manager.adb.OtgActivator.Reason
import frb.axeron.manager.adb.OtgActivator.State
import frb.axeron.manager.adb.OtgTargetMonitor
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.OptionTile
import frb.axeron.manager.ui.component.OptionTileGrid
import frb.axeron.manager.ui.component.OptionsSectionHeader
import frb.axeron.manager.ui.component.axPanel
import kotlinx.coroutines.delay

/** Which end of the OTG cable this phone is. */
private enum class OtgRole { COMPUTER, TARGET }

/**
 * OTG activation, opened on both phones: the *computer* drives [OtgActivator] (scan, handshake,
 * start AxManager on the other phone); the *target* watches its own side with
 * [OtgTargetMonitor] (USB debugging, cable link, server coming up) and logs it.
 */
@Composable
fun OtgActivationSheet(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val hostSupported = remember { OtgActivator.isUsbHostSupported(context) }

    // Start from what the cable already says: plugged into a host → we're the target;
    // a device attached to us → we're the computer.
    var role by rememberSaveable {
        mutableStateOf(
            when {
                !hostSupported -> OtgRole.TARGET
                OtgTargetMonitor.isConnectedToHost(context) -> OtgRole.TARGET
                else -> OtgRole.COMPUTER
            }
        )
    }

    AxOptionsSheet(
        title = stringResource(R.string.activate_by_otg),
        description = stringResource(R.string.otg_sheet_desc),
        onDismissRequest = onDismissRequest
    ) {
        OptionsSectionHeader(stringResource(R.string.otg_role_title))
        OptionTileGrid(items = OtgRole.entries, columns = 2) { option, modifier ->
            val computer = option == OtgRole.COMPUTER
            OptionTile(
                modifier = modifier,
                icon = if (computer) Icons.Outlined.Computer else Icons.Outlined.PhoneAndroid,
                label = stringResource(if (computer) R.string.otg_role_computer else R.string.otg_role_target),
                subtitle = stringResource(
                    when {
                        computer && !hostSupported -> R.string.otg_role_unsupported
                        computer -> R.string.otg_role_computer_desc
                        else -> R.string.otg_role_target_desc
                    }
                ),
                selected = role == option,
                enabled = !computer || hostSupported,
                onClick = { role = option }
            )
        }

        when (role) {
            OtgRole.COMPUTER -> OtgComputerPane(onDismissRequest)
            OtgRole.TARGET -> OtgTargetPane(onDismissRequest)
        }
    }
}

// ── Computer ─────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.OtgComputerPane(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val state by OtgActivator.state.collectAsState()
    val log by OtgActivator.log.collectAsState()

    // Re-scan whenever a USB device comes or goes, so plugging the cable in "just works".
    DisposableEffect(Unit) {
        OtgActivator.scan(context)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                OtgActivator.scan(ctx)
            }
        }
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            OtgActivator.reset()
        }
    }

    OtgComputerStatusCard(state)

    // Nothing found is often just the wrong side being host with a USB-C ↔ USB-C cable.
    if (state is State.NoDevice) {
        OtgNotice(stringResource(R.string.otg_no_device_hint), MaterialTheme.colorScheme.tertiary)
    }

    OptionsSectionHeader(stringResource(R.string.otg_steps_title))
    AxCard {
        Row(modifier = Modifier.padding(16.dp)) {
            IconBadge(imageVector = Icons.Outlined.PhoneAndroid, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.otg_steps),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (log.isNotEmpty()) {
        ActivationLogPanel(lines = log, modifier = Modifier.heightIn(max = 320.dp))
        CopyLogButton(lines = log, modifier = Modifier.fillMaxWidth())
    }

    val busy = state is State.Connecting || state is State.Activating
    val found = state as? State.DeviceFound
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy,
        onClick = {
            when {
                state is State.Success -> onDismissRequest()
                found != null -> OtgActivator.activate(context, found.device)
                else -> OtgActivator.scan(context, force = true)
            }
        }
    ) {
        when {
            busy -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            state is State.Success -> Text(stringResource(R.string.close))
            found != null -> {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.otg_activate))
            }

            else -> {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.otg_scan))
            }
        }
    }
}

@Composable
private fun OtgComputerStatusCard(state: State) {
    val error = MaterialTheme.colorScheme.error
    val primary = MaterialTheme.colorScheme.primary

    val (icon, tint, text) = when (state) {
        State.Idle, State.Scanning ->
            Triple(Icons.Outlined.Usb, primary, stringResource(R.string.otg_status_scanning))

        State.NoUsbHost ->
            Triple(Icons.Outlined.ErrorOutline, error, stringResource(R.string.otg_not_supported))

        State.NoDevice ->
            Triple(Icons.Outlined.Usb, error, stringResource(R.string.otg_status_no_device))

        is State.DeviceFound -> Triple(
            Icons.Outlined.PhoneAndroid, primary,
            stringResource(R.string.otg_status_found, "${state.vendor} ${state.name}".trim())
        )

        State.Connecting ->
            Triple(Icons.Outlined.Usb, primary, stringResource(R.string.otg_status_connecting))

        State.Activating ->
            Triple(Icons.Outlined.Usb, primary, stringResource(R.string.otg_status_activating))

        State.Success ->
            Triple(Icons.Filled.CheckCircle, primary, stringResource(R.string.otg_status_success))

        is State.Failed -> Triple(
            Icons.Outlined.ErrorOutline, error,
            stringResource(
                when (state.reason) {
                    Reason.PERMISSION_DENIED -> R.string.otg_failed_permission
                    Reason.CONNECTION_FAILED -> R.string.otg_failed_connection
                    Reason.NOT_INSTALLED -> R.string.otg_failed_not_installed
                    Reason.START_FAILED -> R.string.otg_failed_start
                }
            )
        )
    }

    OtgStatusCard(
        icon = icon,
        tint = tint,
        text = text,
        busy = state is State.Scanning || state is State.Connecting || state is State.Activating
    )
}

// ── Target ───────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.OtgTargetPane(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val status by OtgTargetMonitor.status.collectAsState()
    val log by OtgTargetMonitor.log.collectAsState()

    DisposableEffect(Unit) {
        OtgTargetMonitor.start(context)
        onDispose { OtgTargetMonitor.stop() }
    }

    // Success is the server coming up *while* this is open; then close after a short read.
    var sawServerDown by remember { mutableStateOf(!status.serverRunning) }
    var autoCloseIn by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(status.serverRunning) {
        if (!status.serverRunning) {
            sawServerDown = true
        } else if (sawServerDown) {
            for (seconds in 3 downTo 1) {
                autoCloseIn = seconds
                delay(1000)
            }
            onDismissRequest()
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    OtgStatusCard(
        icon = when {
            status.serverRunning -> Icons.Filled.CheckCircle
            status.actingAsHost -> Icons.Outlined.ErrorOutline
            else -> Icons.Outlined.PhoneAndroid
        },
        tint = if (status.actingAsHost && !status.serverRunning) MaterialTheme.colorScheme.error else primary,
        text = stringResource(
            when {
                status.serverRunning -> R.string.otg_target_status_done
                status.actingAsHost -> R.string.otg_target_status_host
                status.adbLink -> R.string.otg_target_status_linked
                else -> R.string.otg_target_status_waiting
            }
        ),
        busy = !status.serverRunning && !status.actingAsHost
    )

    if (status.actingAsHost && !status.serverRunning) {
        OtgNotice(stringResource(R.string.otg_target_host_warning), MaterialTheme.colorScheme.error)
    }

    AxCard {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ChecklistRow(
                done = status.usbDebugging,
                text = stringResource(
                    if (status.usbDebugging) R.string.otg_target_check_debugging
                    else R.string.otg_target_check_debugging_off
                ),
                action = if (status.usbDebugging) null else {
                    {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }) { Text(stringResource(R.string.otg_target_open_dev)) }
                    }
                }
            )
            ChecklistRow(
                done = status.adbLink,
                text = stringResource(
                    when {
                        status.adbLink -> R.string.otg_target_check_link
                        status.cableToHost -> R.string.otg_target_check_cable
                        else -> R.string.otg_target_check_waiting_cable
                    }
                )
            )
            ChecklistRow(
                done = status.serverRunning,
                text = stringResource(
                    if (status.serverRunning) R.string.otg_target_check_server
                    else R.string.otg_target_check_server_waiting
                )
            )
        }
    }

    if (log.isNotEmpty()) {
        ActivationLogPanel(lines = log, modifier = Modifier.heightIn(max = 320.dp))
        CopyLogButton(lines = log, modifier = Modifier.fillMaxWidth())
    }

    Button(modifier = Modifier.fillMaxWidth(), onClick = onDismissRequest) {
        val countdown = autoCloseIn
        Text(
            if (countdown != null) "${stringResource(R.string.close)} ($countdown)"
            else stringResource(R.string.close)
        )
    }
}

@Composable
private fun ChecklistRow(
    done: Boolean,
    text: String,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        action?.invoke()
    }
}

// ── Shared ───────────────────────────────────────────────────────────────────

@Composable
private fun OtgStatusCard(icon: ImageVector, tint: Color, text: String, busy: Boolean) {
    AxCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(imageVector = icon, tint = tint)
            Spacer(Modifier.width(12.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (busy) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun OtgNotice(text: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .axPanel(tint.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
