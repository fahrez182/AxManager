package frb.axeron.manager.ui.screen

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Adb
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.adb.AdbPairingService
import frb.axeron.adb.util.AdbEnvironment
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.manager.R
import frb.axeron.manager.adb.AdbStateInfo
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.viewmodel.ActivateViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.compatibility.DeviceCompatibility

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ActivateScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val activateViewModel = viewModelGlobal.activateViewModel
    val axeronInfo = activateViewModel.axeronInfo

    if (axeronInfo.isRunning() && !axeronInfo.isNeedUpdate()) {
        navigator.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (DeviceCompatibility.isMiui()) {
                val notifStyle = Settings.System.getInt(
                    LocalContext.current.contentResolver,
                    "status_bar_notification_style",
                    1
                )
                if (notifStyle != 1) {
                    ElevatedCard(
                        colors = CardDefaults.cardColors().copy(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.notification_warn_miui),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.padding(4.dp))
                            Text(
                                text = stringResource(R.string.notification_warn_miui_2),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            if (AdbEnvironment.getAdbTcpPort() > 0) {
                TcpDebuggingCard(navigator, activateViewModel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WirelessDebuggingCard(navigator, activateViewModel)
            }
            RootCard(navigator, activateViewModel)
            ComputerCard()
        }
    }
}

@Composable
fun TcpDebuggingCard(
    navigator: DestinationsNavigator,
    activateViewModel: ActivateViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Adb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    text = stringResource(R.string.activate_by_tcp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.size(20.dp))

            Text(
                text = stringResource(R.string.activate_by_tcp_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.tcp_port_value, AdbEnvironment.getAdbTcpPort()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(20.dp))

            Button(
                onClick = {
                    activateViewModel.startAdbTcp(context) { ai ->
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, ai.message, Toast.LENGTH_SHORT).show()
                        }

                        Log.e("AxManagerStartAdb", ai.message, ai.cause)
                        activateViewModel.setTryToActivate(false)
                    }
                }
            ) {
                if (AdbEnvironment.getAdbTcpPort() != AxeronSettings.getTcpPort()) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(16.dp),
                        contentDescription = "Restart"
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(16.dp),
                        contentDescription = "Start"
                    )
                }
                Text(stringResource(R.string.connect_tcp_debugging))
            }

            Spacer(Modifier.size(8.dp))

            Button(
                onClick = {
                    activateViewModel.stopAdbTcp(context) { ai ->
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, ai.message, Toast.LENGTH_SHORT).show()
                        }

                        Log.e("AxManagerStartAdb", ai.message, ai.cause)
                        activateViewModel.setTryToActivate(false)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(16.dp),
                    contentDescription = "Stop"
                )
                Text(stringResource(R.string.stop_tcp_debugging))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun WirelessDebuggingCard(
    navigator: DestinationsNavigator,
    activateViewModel: ActivateViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Panggil sekali untuk update state dari ViewModel
    LaunchedEffect(Unit) {
        activateViewModel.updateNotificationState(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        activateViewModel.updateNotificationState(context) // auto re-check izin
    }

    val launcherDeveloper = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        activateViewModel.startAdbWireless(context) { ai ->
            activateViewModel.setTryToActivate(false)
            if (ai is AdbStateInfo.Success) {
                val intent = AdbPairingService.stopIntent(context)
                context.startService(intent)
            }
        }
    }

    val dialogDeveloper = rememberConfirmDialog()


    val uriHandler = LocalUriHandler.current
    val stepByStepUrl =
        "https://fahrez182.github.io/AxManager/guide/user-manual.html#start-with-wireless-debugging"

    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = stringResource(R.string.activate_by_wireless),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.size(20.dp))

            Text(
                text = stringResource(R.string.activate_by_wireless_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(20.dp))

            val title = stringResource(R.string.enable_wireless_debugging)
            val content = stringResource(R.string.enable_wireless_debugging_msg)
            val confirm = stringResource(R.string.open_developer_opt)
            val cancel = stringResource(R.string.cancel)
            val neutral = stringResource(R.string.step_by_step)
            Button(
                onClick = {
                    scope.launch {
                        val confirmResult = dialogDeveloper.awaitConfirm(
                            title = title,
                            content = content,
                            confirm = confirm,
                            dismiss = cancel,
                            neutral = neutral
                        )
                        if (confirmResult == ConfirmResult.Confirmed) {
                            activateViewModel.setLaunchDevSettings(true)
                        }
                        if (confirmResult == ConfirmResult.Neutral) {
                            uriHandler.openUri(stepByStepUrl)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(16.dp),
                    contentDescription = "Instruction"
                )
                Text(stringResource(R.string.instruction))
            }
            Spacer(modifier = Modifier.size(8.dp))

            LaunchedEffect(activateViewModel.devSettings) {
                if (activateViewModel.devSettings) {
                    try {
                        val intent = Intent(TileService.ACTION_QS_TILE_PREFERENCES).apply {
                            val packageName = "com.android.settings"
                            setPackage(packageName)
                            putExtra(
                                Intent.EXTRA_COMPONENT_NAME,
                                ComponentName(
                                    packageName,
                                    $$"com.android.settings.development.qstile.DevelopmentTiles$WirelessDebugging"
                                )
                            )
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            )
                        }
                        launcherDeveloper.launch(intent)
                    } catch (_: Exception) {
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                                putExtra(":settings:fragment_args_key", "toggle_adb_wireless")
                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_NO_HISTORY or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                )
                            }
                        launcherDeveloper.launch(intent)
                    }
                    Log.d("AxManager", "launchDevSettings")
                    activateViewModel.setLaunchDevSettings(false)
                }
            }

            Button(
                onClick = {
                    if (!activateViewModel.isNotificationEnabled) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        launcher.launch(intent)
                        return@Button
                    } else {
                        activateViewModel.startAdbWireless(context) { ai ->
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(context, ai.message, Toast.LENGTH_SHORT).show()
                            }

                            Log.e("AxManagerStartAdb", ai.message, ai.cause)
                            activateViewModel.setTryToActivate(false)
                            if (ai is AdbStateInfo.Failed) {
                                activateViewModel.startPairingService(context)
                            } else if (ai is AdbStateInfo.Success) {
                                val intent = AdbPairingService.stopIntent(context)
                                context.startService(intent)
                            }
                        }
                    }
                }
            ) {
                when {
                    !activateViewModel.isNotificationEnabled -> {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(16.dp),
                            contentDescription = null
                        )
                        Text(stringResource(R.string.enable_notification))
                    }

                    else -> {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(16.dp),
                            contentDescription = "Start"
                        )
                        Text(stringResource(R.string.start_pairing))
                    }
                }

            }
        }
    }
}

@SuppressLint("ShowToast")
@Composable
fun RootCard(
    navigator: DestinationsNavigator,
    activateViewModel: ActivateViewModel
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = stringResource(R.string.activate_by_root),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.size(20.dp))

            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        text = Html.fromHtml(
                            context.getString(
                                R.string.activate_by_root_msg,
                                "<b><a href=\"https://dontkillmyapp.com/\">Don\'t kill my app!</a></b>"
                            ),
                            Html.FROM_HTML_MODE_LEGACY
                        )
                        movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            )
            Spacer(modifier = Modifier.size(20.dp))
            val failed = stringResource(R.string.failed_to_start)
            val success = stringResource(R.string.activate_success)
            stringResource(R.string.please_wait)
            Button(
                onClick = {
                    activateViewModel.startRoot { state ->
                        scope.launch(Dispatchers.Main) {

                            when (state) {
                                ActivateViewModel.ACTIVATE_FAILED -> {
                                    Toast.makeText(
                                        ctx,
                                        failed,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                ActivateViewModel.ACTIVATE_PROCESS -> {
                                    Toast.makeText(
                                        ctx,
                                        "",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                ActivateViewModel.ACTIVATE_SUCCESS -> {
                                    Toast.makeText(
                                        ctx,
                                        success,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        activateViewModel.setTryToActivate(false)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(16.dp),
                    contentDescription = "Start"
                )
                Text(stringResource(R.string.start))
            }
        }
    }
}

@Composable
fun ComputerCard() {
    val context = LocalContext.current

    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Result kalau butuh, biasanya kirim aja kosong kalau cuma share
    }

    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Computer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = stringResource(R.string.activate_by_computer),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = stringResource(R.string.activate_by_computer_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val dialogDeveloper = rememberConfirmDialog()
            val scope = rememberCoroutineScope()

            val title = stringResource(R.string.view_command)
            val content = stringResource(
                R.string.view_command_message,
                Starter.adbCommand
            )
            val confirm = stringResource(R.string.copy)
            val dismiss = stringResource(R.string.cancel)
            val neutral = stringResource(R.string.send)
            val share = stringResource(R.string.share_command)
            val copied = stringResource(R.string.copied)

            Button(
                onClick = {

                    scope.launch {
                        val confirmResult = dialogDeveloper.awaitConfirm(
                            title = title,
                            content = content,
                            markdown = true,
                            confirm = confirm,
                            dismiss = dismiss,
                            neutral = neutral
                        )
                        if (confirmResult == ConfirmResult.Confirmed) {
                            if (ClipboardUtil.put(context, Starter.adbCommand)) {
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            }
                        }
                        if (confirmResult == ConfirmResult.Neutral) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, Starter.adbCommand)
                            }

                            shareLauncher.launch(
                                Intent.createChooser(
                                    intent,
                                    share
                                )
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Code,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(16.dp),
                    contentDescription = title
                )
                Text(title)
            }
        }
    }
}