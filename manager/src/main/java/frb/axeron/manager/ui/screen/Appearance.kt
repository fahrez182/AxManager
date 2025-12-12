package frb.axeron.manager.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.ui.component.SettingsItem
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppearanceScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val settingsViewModel = viewModelGlobal.settingsViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current

    Scaffold(
        topBar = {
            TopBar(
                onBack = { navigator.popBackStack() },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost) }
    ) { paddingValues ->

        val isDarkMode = isSystemInDarkTheme()

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsItem(
                iconVector = Icons.Filled.DarkMode,
                label = "Auto Theme",
                description = "Select theme automatically",
                checked = settingsViewModel.getAppThemeId == 0,
                onSwitchChange = {
                    if (!it) {
                        settingsViewModel.setAppTheme(
                            if (isDarkMode) {
                                1
                            } else {
                                2
                            }
                        )
                    } else {
                        settingsViewModel.setAppTheme(0)
                    }
                }
            ) { enabled, checked ->
                AnimatedVisibility(!checked) {
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        settingsViewModel.themeOptions.forEachIndexed { index, text ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = (index == settingsViewModel.getAppThemeId),
                                    onClick = { settingsViewModel.setAppTheme(index) }
                                )
                                Text(
                                    text = text,
                                    modifier = Modifier.padding(start = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            SettingsItem(
                iconVector = Icons.Filled.Palette,
                label = "Dynamic Color",
                description = "Select colors dynamically",
                checked = settingsViewModel.isDynamicColorEnabled,
                onSwitchChange = {
                    settingsViewModel.setDynamicColor(it)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }, navigationIcon = {
            IconButton(
                onClick = onBack
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
        scrollBehavior = scrollBehavior
    )
}
