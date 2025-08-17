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
import com.frb.axmanager.ui.screen.AddAppsScreen
import com.frb.axmanager.ui.screen.AppsScreen
import com.frb.axmanager.ui.screen.HomeScreen
import com.frb.axmanager.ui.viewmodel.AppsViewModel

@Composable
fun NavHostContainer(navController: NavHostController, modifier: Modifier = Modifier, appsViewModel: AppsViewModel) {
    NavHost(navController = navController, startDestination = BottomNavItem.Home.route, modifier = modifier) {
        composable(BottomNavItem.Home.route) { HomeScreen(navController, appsViewModel) }
        composable(BottomNavItem.Apps.route) { AppsScreen(navController, appsViewModel) }
        composable(BottomNavItem.Plugins.route) { SimpleScreen("Plugins Screen") }
        composable(BottomNavItem.Settings.route) { SimpleScreen("Settings Screen") }
        composable(BottomNavItem.AddApps.route) { AddAppsScreen(navController, appsViewModel) }
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
