package frb.axeron.manager.ui.screen

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
                        onClick = {
                            navigator.navigate(PrivilegeDetailScreenDestination(appInfo = app))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivilegeAppRow(
    app: AppsViewModel.AppInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val iconRequest = remember(app.packageInfo) {
        ImageRequest.Builder(context)
            .data(app.packageInfo)
            .crossfade(true)
            .build()
    }

    // The right side stays tight so labels get room.
    AxCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = iconRequest,
                contentDescription = app.label,
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
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    if (app.isAdded) R.string.privilege_allowed else R.string.privilege_not_allowed
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (app.isAdded) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

        OptionsSectionHeader(stringResource(R.string.sort_order))
        OrderToggle(
            ascending = privilegeViewModel.ascending,
            onChange = privilegeViewModel::updateAscending
        )
    }
}
