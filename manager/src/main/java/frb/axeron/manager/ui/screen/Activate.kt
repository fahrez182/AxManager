package frb.axeron.manager.ui.screen

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import frb.axeron.manager.ui.component.AxPanelShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Adb
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.adb.AdbPairingService
import frb.axeron.adb.util.AdbEnvironment
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.manager.R
import frb.axeron.manager.adb.ActivationLog
import frb.axeron.manager.adb.AdbStateInfo
import frb.axeron.manager.adb.OtgActivator
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxTopAppBar
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.SectionHeader
import frb.axeron.manager.ui.component.axPanel
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.viewmodel.ActivateViewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.compatibility.DeviceCompatibility

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ActivateScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val activateViewModel = viewModelGlobal.activateViewModel
    val settingsViewModel = viewModelGlobal.settingsViewModel
    val axeronInfo = activateViewModel.axeronInfo
    val context = LocalContext.current

    // One output sheet for every activation method on this screen.
    val runner = rememberActivationRunner()

    // Close automatically only when an activation done here brings the server up. Opening this
    // screen while the server already runs (from the Home status card) must not bounce back.
    var sawInactive by remember {
        mutableStateOf(!(axeronInfo.isRunning() && !axeronInfo.isNeedUpdate()))
    }
    // Hoisted so the auto-pop can wait for it: on an OTG *target* the server comes up while the
    // sheet is open, and its success countdown should be seen first.
    var otgSheetOpen by remember { mutableStateOf(false) }

    // While a sheet is open (incl. its success countdown) the pop waits for it to close.
    LaunchedEffect(axeronInfo, runner.visible, otgSheetOpen) {
        val ready = axeronInfo.isRunning() && !axeronInfo.isNeedUpdate()
        if (!ready) {
            sawInactive = true
        } else if (sawInactive && !runner.visible && !otgSheetOpen) {
            navigator.popBackStack()
        }
    }

    // ADB-over-TCP can be switched on/off while this screen is open (TCP mode, OTG, Stop TCP,
    // a PC's `adb tcpip`), so re-read the port instead of sampling it once.
    val tcpPort by produceState(AdbEnvironment.getAdbTcpPort()) {
        while (true) {
            delay(2000)
            value = withContext(Dispatchers.IO) { AdbEnvironment.getAdbTcpPort() }
        }
    }

    Scaffold(
        topBar = {
            AxTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.activate),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                // Padding inside the scroll lets content pass under the fading top bar.
                .padding(
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (DeviceCompatibility.isMiui()) {
                val notifStyle = Settings.System.getInt(
                    context.contentResolver,
                    "status_bar_notification_style",
                    1
                )
                if (notifStyle != 1) MiuiNotificationWarning()
            }

            ActivateSectionHeader(stringResource(R.string.activate_section_this_device))
            // Always shown: it also holds the TCP mode/port settings, which matter before TCP is on.
            TcpDebuggingCard(activateViewModel, settingsViewModel, tcpPort, runner)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WirelessDebuggingCard(activateViewModel, runner)
            }
            RootCard(activateViewModel, runner)

            ActivateSectionHeader(stringResource(R.string.activate_section_other_device))
            OtgCard(showSheet = otgSheetOpen, onShowSheetChange = { otgSheetOpen = it })
            ComputerCard()
        }
    }
}

@Composable
private fun ActivateSectionHeader(text: String) {
    SectionHeader(text = text, modifier = Modifier.padding(top = 8.dp))
}

/** Shared frame for every activation method: icon badge, title and description, then actions. */
@Composable
private fun ActivationMethodCard(
    icon: ImageVector,
    title: String,
    description: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    headerTrailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    AxCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(imageVector = icon, tint = tint)
                Spacer(Modifier.width(12.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (headerTrailing != null) {
                    Spacer(Modifier.width(8.dp))
                    headerTrailing()
                }
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

/** Row of up to two equal-width actions: a tonal secondary one and a filled primary one. */
@Composable
private fun ActionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun ActionLabel(icon: ImageVector, text: String) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun MiuiNotificationWarning() {
    ActivationMethodCard(
        icon = Icons.Outlined.NotificationsActive,
        title = stringResource(R.string.notification_warn_miui_2),
        description = stringResource(R.string.notification_warn_miui),
        tint = MaterialTheme.colorScheme.error
    )
}

/**
 * A starter that ran isn't proof of success: only call it done once the server binds back,
 * otherwise report a failure that points at the log.
 */
private suspend fun ActivateViewModel.awaitServer(
    success: String,
    timeout: String,
    timeoutMs: Long = 10_000
): ActivationStatus {
    ActivationLog.info("Waiting for the AxManager server to connect…")
    awaitRunning(timeoutMs)
    val up = activateStatus is ActivateViewModel.ActivateStatus.Running ||
            activateStatus is ActivateViewModel.ActivateStatus.NeedExtraStep
    return if (up) {
        ActivationLog.success("AxManager server connected")
        ActivationStatus.Success(success)
    } else {
        ActivationLog.error("No connection from the AxManager server after ${timeoutMs / 1000} s")
        ActivationStatus.Failed(timeout)
    }
}

/**
 * ADB over TCP in one card: the TCP mode switch next to the title, the port while TCP mode is on,
 * and connect/stop actions while adbd actually listens on TCP ([tcpPort] > 0).
 */
@Composable
fun TcpDebuggingCard(
    activateViewModel: ActivateViewModel,
    settings: SettingsViewModel,
    tcpPort: Int,
    runner: ActivationRunner
) {
    val context = LocalContext.current
    val tcpTitle = stringResource(R.string.tcp_debugging)
    val stopTitle = stringResource(R.string.activation_tcp_stop)
    val success = stringResource(R.string.activate_success)
    val timeout = stringResource(R.string.activation_server_timeout)

    ActivationMethodCard(
        icon = Icons.Outlined.Adb,
        title = tcpTitle,
        description = stringResource(R.string.tcp_mode_desc) +
                if (tcpPort > 0) "\n" + stringResource(R.string.tcp_port_value, tcpPort) else "",
        headerTrailing = {
            Switch(
                checked = settings.isTcpModeEnabled,
                onCheckedChange = { settings.setTcpMode(it) }
            )
        }
    ) {
        AnimatedVisibility(settings.isTcpModeEnabled) {
            TcpPortField(settings, tcpPort)
        }

        // TCP is up but TCP mode is off: the next wireless activation switches adbd back to USB.
        AnimatedVisibility(!settings.isTcpModeEnabled && tcpPort > 0) {
            TcpWillStopNotice()
        }

        if (tcpPort > 0) ActionRow {
            FilledTonalButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    runner.run(stopTitle) {
                        var outcome: AdbStateInfo? = null
                        activateViewModel.stopAdbTcp(context) { outcome = it }
                        when (val ai = outcome) {
                            is AdbStateInfo.Success -> ActivationStatus.Success(ai.message)
                            null -> ActivationStatus.Failed("No result")
                            else -> ActivationStatus.Failed(ai.message)
                        }
                    }
                }
            ) {
                ActionLabel(Icons.Filled.Stop, stringResource(R.string.stop_tcp_debugging))
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    runner.run(tcpTitle) {
                        try {
                            val ai = activateViewModel.startAdbTcp(context)
                            if (ai is AdbStateInfo.Success) {
                                activateViewModel.awaitServer(success, timeout)
                            } else {
                                ActivationStatus.Failed(ai.message)
                            }
                        } finally {
                            activateViewModel.setTryToActivate(false)
                        }
                    }
                }
            ) {
                // Restart icon when adbd listens on a different port than the configured one.
                ActionLabel(
                    if (tcpPort != AxeronSettings.getTcpPort()) Icons.Filled.RestartAlt
                    else Icons.Filled.PlayArrow,
                    stringResource(R.string.connect_tcp_debugging)
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun WirelessDebuggingCard(activateViewModel: ActivateViewModel, runner: ActivationRunner) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wirelessTitle = stringResource(R.string.activate_by_wireless)
    val success = stringResource(R.string.activate_success)
    val timeout = stringResource(R.string.activation_server_timeout)
    val pairingStarted = stringResource(R.string.activation_pairing_started)

    /** One wireless attempt; from the Start button a failure falls back to pairing. */
    fun startWireless(pairOnFailure: Boolean) = runner.run(wirelessTitle) {
        try {
            when (val ai = activateViewModel.startAdbWireless(context)) {
                is AdbStateInfo.Success -> {
                    context.startService(AdbPairingService.stopIntent(context))
                    activateViewModel.awaitServer(success, timeout)
                }

                is AdbStateInfo.Failed -> if (pairOnFailure) {
                    ActivationLog.info("Not paired yet? Starting the pairing service — follow its notification")
                    activateViewModel.startPairingService(context)
                    // The attempt goes on in the notification, which starts the server itself:
                    // its outcome is this one's, so wait for the server as long as it can pair.
                    // Other methods aren't held back meanwhile.
                    activateViewModel.setTryToActivate(false)
                    activateViewModel.awaitServer(
                        success,
                        "${ai.message}\n$pairingStarted",
                        AdbPairingService.LIFETIME_MS
                    )
                } else {
                    ActivationStatus.Failed(ai.message)
                }

                else -> ActivationStatus.Failed(ai.message)
            }
        } finally {
            activateViewModel.setTryToActivate(false)
        }
    }

    LaunchedEffect(Unit) {
        activateViewModel.updateNotificationState(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        activateViewModel.updateNotificationState(context) // re-check after returning
    }

    val launcherDeveloper = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        scope.launch {
            delay(500)
            if (activateViewModel.axeronInfo.isRunning()) {
                activateViewModel.setTryToActivate(false)
                return@launch
            }
            startWireless(pairOnFailure = false)
        }
    }

    val dialogDeveloper = rememberConfirmDialog()
    val uriHandler = LocalUriHandler.current
    val stepByStepUrl =
        "https://fahrez182.github.io/AxManager/guide/user-manual.html#start-with-wireless-debugging"

    LaunchedEffect(activateViewModel.devSettings) {
        if (activateViewModel.devSettings) {
            val flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

            // Wireless debugging tile settings, then developer options, then plain Settings.
            val candidates = listOf(
                Intent(TileService.ACTION_QS_TILE_PREFERENCES).apply {
                    putExtra(
                        Intent.EXTRA_COMPONENT_NAME,
                        ComponentName(
                            "com.android.settings",
                            "com.android.settings.development.qstile.DevelopmentTiles\$WirelessDebugging"
                        )
                    )
                    addFlags(flags)
                },
                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                    putExtra(":settings:fragment_args_key", "toggle_adb_wireless")
                    addFlags(flags)
                },
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            val opened = candidates.any { runCatching { launcherDeveloper.launch(it) }.isSuccess }
            if (!opened) {
                Toast.makeText(context, "Tidak dapat membuka pengaturan", Toast.LENGTH_SHORT).show()
            }
            activateViewModel.setLaunchDevSettings(false)
        }
    }

    val title = stringResource(R.string.enable_wireless_debugging)
    val content = stringResource(R.string.enable_wireless_debugging_msg)
    val confirm = stringResource(R.string.open_developer_opt)
    val cancel = stringResource(R.string.cancel)
    val neutral = stringResource(R.string.step_by_step)

    ActivationMethodCard(
        icon = Icons.Outlined.Wifi,
        title = stringResource(R.string.activate_by_wireless),
        description = stringResource(R.string.activate_by_wireless_desc)
    ) {
        ActionRow {
            FilledTonalButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    scope.launch {
                        when (
                            dialogDeveloper.awaitConfirm(
                                title = title,
                                content = content,
                                confirm = confirm,
                                dismiss = cancel,
                                neutral = neutral
                            )
                        ) {
                            ConfirmResult.Confirmed -> activateViewModel.setLaunchDevSettings(true)
                            ConfirmResult.Neutral -> uriHandler.openUri(stepByStepUrl)
                            else -> Unit
                        }
                    }
                }
            ) {
                ActionLabel(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.instruction))
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!activateViewModel.isNotificationEnabled) {
                        launcher.launch(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    } else {
                        startWireless(pairOnFailure = true)
                    }
                }
            ) {
                if (!activateViewModel.isNotificationEnabled) {
                    ActionLabel(Icons.Filled.Notifications, stringResource(R.string.enable_notification))
                } else {
                    ActionLabel(Icons.Filled.PlayArrow, stringResource(R.string.start_pairing))
                }
            }
        }
    }
}

@Composable
fun RootCard(activateViewModel: ActivateViewModel, runner: ActivationRunner) {
    val rootTitle = stringResource(R.string.activate_by_root)
    val failed = stringResource(R.string.failed_to_start)
    val success = stringResource(R.string.activate_success)
    val timeout = stringResource(R.string.activation_server_timeout)

    fun start(asShell: Boolean) = runner.run(rootTitle) {
        try {
            val state = if (asShell) activateViewModel.startRootAsShell()
            else activateViewModel.startRoot()
            when (state) {
                ActivateViewModel.ACTIVATE_SUCCESS -> activateViewModel.awaitServer(success, timeout)
                else -> ActivationStatus.Failed(failed)
            }
        } finally {
            activateViewModel.setTryToActivate(false)
        }
    }

    // Themed HTML (the old TextView ignored dark mode); the link stays tappable.
    val linkColor = MaterialTheme.colorScheme.primary
    val description = AnnotatedString.fromHtml(
        stringResource(
            R.string.activate_by_root_msg,
            "<b><a href=\"https://dontkillmyapp.com/\">Don't kill my app!</a></b>"
        ),
        linkStyles = TextLinkStyles(style = SpanStyle(color = linkColor))
    )

    AxCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(imageVector = Icons.Outlined.Security)
                Spacer(Modifier.width(12.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.activate_by_root),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ActionRow {
                FilledTonalButton(modifier = Modifier.weight(1f), onClick = { start(asShell = true) }) {
                    ActionLabel(Icons.Outlined.Terminal, stringResource(R.string.start_as_shell))
                }
                Button(modifier = Modifier.weight(1f), onClick = { start(asShell = false) }) {
                    ActionLabel(Icons.Filled.PlayArrow, stringResource(R.string.start))
                }
            }
        }
    }
}

@Composable
private fun OtgCard(showSheet: Boolean, onShowSheetChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val supported = remember { OtgActivator.isUsbHostSupported(context) }
    if (showSheet) {
        OtgActivationSheet(onDismissRequest = { onShowSheetChange(false) })
    }

    // Always available: a phone without USB host can still be the target.
    ActivationMethodCard(
        icon = Icons.Outlined.Usb,
        title = stringResource(R.string.activate_by_otg),
        description = stringResource(R.string.activate_by_otg_desc) +
                if (supported) "" else "\n" + stringResource(R.string.otg_target_only)
    ) {
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onShowSheetChange(true) }) {
            ActionLabel(Icons.Filled.PlayArrow, stringResource(R.string.otg_open))
        }
    }
}

@Composable
fun ComputerCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogDeveloper = rememberConfirmDialog()

    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    val title = stringResource(R.string.view_command)
    val content = stringResource(R.string.view_command_message, Starter.adbCommand)
    val confirm = stringResource(R.string.copy)
    val dismiss = stringResource(R.string.cancel)
    val neutral = stringResource(R.string.send)
    val share = stringResource(R.string.share_command)
    val copied = stringResource(R.string.copied)

    ActivationMethodCard(
        icon = Icons.Outlined.Computer,
        title = stringResource(R.string.activate_by_computer),
        description = stringResource(R.string.activate_by_computer_desc)
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    when (
                        dialogDeveloper.awaitConfirm(
                            title = title,
                            content = content,
                            markdown = true,
                            confirm = confirm,
                            dismiss = dismiss,
                            neutral = neutral
                        )
                    ) {
                        ConfirmResult.Confirmed -> {
                            if (ClipboardUtil.put(context, Starter.adbCommand)) {
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            }
                        }

                        ConfirmResult.Neutral -> shareLauncher.launch(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, Starter.adbCommand)
                                },
                                share
                            )
                        )

                        else -> Unit
                    }
                }
            }
        ) {
            ActionLabel(Icons.Outlined.Code, title)
        }
    }
}

/** The TCP mode port (moved here from Settings); its switch lives in the TCP card's header. */
@Composable
private fun TcpPortField(settings: SettingsViewModel, activeTcpPort: Int) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val reactivateToApply = stringResource(R.string.reactive_to_apply)

    var tcpPortText by remember { mutableStateOf(settings.tcpPortInt.toString()) }
    var isFocused by remember { mutableStateOf(false) }

    val portInt = tcpPortText.toIntOrNull()
    val isError = tcpPortText.isNotEmpty() && (portInt == null || portInt !in 1024..65535)

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        value = tcpPortText,
        onValueChange = { newValue ->
            if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                tcpPortText = newValue
            }
        },
        label = { Text(stringResource(R.string.tcp_port)) },
        supportingText = if (isError) {
            {
                Text(
                    text = stringResource(R.string.invalid_port),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else null,
        isError = isError,
        trailingIcon = {
            if (isFocused) {
                IconButton(
                    enabled = !isError && portInt != null,
                    onClick = {
                        portInt?.let {
                            settings.setTcpPort(it)
                            focusManager.clearFocus()
                        }
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save TCP port")
                }
            } else if (activeTcpPort > 0 && settings.tcpPortInt != activeTcpPort) {
                // adbd still listens on the old port until the next activation.
                IconButton(
                    onClick = {
                        Toast.makeText(context, reactivateToApply, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = reactivateToApply)
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = AxPanelShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

/** Shown while TCP is up with TCP mode off: wireless activation will switch adbd back to USB. */
@Composable
private fun TcpWillStopNotice() {
    val tint = MaterialTheme.colorScheme.tertiary
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
            text = stringResource(R.string.tcp_will_stop_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
