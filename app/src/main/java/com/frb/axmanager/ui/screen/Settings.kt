package com.frb.axmanager.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frb.axmanager.ui.component.ConfirmResult
import com.frb.axmanager.ui.component.rememberConfirmDialog
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.frb.engine.client.Axeron
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.DeveloperScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val adbViewModel = viewModelGlobal.adbViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val confirmDialog = rememberConfirmDialog()
    val scope = rememberCoroutineScope()
    LocalContext.current

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
                            fontWeight = FontWeight.Black,
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
                .verticalScroll(rememberScrollState())
        ) {
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.DeveloperBoard,
                        null
                    )
                },
                headlineContent = { Text(
                    text = "Developer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                ) },
                modifier = Modifier.clickable {
                    navigator.navigate(DeveloperScreenDestination)
                }
            )

            AnimatedVisibility(visible = adbViewModel.axeronInfo.isRunning()) {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.PowerSettingsNew, "power") },
                    headlineContent = { Text(
                        text = "Stop AxManager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    ) },
                    supportingContent = { Text("This action will not disable your plugins") },
                    modifier = Modifier.clickable {
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
                ListItem(
                    leadingContent = { Icon(Icons.Filled.FolderDelete, "reset") },
                    headlineContent = { Text(
                        text = "Uninstall AxManager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    ) },
                    supportingContent = { Text("This action will disable all of your plugins") },
                    modifier = Modifier.clickable {
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