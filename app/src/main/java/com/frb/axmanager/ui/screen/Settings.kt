package com.frb.axmanager.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frb.axmanager.R
import com.frb.axmanager.ui.component.ConfirmResult
import com.frb.axmanager.ui.component.SettingsItem
import com.frb.axmanager.ui.component.SettingsItemType
import com.frb.axmanager.ui.component.rememberConfirmDialog
import com.frb.axmanager.ui.viewmodel.QuickShellViewModel
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.frb.engine.client.Axeron
import com.frb.engine.utils.Starter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AppearanceScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DeveloperScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PermissionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val adbViewModel = viewModelGlobal.adbViewModel
    val settings = viewModelGlobal.settingsViewModel
    val permissionViewModel = viewModelGlobal.permissionViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val confirmDialog = rememberConfirmDialog()
    val scope = rememberCoroutineScope()

    var showDevDialog by remember { mutableStateOf(false) }

    DeveloperInfo(
        showDevDialog
    ) {
        showDevDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showDevDialog = true
                        })
                    {
                        Icon(Icons.Outlined.Info, null)
                    }
                },
                windowInsets = WindowInsets(top = 0)
            )
        },
        contentWindowInsets = WindowInsets(top = 0, bottom = 0)
    ) { paddingValues ->

        val axeronRunning = adbViewModel.axeronInfo.isRunning()

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(vertical = 16.dp)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            AnimatedVisibility(visible = axeronRunning) {
                SettingsItem(
                    iconPainter = painterResource(com.frb.engine.R.drawable.ic_axeron),
                    label = "Ax Permission Companion",
                    description = "[Experimental] Alternative permission manager built on Shizuku-API for development and testing use",
                    checked = permissionViewModel.isPermissionManagerEnabled,
                    onCheckedClick = {
                        if (it) {
                            navigator.navigate(PermissionScreenDestination)
                        }
                    },
                    onSwitchChange = {
                        permissionViewModel.enablePermissionManager(it)
                    }
                )
            }

            SettingsItem(
                iconVector = Icons.Filled.Refresh,
                label = "Relog to Ignite",
                description = "When you relog AxManager will Re-Ignite",
                checked = settings.isIgniteWhenRelogEnabled,
                onSwitchChange = {
                    settings.setIgniteWhenRelog(it)
                }
            )


            AnimatedVisibility(visible = axeronRunning) {
                SettingsItem(
                    iconVector = Icons.Filled.PowerSettingsNew,
                    label = "Stop/Restart Service",
                    description = "This action will not disable/stop your plugins",
                    onClick = {
                        scope.launch {
                            val confirmResult = confirmDialog.awaitConfirm(
                                "Stop Now?",
                                content = "This action will not disable your plugins",
                                confirm = "Stop",
                                dismiss = "Cancel",
                                neutral = "Restart"
                            )
                            if (confirmResult == ConfirmResult.Confirmed) {
                                Axeron.destroy()
                            } else if (confirmResult == ConfirmResult.Neutral) {
                                Axeron.newProcess(QuickShellViewModel.getQuickCmd(Starter.internalCommand))
                            }
                        }
                    }
                )
            }

            AnimatedVisibility(visible = axeronRunning) {
                SettingsItem(
                    iconVector = Icons.Filled.FolderDelete,
                    label = "Uninstall AxManager",
                    description = "This action will disable all of your plugins",
                    onClick = {
                        scope.launch {
                            val confirmResult = confirmDialog.awaitConfirm(
                                "Uninstall Now?",
                                content = "This action will disable all of your plugins",
                                confirm = "Uninstall",
                                dismiss = "Cancel"
                            )
                            if (confirmResult == ConfirmResult.Confirmed) {
                                navigator.navigate(FlashScreenDestination(FlashIt.FlashUninstall))
                            }
                        }
                    }
                )
            }

            SettingsItem { enabled, checked ->
                AnimatedVisibility(visible = axeronRunning) {
                    SettingsItem(
                        type = SettingsItemType.CHILD,
                        iconVector = Icons.Filled.Apps,
                        label = "AppList Manager",
                        onClick = {
                            navigator.navigate(AppsScreenDestination)
                        }
                    )
                }

                SettingsItem(
                    type = SettingsItemType.CHILD,
                    iconVector = Icons.Filled.Palette,
                    label = "Appearance",
                    onClick = {
                        navigator.navigate(AppearanceScreenDestination)
                    }
                )

                SettingsItem(
                    type = SettingsItemType.CHILD,
                    iconVector = Icons.Filled.BugReport,
                    label = "Developer",
                    onClick = {
                        navigator.navigate(DeveloperScreenDestination)
                    }
                )

            }
        }
    }
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
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.fahrez182),
                        contentDescription = "Developer Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }


                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "fahrez182 (FahrezONE)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Developer | Maintainer",
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
                        Text("GitHub")
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
                        Text("Telegram")
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
                    Text("Support")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview
@Composable
fun SetPreview() {
    SettingsItem(
        label = "Uninstall AxManager"
    )
}