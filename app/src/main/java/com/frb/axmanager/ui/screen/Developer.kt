package com.frb.axmanager.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import com.frb.axmanager.ui.component.SwitchItem
import com.frb.axmanager.ui.util.LocalSnackbarHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun DeveloperScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current

    Scaffold(
        topBar = {
            TopBar(
                onBack = { navigator.popBackStack() },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {
            val context = LocalContext.current
            rememberCoroutineScope()
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

            // --- Developer Options Switch ---
            var developerOptionsEnabled by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("enable_developer_options", false)
                )
            }
            SwitchItem(
                icon = Icons.Filled.DeveloperMode,
                title = "Enable developer options",
                summary = "Show hidden settings and debug info relevant only for developers.",
                checked = developerOptionsEnabled
            ) {
                prefs.edit { putBoolean("enable_developer_options", it) }
                developerOptionsEnabled = it
            }

            var enableWebDebugging by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("enable_web_debugging", false)
                )
            }
            SwitchItem(
                enabled = developerOptionsEnabled,
                icon = Icons.Filled.Web,
                title = "Enable web debugging",
                summary = "Can be used to debug WebUI. Please enable only when needed.",
                checked = enableWebDebugging
            ) {
                prefs.edit { putBoolean("enable_web_debugging", it) }
                enableWebDebugging = it
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = { Text(
                text = "Developer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            ) }, navigationIcon = {
            IconButton(
                onClick = onBack
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
        windowInsets = WindowInsets(top = 0),
        scrollBehavior = scrollBehavior
    )
}

@Preview
@Composable
private fun DeveloperPreview() {
    DeveloperScreen(EmptyDestinationsNavigator)
}
