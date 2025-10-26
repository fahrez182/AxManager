package com.frb.axmanager.ui.screen

import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frb.axmanager.BuildConfig
import com.frb.axmanager.R
import com.frb.axmanager.ui.component.ExtraLabel
import com.frb.axmanager.ui.component.ExtraLabelDefaults
import com.frb.axmanager.ui.component.rememberConfirmDialog
import com.frb.axmanager.ui.component.rememberLoadingDialog
import com.frb.axmanager.ui.component.scaleDp
import com.frb.axmanager.ui.util.checkNewVersion
import com.frb.axmanager.ui.util.module.LatestVersionInfo
import com.frb.axmanager.ui.viewmodel.AdbViewModel
import com.frb.axmanager.ui.viewmodel.AppsViewModel
import com.frb.axmanager.ui.viewmodel.PluginViewModel
import com.frb.axmanager.ui.viewmodel.QuickShellViewModel
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val pluginViewModel = viewModelGlobal.pluginViewModel
    val adbViewModel = viewModelGlobal.adbViewModel

    val axeronInfo = adbViewModel.axeronInfo

    LaunchedEffect(axeronInfo) {
        if (axeronInfo.isNeedUpdate()) {
            Log.d(
                "AxManager",
                "NeedUpdate ${Axeron.getInfo().versionCode} > ${AxeronService.VERSION_CODE}"
            )
            adbViewModel.isUpdating = true
            Axeron.newProcess(QuickShellViewModel.getQuickCmd(Starter.internalCommand), null, null)
        } else {
            adbViewModel.isUpdating = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                modifier = Modifier.padding(start = 10.dp),
                                text = "AxManager",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                modifier = Modifier.padding(start = 10.dp),
                                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                },
                actions = {
                    val loadingDialog = rememberLoadingDialog()
                    val scope = rememberCoroutineScope()

                    AnimatedVisibility(visible = adbViewModel.axeronInfo.isRunning()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors().copy(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            elevation = CardDefaults.cardElevation(4.dp),
                            onClick = {
                                scope.launch {
                                    val success = loadingDialog.withLoading {
                                        PluginService.igniteSuspendService()
                                    }

                                    if (success) {
                                        pluginViewModel.fetchModuleList()
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 9.dp)
                                    .padding(start = 16.dp, end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Re-ignite",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.LocalFireDepartment,
                                    contentDescription = "Reignite",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(24.dp)
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
        floatingActionButton = {
            AnimatedVisibility(visible = adbViewModel.axeronInfo.isRunning()) {
                FloatingActionButton(
                    onClick = {
                        navigator.navigate(QuickShellScreenDestination)
                    }
                ) {
                    Icon(Icons.Filled.Terminal, null)
                }
            }
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
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                adbViewModel = adbViewModel,
                pluginViewModel = pluginViewModel,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!it) {
                    navigator.navigate(ActivateScreenDestination)
                }
            }

            UpdateCard()
            InfoCard(adbViewModel)

            SupportCard()
            LearnCard()
            IssueReportCard()
        }
    }
}

@Composable
fun SupportCard() {
    val uriHandler = LocalUriHandler.current
    val githubFahrez182 = "https://github.com/fahrez182/AxManager"

    ElevatedCard(
        onClick = {
            uriHandler.openUri(githubFahrez182)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Support Me",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "AxManager is completely free and open-source. If you find it useful, please follow the project and consider giving it a star on GitHub.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Icon(
                modifier = Modifier.padding(end = 10.dp, start = 24.dp),
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Support to github",
            )
        }
    }
}

@Composable
fun LearnCard() {
    val uriHandler = LocalUriHandler.current
    val learnAxManager = "https://fahrez182.github.io/AxManager"

    ElevatedCard(
        onClick = {
            uriHandler.openUri(learnAxManager)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Learn AxManager",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Learn how to use AxManager and how to make a plugin",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Icon(
                modifier = Modifier.padding(end = 10.dp, start = 24.dp),
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Support to github",
            )
        }
    }
}

@Composable
fun StatusCard(
    modifier: Modifier,
    adbViewModel: AdbViewModel,
    pluginViewModel: PluginViewModel,
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
        ) {
            val isDark = isSystemInDarkTheme()
            val colorScheme = MaterialTheme.colorScheme
            val fadeColor = when {
                isDark -> colorScheme.surfaceVariant
                else -> colorScheme.surfaceVariant
            }

            when {
                adbViewModel.isUpdating -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(38.dp, 45.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(150.dp),
                            imageVector = Icons.Outlined.Update,
                            contentDescription = null
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        fadeColor.copy(alpha = 0.0f),
                                        fadeColor.copy(alpha = 0.55f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                            .offset(42.dp, 60.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(150.dp),
                            imageVector = Icons.Outlined.Build,
                            contentDescription = null
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        fadeColor.copy(alpha = 0.0f),
                                        fadeColor.copy(alpha = 0.55f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                            .fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(145.dp).offset(38.dp, 45.dp),
                            painter = painterResource(com.frb.engine.R.drawable.ic_axeron),
                            contentDescription = null
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        fadeColor.copy(alpha = 0.0f),
                                        fadeColor.copy(alpha = 0.55f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.matchParentSize().padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_running),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            ExtraLabel(
                                text = axeronInfo.getMode(),
                                style = ExtraLabelDefaults.style.copy(
                                    allCaps = false
                                )
                            )
                        }

                        Text(
                            text = "Version: ${axeronInfo.versionCode} | Pid: ${axeronInfo.pid}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(Modifier.weight(1f))

                        PluginCard(
                            pluginViewModel
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(38.dp, 45.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(150.dp),
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = null
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        fadeColor.copy(alpha = 0.0f),
                                        fadeColor.copy(alpha = 0.55f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
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
fun PluginCard(pluginViewModel: PluginViewModel) {
    val count = pluginViewModel.plugins.size
    ElevatedCard(
        modifier = Modifier.width(180.dp),
    ) {
        Column(
            modifier = Modifier
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(22.scaleDp),
                    imageVector = Icons.Filled.Extension,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = null
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Filled.Apps,
                    contentDescription = null
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            when (icon) {
                                is ImageVector -> Icon(
                                    imageVector = icon,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(end = 22.dp)
                                        .size(22.dp)
                                )

                                is Painter -> Icon(
                                    painter = icon,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(end = 22.dp)
                                        .size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(Modifier.padding(top = 12.dp), DividerDefaults.Thickness, MaterialTheme.colorScheme.surfaceContainerHighest)
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

            if (axeronInfo.isRunning()) {
                InfoCardItem(
                    label = "Service Runtime",
                    content = formatUptime(time),
                    icon = Icons.Filled.MiscellaneousServices,
                )
                Spacer(Modifier.height(16.dp))
            }

            InfoCardItem(
                label = "Android Version",
                content = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
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
                content = axeronInfo.selinuxContext,
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
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Report it as soon as possible!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
