package com.frb.axmanager.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.frb.axmanager.ui.screen.ActivateScreen
import com.frb.axmanager.ui.screen.AddAppsScreen
import com.frb.axmanager.ui.screen.AppsScreen
import com.frb.axmanager.ui.screen.HomeScreen
import com.frb.axmanager.ui.screen.PluginsScreen
import com.frb.axmanager.ui.screen.QuickShellScreen
import com.frb.axmanager.ui.screen.SettingsScreen
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal

@Composable
fun NavHostContainer(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModelGlobal: ViewModelGlobal
) {
    NavHost(navController = navController, startDestination = ScreenItem.Home.route, modifier = modifier) {
        composable(ScreenItem.Home.route) { HomeScreen(navController, viewModelGlobal) }
        composable(ScreenItem.Apps.route) { AppsScreen(navController, viewModelGlobal) }
        composable(ScreenItem.Plugins.route) { PluginsScreen(navController, viewModelGlobal) }
        composable(ScreenItem.Settings.route) { SettingsScreen(navController, viewModelGlobal) }

        composable(ScreenItem.AddApps.route) { AddAppsScreen(navController, viewModelGlobal) }
        composable(ScreenItem.Activate.route) { ActivateScreen(navController, viewModelGlobal) }
        composable(ScreenItem.QuickShell.route) { QuickShellScreen(navController, viewModelGlobal) }
    }
}

@Composable
fun SimpleScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text)
    }
}
