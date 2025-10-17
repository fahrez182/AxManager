package com.frb.axmanager.ui.screen

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Code
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.frb.axmanager.R
import com.frb.axmanager.ui.component.ConfirmResult
import com.frb.axmanager.ui.component.rememberConfirmDialog
import com.frb.axmanager.ui.util.ClipboardUtil
import com.frb.axmanager.ui.viewmodel.AdbViewModel
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.frb.engine.utils.Starter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import rikka.compatibility.DeviceCompatibility

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ActivateScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val adbViewModel = viewModelGlobal.adbViewModel
    val axeronInfo = adbViewModel.axeronInfo

    if (axeronInfo.isRunning() && !axeronInfo.isNeedUpdate()) {
        navigator.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Activate",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(top = 0, bottom = 0)
            )
        },
        contentWindowInsets = WindowInsets(top = 0, bottom = 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WirelessDebuggingCard(adbViewModel)
            }
            ComputerCard()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun WirelessDebuggingCard(adbViewModel: AdbViewModel) {
    val context = LocalContext.current

    // Panggil sekali untuk update state dari ViewModel
    LaunchedEffect(Unit) {
        adbViewModel.updateNotificationState(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        adbViewModel.updateNotificationState(context) // auto re-check izin
    }

    val launcherDeveloper = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        adbViewModel.startAdb(context, true)
    }

    val dialogDeveloper = rememberConfirmDialog()
    val scope = rememberCoroutineScope()

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
            Text(
                text = "Start via Wireless debugging",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(20.dp))

            Text(
                text = stringResource(R.string.activate_by_wireless_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(20.dp))

            Button(
                onClick = {
                    scope.launch {
                        val confirmResult = dialogDeveloper.awaitConfirm(
                            title = "Enable Wireless Debugging",
                            content = context.getString(R.string.enable_wireless_debugging_message),
                            confirm = "Developer",
                            dismiss = "Cancel",
                            neutral = "Step-by-Step"
                        )
                        if (confirmResult == ConfirmResult.Confirmed) {
                            adbViewModel.launchDevSettings = true
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
                Text("Instruction")
            }
            Spacer(modifier = Modifier.size(8.dp))

            LaunchedEffect(adbViewModel.launchDevSettings) {
                if (adbViewModel.launchDevSettings) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                        putExtra(":settings:fragment_args_key", "toggle_adb_wireless")
                    }
                    launcherDeveloper.launch(intent)
                    Log.d("AxManager", "launchDevSettings")
                    adbViewModel.launchDevSettings = false
                }
            }

            Button(
                onClick = {
                    if (!adbViewModel.isNotificationEnabled) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        launcher.launch(intent)
                        return@Button
                    } else {
                        if (adbViewModel.tryActivate) {
                            Toast.makeText(context, "Please Wait...", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        adbViewModel.startAdb(context)
                    }
                }
            ) {
                when {
                    !adbViewModel.isNotificationEnabled -> {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(16.dp),
                            contentDescription = null
                        )
                        Text("Enable Notification")
                    }

                    else -> {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(16.dp),
                            contentDescription = "Start"
                        )
                        Text("Start")
                    }
                }

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
            Text(
                text = "Start by connecting to a computer",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.activate_by_computer_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val dialogDeveloper = rememberConfirmDialog()
            val scope = rememberCoroutineScope()

            Button(
                onClick = {

                    scope.launch {
                        val confirmResult = dialogDeveloper.awaitConfirm(
                            title = "View Command",
                            content = context.getString(R.string.view_command_message, Starter.adbCommand),
                            markdown = true,
                            confirm = "Copy",
                            dismiss = "Cancel",
                            neutral = "Send"
                        )
                        if (confirmResult == ConfirmResult.Confirmed) {
                            if (ClipboardUtil.put(context, Starter.adbCommand)) {
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
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
                                    "Share Command"
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
                    contentDescription = "Command"
                )
                Text("View Command")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = false)
@Composable
fun CardPreview() {
}