package frb.axeron.manager.ui.screen

import android.content.pm.ApplicationInfo
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.core.content.pm.PackageInfoCompat
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.Axeron
import frb.axeron.manager.R
import frb.axeron.manager.data.PackageControl
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxCardShape
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.AxTopAppBar
import frb.axeron.manager.ui.component.ConfirmOption
import frb.axeron.manager.ui.component.CountdownConfirmSheet
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.SectionHeader
import frb.axeron.manager.ui.component.SettingsDivider
import frb.axeron.manager.ui.component.StatusPill
import frb.axeron.manager.ui.component.UseLifecycle
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.viewmodel.AppsViewModel
import frb.axeron.manager.ui.viewmodel.PrivilegeDetailViewModel
import frb.axeron.manager.ui.viewmodel.PrivilegeDetailViewModel.GeneralItem
import frb.axeron.manager.ui.viewmodel.PrivilegeDetailViewModel.Kind
import frb.axeron.manager.ui.viewmodel.PrivilegeDetailViewModel.Permission
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PrivilegeDetailScreen(
    appInfo: AppsViewModel.AppInfo,
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal,
) {
    val privilegeViewModel = viewModelGlobal.privilegeViewModel
    val context = LocalContext.current

    // Prefer the live entry so grant/revoke reflects immediately.
    val live = privilegeViewModel.privileges[appInfo.uid] ?: appInfo
    val allowed = live.isAdded
    val setAllowed: (Boolean) -> Unit = { checked ->
        if (checked) privilegeViewModel.grant(appInfo.uid)
        else privilegeViewModel.revoke(appInfo.uid)
    }

    val iconRequest = remember(appInfo.packageInfo) {
        ImageRequest.Builder(context)
            .data(appInfo.packageInfo)
            .crossfade(true)
            .build()
    }

    // ---- live app state (permissions, general) ----
    val detail: PrivilegeDetailViewModel = viewModel(key = "privilege-${appInfo.packageName}")
    LaunchedEffect(appInfo.packageName) { detail.bind(appInfo.packageName, appInfo.uid) }

    // ---- package actions ----
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHost.current
    var serverRunning by remember { mutableStateOf(Axeron.pingBinder()) }

    // State can change while this page is in the background (system Settings, the Shizuku app,
    // the server starting or stopping), so re-read all of it each time the page comes back.
    UseLifecycle(
        onResume = {
            serverRunning = Axeron.pingBinder()
            privilegeViewModel.loadInstalledApps(false) // re-reads the Axeron permission flags
            detail.refresh()
        }
    )

    // Uninstalled from outside (Settings, a store, adb): nothing left to show, so leave the page.
    val goneText = stringResource(R.string.privilege_app_gone, appInfo.label)
    LaunchedEffect(detail.gone) {
        if (!detail.gone) return@LaunchedEffect
        Toast.makeText(context, goneText, Toast.LENGTH_SHORT).show()
        privilegeViewModel.loadInstalledApps()
        navigator.popBackStack()
    }
    val isRoot = remember(serverRunning) {
        serverRunning && runCatching { Axeron.getAxeronInfo().isRoot() }.getOrDefault(false)
    }
    val systemApp = ((appInfo.packageInfo.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) != 0
    val userId = PackageControl.userIdOf(appInfo.uid)
    var confirming by remember { mutableStateOf<PackageAction?>(null) }
    var running by remember { mutableStateOf<PackageAction?>(null) }
    var keepData by remember { mutableStateOf(false) }

    val doneText = stringResource(R.string.privilege_action_done)
    val failedText = stringResource(R.string.privilege_action_failed)
    val partialCacheText = stringResource(R.string.privilege_clear_cache_partial)
    val needsServerText = stringResource(R.string.privilege_needs_server)
    val needsRootText = stringResource(R.string.privilege_needs_root)
    val permissionFailed = stringResource(R.string.privilege_permission_failed)
    val actionLabels = PackageAction.entries.associateWith { stringResource(it.labelId) }
    val generalLabels = GeneralItem.entries.associateWith { stringResource(it.labelId()) }

    fun runAction(action: PackageAction) {
        val pkg = appInfo.packageName
        scope.launch {
            running = action
            val outcome = when (action) {
                PackageAction.FORCE_STOP -> PackageControl.forceStop(pkg, userId)
                PackageAction.CLEAR_CACHE -> PackageControl.clearCache(pkg, userId)
                PackageAction.CLEAR_DATA -> PackageControl.clearData(pkg, userId)
                PackageAction.UNINSTALL -> PackageControl.uninstall(pkg, userId, systemApp, keepData)
            }
            running = null
            val label = actionLabels.getValue(action)
            val message = when {
                outcome.ok && outcome.partial -> partialCacheText
                outcome.ok -> doneText.format(label)
                outcome.serverOff -> needsServerText
                outcome.needsRoot -> failedText.format(label, needsRootText)
                else -> failedText.format(label, outcome.error.orEmpty())
            }
            if (outcome.ok && action == PackageAction.UNINSTALL) {
                // The app is gone (for this user): leave its page and drop it from the list.
                // A toast, since leaving cancels this scope before a snackbar could show.
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                privilegeViewModel.loadInstalledApps()
                navigator.popBackStack()
                return@launch
            }
            // Clearing data also resets the app's runtime permission grants.
            if (outcome.ok && action == PackageAction.CLEAR_DATA) detail.refresh()
            snackbar.showSnackbar(message)
        }
    }

    /** Says why a control didn't change; it stays as it was. */
    suspend fun reportFailure(outcome: PackageControl.Outcome, label: String) {
        if (outcome.ok) return
        val reason = when {
            outcome.serverOff -> needsServerText
            outcome.needsRoot -> needsRootText
            else -> outcome.error.orEmpty()
        }
        snackbar.showSnackbar(permissionFailed.format(label, reason))
    }

    // The target app, shown in every confirmation so it is clear what is about to change.
    val subject: @Composable () -> Unit = {
        AppChip(iconRequest = iconRequest, label = appInfo.label, packageName = appInfo.packageName)
    }

    confirming?.let { action ->
        CountdownConfirmSheet(
            title = actionLabels.getValue(action),
            message = when {
                action == PackageAction.UNINSTALL && systemApp ->
                    stringResource(R.string.privilege_uninstall_system_desc, appInfo.label)
                else -> stringResource(action.messageId, appInfo.label)
            },
            confirmLabel = actionLabels.getValue(action),
            icon = action.icon,
            destructive = action.destructive,
            subject = subject,
            onConfirm = {
                confirming = null
                runAction(action)
            },
            onDismissRequest = { confirming = null },
            extra = if (action == PackageAction.UNINSTALL) {
                {
                    ConfirmOption(
                        icon = Icons.Outlined.Inventory2,
                        label = stringResource(R.string.privilege_keep_data),
                        description = stringResource(R.string.privilege_keep_data_desc),
                        checked = keepData,
                        onCheckedChange = { keepData = it }
                    )
                }
            } else null
        )
    }

    var confirmDisable by remember { mutableStateOf(false) }
    if (confirmDisable) {
        CountdownConfirmSheet(
            title = stringResource(R.string.privilege_disable_app),
            message = stringResource(R.string.privilege_disable_app_desc, appInfo.label),
            confirmLabel = stringResource(R.string.privilege_disable_app),
            icon = Icons.Outlined.Block,
            subject = subject,
            onConfirm = {
                confirmDisable = false
                scope.launch {
                    reportFailure(
                        detail.setGeneral(GeneralItem.ENABLED, false),
                        generalLabels.getValue(GeneralItem.ENABLED)
                    )
                }
            },
            onDismissRequest = { confirmDisable = false }
        )
    }

    Scaffold(
        snackbarHost = { AxSnackBarHost(snackbar) },
        topBar = {
            AxTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.privilege),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Padding inside the list lets content pass under the fading top bar.
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + LocalFloatingBarInset.current + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                HeaderCard(
                    appInfo = appInfo,
                    iconRequest = iconRequest,
                    allowed = allowed,
                    onAllowedChange = setAllowed,
                    serverRunning = serverRunning,
                    actionLabels = actionLabels,
                    actionNote = { action ->
                        when {
                            action == PackageAction.UNINSTALL && systemApp ->
                                stringResource(R.string.privilege_uninstall_user_only_short)
                            action == PackageAction.CLEAR_CACHE && serverRunning && !isRoot ->
                                stringResource(R.string.privilege_clear_cache_external_short)
                            else -> null
                        }
                    },
                    running = running,
                    onAction = { action ->
                        keepData = false
                        confirming = action
                    }
                )
            }

            item(key = "general") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GeneralSection(
                        viewModel = detail,
                        labels = generalLabels,
                        enabled = serverRunning,
                        onToggle = { item, on ->
                            if (item == GeneralItem.ENABLED && !on) {
                                confirmDisable = true
                            } else scope.launch {
                                reportFailure(detail.setGeneral(item, on), generalLabels.getValue(item))
                            }
                        }
                    )
                }
            }

            item(key = "permissions") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PermissionsSection(
                        viewModel = detail,
                        enabled = serverRunning,
                        onToggle = { permission, grant ->
                            scope.launch { reportFailure(detail.setGranted(permission, grant), permission.label) }
                        },
                        onSetMode = { permission, mode ->
                            scope.launch { reportFailure(detail.setAppOpMode(permission, mode), permission.label) }
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Header: identity, privilege switch and quick actions in one card
// ---------------------------------------------------------------------------------------------

@Composable
private fun HeaderCard(
    appInfo: AppsViewModel.AppInfo,
    iconRequest: ImageRequest,
    allowed: Boolean,
    onAllowedChange: (Boolean) -> Unit,
    serverRunning: Boolean,
    actionLabels: Map<PackageAction, String>,
    actionNote: @Composable (PackageAction) -> String?,
    running: PackageAction?,
    onAction: (PackageAction) -> Unit
) {
    AxCard {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = iconRequest,
                contentDescription = appInfo.label,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                WrappingText(
                    text = appInfo.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                WrappingText(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    val versionCode = PackageInfoCompat.getLongVersionCode(appInfo.packageInfo)
                    MetaChip(
                        appInfo.packageInfo.versionName?.let { "v$it ($versionCode)" } ?: "($versionCode)"
                    )
                    MetaChip("${stringResource(R.string.uid_label)} ${appInfo.uid}")
                }
            }
        }

        SettingsDivider()

        // Privilege switch, compact: the whole row toggles.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAllowedChange(!allowed) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                imageVector = Icons.Outlined.AdminPanelSettings,
                size = 32.dp,
                tint = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.privilege_grant),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        if (allowed) R.string.privilege_allowed else R.string.privilege_not_allowed
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            Switch(checked = allowed, onCheckedChange = onAllowedChange)
        }

        SettingsDivider()

        // Quick actions: one round button each, like the system's App info.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PackageAction.entries.forEach { action ->
                QuickAction(
                    action = action,
                    label = actionLabels.getValue(action),
                    note = actionNote(action),
                    running = running == action,
                    enabled = serverRunning && running == null,
                    onClick = { onAction(action) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (!serverRunning) {
            Text(
                text = stringResource(R.string.privilege_needs_server),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }
    }
}

/** A title that wraps onto as many lines as it needs instead of cutting off with "…". */
@Composable
private fun WrappingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = style,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        color = color,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
private fun MetaChip(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/** The target app as a chip (icon, name, package): shown in confirmations under the title. */
@Composable
private fun AppChip(iconRequest: ImageRequest, label: String, packageName: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
        ) {
            AsyncImage(
                model = iconRequest,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                WrappingText(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                WrappingText(
                    text = packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    action: PackageAction,
    label: String,
    note: String?,
    running: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val tint = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .alpha(if (enabled || running) 1f else 0.45f)
            .clip(AxPanelShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Surface(shape = CircleShape, color = tint.copy(alpha = 0.12f), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                if (running) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = tint)
                } else {
                    Icon(action.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(Modifier.size(6.dp))
        WrappingText(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** What the header can do to the app; each is confirmed with a countdown sheet. */
private enum class PackageAction(
    val labelId: Int,
    val messageId: Int,
    val icon: ImageVector,
    val destructive: Boolean
) {
    FORCE_STOP(R.string.privilege_force_stop, R.string.privilege_force_stop_desc, Icons.Outlined.StopCircle, false),
    CLEAR_CACHE(R.string.privilege_clear_cache, R.string.privilege_clear_cache_desc, Icons.Outlined.CleaningServices, false),
    CLEAR_DATA(R.string.privilege_clear_data, R.string.privilege_clear_data_desc, Icons.Outlined.DeleteSweep, true),
    UNINSTALL(R.string.privilege_uninstall, R.string.privilege_uninstall_desc, Icons.Outlined.Delete, true)
}

// ---------------------------------------------------------------------------------------------
// General: toggle tiles
// ---------------------------------------------------------------------------------------------

/** Battery optimization, background activity, enabled: one row each, saying what it does. */
@Composable
private fun GeneralSection(
    viewModel: PrivilegeDetailViewModel,
    labels: Map<GeneralItem, String>,
    enabled: Boolean,
    onToggle: (GeneralItem, Boolean) -> Unit
) {
    val state = viewModel.general
    // Only what could be read: background activity needs the server to read its app-op.
    val items = GeneralItem.entries.filter { it in state }
    if (items.isEmpty()) return

    SectionHeader(stringResource(R.string.privilege_general))
    AxCard {
        items.forEachIndexed { index, item ->
            if (index > 0) SettingsDivider()
            val on = state[item] == true
            ToggleRow(
                icon = item.icon(),
                title = labels.getValue(item),
                description = stringResource(item.descriptionId()),
                on = on,
                busy = viewModel.changing[item.name] == true,
                enabled = enabled,
                onToggle = { onToggle(item, it) }
            )
        }
    }
}

/** Icon, title, what it does, and a switch (a spinner while a change is in flight). */
@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    on: Boolean,
    busy: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !busy) { onToggle(!on) }
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
    ) {
        IconBadge(
            imageVector = icon,
            size = 32.dp,
            tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            WrappingText(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(contentAlignment = Alignment.Center) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Switch(checked = on, enabled = enabled, onCheckedChange = onToggle)
        }
    }
}

private fun GeneralItem.labelId(): Int = when (this) {
    GeneralItem.BATTERY_UNRESTRICTED -> R.string.privilege_battery_unrestricted
    GeneralItem.BACKGROUND_ACTIVITY -> R.string.privilege_background_activity
    GeneralItem.ENABLED -> R.string.privilege_app_enabled
}

private fun GeneralItem.descriptionId(): Int = when (this) {
    GeneralItem.BATTERY_UNRESTRICTED -> R.string.privilege_battery_unrestricted_desc
    GeneralItem.BACKGROUND_ACTIVITY -> R.string.privilege_background_activity_desc
    GeneralItem.ENABLED -> R.string.privilege_app_enabled_desc
}

private fun GeneralItem.icon(): ImageVector = when (this) {
    GeneralItem.BATTERY_UNRESTRICTED -> Icons.Outlined.BatteryChargingFull
    GeneralItem.BACKGROUND_ACTIVITY -> Icons.Outlined.Sync
    GeneralItem.ENABLED -> Icons.Outlined.PowerSettingsNew
}

// ---------------------------------------------------------------------------------------------
// Special access and permissions
// ---------------------------------------------------------------------------------------------

/**
 * The app's requested permissions: app-op tiles (special access), switches for what `pm grant`
 * can change, and a collapsed read-only rest.
 */
@Composable
private fun PermissionsSection(
    viewModel: PrivilegeDetailViewModel,
    enabled: Boolean,
    onToggle: (Permission, Boolean) -> Unit,
    onSetMode: (Permission, String) -> Unit
) {
    val permissions = viewModel.permissions
    val changeable = remember(permissions) { permissions.filter { it.kind.grantable } }
    val special = remember(permissions) { permissions.filter { it.appOp != null } }
    val other = remember(permissions) { permissions.filter { !it.kind.grantable && it.appOp == null } }

    if (special.isNotEmpty()) {
        // App-ops have more than on/off, and two places they can be set: pick a mode instead.
        var picking by remember { mutableStateOf<Permission?>(null) }
        picking?.let { permission ->
            AppOpModeSheet(
                permission = permission,
                onSelect = { mode ->
                    picking = null
                    onSetMode(permission, mode)
                },
                onDismissRequest = { picking = null }
            )
        }
        SectionHeader(stringResource(R.string.privilege_special_access))
        AxCard {
            special.forEachIndexed { index, permission ->
                if (index > 0) SettingsDivider()
                SpecialAccessRow(
                    permission = permission,
                    busy = viewModel.changing[permission.name] == true,
                    enabled = enabled,
                    onClick = { picking = permission }
                )
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionHeader(stringResource(R.string.privilege_permissions), Modifier.weight(1f))
        if (permissions.isNotEmpty()) {
            StatusPill(
                text = stringResource(
                    R.string.privilege_permissions_summary,
                    permissions.count { it.granted },
                    permissions.size
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    when {
        viewModel.loading -> Box(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }

        permissions.isEmpty() -> Text(
            text = stringResource(R.string.privilege_permissions_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        else -> {
            if (changeable.isNotEmpty()) {
                AxCard {
                    changeable.forEachIndexed { index, permission ->
                        if (index > 0) SettingsDivider()
                        PermissionRow(permission = permission) {
                            Switch(
                                checked = permission.granted,
                                enabled = enabled && viewModel.changing[permission.name] != true,
                                onCheckedChange = { onToggle(permission, it) }
                            )
                        }
                    }
                }
            }
            if (other.isNotEmpty()) {
                var expanded by rememberSaveable { mutableStateOf(false) }
                val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "otherArrow")
                AxCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.privilege_permissions_other, other.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.privilege_permissions_other_short),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.rotate(arrow)
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column {
                            other.forEach { permission ->
                                SettingsDivider()
                                PermissionRow(permission = permission) {
                                    GrantedMark(permission.granted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Group icon, the permission's name, what it is; [trailing] is the switch or the granted mark. */
@Composable
private fun PermissionRow(permission: Permission, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            imageVector = permissionIcon(permission.name),
            size = 30.dp,
            tint = if (permission.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // The name and its kind sit small on top; what it means is the line to read.
            Row(verticalAlignment = Alignment.CenterVertically) {
                PermissionName(permission.shortName, Modifier.weight(1f, fill = false))
                // Only the kinds worth calling out; runtime is the expected case.
                if (permission.kind != Kind.RUNTIME) {
                    Spacer(Modifier.width(6.dp))
                    KindTag(permission.kind)
                }
            }
            WrappingText(
                text = permission.detail,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

/** A permission's own name above its meaning: small, regular weight, muted, monospace. */
@Composable
private fun PermissionName(name: String, modifier: Modifier = Modifier) {
    WrappingText(
        text = name,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun KindTag(kind: Kind) {
    val color = when (kind) {
        Kind.DEVELOPMENT -> MaterialTheme.colorScheme.tertiary
        Kind.SIGNATURE, Kind.UNKNOWN -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text = stringResource(kind.labelId()),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun GrantedMark(granted: Boolean) {
    val color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Surface(shape = CircleShape, color = color.copy(alpha = 0.12f), modifier = Modifier.size(28.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (granted) Icons.Outlined.Check else Icons.Outlined.Close,
                contentDescription = stringResource(
                    if (granted) R.string.permission_granted else R.string.permission_not_granted
                ),
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * A special-access permission: its name, what it is, the app-op behind it when that is named
 * differently, and the mode in effect. Tapping opens the mode picker.
 */
@Composable
private fun SpecialAccessRow(
    permission: Permission,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val accent = if (permission.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !busy, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
    ) {
        IconBadge(imageVector = specialIcon(permission.appOp), size = 32.dp, tint = accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // The names sit small on top; what it is, then what it allows, are the lines to read.
            PermissionName(permission.namesLine)
            WrappingText(
                text = permission.detail,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            permission.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (busy) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            StatusPill(
                text = stringResource(appOpModeLabel(permission.appOpState?.effective)),
                color = accent
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The permission's own name without its package, e.g. "CAMERA": the row's title. */
private val Permission.shortName: String
    get() = name.substringAfterLast('.')

/** [shortName], plus the app-op behind it when that is named differently. */
private val Permission.namesLine: String
    get() = appOp?.takeIf { it != shortName }?.let { "$shortName · $it" } ?: shortName

/**
 * What the permission is, as the row's description: its label, or the full name when it has
 * no label of its own (the label then just repeats [shortName]).
 */
private val Permission.detail: String
    get() = if (label.equals(shortName, ignoreCase = true)) name else label

/** Icon by the permission's group, read from its name; a key for anything unrecognised. */
private fun permissionIcon(name: String): ImageVector {
    val n = name.substringAfterLast('.').uppercase()
    return when {
        "CAMERA" in n -> Icons.Outlined.PhotoCamera
        "AUDIO" in n && "RECORD" in n || "MICROPHONE" in n -> Icons.Outlined.Mic
        "LOCATION" in n -> Icons.Outlined.LocationOn
        "CONTACTS" in n || "ACCOUNTS" in n -> Icons.Outlined.Contacts
        "SMS" in n || "MMS" in n || "WAP_PUSH" in n -> Icons.Outlined.Sms
        "CALL" in n || "PHONE" in n || "VOICEMAIL" in n || "SIP" in n -> Icons.Outlined.Call
        "CALENDAR" in n -> Icons.Outlined.CalendarMonth
        "MEDIA" in n || "STORAGE" in n || "IMAGES" in n || "VIDEO" in n -> Icons.Outlined.Photo
        "SENSORS" in n || "ACTIVITY_RECOGNITION" in n -> Icons.Outlined.Sensors
        "NOTIFICATION" in n -> Icons.Outlined.Notifications
        "BLUETOOTH" in n || "NEARBY" in n || "UWB" in n -> Icons.Outlined.Bluetooth
        else -> Icons.Outlined.Key
    }
}

/** Icon by app-op; a shield for app-ops without a dedicated one. */
private fun specialIcon(appOp: String?): ImageVector = when (appOp) {
    "SYSTEM_ALERT_WINDOW" -> Icons.Outlined.Layers
    "WRITE_SETTINGS" -> Icons.Outlined.Settings
    "MANAGE_EXTERNAL_STORAGE" -> Icons.Outlined.FolderOpen
    "REQUEST_INSTALL_PACKAGES" -> Icons.Outlined.InstallMobile
    "GET_USAGE_STATS" -> Icons.Outlined.QueryStats
    "SCHEDULE_EXACT_ALARM" -> Icons.Outlined.Alarm
    else -> Icons.Outlined.Shield
}

// ---------------------------------------------------------------------------------------------
// App-op mode picker
// ---------------------------------------------------------------------------------------------

/** App-op modes as `cmd appops` takes them, in the order the picker lists them. */
private val APP_OP_MODES = listOf("allow", "foreground", "ignore", "deny", "default")

private fun appOpModeLabel(mode: String?): Int = when (mode) {
    "allow" -> R.string.appop_mode_allow
    "foreground" -> R.string.appop_mode_foreground
    "ignore" -> R.string.appop_mode_ignore
    "deny", "errored" -> R.string.appop_mode_deny
    else -> R.string.appop_mode_default
}

private fun appOpModeDescription(mode: String): Int = when (mode) {
    "allow" -> R.string.appop_mode_allow_desc
    "foreground" -> R.string.appop_mode_foreground_desc
    "ignore" -> R.string.appop_mode_ignore_desc
    "deny" -> R.string.appop_mode_deny_desc
    else -> R.string.appop_mode_default_desc
}

/**
 * Picks the app-op mode of a special-access permission. Shows the package and the uid-wide
 * mode, since a uid-wide one overrides the package's (and is reset when a mode is picked).
 */
@Composable
private fun AppOpModeSheet(
    permission: Permission,
    onSelect: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val state = permission.appOpState
    val current = state?.packageMode ?: "default"
    AxOptionsSheet(
        // Same hierarchy as the row: the names small on top, what it is as the title, then
        // what it allows. With no description of its own, the full name takes that place.
        overline = { PermissionName(permission.namesLine) },
        title = permission.detail,
        description = permission.description ?: permission.name,
        onDismissRequest = onDismissRequest
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeFact(
                label = stringResource(R.string.appop_package_mode_short),
                value = stringResource(appOpModeLabel(state?.packageMode)),
                modifier = Modifier.weight(1f)
            )
            ModeFact(
                label = stringResource(R.string.appop_uid_mode_short),
                value = state?.uidMode?.let { stringResource(appOpModeLabel(it)) }
                    ?: stringResource(R.string.appop_not_set),
                highlight = state?.uidOverrides == true,
                modifier = Modifier.weight(1f)
            )
        }
        if (state?.uidOverrides == true) {
            Text(
                text = stringResource(R.string.appop_uid_override, stringResource(appOpModeLabel(state.uidMode))),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            APP_OP_MODES.forEach { mode ->
                val selected = current == mode
                Surface(
                    onClick = { onSelect(mode) },
                    shape = AxPanelShape,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        RadioButton(selected = selected, onClick = { onSelect(mode) })
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(appOpModeLabel(mode)),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(appOpModeDescription(mode)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeFact(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    val color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = AxPanelShape,
        color = if (highlight) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (highlight) color else Color.Unspecified
            )
        }
    }
}

private fun Kind.labelId(): Int = when (this) {
    Kind.RUNTIME -> R.string.permission_kind_runtime
    Kind.DEVELOPMENT -> R.string.permission_kind_development
    Kind.SPECIAL -> R.string.permission_kind_special
    Kind.NORMAL -> R.string.permission_kind_normal
    Kind.SIGNATURE -> R.string.permission_kind_signature
    Kind.UNKNOWN -> R.string.permission_kind_unknown_short
}
