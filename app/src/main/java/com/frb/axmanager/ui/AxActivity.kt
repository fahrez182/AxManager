package com.frb.axmanager.ui

import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.frb.axmanager.ui.navigation.BottomNavItem
import com.frb.axmanager.ui.navigation.NavHostContainer
import com.frb.axmanager.ui.theme.AxManagerTheme
import com.frb.axmanager.ui.viewmodel.AppsViewModel

class AxActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    val appsViewModel: AppsViewModel = viewModel()

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination


    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Apps,
        BottomNavItem.Plugins,
        BottomNavItem.Settings
    )

    val showBottomBar = when (currentDestination?.route) {
        BottomNavItem.AddApps.route -> false // Hide for AddApps
        else -> true
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BottomBar(navController, items)
            }

        }
    ) { innerPadding ->
        NavHostContainer(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            appsViewModel = appsViewModel)
    }
}

@Composable
fun BottomBar(navController: NavHostController, items: List<BottomNavItem>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            val route by remember {
                mutableStateOf(item.route)
            }
            val selected = currentRoute == route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.iconSelected else item.iconNotSelected,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
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