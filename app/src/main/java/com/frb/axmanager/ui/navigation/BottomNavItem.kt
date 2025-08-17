package com.frb.axmanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val iconSelected: ImageVector,
    val iconNotSelected: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "Home",
        iconSelected = Icons.Filled.Home,
        iconNotSelected = Icons.Outlined.Home
    )

    object Apps : BottomNavItem(
        route = "apps",
        title = "Apps",
        iconSelected = Icons.Filled.Apps,
        iconNotSelected = Icons.Outlined.Apps
    )

    object AddApps : BottomNavItem(
        route = "addApp",
        title = "AddApps",
        iconSelected = Icons.Filled.Add,
        iconNotSelected = Icons.Outlined.Add
    )

    object Plugins : BottomNavItem(
        route = "plugins",
        title = "Plugins",
        iconSelected = Icons.Filled.Extension,
        iconNotSelected = Icons.Outlined.Extension
    )

    object Settings : BottomNavItem(
        route = "settings",
        title = "Settings",
        iconSelected = Icons.Filled.Settings,
        iconNotSelected = Icons.Outlined.Settings
    )
}