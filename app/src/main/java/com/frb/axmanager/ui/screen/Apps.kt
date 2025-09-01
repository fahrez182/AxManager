package com.frb.axmanager.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AddAppsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppsScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val addedApps by viewModelGlobal.appsViewModel.addedApps.collectAsState()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val pm = LocalContext.current.packageManager

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navigator.navigate(AddAppsScreenDestination)
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add App")
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Applications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(top = 0)
            )
        },
        contentWindowInsets = WindowInsets(top = 0, bottom = 0)
    ) { paddingValues ->
        if (addedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No apps added yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(
                    addedApps,
                    key = { it.packageName + it.uid }
                ) { app ->
                    val appIcon by remember(app.packageName) {
                        mutableStateOf(pm.getApplicationIcon(app.packageName))
                    }
                    ElevatedCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = appIcon,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .width(48.dp)
                                    .height(48.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                    }
                }
            }
        }
    }
}
