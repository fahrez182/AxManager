package frb.axeron.manager.ui.screen

import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import frb.axeron.manager.ui.component.AxTopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ActivateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuickShellScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronCommandSession
import frb.axeron.api.AxeronPluginService
import frb.axeron.api.core.Starter
import frb.axeron.manager.BuildConfig
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxCardShape
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.SectionHeader
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.InlineDivider
import frb.axeron.manager.ui.component.NeedMoreStepDialog
import frb.axeron.manager.ui.component.PowerDialog
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.component.FloatingBarFab
import frb.axeron.manager.ui.util.checkNewVersion
import frb.axeron.manager.ui.util.module.LatestVersionInfo
import frb.axeron.manager.ui.viewmodel.ActivateViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.server.Mode
import frb.axeron.shared.AxeronApiConstant.server.VERSION_CODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    val isRunning = activateViewModel.activateStatus is ActivateViewModel.ActivateStatus.Running

    val loadingDialog = rememberLoadingDialog()
    val scope = rememberCoroutineScope()
    var showPowerDialog by remember { mutableStateOf(false) }

    if (showPowerDialog) {
        PowerDialog(
            onDismiss = { showPowerDialog = false },
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
                    AxeronCommandSession.getQuickCmd(
                        Starter.internalCommand,
                        true,
                        false
                    ),
                    null,
                    null
                )
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
                        Column {
                            Text(
                                modifier = Modifier.padding(start = 10.dp),
                                text = stringResource(R.string.app_name),
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
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        if (isRunning) {
            FloatingBarFab {
                FloatingActionButton(
                    shape = CircleShape,
                    onClick = {
                        navigator.navigate(QuickShellScreenDestination)
                    }
                ) {
                    Icon(Icons.Filled.Terminal, null)
                }
            }
        }

        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                // Padding inside the scroll lets content pass under the fading top bar.
                .padding(
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + LocalFloatingBarInset.current + 16.dp
                )
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                activateViewModel = activateViewModel,
                onOpenActivation = { navigator.navigate(ActivateScreenDestination) },
                onPowerClick = { showPowerDialog = true }
            )

            AnimatedVisibility(visible = isRunning) {
                val pluginCount = pluginViewModel.plugins.size
                val privilegeCount = privilegeViewModel.privilegedCount
                // Plugin and privilege counts share one card, split by a divider.
                AxCard {
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CounterItem(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Extension,
                            label = stringResource(
                                if (pluginCount <= 1) R.string.plugin else R.string.plugin_plural
                            ),
                            value = pluginCount
                        )
                        VerticalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        CounterItem(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.AdminPanelSettings,
                            label = stringResource(
                                if (privilegeCount <= 1) R.string.privilege else R.string.privilege_plural
                            ),
                            value = privilegeCount
                        )
                    }
                }
            }

            UpdateCard()

            HomeSectionHeader(stringResource(R.string.home_section_device))
            InfoCard(activateViewModel)

            HomeSectionHeader(stringResource(R.string.home_section_help))
            LinksCard()
        }
    }
}

@Composable
fun StatusCard(
    activateViewModel: ActivateViewModel,
    onOpenActivation: () -> Unit,
    onPowerClick: () -> Unit
) {
    val axeronInfo = activateViewModel.axeronInfo
    val context = LocalContext.current
    val isRunning = activateViewModel.activateStatus is ActivateViewModel.ActivateStatus.Running
    val isUpdating = activateViewModel.activateStatus is ActivateViewModel.ActivateStatus.Updating
    val isNeedExtraStep = activateViewModel.activateStatus is ActivateViewModel.ActivateStatus.NeedExtraStep

    val containerColor by animateColorAsState(
        targetValue = when {
            isUpdating -> MaterialTheme.colorScheme.primaryContainer
            isNeedExtraStep -> MaterialTheme.colorScheme.errorContainer
            isRunning -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.errorContainer
        },
        animationSpec = tween(400),
        label = "statusContainer"
    )

    val updating = stringResource(R.string.updating)
    var showNeedMoreStepDialog by remember { mutableStateOf(false) }

    if (showNeedMoreStepDialog) {
        NeedMoreStepDialog(onDismiss = { showNeedMoreStepDialog = false })
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        shape = AxCardShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // The activation screen is always reachable from here, except mid-update.
                .clickable {
                    if (isUpdating) {
                        Toast.makeText(context, updating, Toast.LENGTH_SHORT).show()
                    } else {
                        onOpenActivation()
                    }
                }
        ) {
            val fadeColor = MaterialTheme.colorScheme.surfaceVariant

            // Big watermark icon per state, faded into the card by a gradient
            val watermark = when {
                isUpdating -> rememberVectorPainter(Icons.Outlined.Update)
                isNeedExtraStep -> rememberVectorPainter(Icons.Outlined.Build)
                isRunning -> painterResource(R.drawable.ic_axeron)
                else -> rememberVectorPainter(Icons.Outlined.Cancel)
            }
            val watermarkOffset = when {
                isNeedExtraStep -> DpOffset(40.dp, 40.dp)
                isRunning -> DpOffset(10.dp, 30.dp)
                else -> DpOffset(20.dp, 30.dp)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(watermarkOffset.x, watermarkOffset.y),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    modifier = Modifier.size(145.dp),
                    painter = watermark,
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
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: state title (+ mode badge), power button, and a chevron into activation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when {
                                    isUpdating -> updating
                                    isNeedExtraStep -> stringResource(R.string.home_need_fix)
                                    isRunning -> stringResource(R.string.home_running)
                                    else -> stringResource(R.string.home_not_running)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isRunning) {
                                ModeBadge(axeronInfo.serverInfo.getMode())
                            }
                        }
                        when {
                            isUpdating -> Text(
                                text = stringResource(R.string.server_updating_version)
                                    .format(axeronInfo.getVersionCode(), VERSION_CODE),
                                style = MaterialTheme.typography.bodySmall
                            )

                            isNeedExtraStep -> Unit
                            isRunning -> Unit
                            else -> Text(
                                text = stringResource(R.string.home_not_running_msg),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (isRunning) {
                        // Solid fill + tight inner padding so it reads clearly on the tinted card.
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                                .clickable(onClick = onPowerClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Power",
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    if (!isUpdating) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(LocalContentColor.current.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(22.dp),
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = LocalContentColor.current
                            )
                        }
                    }
                }

                if (isNeedExtraStep) {
                    // The fix guide gets its own tap target; the card itself opens activation.
                    Surface(
                        onClick = { showNeedMoreStepDialog = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.error
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(R.string.home_need_fix_msg),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (isRunning) {
                    // The watermark sets a taller card than the content; pin the strip to the bottom.
                    Spacer(Modifier.weight(1f))

                    var time by remember { mutableLongStateOf(0) }

                    LaunchedEffect(Unit) {
                        while (true) {
                            time = SystemClock.elapsedRealtime() - axeronInfo.serverInfo.starting
                            delay(1000)
                        }
                    }

                    ServiceInfoStrip(
                        uptime = formatUptime(
                            time,
                            stringResource(R.string.day_singular),
                            stringResource(R.string.day_plural)
                        ),
                        pid = axeronInfo.serverInfo.pid.toString(),
                        uid = axeronInfo.serverInfo.uid.toString(),
                        version = axeronInfo.getVersionCode().toString()
                    )
                }
            }
        }
    }
}

/**
 * Filled pill for the server mode. Also hosts the hidden crash test (tap 8x quickly),
 * since tapping the card itself now opens the activation screen.
 */
@Composable
private fun ModeBadge(mode: Mode) {
    val scope = rememberCoroutineScope()
    var debugClickCount by remember { mutableIntStateOf(0) }
    var debugJob: Job? by remember { mutableStateOf(null) }

    val (container, content) = when (mode) {
        Mode.ROOT -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
    }
    val icon = when (mode) {
        Mode.ROOT -> Icons.Outlined.Tag
        else -> Icons.Outlined.Terminal
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                debugClickCount++
                debugJob?.cancel()
                debugJob = scope.launch {
                    delay(1000)
                    debugClickCount = 0
                }
                if (debugClickCount >= 8) {
                    throw RuntimeException("AxManager Manual Crash Test")
                }
            }
            .padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            imageVector = icon,
            contentDescription = null,
            tint = content
        )
        Text(
            text = mode.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content
        )
    }
}

/** One half of the plugin / privilege card: icon badge, small label, big count. */
@Composable
private fun CounterItem(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: Int
) {
    Row(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconBadge(imageVector = icon, size = 32.dp)
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Section title between Home cards, matching the Settings section headers. */
@Composable
private fun HomeSectionHeader(text: String) {
    SectionHeader(
        text = text,
        modifier = Modifier.padding(top = 8.dp)
    )
}

private fun formatUptime(millis: Long, daySingular: String, dayPlural: String): String {
    val totalSeconds = millis / 1000
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val dayPart = when {
        days == 1L -> "1 $daySingular "
        days > 1 -> "$days $dayPlural "
        else -> ""
    }

    return "$dayPart%02d:%02d:%02d".format(hours, minutes, seconds)
}

/** AXGC-style sub card: uptime, PID, UID and server version in one strip with dividers. */
@Composable
private fun ServiceInfoStrip(uptime: String, pid: String, uid: String, version: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AxPanelShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = uptime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            InlineDivider(height = 12.dp)
            StripItem(label = "PID", value = pid)
            InlineDivider(height = 12.dp)
            StripItem(label = "UID", value = uid)
            InlineDivider(height = 12.dp)
            StripItem(label = "Ver", value = version)
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
    val title = stringResource(R.string.changelog)
    val updateText = stringResource(R.string.update)
    val newVersionAvailable = stringResource(R.string.new_version_available)

    AnimatedVisibility(
        visible = newVersionCode > currentVersionCode,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { uriHandler.openUri(newVersionUrl) })
        WarningCard(
            message = newVersionAvailable.format(newVersionCode),
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Outlined.Update
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
    message: String,
    color: Color = MaterialTheme.colorScheme.error,
    icon: ImageVector = Icons.Outlined.Cancel,
    onClick: (() -> Unit)? = null
) {
    AxCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(imageVector = icon, tint = color)
            Text(
                modifier = Modifier.weight(1f),
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            if (onClick != null) {
                Chevron()
            }
        }
    }
}

@Composable
fun InfoCard(activateViewModel: ActivateViewModel) {
    val axeronInfo = activateViewModel.axeronInfo

    AxCard {
        // Device facts only; server UID/PID/version live in the status card strip.
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            InfoRow(
                label = stringResource(R.string.device_model),
                value = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                icon = Icons.Outlined.PhoneAndroid
            )
            InfoRowDivider()
            InfoRow(
                label = stringResource(R.string.android_version),
                value = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                icon = Icons.Filled.Android
            )
            InfoRowDivider()
            InfoRow(
                label = stringResource(R.string.security_patch),
                value = Build.VERSION.SECURITY_PATCH,
                icon = Icons.Outlined.Shield
            )
            InfoRowDivider()
            InfoRow(
                label = stringResource(R.string.kernel_version),
                value = System.getProperty("os.version").orEmpty(),
                icon = Icons.Outlined.DeveloperBoard
            )
            InfoRowDivider()
            InfoRow(
                label = stringResource(R.string.abi_supported),
                value = Build.SUPPORTED_ABIS.joinToString(", "),
                icon = Icons.Filled.Memory
            )
            InfoRowDivider()
            InfoRow(
                label = stringResource(R.string.selinux_context),
                value = axeronInfo.serverInfo.selinuxContext,
                icon = Icons.Filled.Security
            )
        }
    }
}

/** Support, docs and issue links in one card instead of three separate ones. */
@Composable
fun LinksCard() {
    val uriHandler = LocalUriHandler.current
    val githubFahrez182 = "https://github.com/fahrez182/AxManager"
    val learnAxManager = "https://fahrez182.github.io/AxManager"
    val githubIssueUrl = "https://github.com/fahrez182/AxManager/issues"
    val telegramUrl = "https://t.me/axeron_manager"

    AxCard {
        LinkRow(
            icon = Icons.Outlined.FavoriteBorder,
            title = stringResource(R.string.support_me),
            message = stringResource(R.string.support_me_msg),
            onClick = { uriHandler.openUri(githubFahrez182) }
        ) {
            LinkTrailingIcon()
        }
        InfoRowDivider()
        LinkRow(
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            title = stringResource(R.string.learn_more),
            message = stringResource(R.string.learn_more_msg),
            onClick = { uriHandler.openUri(learnAxManager) }
        ) {
            LinkTrailingIcon()
        }
        InfoRowDivider()
        LinkRow(
            icon = Icons.Outlined.BugReport,
            title = stringResource(R.string.report_issue),
            message = "${stringResource(R.string.report_issue_msg)} ${stringResource(R.string.report_issue_msg2)}",
            onClick = null
        ) {
            IconButton(onClick = { uriHandler.openUri(githubIssueUrl) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_github),
                    contentDescription = "Report to github",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { uriHandler.openUri(telegramUrl) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_telegram),
                    contentDescription = "Report to telegram",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    title: String,
    message: String,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(imageVector = icon, size = 30.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun LinkTrailingIcon() {
    Icon(
        modifier = Modifier.size(20.dp),
        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun InfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(imageVector = icon, size = 30.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoRowDivider() {
    // Inset to line up with the row text: 14dp gutter + 30dp badge + 12dp gap.
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = 14.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
}

@Composable
private fun StripItem(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
