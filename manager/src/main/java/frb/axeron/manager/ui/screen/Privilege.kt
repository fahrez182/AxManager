package frb.axeron.manager.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import frb.axeron.api.Axeron
import frb.axeron.manager.data.PackageControl
import frb.axeron.manager.ui.component.AxDropdownDivider
import frb.axeron.manager.ui.component.AxDropdownHeader
import frb.axeron.manager.ui.component.AxDropdownMenu
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.CountdownConfirmSheet
import frb.axeron.manager.ui.util.LocalSnackbarHost
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.PrivilegeDetailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.OptionTile
import frb.axeron.manager.ui.component.OptionTileGrid
import frb.axeron.manager.ui.component.OptionsSectionHeader
import frb.axeron.manager.ui.component.OrderToggle
import frb.axeron.manager.ui.component.AxPullToRefreshBox
import frb.axeron.manager.ui.component.EmptyState
import frb.axeron.manager.ui.component.SearchAppBar
import frb.axeron.manager.ui.component.UseLifecycle
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.viewmodel.AppsViewModel
import frb.axeron.manager.ui.viewmodel.PrivilegeViewModel
import frb.axeron.manager.ui.viewmodel.PrivilegeViewModel.Access
import frb.axeron.manager.ui.viewmodel.PrivilegeViewModel.AccessFilter
import frb.axeron.manager.ui.viewmodel.access
import frb.axeron.manager.ui.viewmodel.PrivilegeViewModel.AppFilter
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PrivilegeScreen(
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal
) {
    val privilegeViewModel = viewModelGlobal.privilegeViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()

    UseLifecycle(
        {
            privilegeViewModel.loadInstalledApps(false)
        }
    )

    var showFilterSheet by remember { mutableStateOf(false) }
    if (showFilterSheet) {
        PrivilegeFilterSheet(
            privilegeViewModel = privilegeViewModel,
            onDismissRequest = { showFilterSheet = false }
        )
    }

    Scaffold(
        snackbarHost = { AxSnackBarHost(LocalSnackbarHost.current) },
        topBar = {
            SearchAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.privilege_manager),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                searchLabel = stringResource(R.string.search_label_apps),
                searchText = privilegeViewModel.search,
                onSearchTextChange = { privilegeViewModel.search = it },
                onClearClick = { privilegeViewModel.search = "" },
                action = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        // A dot marks that the list is filtered/sorted differently from the default.
                        BadgedBox(
                            badge = {
                                if (!privilegeViewModel.isFilterDefault) Badge()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = stringResource(R.string.filter_settings)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
//                onBackClick = {
//                    navigator.popBackStack()
//                },
            )
        }
    ) { paddingValues ->
        AxPullToRefreshBox(
            isRefreshing = privilegeViewModel.isRefreshing,
            onRefresh = {
                privilegeViewModel.loadInstalledApps()
            },
            topInset = paddingValues.calculateTopPadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                // Scroll under the fading top bar and clear the floating bar.
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = paddingValues.calculateTopPadding() + 10.dp,
                    end = 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + LocalFloatingBarInset.current + 16.dp
                )
            ) {
                if (privilegeViewModel.privilegeList.isEmpty() && !privilegeViewModel.isRefreshing) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.Lock,
                            title = stringResource(R.string.privilege_empty_title),
                            subtitle = stringResource(R.string.privilege_empty_desc)
                        )
                    }
                }
                items(
                    privilegeViewModel.privilegeList,
                    key = { it.packageName + it.uid }
                ) { app ->
                    PrivilegeAppRow(
                        app = app,
                        onDetails = {
                            navigator.navigate(PrivilegeDetailScreenDestination(appInfo = app))
                        },
                        onAccessChange = { privilegeViewModel.setAccess(app.uid, it) },
                        onPackageChanged = { privilegeViewModel.refreshPackage(app.uid) }
                    )
                }
            }
        }
    }
}

/** Grayscale, for an app that is disabled. */
private val DisabledIconFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** What the quick menu asks to confirm first: both lose state the user may care about. */
private enum class QuickConfirm { FORCE_STOP, DISABLE }

/**
 * One app. A tap opens its details; a long press opens a quick menu at the right: the Axeron
 * permission, open, clear cache, force stop and enable/disable. A disabled app is in grayscale.
 */
@Composable
private fun PrivilegeAppRow(
    app: AppsViewModel.AppInfo,
    onDetails: () -> Unit,
    onAccessChange: (Access) -> Unit,
    onPackageChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHost.current
    val outcomes = outcomeTexts()
    val iconRequest = remember(app.packageInfo) {
        ImageRequest.Builder(context)
            .data(app.packageInfo)
            .crossfade(true)
            .build()
    }

    val disabled = app.packageInfo.applicationInfo?.enabled == false
    val userId = PackageControl.userIdOf(app.uid)
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<QuickConfirm?>(null) }
    // Re-read each time the menu opens: the server may have started or stopped meanwhile.
    var serverRunning by remember { mutableStateOf(false) }
    val launchIntent = remember(app.packageName, disabled) {
        if (disabled) null else context.packageManager.getLaunchIntentForPackage(app.packageName)
    }

    fun run(label: String, block: suspend () -> PackageControl.Outcome) {
        scope.launch {
            val outcome = block()
            if (outcome.ok) onPackageChanged()
            snackbar.showSnackbar(outcomes.of(outcome, label))
        }
    }

    val forceStopLabel = stringResource(R.string.privilege_force_stop)
    val clearCacheLabel = stringResource(R.string.privilege_clear_cache)
    val disableLabel = stringResource(R.string.privilege_disable_app)
    val enableLabel = stringResource(R.string.privilege_enable_app)

    val subject: @Composable () -> Unit = {
        AppChip(iconRequest = iconRequest, label = app.label, packageName = app.packageName)
    }
    when (confirm) {
        QuickConfirm.FORCE_STOP -> CountdownConfirmSheet(
            title = forceStopLabel,
            message = stringResource(R.string.privilege_force_stop_desc, app.label),
            confirmLabel = forceStopLabel,
            icon = Icons.Outlined.StopCircle,
            destructive = false,
            subject = subject,
            onConfirm = {
                confirm = null
                run(forceStopLabel) { PackageControl.forceStop(app.packageName, userId) }
            },
            onDismissRequest = { confirm = null }
        )
        QuickConfirm.DISABLE -> CountdownConfirmSheet(
            title = disableLabel,
            message = stringResource(R.string.privilege_disable_app_desc, app.label),
            confirmLabel = disableLabel,
            icon = Icons.Outlined.Block,
            subject = subject,
            onConfirm = {
                confirm = null
                run(disableLabel) { PackageControl.setEnabled(app.packageName, userId, false) }
            },
            onDismissRequest = { confirm = null }
        )
        null -> Unit
    }

    Box {
        // The right side stays tight so labels get room.
        AxCard(
            onClick = onDetails,
            onLongClick = {
                serverRunning = Axeron.pingBinder()
                menu = true
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                    .alpha(if (disabled) 0.6f else 1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = iconRequest,
                    contentDescription = app.label,
                    colorFilter = if (disabled) DisabledIconFilter else null,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (disabled) {
                            stringResource(R.string.privilege_disabled_tag, app.packageName)
                        } else app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(app.access.labelId()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = app.access.color()
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // A zero-size anchor at the card's bottom-right corner: the menu opens on the right,
        // under the access label, and the popup shifts left only as far as the screen needs.
        Box(Modifier.align(Alignment.BottomEnd)) {
            AxDropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                minWidth = 260.dp,
                maxWidth = 340.dp
            ) {
                AxDropdownHeader(app.label)
                Box(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                    AccessOptions(
                        access = app.access,
                        enabled = serverRunning,
                        onSelect = {
                            menu = false
                            onAccessChange(it)
                        }
                    )
                }
                AxDropdownDivider()
                // Icon-only actions in one row; each says what it is to TalkBack.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickIconAction(
                        icon = Icons.Outlined.PlayArrow,
                        label = stringResource(R.string.privilege_launch),
                        enabled = launchIntent != null
                    ) {
                        menu = false
                        val intent = launchIntent ?: return@QuickIconAction
                        try {
                            context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: ActivityNotFoundException) {
                            scope.launch {
                                snackbar.showSnackbar(context.getString(R.string.privilege_launch_failed, app.label))
                            }
                        }
                    }
                    QuickIconAction(
                        icon = Icons.Outlined.CleaningServices,
                        label = clearCacheLabel,
                        enabled = serverRunning
                    ) {
                        menu = false
                        run(clearCacheLabel) { PackageControl.clearCache(app.packageName, userId) }
                    }
                    QuickIconAction(
                        icon = Icons.Outlined.StopCircle,
                        label = forceStopLabel,
                        enabled = serverRunning
                    ) {
                        menu = false
                        confirm = QuickConfirm.FORCE_STOP
                    }
                    QuickIconAction(
                        icon = Icons.Outlined.PowerSettingsNew,
                        label = if (disabled) enableLabel else disableLabel,
                        enabled = serverRunning,
                        destructive = !disabled
                    ) {
                        menu = false
                        // Enabling gives nothing up, so only disabling asks first.
                        if (disabled) {
                            run(enableLabel) { PackageControl.setEnabled(app.packageName, userId, true) }
                        } else {
                            confirm = QuickConfirm.DISABLE
                        }
                    }
                }
            }
        }
    }
}

/** One action of the quick menu's icon row; [label] is what TalkBack says. */
@Composable
private fun QuickIconAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = if (destructive) {
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        } else IconButtonDefaults.filledTonalIconButtonColors()
    ) {
        Icon(imageVector = icon, contentDescription = label)
    }
}

/**
 * Allowed / Ask / Denied as a column of radio options, in the style of the app-op mode picker.
 * [withDescriptions] adds what each does (the detail sheet); the quick menu keeps to the names.
 */
@Composable
internal fun AccessOptions(
    access: Access,
    onSelect: (Access) -> Unit,
    enabled: Boolean = true,
    withDescriptions: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (withDescriptions) 6.dp else 4.dp)) {
        Access.entries.forEach { option ->
            val selected = option == access
            Surface(
                onClick = { if (!selected) onSelect(option) },
                enabled = enabled,
                shape = AxPanelShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.alpha(if (enabled) 1f else 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 12.dp, top = 2.dp, bottom = 2.dp)
                ) {
                    RadioButton(
                        selected = selected,
                        enabled = enabled,
                        onClick = { if (!selected) onSelect(option) }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(option.labelId()),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (withDescriptions) {
                            Text(
                                text = stringResource(option.descriptionId()),
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
private fun PrivilegeFilterSheet(
    privilegeViewModel: PrivilegeViewModel,
    onDismissRequest: () -> Unit
) {
    val counts = privilegeViewModel.filterCounts

    AxOptionsSheet(
        title = stringResource(R.string.filter_settings),
        description = stringResource(R.string.privilege_filter_desc),
        onDismissRequest = onDismissRequest,
        headerAction = {
            TextButton(
                enabled = !privilegeViewModel.isFilterDefault,
                onClick = privilegeViewModel::resetFilter
            ) {
                Text(stringResource(R.string.reset))
            }
        }
    ) {
        OptionsSectionHeader(stringResource(R.string.privilege_filter_show))
        OptionTileGrid(
            items = listOf(
                Triple(AppFilter.INSTALLED, Icons.Outlined.Apps, R.string.privilege_filter_installed),
                Triple(AppFilter.SYSTEM, Icons.Outlined.Android, R.string.privilege_filter_system),
                Triple(AppFilter.SHIZUKU, Icons.Outlined.Api, R.string.privilege_filter_shizuku)
            )
        ) { (filter, icon, labelId), modifier ->
            OptionTile(
                modifier = modifier,
                icon = icon,
                label = stringResource(labelId),
                subtitle = stringResource(R.string.privilege_filter_count, counts[filter] ?: 0),
                selected = privilegeViewModel.filter == filter,
                onClick = { privilegeViewModel.updateFilter(filter) }
            )
        }

        val accessCounts = privilegeViewModel.accessCounts
        OptionsSectionHeader(stringResource(R.string.privilege_filter_access))
        OptionTileGrid(
            items = listOf(
                Triple(AccessFilter.ALL, Icons.Outlined.DoneAll, R.string.privilege_filter_all),
                Triple(AccessFilter.ALLOWED, Icons.Outlined.CheckCircle, R.string.privilege_access_allowed),
                Triple(AccessFilter.ASK, Icons.AutoMirrored.Outlined.HelpOutline, R.string.privilege_access_ask),
                Triple(AccessFilter.DENIED, Icons.Outlined.Block, R.string.privilege_access_denied)
            ),
            columns = 2
        ) { (access, icon, labelId), modifier ->
            OptionTile(
                modifier = modifier,
                icon = icon,
                label = stringResource(labelId),
                subtitle = stringResource(R.string.privilege_filter_count, accessCounts[access] ?: 0),
                selected = privilegeViewModel.access == access,
                onClick = { privilegeViewModel.updateAccess(access) }
            )
        }

        OptionsSectionHeader(stringResource(R.string.sort_order))
        OrderToggle(
            ascending = privilegeViewModel.ascending,
            onChange = privilegeViewModel::updateAscending
        )
    }
}

/** "Allowed" / "Ask (default)" / "Denied", as the list and the detail screen show it. */
internal fun Access.labelId(): Int = when (this) {
    Access.ALLOWED -> R.string.privilege_access_allowed
    Access.ASK -> R.string.privilege_access_ask
    Access.DENIED -> R.string.privilege_access_denied
}

/** What the server does when the app asks, for the detail screen. */
internal fun Access.descriptionId(): Int = when (this) {
    Access.ALLOWED -> R.string.privilege_access_allowed_desc
    Access.ASK -> R.string.privilege_access_ask_desc
    Access.DENIED -> R.string.privilege_access_denied_desc
}

@Composable
internal fun Access.color(): Color = when (this) {
    Access.ALLOWED -> MaterialTheme.colorScheme.primary
    Access.ASK -> MaterialTheme.colorScheme.onSurfaceVariant
    Access.DENIED -> MaterialTheme.colorScheme.error
}
