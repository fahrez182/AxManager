package com.frb.axmanager.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.frb.axmanager.ui.navigation.NavHostContainer
import com.frb.axmanager.ui.navigation.ScreenItem
import com.frb.axmanager.ui.theme.AxManagerTheme
import com.frb.axmanager.ui.viewmodel.AdbViewModel
import com.frb.axmanager.ui.viewmodel.AppsViewModel
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.frb.engine.client.Axeron


class AxActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Axeron.pingBinder()) {
            Log.i("AxManagerBinder", "pingBinder Success!")
        } else {
            Log.i("AxManagerBinder", "pingBinder Failed!")
        }
        setContent {
            AxManagerTheme {
                MainScreen()
            }
        }
    }

}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination


    val appsViewModel: AppsViewModel = viewModel<AppsViewModel>()
    val adbViewModel: AdbViewModel = viewModel<AdbViewModel>()

    DisposableEffect(Unit) {
        val receivedListener = Axeron.OnBinderReceivedListener {
            Log.i("AxManagerBinder", "onBinderReceived")
            adbViewModel.checkAxeronService()
        }
        val deadListener = Axeron.OnBinderDeadListener {
            Log.i("AxManagerBinder", "onBinderDead")
            adbViewModel.checkAxeronService()
        }

        Axeron.addBinderReceivedListener(receivedListener)
        Axeron.addBinderDeadListener(deadListener)

        onDispose {
            Axeron.removeBinderReceivedListener(receivedListener)
            Axeron.removeBinderDeadListener(deadListener)
        }
    }

    val viewModelGlobal = remember {
        ViewModelGlobal(
            appsViewModel,
            adbViewModel
        )
    }

    val axeronServiceInfo by adbViewModel.axeronServiceInfo.collectAsState()

    val showBottomBar = when (currentDestination?.route) {
        ScreenItem.AddApps.route -> false // Hide for AddApps
        ScreenItem.Activate.route -> false // Hide for Activate
        ScreenItem.QuickShell.route -> false // Hide for QuickShell
        else -> true
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BottomBar(navController, axeronServiceInfo)
            }

        }
    ) { innerPadding ->
        NavHostContainer(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            viewModelGlobal = viewModelGlobal)
    }
}

@Composable
fun BottomBar(navController: NavHostController, axeronServiceInfo: AdbViewModel.AxeronServiceInfo) {
    val items = listOf(
        ScreenItem.Home,
        ScreenItem.Apps,
        ScreenItem.Plugins,
        ScreenItem.Settings
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            if (!axeronServiceInfo.isRunning() && item.needAxeron) return@forEach
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    if (selected) item.iconSelected?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = item.route
                        )
                    } else item.iconNotSelected?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = item.route
                        )
                    }
                },
                label = { Text(item.route) },
                selected = selected,
                onClick = {
                    if (selected) {
                        navController.popBackStack(item.route, false)
                    }
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AxComposablePreview(modifier: Modifier = Modifier) {
    MainScreen()
}