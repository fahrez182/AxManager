package com.frb.axmanager.ui.screen

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.dergoogler.mmrl.ui.component.text.TextRow
import com.frb.axmanager.BuildConfig
import com.frb.axmanager.R
import com.frb.axmanager.ui.navigation.ScreenItem
import com.frb.axmanager.ui.viewmodel.AppsViewModel
import com.frb.axmanager.ui.viewmodel.HomeViewModel
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, viewModelGlobal: ViewModelGlobal) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val appsViewModel = viewModelGlobal.appsViewModel
    val homeViewModel = viewModelGlobal.homeViewModel

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = "AxManager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(top = 0)
            )
        },
        contentWindowInsets = WindowInsets(top = 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(homeViewModel) {
                if (it) {
                    navController.navigate(ScreenItem.QuickShell.route)
                } else {
                    navController.navigate(ScreenItem.Activate.route)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppsCard(appsViewModel, modifier = Modifier.weight(1f))
                PluginCard(modifier = Modifier.weight(1f))
            }

            InfoCard(homeViewModel)
        }
    }
}

@Composable
fun StatusCard(homeViewModel: HomeViewModel, onClick: (Boolean) -> Unit = {}) {
    val axeronServiceInfo by homeViewModel.axeronServiceInfo.collectAsState()

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = run {
                if (axeronServiceInfo.isRunning()) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onClick(axeronServiceInfo.isRunning())
                }
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                axeronServiceInfo.isRunning() -> {
                    Icon(
                        imageVector = Icons.Filled.KeyboardCommandKey,
                        contentDescription = "Running"
                    )
                    Column(
                        modifier = Modifier.padding(start = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val labelStyle = LabelItemDefaults.style
                        TextRow(
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LabelItem(
                                        text = {
                                            Text(
                                                text = axeronServiceInfo.getMode(),
                                                style = labelStyle.textStyle.copy(color = labelStyle.contentColor),
                                            )
                                        }
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_running),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "Version: ${axeronServiceInfo.versionCode}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                else -> {
                    Icon(Icons.Filled.Cancel, "NotActivated")
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(
                            text = "Need to Activate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Please click me to activating AxManager",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PluginCard(modifier: Modifier) {
    val count = 0
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (count <= 1) {
                    "Plugin"
                } else {
                    "Plugins"
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AppsCard(appsViewModel: AppsViewModel, modifier: Modifier) {
    val addedApp by appsViewModel.addedApps.collectAsState()
    val count = addedApp.size

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (count <= 1) {
                    "Application"
                } else {
                    "Applications"
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

    }
}

@Composable
fun InfoCard(homeViewModel: HomeViewModel) {
    val axeronServiceInfo by homeViewModel.axeronServiceInfo.collectAsState()

    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            @Composable
            fun InfoCardItem(label: String, content: String, icon: Any? = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        when (icon) {
                            is ImageVector -> Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 20.dp)
                                    .size(26.dp)
                            )

                            is Painter -> Icon(
                                painter = icon,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 20.dp)
                                    .size(26.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            InfoCardItem(
                label = "Manager Version",
                content = BuildConfig.VERSION_NAME,
                icon = painterResource(R.drawable.ic_axeron),
            )

            Spacer(Modifier.height(16.dp))
            InfoCardItem(
                label = "Android Version",
                content = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
                icon = Icons.Filled.Android,
            )

            Spacer(Modifier.height(16.dp))
            InfoCardItem(
                label = "ABI Supported",
                content = Build.SUPPORTED_ABIS.joinToString(", "),
                icon = Icons.Filled.Memory,
            )

            Spacer(Modifier.height(16.dp))
            InfoCardItem(
                label = "SELinux Context",
                content = axeronServiceInfo.selinuxContext ?: "Unknown",
                icon = Icons.Filled.Security,
            )

        }
    }
}

//@Preview(showBackground = false)
//@Composable
//fun CardPreview() {
//    StatusCard(homeViewModel)
//}
