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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.Coffee
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
import androidx.compose.ui.unit.Dp
import frb.axeron.manager.ui.component.SettingsDivider
import frb.axeron.api.core.AxeronSettings
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.outlined.History
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
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
import frb.axeron.manager.BuildConfig
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxCardShape
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.SectionHeader
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.StatusPill
import frb.axeron.manager.data.QuickSettingsStore
import frb.axeron.manager.data.SettingsAccess
import androidx.compose.material.icons.outlined.Bolt
import frb.axeron.manager.ui.component.InlineDivider
import frb.axeron.manager.ui.component.NeedMoreStepDialog
import frb.axeron.manager.ui.component.PowerDialog
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.component.rememberReigniteSheet
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.component.FloatingBarFab
import frb.axeron.manager.ui.util.checkNewVersion
import frb.axeron.manager.ui.util.module.LatestVersionInfo
import frb.axeron.manager.ui.viewmodel.ActivateViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.server.Mode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
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

    val reigniteSheet = rememberReigniteSheet()
    val shutdownTitle = stringResource(R.string.server_shutdown)
    val restartTitle = stringResource(R.string.server_restart)
    val scope = rememberCoroutineScope()
    var showPowerDialog by remember { mutableStateOf(false) }

    if (showPowerDialog) {
        PowerDialog(
            onDismiss = { showPowerDialog = false },
            onReignite = {
                scope.launch {
                    val success = reigniteSheet.reignite()

                    if (success) {
                        pluginViewModel.fetchModuleList()
                    }
                }
            },
            onShutdown = {
                scope.launch {
                    reigniteSheet.run(shutdownTitle) { activateViewModel.shutdownServer(it) }
                }
            },
            onRestart = {
                scope.launch {
                    val success = reigniteSheet.run(restartTitle) {
                        activateViewModel.restartServer(it)
                    }
                    if (success) {
                        pluginViewModel.fetchModuleList()
                    }
                }
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
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
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
            // Status and the plugin/privilege counts read as one card: square inner edges,
            // rounded only on the outside of the group (just the status card when stopped).
            Column(modifier = Modifier.clip(AxCardShape)) {
                StatusCard(
                    activateViewModel = activateViewModel,
                    onOpenActivation = { navigator.navigate(ActivateScreenDestination) },
                    onPowerClick = { showPowerDialog = true },
                    shape = RectangleShape
                )

                AnimatedVisibility(visible = isRunning) {
                    val pluginCount = pluginViewModel.plugins.size
                    val privilegeCount = privilegeViewModel.privilegedCount
                    // Privilege and plugin counts share one section, split by a divider.
                    AxCard(shape = RectangleShape) {
                        Row(
                            modifier = Modifier.height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CounterItem(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.AdminPanelSettings,
                                label = stringResource(
                                    if (privilegeCount <= 1) R.string.privilege else R.string.privilege_plural
                                ),
                                value = privilegeCount
                            )
                            VerticalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                            CounterItem(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.Extension,
                                label = stringResource(
                                    if (pluginCount <= 1) R.string.plugin else R.string.plugin_plural
                                ),
                                value = pluginCount
                            )
                        }
                    }
                }
            }

            // With the server off, Quick Settings still works through the settings permissions
            // the server granted earlier; offer it while both are held.
            val context = LocalContext.current
            val quickSettingsOffline = remember(isRunning) {
                !isRunning && SettingsAccess.canWriteWithoutServer(context)
            }
            var showQuickSettings by remember { mutableStateOf(false) }
            if (showQuickSettings) {
                QuickSettingsSheet(onDismissRequest = { showQuickSettings = false })
            }
            AnimatedVisibility(visible = quickSettingsOffline) {
                QuickSettingsCard(
                    count = QuickSettingsStore.items.size,
                    onClick = { showQuickSettings = true }
                )
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
    onPowerClick: () -> Unit,
    shape: Shape = AxCardShape
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
        shape = shape,
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
                // Header: state title (+ badge), power button, and a chevron into activation.
                // Top-aligned so the pill stays in the top-right corner even when a subtitle
                // sits under the title (stopped/updating).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
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
                            // Every state gets a badge, like the server mode while running.
                            val colors = MaterialTheme.colorScheme
                            when {
                                isRunning -> ModeBadge(
                                    axeronInfo.serverInfo.getMode(),
                                    axeronInfo.serverInfo.uid
                                )
                                isUpdating -> StatusBadge(
                                    text = "v${axeronInfo.getVersionCode()} → v$VERSION_CODE",
                                    container = colors.primary,
                                    content = colors.onPrimary
                                )

                                isNeedExtraStep -> StatusBadge(
                                    text = stringResource(R.string.home_badge_action),
                                    container = colors.error,
                                    content = colors.onError
                                )

                                else -> StatusBadge(
                                    text = stringResource(R.string.home_badge_stopped),
                                    container = colors.error,
                                    content = colors.onError
                                )
                            }
                        }
                        if (!isRunning && !isNeedExtraStep) {
                            Text(
                                text = stringResource(
                                    if (isUpdating) R.string.home_updating_msg else R.string.home_not_running_msg
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (!isUpdating) {
                        HeaderActionsPill(
                            showPower = isRunning,
                            onPowerClick = onPowerClick
                        )
                    }
                }

                // The watermark sets a taller card than the content; pin the strip to the bottom.
                Spacer(Modifier.weight(1f))

                when {
                    isUpdating -> UpdatingStrip()
                    // The fix guide gets its own tap target; the card itself opens activation.
                    isNeedExtraStep -> FixGuideStrip(onClick = { showNeedMoreStepDialog = true })
                    !isRunning -> StoppedStrip()
                }

                if (isRunning) {
                    var time by remember { mutableLongStateOf(0) }

                    LaunchedEffect(Unit) {
                        while (true) {
                            time = SystemClock.elapsedRealtime() - axeronInfo.serverInfo.starting
                            delay(1000)
                        }
                    }

                    if (activateViewModel.wakeLockUsable) {
                        WakeLockStrip(
                            held = activateViewModel.wakeLockHeld,
                            onChange = activateViewModel::setWakeLock
                        )
                    }
                    ServiceInfoStrip(
                        uptime = formatUptime(
                            time,
                            stringResource(R.string.day_singular),
                            stringResource(R.string.day_plural)
                        ),
                        pid = axeronInfo.serverInfo.pid.toString(),
                        version = axeronInfo.getVersionCode().toString()
                    )
                }
            }
        }
    }
}

/**
 * Power (while running) and the "opens activation" chevron as one small pill split by a divider.
 * Only the power half is its own tap target; taps on the chevron half fall through to the card.
 */
@Composable
private fun HeaderActionsPill(showPower: Boolean, onPowerClick: () -> Unit) {
    val content = LocalContentColor.current
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(CircleShape)
            .background(content.copy(alpha = 0.12f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showPower) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(onClick = onPowerClick)
                    .padding(start = 10.dp, end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power",
                    tint = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(content.copy(alpha = 0.25f))
            )
        }
        Icon(
            modifier = Modifier
                .padding(start = if (showPower) 4.dp else 6.dp, end = 6.dp)
                .size(18.dp),
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = content
        )
    }
}

/**
 * Filled pill with the server's uid, colored by its mode. Also hosts the hidden crash test (tap
 * 8x quickly), since tapping the card itself now opens the activation screen.
 */
@Composable
private fun ModeBadge(mode: Mode, uid: Int) {
    val scope = rememberCoroutineScope()
    var debugClickCount by remember { mutableIntStateOf(0) }
    var debugJob: Job? by remember { mutableStateOf(null) }

    val (container, content) = when (mode) {
        Mode.ROOT -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
    }
    UidBadge(
        uid = uid,
        container = container,
        content = content,
        modifier = Modifier.clickable(
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
    )
}

/**
 * Two-part pill: a tinted "UID" label segment, a hairline, then the uid itself in bold
 * monospace, so the number reads as the value and the label as its caption.
 */
@Composable
private fun UidBadge(uid: Int, container: Color, content: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .clip(CircleShape)
            .background(container)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(content.copy(alpha = 0.16f))
                .padding(start = 10.dp, end = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "UID",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
                color = content.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(content.copy(alpha = 0.35f))
        )
        Text(
            text = uid.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(start = 7.dp, end = 10.dp, top = 3.dp, bottom = 3.dp)
        )
    }
}

/** Filled pill next to the status title (server mode, "Stopped", version change…). */
@Composable
private fun StatusBadge(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .then(modifier)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1
        )
    }
}

/**
 * The sub card pinned to the bottom of the status card, in every state: server facts while
 * running, last method/boot while stopped, the fix guide, or update progress.
 */
@Composable
private fun StatusStrip(
    onClick: (() -> Unit)? = null,
    /** False wraps the strip around its content, as a chip (the wakelock toggle). */
    fillWidth: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    val width = if (fillWidth) Modifier.fillMaxWidth() else Modifier
    val row: @Composable () -> Unit = {
        Row(
            modifier = width.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
    // Plain clickable instead of Surface(onClick): that one pads the strip to a 48dp touch target
    // with transparent space around the visible part, which lifted it off the card's bottom edge.
    Surface(
        modifier = width
            .clip(AxPanelShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = AxPanelShape,
        color = color
    ) { row() }
}

@Composable
private fun StripIconText(icon: ImageVector, text: String, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(modifier = Modifier.size(14.dp), imageVector = icon, contentDescription = null, tint = tint)
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Stopped: how AxManager was last started and whether boot will start it again. */
@Composable
private fun StoppedStrip() {
    val method = when (AxeronSettings.getLastLaunchMode()) {
        AxeronSettings.LaunchMethod.ADB -> stringResource(R.string.home_method_adb)
        AxeronSettings.LaunchMethod.ROOT -> stringResource(R.string.home_method_root)
        AxeronSettings.LaunchMethod.ROOT_AS_SHELL -> stringResource(R.string.home_method_root_shell)
        else -> stringResource(R.string.home_method_never)
    }
    StatusStrip {
        StripIconText(
            icon = Icons.Outlined.History,
            text = stringResource(R.string.home_strip_last_method, method),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f, fill = false)
        )
        InlineDivider(height = 12.dp)
        StripItem(
            label = stringResource(R.string.home_strip_boot),
            value = stringResource(if (AxeronSettings.getStartOnBoot()) R.string.home_strip_on else R.string.home_strip_off)
        )
    }
}

/** Needs an extra step: the fix guide as the strip itself, tappable. */
@Composable
private fun FixGuideStrip(onClick: () -> Unit) {
    StatusStrip(onClick = onClick) {
        StripIconText(
            icon = Icons.Outlined.Info,
            text = stringResource(R.string.home_need_fix_msg),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
    }
}

/** Updating: indeterminate progress while the server is replaced. */
@Composable
private fun UpdatingStrip() {
    StatusStrip {
        StripIconText(
            icon = Icons.Outlined.Update,
            text = stringResource(R.string.please_wait),
            tint = MaterialTheme.colorScheme.primary
        )
        LinearProgressIndicator(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
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
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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

/**
 * Server wakelock (keeps the CPU awake with the screen off) as a chip in the info strip's
 * style: tap it to toggle. The icon and label turn primary while it is held.
 */
@Composable
private fun WakeLockStrip(held: Boolean, onChange: (Boolean) -> Unit) {
    StatusStrip(onClick = { onChange(!held) }, fillWidth = false) {
        StripIconText(
            icon = Icons.Outlined.Coffee,
            text = stringResource(R.string.home_wakelock),
            tint = if (held) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        InlineDivider(height = 12.dp)
        Text(
            text = stringResource(if (held) R.string.home_strip_on else R.string.home_strip_off),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (held) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** AXGC-style sub card: uptime, PID and server version in one strip with dividers. */
@Composable
private fun ServiceInfoStrip(uptime: String, pid: String, version: String) {
    StatusStrip {
        StripIconText(icon = Icons.Outlined.Timer, text = uptime, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.weight(1f))
        InlineDivider(height = 12.dp)
        StripItem(label = "PID", value = pid)
        InlineDivider(height = 12.dp)
        StripItem(label = "Ver", value = version)
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

/** Entry to Quick Settings while the server is off. */
@Composable
private fun QuickSettingsCard(count: Int, onClick: () -> Unit) {
    AxCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(imageVector = Icons.Outlined.Bolt)
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quick_settings),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.quick_settings_offline_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (count > 0) {
                StatusPill(text = count.toString(), color = MaterialTheme.colorScheme.primary)
            }
            Chevron()
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

/** A device fact: [shown] is the short form for the grid, [full] is what a tap copies. */
private data class DeviceFact(val label: String, val shown: String, val full: String = shown)

/**
 * Device facts as a calm 2-column grid: no icon badges, one line per value, long values
 * shortened (kernel version number, primary ABI). Tapping a cell copies its full value.
 * Server UID/PID/version live in the status card strip.
 */
@Composable
fun InfoCard(activateViewModel: ActivateViewModel) {
    val axeronInfo = activateViewModel.axeronInfo
    val kernel = System.getProperty("os.version").orEmpty()

    val facts = listOf(
        DeviceFact(
            stringResource(R.string.device_model),
            "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        ),
        DeviceFact(
            stringResource(R.string.android_version),
            "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        ),
        DeviceFact(stringResource(R.string.security_patch), Build.VERSION.SECURITY_PATCH),
        DeviceFact(stringResource(R.string.kernel_version), kernel.substringBefore('-'), kernel),
        DeviceFact(
            stringResource(R.string.abi_supported),
            Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
            Build.SUPPORTED_ABIS.joinToString(", ")
        ),
        DeviceFact(stringResource(R.string.selinux_context), axeronInfo.serverInfo.selinuxContext)
    )

    AxCard {
        facts.chunked(2).forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )
            }
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                DeviceFactCell(row[0], Modifier.weight(1f))
                VerticalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )
                row.getOrNull(1)?.let { DeviceFactCell(it, Modifier.weight(1f)) }
                    ?: Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DeviceFactCell(fact: DeviceFact, modifier: Modifier) {
    val context = LocalContext.current
    val copied = stringResource(R.string.copied)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable {
                // Android 13+ shows its own clipboard confirmation.
                if (ClipboardUtil.put(context, fact.full) &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                ) {
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = fact.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = fact.shown.ifEmpty { "—" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            onClick = null,
            // IconButtons carry 12dp of their own padding; trim the row's so the icons line up
            // with the ↗ icons above (16dp from the edge).
            endPadding = 4.dp
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
    endPadding: Dp = 16.dp,
    trailing: @Composable () -> Unit
) {
    // Same row metrics as Settings: 16dp gutter, 36dp badge, 12dp gap.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = endPadding, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(imageVector = icon)
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
private fun InfoRowDivider() {
    // Same inset as Settings rows: lines up with the text after the badge.
    SettingsDivider()
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
