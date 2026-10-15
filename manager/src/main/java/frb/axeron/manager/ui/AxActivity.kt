package frb.axeron.manager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ActivateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuickShellScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import frb.axeron.api.AxeronInfo
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.FloatingBarFabHost
import frb.axeron.manager.ui.component.bottomBarFadeBrush
import frb.axeron.manager.ui.component.FloatingBarFabSlot
import frb.axeron.manager.ui.component.LocalFloatingBarFabHost
import frb.axeron.manager.ui.navigation.BottomBarDestination
import frb.axeron.manager.ui.screen.FlashIt
import frb.axeron.manager.ui.theme.AxManagerTheme
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.util.LocaleHelper
import frb.axeron.manager.ui.viewmodel.ActivateViewModel
import frb.axeron.manager.ui.viewmodel.AppsViewModel
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.PrivilegeViewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.server.PluginInstaller
import kotlinx.coroutines.delay

class AxActivity : ComponentActivity() {

    companion object {
        const val OPEN_QUICK_SHELL = "AxManager.QUICK_SHELL"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    var zipUri by mutableStateOf<List<PluginInstaller>?>(emptyList())

    val shortcut by lazy {
        ShortcutInfoCompat.Builder(this, "quick-shell")
            .setShortLabel(getString(R.string.quick_shell_short))
            .setLongLabel(getString(R.string.quick_shell))
            .setDisabledMessage(getString(R.string.quick_shell_not_supported))
            .setIcon(IconCompat.createWithResource(this, R.drawable.terminal))
            .setIntent(
                Intent(this, AxActivity::class.java).apply {
                    action = OPEN_QUICK_SHELL
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            .build()
    }

    private var intentState: Intent? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ShortcutManagerCompat.pushDynamicShortcut(this@AxActivity, shortcut)

        intentState = intent

        setContent {
            AxManagerTheme {
                MainScreen(it)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentState = intent
        Log.i("AxActivity", "onNewIntent")
    }

    @Composable
    fun MainScreen(settingsViewModel: SettingsViewModel) {
        val snackBarHostState = remember { SnackbarHostState() }
        val navController = rememberNavController()
        val navigator = navController.rememberDestinationsNavigator()
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination

        val activateViewModel: ActivateViewModel = viewModel<ActivateViewModel>()

        val appsViewModel: AppsViewModel = viewModel<AppsViewModel>()
        val privilegeViewModel: PrivilegeViewModel = viewModel<PrivilegeViewModel>()
        val pluginViewModel: PluginViewModel = viewModel<PluginViewModel>()

        val axeronInfo = activateViewModel.axeronInfo

        LaunchedEffect(intentState) {
            if (intentState == null) return@LaunchedEffect
            Log.i("AxActivity", "intent: $intentState")
            when (intentState!!.action) {
                OPEN_QUICK_SHELL -> {
                    if (axeronInfo.isRunning()) {
                        navigator.navigate(HomeScreenDestination()) {
                            popUpTo(NavGraphs.root) {
                                saveState = true
                            }
                        }
                        if (navController.currentDestination?.route == HomeScreenDestination.route) {
                            navigator.navigate(QuickShellScreenDestination())
                        }
                    }
                }

                Intent.ACTION_VIEW -> {
                    if (intentState!!.data?.scheme == "content") {
                        zipUri =
                            intent.data?.let { uri ->
                                arrayListOf(PluginInstaller(uri))
                            } ?: run {
                                val uris: ArrayList<Uri>? =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        intent.getParcelableArrayListExtra("uris", Uri::class.java)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        intent.getParcelableArrayListExtra("uris")
                                    }

                                uris
                                    ?.map { PluginInstaller(it) }
                                    ?.toCollection(arrayListOf())
                                    ?: arrayListOf()
                            }
                        if (!zipUri.isNullOrEmpty()) {
                            navigator.navigate(
                                FlashScreenDestination(
                                    flashIt = FlashIt.FlashPlugins(zipUri!!)
                                )
                            )
                            zipUri = null
                        }
                    }
                }
            }

            intentState = null
        }

        // Reload only when the server process changes, not on every AxeronInfo emission.
        val serverProcess = axeronInfo.serverInfo.uid to axeronInfo.serverInfo.pid
        LaunchedEffect(serverProcess) {
            if (axeronInfo.isRunning()) {
                // pluginViewModel is prioritized because of BottomBar badge
                pluginViewModel.fetchModuleList()

                // Stagger other loads to avoid UI stutter during startup/navigation
                delay(500)
                appsViewModel.loadInstalledApps()

                delay(500)
                privilegeViewModel.loadInstalledApps()
            }
        }

        val viewModelGlobal = remember {
            ViewModelGlobal(
                settingsViewModel = settingsViewModel,
                appsViewModel = appsViewModel,
                activateViewModel = activateViewModel,
                pluginViewModel = pluginViewModel,
                privilegeViewModel = privilegeViewModel
            )
        }

        val showBottomBar = when (currentDestination?.route) {
            ActivateScreenDestination.route -> false // Hide for Activate
            FlashScreenDestination.route -> false // Hide for Flash and plugin actions
            else -> true
        }

        val density = LocalDensity.current
        var bottomBarHeight by remember { mutableStateOf(0.dp) }
        val fabHost = remember { FloatingBarFabHost() }

        Box {
            Scaffold(
                contentWindowInsets = WindowInsets()
            ) { innerPadding ->
                CompositionLocalProvider(
                    LocalSnackbarHost provides snackBarHostState,
                    LocalFloatingBarInset provides if (showBottomBar) bottomBarHeight else 0.dp,
                    LocalFloatingBarFabHost provides fabHost,
                ) {
                    DestinationsNavHost(
                        // Full-bleed content; the floating bar overlays it and each screen adds its own bottom inset.
                        modifier = Modifier.padding(innerPadding),
                        navGraph = NavGraphs.root,
                        navController = navController,
                        dependenciesContainerBuilder = {
                            dependency(viewModelGlobal)
                        },
                        defaultTransitions = object : NavHostAnimatedDestinationStyle() {
                            override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
                                get() = {
                                    val initialState = initialState.destination.route
                                    val targetState = targetState.destination.route

                                    // Cari indeks destinasi di BottomBar
                                    val initialIndex = BottomBarDestination.entries.find { it.direction.route == initialState }?.ordinal ?: -1
                                    val targetIndex = BottomBarDestination.entries.find { it.direction.route == targetState }?.ordinal ?: -1

                                    if (initialIndex != -1 && targetIndex != -1) {
                                        // Jika pindah antar tab BottomBar
                                        if (targetIndex > initialIndex) {
                                            // Geser ke kiri (masuk dari kanan)
                                            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn()
                                        } else {
                                            // Geser ke kanan (masuk dari kiri)
                                            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn()
                                        }
                                    } else {
                                        // Animasi default jika bukan antar tab (misal masuk ke detail)
                                        fadeIn(animationSpec = tween(300))
                                    }
                                }

                            override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
                                get() = {
                                    val initialState = initialState.destination.route
                                    val targetState = targetState.destination.route

                                    val initialIndex = BottomBarDestination.entries.find { it.direction.route == initialState }?.ordinal ?: -1
                                    val targetIndex = BottomBarDestination.entries.find { it.direction.route == targetState }?.ordinal ?: -1

                                    if (initialIndex != -1 && targetIndex != -1) {
                                        if (targetIndex > initialIndex) {
                                            // Keluar ke kiri
                                            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut()
                                        } else {
                                            // Keluar ke kanan
                                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut()
                                        }
                                    } else {
                                        fadeOut(animationSpec = tween(300))
                                    }
                                }
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                CompositionLocalProvider(LocalSnackbarHost provides snackBarHostState) {
                    val navBarInset =
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Soft fade behind the pill, down through the system nav bar, mirroring
                        // the top bar's gradient. Sized from the pill row only, so FABs stacking
                        // up (e.g. re-ignite) never resize it.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(bottomBarHeight + navBarInset)
                                .background(bottomBarFadeBrush(MaterialTheme.colorScheme.background))
                        )

                        // Pill and the page's FABs form one centered group on a shared bottom
                        // baseline. No animateContentSize here: it lays content out from the top,
                        // which made the bottom-anchored pill jump whenever the FAB stack resized.
                        // The FAB slot animates its own width, so the pill glides to re-center.
                        Row(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            BottomBar(
                                navController,
                                navigator,
                                activateViewModel.axeronInfo,
                                pluginViewModel.pluginUpdateCount,
                                // Screens pad for the pill row only (+12dp above and below);
                                // extra stacked FABs float over content like a normal FAB.
                                modifier = Modifier.onSizeChanged {
                                    bottomBarHeight = with(density) { it.height.toDp() } + 24.dp
                                }
                            )
                            FloatingBarFabSlot(fabHost)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BottomBar(
        navController: NavHostController,
        navigator: DestinationsNavigator,
        axeronServerInfo: AxeronInfo,
        moduleUpdateCount: Int,
        modifier: Modifier = Modifier
    ) {
        // Icon-only pill, 56dp tall to match a FAB. Labels show as a tooltip on long-press.
        Surface(
            modifier = modifier,
            shape = CircleShape,
            // Tonal secondary so the bar stands apart from the surfaceContainerHigh cards behind it.
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(6.dp)
                    .animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomBarDestination.entries
                    .forEach { destination ->
                        if (!axeronServerInfo.isRunning() && destination.needAxeron) return@forEach

                        val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(
                            destination.direction
                        )
                        BottomBarItem(
                            selected = isCurrentDestOnBackStack,
                            icon = if (isCurrentDestOnBackStack) destination.iconSelected else destination.iconNotSelected,
                            label = stringResource(id = destination.labelId),
                            badgeCount = if (destination == BottomBarDestination.Plugin) moduleUpdateCount else 0,
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
                            }
                        )
                    }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BottomBarItem(
        selected: Boolean,
        icon: ImageVector,
        label: String,
        badgeCount: Int,
        onClick: () -> Unit
    ) {
        val containerColor by animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            label = "navItemContainer"
        )
        val contentColor by animateColorAsState(
            targetValue = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
            },
            label = "navItemContent"
        )

        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(label) } },
            state = rememberTooltipState()
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(containerColor)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (badgeCount > 0) {
                    BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
                        Icon(imageVector = icon, contentDescription = label, tint = contentColor)
                    }
                } else {
                    Icon(imageVector = icon, contentDescription = label, tint = contentColor)
                }
            }
        }
    }
}
