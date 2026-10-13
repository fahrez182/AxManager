package frb.axeron.manager.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import frb.axeron.manager.ui.component.AxTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.rememberLifecycleOwner
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AppearanceScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DeveloperScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.adb.util.AdbEnvironment
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.SectionHeader
import frb.axeron.manager.ui.component.SettingsDivider
import frb.axeron.manager.ui.component.SettingsGroup
import frb.axeron.manager.ui.component.SettingsRow
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val activateViewModel = viewModelGlobal.activateViewModel
    val settings = viewModelGlobal.settingsViewModel
//    val privilegeViewModel = viewModelGlobal.privilegeViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var showDevDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    DeveloperInfo(
        showDevDialog
    ) {
        showDevDialog = false
    }

    if (showResetDialog) {
        ResetBottomSheet(
            onDismissRequest = { showResetDialog = false },
            isRoot = activateViewModel.axeronInfo.isRoot(),
            onReset = { rootMode, both ->
                showResetDialog = false
                navigator.navigate(FlashScreenDestination(FlashIt.FlashUninstall(rootMode, both)))
            }
        )
    }

    Scaffold(
        topBar = {
            AxTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 5.dp),
                        onClick = {
                            showDevDialog = true
                        })
                    {
                        Icon(Icons.Outlined.Info, null)
                    }
                }
            )
        }
    ) { paddingValues ->

        val axeronRunning = activateViewModel.axeronInfo.isRunning()

        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                // Padding inside the scroll lets content pass under the fading top bar.
                .padding(
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + LocalFloatingBarInset.current + 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            SettingsSectionHeader(stringResource(R.string.settings_section_environment))
            SettingsGroup {
                if (axeronRunning) {
                    val lifecycleOwner = rememberLifecycleOwner()
                    DisposableEffect(Unit) {
                        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
                            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                                activateViewModel.checkShizukuIntercept()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }
                    SettingsRow(
                        iconPainter = painterResource(R.drawable.ic_axeron),
                        label = stringResource(R.string.axeron_permission),
                        description = stringResource(R.string.axeron_permission_desc),
                        checked = activateViewModel.isShizukuActive,
                        onSwitchChange = { activateViewModel.setShizukuIntercept(it) }
                    )
                }

                SettingsRow(
                    iconVector = Icons.Filled.Terminal,
                    label = stringResource(R.string.prefer_system_binaries),
                    description = stringResource(R.string.prefer_system_binaries_desc),
                    checked = settings.isPreferSystemBinariesEnabled,
                    onSwitchChange = { settings.setPreferSystemBinaries(it) },
                    showDivider = axeronRunning
                )

                SettingsRow(
                    iconVector = Icons.Filled.Adb,
                    label = stringResource(R.string.tcp_mode),
                    description = stringResource(R.string.tcp_mode_desc),
                    checked = settings.isTcpModeEnabled,
                    onSwitchChange = { settings.setTcpMode(it) },
                    showDivider = true
                ) {
                    AnimatedVisibility(settings.isTcpModeEnabled) {
                        var tcpPortText by remember {
                            mutableStateOf(settings.tcpPortInt.toString())
                        }
                        val context = LocalContext.current

                        Column(
                            modifier = Modifier.padding(start = 64.dp, end = 16.dp, bottom = 12.dp)
                        ) {
                            var isFocused by remember { mutableStateOf(false) }
                            val focusManager = LocalFocusManager.current

                            val portInt = tcpPortText.toIntOrNull()
                            val isError = tcpPortText.isNotEmpty() &&
                                    (portInt == null || portInt !in 1024..65535)

                            TextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { state ->
                                        isFocused = state.isFocused
                                    },
                                value = tcpPortText,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                                        tcpPortText = newValue
                                    }
                                },
                                label = {
                                    Text(stringResource(R.string.tcp_port))
                                },
                                supportingText = {
                                    AnimatedVisibility(
                                        visible = isError,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.invalid_port),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
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
                                            Icon(
                                                imageVector = Icons.Default.Save,
                                                contentDescription = "Save TCP port"
                                            )
                                        }
                                    } else if (settings.tcpPortInt != AdbEnvironment.getAdbTcpPort()) {
                                        val reactiveToChange = stringResource(R.string.reactive_to_apply)
                                        IconButton(
                                            onClick = {
                                                Toast.makeText(
                                                    context,
                                                    reactiveToChange,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RestartAlt,
                                                contentDescription = "Re-Activate AxManager"
                                            )
                                        }
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            SettingsSectionHeader(stringResource(R.string.settings_section_general))
            SettingsGroup {
                SettingsRow(
                    iconVector = Icons.Filled.RestartAlt,
                    label = stringResource(R.string.active_on_boot),
                    description = stringResource(R.string.active_on_boot_desc),
                    checked = settings.isActivateOnBootEnabled,
                    onSwitchChange = { settings.setActivateOnBoot(it) }
                )

                SettingsRow(
                    iconVector = Icons.Filled.Refresh,
                    label = stringResource(R.string.ignite_when_relog),
                    description = stringResource(R.string.ignite_when_relog_desc),
                    checked = settings.isIgniteWhenRelogEnabled,
                    onSwitchChange = { settings.setIgniteWhenRelog(it) },
                    showDivider = true
                )
            }

            if (axeronRunning) {
                SettingsSectionHeader(stringResource(R.string.settings_section_maintenance))
                SettingsGroup {
                    SettingsRow(
                        iconVector = Icons.Filled.FolderDelete,
                        label = stringResource(R.string.reset_path),
                        description = stringResource(R.string.reset_path_desc),
                        onClick = { showResetDialog = true }
                    )
                }
            }

            SettingsSectionHeader(stringResource(R.string.settings_section_more))
            SettingsGroup {
                if (axeronRunning) {
                    SettingsRow(
                        iconVector = Icons.Filled.Edit,
                        label = stringResource(R.string.settings_editor),
                        onClick = { navigator.navigate(SettingsEditorScreenDestination) }
                    )
                }

                SettingsRow(
                    iconVector = Icons.Filled.Palette,
                    label = stringResource(R.string.appearance),
                    onClick = { navigator.navigate(AppearanceScreenDestination) },
                    showDivider = axeronRunning
                )

                SettingsRow(
                    iconVector = Icons.Filled.BugReport,
                    label = stringResource(R.string.developer),
                    onClick = { navigator.navigate(DeveloperScreenDestination) },
                    showDivider = true
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    SectionHeader(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperInfo(
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/fahrez182"
    val telegramUrl = "https://t.me/fahrezone"
    val sociabuzzUrl = "https://sociabuzz.com/fahrezone/tribe"

    if (showDialog) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onDismissRequest = onDismissRequest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF303030)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.fahrez182),
                        contentDescription = "Developer Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF303030))
                    )
                }


                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "fahrez182 (FahrezONE)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.developer_and_maintainer),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "“Everything is an Idea”",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tombol GitHub
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(githubUrl) },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = "GitHub",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.github))
                    }

                    // Tombol Telegram
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(telegramUrl) },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_telegram),
                            contentDescription = "Telegram",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.telegram))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = { uriHandler.openUri(sociabuzzUrl) },
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Coffee,
                        contentDescription = "Support / Donate",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.support_or_donate))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetBottomSheet(
    onDismissRequest: () -> Unit,
    isRoot: Boolean,
    onReset: (rootMode: Boolean, both: Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.reset_path),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.reset_path_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isRoot) {
                AxCard {
                    ResetOptionItem(
                        title = stringResource(R.string.reset_root),
                        path = PathHelper.getWorkingPath(true, AxeronApiConstant.folder.PARENT).absolutePath,
                        onClick = { onReset(true, false) }
                    )
                    SettingsDivider()
                    ResetOptionItem(
                        title = stringResource(R.string.reset_shell),
                        path = PathHelper.getWorkingPath(false, AxeronApiConstant.folder.PARENT).absolutePath,
                        onClick = { onReset(false, false) }
                    )
                    SettingsDivider()
                    ResetOptionItem(
                        title = stringResource(R.string.reset_both),
                        path = "${PathHelper.getWorkingPath(true, AxeronApiConstant.folder.PARENT).absolutePath}\n${PathHelper.getWorkingPath(false, AxeronApiConstant.folder.PARENT).absolutePath}",
                        onClick = { onReset(true, true) }
                    )
                }
            } else {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onReset(false, false) }
                ) {
                    Text(stringResource(R.string.reset))
                }
            }
        }
    }
}

@Composable
fun ResetOptionItem(
    title: String,
    path: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            imageVector = Icons.Default.FolderDelete,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
