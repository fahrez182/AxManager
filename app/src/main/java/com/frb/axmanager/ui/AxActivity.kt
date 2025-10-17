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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import com.frb.axmanager.ui.viewmodel.PluginViewModel
import com.frb.axmanager.ui.viewmodel.SettingsViewModel
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.frb.engine.client.Axeron
import com.frb.engine.implementation.AxeronInfo
import com.frb.engine.implementation.AxeronService
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ActivateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ExecutePluginActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator


class AxActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AxManagerTheme {
                MainScreen(it)
            }
        }
    }

}

@Composable
fun MainScreen(settingsViewModel: SettingsViewModel) {
    val snackBarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination


    val appsViewModel: AppsViewModel = viewModel<AppsViewModel>()
    val adbViewModel: AdbViewModel = viewModel<AdbViewModel>()
    val pluginViewModel: PluginViewModel = viewModel<PluginViewModel>()

    DisposableEffect(Unit) {

        adbViewModel.checkAxeronService()

        if (Axeron.pingBinder() && AxeronService.VERSION_CODE <= Axeron.getInfo().versionCode) {
            pluginViewModel.fetchModuleList()
            appsViewModel.loadInstalledApps()
        }

        val receivedListener = Axeron.OnBinderReceivedListener {
            Log.i("AxManagerBinder", "onBinderReceived")
            adbViewModel.checkAxeronService()
            if (Axeron.pingBinder() && AxeronService.VERSION_CODE <= Axeron.getInfo().versionCode) {
                pluginViewModel.fetchModuleList()
                appsViewModel.loadInstalledApps()
            }
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
            settingsViewModel,
            appsViewModel,
            adbViewModel,
            pluginViewModel
        )
    }

    val showBottomBar = when (currentDestination?.route) {
        ActivateScreenDestination.route -> false // Hide for Activate
        FlashScreenDestination.route -> false // Hide for Flash
        ExecutePluginActionScreenDestination.route -> false // Hide for ExecutePluginAction
        else -> true
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BottomBar(
                    navController,
                    adbViewModel.axeronInfo,
                    pluginViewModel.pluginUpdateCount
                )
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
fun BottomBar(
    navController: NavHostController,
    axeronServiceInfo: AxeronInfo,
    moduleUpdateCount: Int
) {
    val navigator = navController.rememberDestinationsNavigator()

    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        NavigationBar(
            modifier = Modifier.padding(top = 6.dp),
            containerColor = Color.Transparent,
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                BottomBarDestination.entries
                    .forEach { destination ->
                        if (!axeronServiceInfo.isRunning() && destination.needAxeron) return@forEach

                        val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(
                            destination.direction
                        )
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
                                if (destination == BottomBarDestination.Plugin && moduleUpdateCount > 0) {
                                    BadgedBox(badge = { Badge { Text(moduleUpdateCount.toString()) } }) {
                                        if (isCurrentDestOnBackStack) {
                                            Icon(
                                                destination.iconSelected,
                                                destination.label
                                            )
                                        } else {
                                            Icon(
                                                destination.iconNotSelected,
                                                destination.label
                                            )
                                        }
                                    }
                                } else {
                                    if (isCurrentDestOnBackStack) Icon(
                                        imageVector = destination.iconSelected,
                                        contentDescription = destination.label
                                    ) else Icon(
                                        imageVector = destination.iconNotSelected,
                                        contentDescription = destination.label
                                    )
                                }
                            },
                            label = {
                                Column(
                                    modifier = Modifier
                                        .animateContentSize(
                                            animationSpec = tween(durationMillis = 300)
                                        )
                                        .wrapContentHeight()
                                ) {
                                    AnimatedVisibility(
                                        visible = isCurrentDestOnBackStack,
                                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                                            initialOffsetY = { it / 2 }, animationSpec = tween(300)
                                        ),
                                        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                                            targetOffsetY = { it / 2 }, animationSpec = tween(300)
                                        )
                                    ) {
                                        Text(destination.label)
                                    }
                                }
                            },
                        )
                    }
            }
        }
    }

}