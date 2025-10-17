package com.frb.axmanager.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.frb.axmanager.ui.component.ConfirmResult
import com.frb.axmanager.ui.component.SettingsItem
import com.frb.axmanager.ui.component.rememberConfirmDialog
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.frb.engine.client.Axeron
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AppearanceScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DeveloperScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val adbViewModel = viewModelGlobal.adbViewModel
    val settings = viewModelGlobal.settingsViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val confirmDialog = rememberConfirmDialog()
    val scope = rememberCoroutineScope()

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
                windowInsets = WindowInsets(top = 0)
            )
        },
        contentWindowInsets = WindowInsets(top = 0, bottom = 0)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(bottom = 16.dp)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsItem(
                iconVector = Icons.Filled.DeveloperBoard,
                label = "Developer",
                onClick = {
                    navigator.navigate(DeveloperScreenDestination)
                }
            )

            SettingsItem(
                iconVector = Icons.Filled.Palette,
                label = "Appearance",
                onClick = {
                    navigator.navigate(AppearanceScreenDestination)
                }
            )

            SettingsItem(
                iconVector = Icons.Filled.Refresh,
                label = "Relog to Ignite",
                description = "When you relog AxManager will Re-Ignite",
                checked = settings.isIgniteWhenRelogEnabled,
                onSwitchChange = {
                    settings.setIgniteWhenRelog(it)
                }
            )

            AnimatedVisibility(visible = adbViewModel.axeronInfo.isRunning()) {
                SettingsItem(
                    iconVector = Icons.Filled.PowerSettingsNew,
                    label = "Stop Service",
                    description = "This action will not disable/stop your plugins",
                    onClick = {
                        scope.launch {
                            val confirmResult = confirmDialog.awaitConfirm(
                                "Stop Now?",
                                content = "This action will not disable your plugins",
                                confirm = "Stop",
                                dismiss = "Cancel"
                            )
                            if (confirmResult == ConfirmResult.Confirmed) {
                                Axeron.destroy()
                            }
                        }
                    }
                )
            }

            AnimatedVisibility(visible = adbViewModel.axeronInfo.isRunning()) {
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