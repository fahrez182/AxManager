package frb.axeron.manager.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import frb.axeron.manager.ui.component.AxPullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.PrivilegeDetailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import frb.axeron.manager.ui.component.EmptyState
import frb.axeron.manager.ui.component.SearchAppBar
import frb.axeron.manager.ui.component.UseLifecycle
import frb.axeron.manager.ui.util.LocalFloatingBarInset
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
                // Scroll under the fading top bar and clear the floating bar.
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
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
                    ListItem(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                navigator.navigate(PrivilegeDetailScreenDestination(appInfo = app))
                            },
                        headlineContent = {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            val context = LocalContext.current
                            val iconRequest = remember(app.packageInfo) {
                                ImageRequest.Builder(context)
                                    .data(app.packageInfo)
                                    .crossfade(true)
                                    .build()
                            }
                            AsyncImage(
                                model = iconRequest,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .width(48.dp)
                                    .height(48.dp)
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(
                                        if (app.isAdded) R.string.privilege_allowed
                                        else R.string.privilege_not_allowed
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
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
                    )
                }
            }

        }
    }
}