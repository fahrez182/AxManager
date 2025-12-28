package frb.axeron.manager.ui.screen

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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ActivateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuickShellScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronPluginService
import frb.axeron.data.AxeronConstant.server.VERSION_CODE
import frb.axeron.manager.BuildConfig
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.ExtraLabel
import frb.axeron.manager.ui.component.ExtraLabelDefaults
import frb.axeron.manager.ui.component.PowerDialog
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.screen.home.PluginCard
import frb.axeron.manager.ui.screen.home.PrivilegeCard
import frb.axeron.manager.ui.util.checkNewVersion
import frb.axeron.manager.ui.util.module.LatestVersionInfo
import frb.axeron.manager.ui.viewmodel.ActivateViewModel
import frb.axeron.manager.ui.viewmodel.QuickShellViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.server.utils.Starter
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
    val privilegeViewModel = viewModelGlobal.privilegeViewModel
    val activateViewModel = viewModelGlobal.activateViewModel

    val axeronInfo = activateViewModel.axeronInfo

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

                    var showDialog by remember { mutableStateOf(false) }

                    if (showDialog) {
                        PowerDialog(
                            onDismiss = { showDialog = false },
                            onReignite = {
                                scope.launch {
                                    val success = loadingDialog.withLoading {
                                        AxeronPluginService.igniteSuspendService()
                                    }

                                    if (success) {
                                        pluginViewModel.fetchModuleList()
                                    }
                                }
                            },
                            onShutdown = { Axeron.destroy() },
                            onRestart = {
                                Axeron.newProcess(
                                    QuickShellViewModel.getQuickCmd(Starter.internalCommand),
                                    null,
                                    null
                                )
                            }
                        )
                    }

                    AnimatedVisibility(visible = axeronInfo.isRunning()) {
                        IconButton(
                            modifier = Modifier.padding(end = 2.dp),
                            onClick = { showDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Shutdown"
                            )
                        }

                    }
                    Spacer(modifier = Modifier.padding(end = 12.dp))
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = axeronInfo.isRunning()) {
                FloatingActionButton(
                    onClick = {
                        navigator.navigate(QuickShellScreenDestination)
                    }
                ) {
                    Icon(Icons.Filled.Terminal, null)
                }
            }
        }
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
                activateViewModel = activateViewModel
            ) {
                if (!it) {
                    navigator.navigate(ActivateScreenDestination)
                }
            }
            AnimatedVisibility(visible = axeronInfo.isRunning()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PluginCard(
                        Modifier.weight(1f),
                        pluginViewModel
                    )
                    PrivilegeCard(
                        Modifier.weight(1f),
                        privilegeViewModel
                    )
                }
            }

            UpdateCard()
            InfoCard(activateViewModel)

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
    activateViewModel: ActivateViewModel,
    onClick: (Boolean) -> Unit = {}
) {
    val axeronInfo = activateViewModel.axeronInfo
    val context = LocalContext.current
    Log.d("AxManager", "NeedUpdate: ${axeronInfo.isNeedUpdate()}")

    val uriHandler = LocalUriHandler.current
    val extraStepUrl =
        "https://fahrez182.github.io/AxManager/guide/faq.html#start-via-wireless-debugging-start-by-connecting-to-a-computer-the-permission-of-adb-is-limited"

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = run {
                when {
                    axeronInfo.isNeedUpdate() -> MaterialTheme.colorScheme.primaryContainer
                    axeronInfo.isNeedExtraStep() -> MaterialTheme.colorScheme.errorContainer
                    axeronInfo.isRunning() -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (axeronInfo.isNeedUpdate()) {
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
                axeronInfo.isNeedUpdate() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(20.dp, 30.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(145.dp),
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Version: ${axeronInfo.serverInfo.versionCode} > $VERSION_CODE",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                axeronInfo.isNeedExtraStep() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(40.dp, 40.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(145.dp),
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
                            style = MaterialTheme.typography.titleLarge,
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
                            .offset(10.dp, 30.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(145.dp),
                            painter = painterResource(R.drawable.ic_axeron),
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
                        modifier = Modifier
                            .matchParentSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_running),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            ExtraLabel(
                                text = axeronInfo.serverInfo.getMode().label,
                                style = ExtraLabelDefaults.style.copy(
                                    allCaps = false
                                )
                            )
                        }

                        Text(
                            text = "Version: ${axeronInfo.serverInfo.versionCode} | Pid: ${axeronInfo.serverInfo.pid}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(Modifier.weight(1f))

                        var time by remember { mutableLongStateOf(0) }

                        LaunchedEffect(Unit) {
                            while (true) {
                                time =
                                    SystemClock.elapsedRealtime() - axeronInfo.serverInfo.starting
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

                        Text(
                            text = formatUptime(time),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                else -> {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(20.dp, 30.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(145.dp),
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
                            style = MaterialTheme.typography.titleLarge,
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
            message = "New version %s is available, click to update.".format(newVersionCode),
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
fun InfoCard(activateViewModel: ActivateViewModel) {
    val axeronInfo = activateViewModel.axeronInfo

    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            @Composable
            fun InfoCardItem(label: String, content: String, icon: Any? = null) {
                Card {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                }
            }

            InfoCardItem(
                label = "Android Version",
                content = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                icon = Icons.Filled.Android,
            )

            InfoCardItem(
                label = "ABI Supported",
                content = Build.SUPPORTED_ABIS.joinToString(", "),
                icon = Icons.Filled.Memory,
            )

            InfoCardItem(
                label = "SELinux Context",
                content = axeronInfo.serverInfo.selinuxContext,
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
