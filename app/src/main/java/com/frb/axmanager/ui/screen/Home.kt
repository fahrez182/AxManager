package com.frb.axmanager.ui.screen

import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.dergoogler.mmrl.ui.component.text.TextRow
import com.frb.axmanager.BuildConfig
import com.frb.axmanager.R
import com.frb.axmanager.ui.component.rememberConfirmDialog
import com.frb.axmanager.ui.util.checkNewVersion
import com.frb.axmanager.ui.util.module.LatestVersionInfo
import com.frb.axmanager.ui.viewmodel.AdbViewModel
import com.frb.axmanager.ui.viewmodel.AppsViewModel
import com.frb.axmanager.ui.viewmodel.PluginsViewModel
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.frb.engine.client.Axeron
import com.frb.engine.client.PluginService
import com.frb.engine.implementation.AxeronService
import com.frb.engine.utils.Starter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ActivateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuickShellScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val appsViewModel = viewModelGlobal.appsViewModel
    val pluginsViewModel = viewModelGlobal.pluginsViewModel
    val adbViewModel = viewModelGlobal.adbViewModel

    val axeronInfo = adbViewModel.axeronInfo

    LaunchedEffect(axeronInfo) {
        if (axeronInfo.isNeedUpdate()) {
            Log.d(
                "AxManager",
                "NeedUpdate ${Axeron.getInfo().versionCode} > ${AxeronService.VERSION_CODE}"
            )
            adbViewModel.isUpdating = true
            Axeron.newProcess(
                arrayOf(
                    "setsid",
                    "sh",
                    "-c",
                    "export PARENT_PID=$$; echo \"\\r\$PARENT_PID\\r\"; exec sh -c \"$0\"",
                    Starter.internalCommand
                )
            )

        } else {
            adbViewModel.isUpdating = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            modifier = Modifier.padding(start = 10.dp),
                            text = "AxManager",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                        )
                    }

                },
                actions = {
                    val scope = rememberCoroutineScope()
                    val rotation = remember { Animatable(0f) }

                    AnimatedVisibility(visible = adbViewModel.axeronInfo.isRunning()) {
                        Card(
                            shape = RoundedCornerShape(48.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            onClick = {
                                scope.launch {
                                    rotation.snapTo(0f) // mulai dari 0
                                    rotation.animateTo(
                                        targetValue = -360f,
                                        animationSpec = tween(
                                            durationMillis = 300,
                                            easing = LinearEasing
                                        )
                                    )
                                    rotation.snapTo(0f) // reset ke 0 setelah animasi selesai
                                }
                                PluginService.startInitService()
                                pluginsViewModel.markNeedRefresh()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 9.dp)
                                    .padding(start = 16.dp, end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Restart",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.RestartAlt,
                                    contentDescription = "Restart",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer {
                                            rotationZ = rotation.value
                                        },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.padding(end = 12.dp))
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
                .padding(top = 12.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                StatusCard(adbViewModel = adbViewModel, modifier = Modifier.weight(1f)) {
                    if (it) {
                        navigator.navigate(QuickShellScreenDestination)
                    } else {
                        navigator.navigate(ActivateScreenDestination)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppsCard(appsViewModel, modifier = Modifier.weight(1f))
                    PluginCard(pluginsViewModel, modifier = Modifier.weight(1f))
                }
            }

            UpdateCard()
            InfoCard(adbViewModel)
            IssueReportCard()
        }
    }
}

@Composable
fun StatusCard(
    modifier: Modifier,
    adbViewModel: AdbViewModel,
    onClick: (Boolean) -> Unit = {}
) {
    val axeronInfo = adbViewModel.axeronInfo
    val context = LocalContext.current
    Log.d("AxManager", "NeedUpdate: ${axeronInfo.isNeedUpdate()}")

    val uriHandler = LocalUriHandler.current
    val extraStepUrl = "https://fahrez182.github.io/AxManager/guide/faq.html#start-via-wireless-debugging-start-by-connecting-to-a-computer-the-permission-of-adb-is-limited"

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = run {
                when {
                    adbViewModel.isUpdating -> MaterialTheme.colorScheme.primaryContainer
                    axeronInfo.isNeedExtraStep() -> MaterialTheme.colorScheme.errorContainer
                    axeronInfo.isRunning() -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            }
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clickable {
                    if (adbViewModel.isUpdating) {
                        Toast.makeText(context, "Updating...", Toast.LENGTH_SHORT).show()
                        return@clickable
                    }
                    if (axeronInfo.isNeedExtraStep()) {
                        uriHandler.openUri(extraStepUrl)
                        return@clickable
                    }
                    onClick(axeronInfo.isRunning())
                }
                .padding(16.dp)
        ) {
            when {
                adbViewModel.isUpdating -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(38.dp, 45.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(150.dp),
                            imageVector = Icons.Outlined.Update,
                            contentDescription = null
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Updating...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Version: ${axeronInfo.versionCode} > ${AxeronService.VERSION_CODE}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                axeronInfo.isNeedExtraStep() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(42.dp, 60.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(150.dp),
                            imageVector = Icons.Outlined.Build,
                            contentDescription = null
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Need extra step",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Click to see how to fix",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                axeronInfo.isRunning() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(38.dp, 45.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(145.dp),
                            imageVector = Icons.Filled.KeyboardCommandKey,
                            contentDescription = null
                        )
                    }
                    Column(
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
                                                text = axeronInfo.getMode(),
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
                            text = "Version: ${axeronInfo.versionCode}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(38.dp, 45.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(150.dp),
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = null
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Need to Activate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Click to activating AxManager",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateCard() {
    val latestVersionInfo = LatestVersionInfo()
    val newVersion by produceState(initialValue = latestVersionInfo) {
        value = withContext(Dispatchers.IO) {
            checkNewVersion()
        }
    }

    val currentVersionCode = BuildConfig.VERSION_CODE
    val newVersionCode = newVersion.versionCode
    val newVersionUrl = newVersion.downloadUrl
    val changelog = newVersion.changelog

    val uriHandler = LocalUriHandler.current
    val title = "Changelog"
    val updateText = "Update"

    AnimatedVisibility(
        visible = newVersionCode > currentVersionCode,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { uriHandler.openUri(newVersionUrl) })
        WarningCard(
            message = "New version %s is available, click to upgrade.".format(newVersionCode),
            MaterialTheme.colorScheme.outlineVariant
        ) {
            if (changelog.isEmpty()) {
                uriHandler.openUri(newVersionUrl)
            } else {
                updateDialog.showConfirm(
                    title = title,
                    content = changelog,
                    markdown = true,
                    confirm = updateText
                )
            }
        }
    }
}

@Composable
fun WarningCard(
    message: String, color: Color = MaterialTheme.colorScheme.error, onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = color
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(24.dp)
        ) {
            Text(
                text = message, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PluginCard(pluginsViewModel: PluginsViewModel, modifier: Modifier) {
    val count = pluginsViewModel.plugins.size
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (count <= 1) {
                    "Plugin"
                } else {
                    "Plugins"
                },
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AppsCard(appsViewModel: AppsViewModel, modifier: Modifier) {
    val count = appsViewModel.addedPackageNames.size

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (count <= 1) {
                    "Application"
                } else {
                    "Applications"
                },
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

    }
}

@Composable
fun InfoCard(adbViewModel: AdbViewModel) {
    val axeronInfo = adbViewModel.axeronInfo

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
                                modifier = Modifier
                                    .padding(end = 20.dp)
                                    .size(26.dp)
                            )

                            is Painter -> Icon(
                                painter = icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 20.dp)
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

            var time by remember { mutableLongStateOf(0) }

            LaunchedEffect(Unit) {
                while (true) {
                    time = SystemClock.elapsedRealtime() - axeronInfo.starting
                    delay(1000)
                }
            }

            fun formatUptime(millis: Long): String {
                val totalSeconds = millis / 1000
                val days = totalSeconds / 86400
                val hours = (totalSeconds % 86400) / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                val dayPart = when {
                    days == 1L -> "1 Day "
                    days > 1 -> "$days Days "
                    else -> ""
                }

                return "T+$dayPart%02d:%02d:%02d".format(hours, minutes, seconds)
            }

            InfoCardItem(
                label = "Manager Version",
                content = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                icon = painterResource(com.frb.engine.R.drawable.ic_axeron),
            )

            if (axeronInfo.isRunning()) {
                Spacer(Modifier.height(16.dp))
                InfoCardItem(
                    label = "Service Uptime",
                    content = formatUptime(time),
                    icon = Icons.Filled.MiscellaneousServices,
                )
            }

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
                content = axeronInfo.selinuxContext ?: "Unknown",
                icon = Icons.Filled.Security,
            )

        }
    }
}

@Composable
fun IssueReportCard() {
    val uriHandler = LocalUriHandler.current
    val githubIssueUrl = "https://github.com/fahrez182/AxManager/issues"
    val telegramUrl = "https://t.me/axeron_manager"

    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Having Trouble?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Encountered a bug or have feedback?",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Report it as soon as possible!",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = { uriHandler.openUri(githubIssueUrl) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = "Report to github",
                    )
                }
                IconButton(onClick = { uriHandler.openUri(telegramUrl) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_telegram),
                        contentDescription = "Report to telegram",
                    )
                }
            }
        }
    }
}

//@Preview(showBackground = false)
//@Composable
//fun CardPreview() {
//    StatusCard(homeViewModel)
//}
