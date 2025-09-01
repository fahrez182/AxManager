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
import com.ramcosta.composedestinations.generated.destinations.AppsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PluginsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

enum class BottomBarDestination(
    val direction: DirectionDestinationSpec,
    val label: String,
    val iconSelected: ImageVector? = null,
    val iconNotSelected: ImageVector? = null,
    val needAxeron: Boolean
) {
    Home(HomeScreenDestination, "Home", Icons.Filled.Home, Icons.Outlined.Home, false),
    Apps(AppsScreenDestination, "Apps", Icons.Filled.Apps, Icons.Outlined.Apps, true),
    Plugins(PluginsScreenDestination, "Plugins", Icons.Filled.Extension, Icons.Outlined.Extension, true),
    Settings(SettingsScreenDestination, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, false),
}