package com.frb.axmanager.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.frb.axmanager.ui.navigation.BottomBarDestination
import com.frb.axmanager.ui.theme.AxManagerTheme
import com.frb.axmanager.ui.util.LocalSnackbarHost
import com.frb.axmanager.ui.viewmodel.AdbViewModel
import com.frb.axmanager.ui.viewmodel.AppsViewModel
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.frb.engine.client.Axeron
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ActivateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AddAppsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuickShellScreenDestination
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator


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
    val snackBarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination


    val appsViewModel: AppsViewModel = viewModel<AppsViewModel>()
    val adbViewModel: AdbViewModel = viewModel<AdbViewModel>()

    DisposableEffect(Unit) {

        adbViewModel.checkAxeronService()

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
        AddAppsScreenDestination.route -> false // Hide for AddApps
        ActivateScreenDestination.route -> false // Hide for Activate
        QuickShellScreenDestination.route -> false // Hide for QuickShell
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
        CompositionLocalProvider(
            LocalSnackbarHost provides snackBarHostState,
        ) {
            DestinationsNavHost(
                modifier = Modifier.padding(innerPadding),
                navGraph = NavGraphs.root,
                navController = navController,
                dependenciesContainerBuilder = {
                    dependency(viewModelGlobal)
                },
                defaultTransitions = object : NavHostAnimatedDestinationStyle() {
                    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
                        get() = { fadeIn(animationSpec = tween(340)) }
                    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
                        get() = { fadeOut(animationSpec = tween(340)) }
                }
            )
        }
    }
}

@Composable
fun BottomBar(navController: NavHostController, axeronServiceInfo: AdbViewModel.AxeronServiceInfo) {
    val navigator = navController.rememberDestinationsNavigator()

    NavigationBar {
        BottomBarDestination.entries
            .forEach { destination ->
                if (!axeronServiceInfo.isRunning() && destination.needAxeron) return@forEach

                val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)
                NavigationBarItem(
                    selected = isCurrentDestOnBackStack,
                    onClick = {
                        if (isCurrentDestOnBackStack) {
                            navigator.popBackStack(destination.direction, false)
                        }
                        navigator.navigate(destination.direction) {
                            popUpTo(NavGraphs.root) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        if (isCurrentDestOnBackStack) destination.iconSelected?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = destination.label
                            )
                        } else destination.iconNotSelected?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = destination.label
                            )
                        }
                    },
                    label = { Text(destination.label) },
                )
            }
    }
}

@Preview(showBackground = true)
@Composable
fun AxComposablePreview(modifier: Modifier = Modifier) {
    MainScreen()
}