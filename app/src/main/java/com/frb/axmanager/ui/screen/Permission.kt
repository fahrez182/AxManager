package com.frb.axmanager.ui.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.frb.axmanager.ui.component.SearchAppBar
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PermissionScreen(
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal
) {
    val permissionViewModel = viewModelGlobal.permissionViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            SearchAppBar(
                title = { Text(
                    text = "Permission Manager",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                ) },
                searchLabel = "Search Apps",
                searchText = permissionViewModel.search,
                onSearchTextChange = { permissionViewModel.search = it },
                onClearClick = { permissionViewModel.search = "" },
                scrollBehavior = scrollBehavior,
                onBackClick = {
                    navigator.popBackStack()
                },
                windowInsets = WindowInsets(top = 0)
            )
        },
        contentWindowInsets = WindowInsets(top = 0, bottom = 0)
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "This features includes adapted code from \"Shizuku\"\n© Rikka Apps, license under the Apache License, Version 2.0",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            items(
                permissionViewModel.needPermissionList,
                key = { it.packageName + it.uid }
            ) { app ->

                var isAdded by remember(app.packageName) {
                    mutableStateOf(permissionViewModel.granted(app.packageInfo.applicationInfo!!.uid))
                }

                // Side-effect pindah ke sini
                LaunchedEffect(isAdded) {
                    if (isAdded) {
                        permissionViewModel.grant(app.packageInfo.applicationInfo!!.uid)
                    } else {
                        permissionViewModel.revoke(app.packageInfo.applicationInfo!!.uid)
                    }
                }

                ListItem(
                    modifier = Modifier.padding(end = 6.dp, top = 6.dp),
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
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(app.packageInfo)
                                .crossfade(true)
                                .build(),
                            contentDescription = app.label,
                            modifier = Modifier
                                .padding(4.dp)
                                .width(48.dp)
                                .height(48.dp)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isAdded,
                            onCheckedChange = { checked ->
                                isAdded = checked
                            }
                        )
                    }
                )
            }
        }
    }
}