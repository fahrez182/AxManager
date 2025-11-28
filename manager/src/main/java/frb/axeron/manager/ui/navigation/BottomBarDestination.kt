package frb.axeron.manager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PluginScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PrivilegeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

enum class BottomBarDestination(
    val direction: DirectionDestinationSpec,
    val label: String,
    val iconSelected: ImageVector,
    val iconNotSelected: ImageVector,
    val needAxeron: Boolean,
    val needShizuku: Boolean = false
) {
    Home(HomeScreenDestination, "Home", Icons.Filled.Home, Icons.Outlined.Home, false),

    //    QuickShell(QuickShellScreenDestination, "QuickShell", Icons.Filled.Terminal, Icons.Outlined.Terminal, true),
    Privilege(
        PrivilegeScreenDestination,
        "Privilege",
        Icons.Filled.AdminPanelSettings,
        Icons.Outlined.AdminPanelSettings,
        true,
        true
    ),
    Plugin(
        PluginScreenDestination,
        "Plugin",
        Icons.Filled.Extension,
        Icons.Outlined.Extension,
        true
    ),
    Settings(
        SettingsScreenDestination,
        "Settings",
        Icons.Filled.Settings,
        Icons.Outlined.Settings,
        false
    ),
}