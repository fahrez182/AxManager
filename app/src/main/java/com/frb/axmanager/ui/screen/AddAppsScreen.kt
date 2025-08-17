package com.frb.axmanager.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.frb.axmanager.ui.util.toBitmapSafely
import com.frb.axmanager.ui.viewmodel.AppsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppsScreen(
    navController: NavHostController,
    viewModel: AppsViewModel = viewModel()
) {
    val installedApps by viewModel.installedApps.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()

    val context = LocalContext.current
    val pm = context.packageManager

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select App") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(top = 0, bottom = 0)
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
            items(
                installedApps,
                key = { it.packageName + it.uid }
            ) { app ->
//                val appIcon by remember(app.packageName) {
//                    mutableStateOf(pm.getApplicationIcon(app.packageName))
//                }

                var appIcon by remember { mutableStateOf<ImageBitmap?>(null) }

                LaunchedEffect(app.packageName) {
                    withContext(Dispatchers.IO) {
                        val drawable = pm.getApplicationIcon(app.packageName)
                        appIcon = drawable.toBitmapSafely(96).asImageBitmap()
                    }
                }

                var isAdded by remember(app.packageName) {
                    mutableStateOf(app.isAdded)
                }

                // Side-effect pindah ke sini
                LaunchedEffect(isAdded) {
                    if (isAdded) {
                        viewModel.addApp(app)
                    } else {
                        viewModel.removeApp(app.packageName)
                    }
                }

                ListItem(
                    modifier = Modifier.padding(16.dp),
                    headlineContent = { Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    ) },
                    supportingContent = { Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall
                    ) },
                    leadingContent = {
//                        AsyncImage(
//                            model = appIcon,
//                            contentDescription = app.label,
//                            modifier = Modifier
//                                .padding(4.dp)
//                                .width(48.dp)
//                                .height(48.dp)
//                        )
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon!!,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(48.dp)
                            )
                        } else {
                            // Placeholder biar nggak blank pas loading
                            Box(
                                Modifier
                                    .padding(4.dp)
                                    .size(48.dp)
                            )
                        }
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



