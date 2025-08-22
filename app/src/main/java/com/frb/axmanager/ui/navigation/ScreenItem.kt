package com.frb.axmanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ScreenItem(
    val route: String,
    val iconSelected: ImageVector? = null,
    val iconNotSelected: ImageVector? = null,
    val needAxeron: Boolean
) {
    object Home : ScreenItem(
        route = "Home",
        iconSelected = Icons.Filled.Home,
        iconNotSelected = Icons.Outlined.Home,
        needAxeron = false
    )

    object Apps : ScreenItem(
        route = "Apps",
        iconSelected = Icons.Filled.Apps,
        iconNotSelected = Icons.Outlined.Apps,
        needAxeron = true
    )

    object Plugins : ScreenItem(
        route = "Plugins",
        iconSelected = Icons.Filled.Extension,
        iconNotSelected = Icons.Outlined.Extension,
        needAxeron = true
    )

    object Settings : ScreenItem(
        route = "Settings",
        iconSelected = Icons.Filled.Settings,
        iconNotSelected = Icons.Outlined.Settings,
        needAxeron = false
    )

    object AddApps : ScreenItem(
        route = "AddApp",
        needAxeron = true
    )

    object Activate : ScreenItem(
        route = "Activate",
        needAxeron = false
    )

    object QuickShell : ScreenItem(
        route = "QuickShell",
        needAxeron = true
    )
}