package frb.axeron.manager.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
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
import frb.axeron.manager.ui.component.AxTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.R
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.component.LanguageSheet
import frb.axeron.manager.ui.component.PaletteDialog
import frb.axeron.manager.ui.component.SettingsItem
import frb.axeron.manager.ui.theme.hexToColor
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.util.LocaleHelper
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppearanceScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val settingsViewModel = viewModelGlobal.settingsViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current
    var showColorPicker by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

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
        val currentColor = hexToColor(settingsViewModel.customPrimaryColorHex)

        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                // Padding inside the scroll lets content pass under the fading top bar.
                .padding(
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + LocalFloatingBarInset.current + 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val context = LocalContext.current
            // The activity is recreated on a language change, so this is read fresh each time.
            val currentAppLocale = remember { LocaleHelper.getCurrentAppLocale(context) }

            SettingsItem(
                iconVector = Icons.Filled.Translate,
                label = stringResource(R.string.settings_language),
                description = currentAppLocale
                    ?.let { it.getDisplayName(it).replaceFirstChar { c -> c.titlecase(it) } }
                    ?: stringResource(R.string.system_default),
                onClick = { showLanguageSheet = true }
            )

            SettingsItem(
                iconVector = Icons.Filled.DarkMode,
                label = stringResource(R.string.auto_theme),
                description = stringResource(R.string.auto_theme_desc),
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
                label = stringResource(R.string.dynamic_color),
                description = stringResource(R.string.dynamic_color_desc),
                checked = settingsViewModel.isDynamicColorEnabled,
                onSwitchChange = {
                    settingsViewModel.setDynamicColor(it)
                }
            )

            SettingsItem(
                iconVector = Icons.Filled.Palette,
                label = stringResource(R.string.color_palette),
                description = if (settingsViewModel.isDynamicColorEnabled)
                    stringResource(R.string.palette_overridden_by_dynamic)
                else stringResource(R.string.customize_color_palette),
                enabled = !settingsViewModel.isDynamicColorEnabled,
                onClick = { showColorPicker = true }
            ) { _, _ ->
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.current_color_palette),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
            }
        }

        if (showLanguageSheet) {
            LanguageSheet(onDismiss = { showLanguageSheet = false })
        }

        if (showColorPicker) {
            PaletteDialog(
                initialColor = currentColor,
                onDismiss = { showColorPicker = false },
                onConfirm = { hex ->
                    settingsViewModel.setCustomPrimaryColor(hex)
                    showColorPicker = false
                },
                onReset = {
                    settingsViewModel.removeCustomPrimaryColor()
                    showColorPicker = false
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
    AxTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.appearance),
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
