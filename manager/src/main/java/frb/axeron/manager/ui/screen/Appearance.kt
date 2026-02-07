package frb.axeron.manager.ui.screen

import android.app.Activity
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.core.content.edit
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.core.AxeronSettings
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.PaletteDialog
import frb.axeron.manager.ui.component.SettingsItem
import frb.axeron.manager.ui.component.rememberCustomDialog
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
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val context = LocalContext.current
            val prefs = AxeronSettings.getPreferences()
            var currentAppLocale by remember {
                mutableStateOf(
                    LocaleHelper.getCurrentAppLocale(
                        context
                    )
                )
            }

            // Listen for preference changes
            LaunchedEffect(Unit) {
                currentAppLocale = LocaleHelper.getCurrentAppLocale(context)
            }

            val settingsLanguage = stringResource(R.string.settings_language)
            val systemDefault = stringResource(R.string.system_default)

            // Language setting with selection dialog
            val languageDialog = rememberCustomDialog { dismiss ->
                // Check if should use system language settings
                if (LocaleHelper.useSystemLanguageSettings) {
                    // Android 13+ - Jump to system settings
                    LocaleHelper.launchSystemLanguageSettings(context)
                    dismiss()
                } else {
                    // Android < 13 - Show app language selector
                    // Dynamically detect supported locales from resources
                    val supportedLocales = remember {
                        val locales = mutableListOf<java.util.Locale>()

                        // Add system default first
                        locales.add(java.util.Locale.ROOT) // This will represent "System Default"

                        // Dynamically detect available locales by checking resource directories
                        val resourceDirs = listOf(
                            "ar", "bg", "de", "es", "fa", "fr", "hu", "in", "it",
                            "ja", "ko", "pl", "pt", "pt-rBR", "ru", "th", "tr",
                            "uk", "vi", "zh", "zh-rCN", "zh-rTW"
                        )

                        resourceDirs.forEach { dir ->
                            try {
                                val locale = when {
                                    dir.contains("-r") -> {
                                        val parts = dir.split("-r")
                                        java.util.Locale.Builder()
                                            .setLanguage(parts[0])
                                            .setRegion(parts[1])
                                            .build()
                                    }

                                    else -> java.util.Locale.Builder()
                                        .setLanguage(dir)
                                        .build()
                                }

                                // Test if this locale has translated resources
                                val config = android.content.res.Configuration()
                                config.setLocale(locale)
                                val localizedContext = context.createConfigurationContext(config)

                                // Try to get a translated string to verify the locale is supported
                                val testString =
                                    localizedContext.getString(R.string.settings_language)

                                // If the string is different or it's English, it's supported
                                if (testString != settingsLanguage || locale.language == "en") {
                                    locales.add(locale)
                                }
                            } catch (_: Exception) {
                                // Skip unsupported locales
                            }
                        }

                        // Sort by display name
                        val sortedLocales = locales.drop(1).sortedBy { it.getDisplayName(it) }
                        mutableListOf<java.util.Locale>().apply {
                            add(locales.first()) // System default first
                            addAll(sortedLocales)
                        }
                    }

                    val allOptions = supportedLocales.map { locale ->
                        val tag = if (locale == java.util.Locale.ROOT) {
                            "system"
                        } else if (locale.country.isEmpty()) {
                            locale.language
                        } else {
                            locale.toLanguageTag()
                        }

                        val displayName = if (locale == java.util.Locale.ROOT) {
                            systemDefault
                        } else {
                            locale.getDisplayName(locale)
                        }

                        tag to displayName
                    }

                    val currentLocale = prefs.getString(AxeronSettings.LANGUAGE, "system") ?: "system"
                    val options = allOptions.map { (tag, displayName) ->
                        ListOption(
                            titleText = displayName,
                            selected = currentLocale == tag
                        )
                    }

                    var selectedIndex by remember {
                        mutableIntStateOf(allOptions.indexOfFirst { (tag, _) -> currentLocale == tag })
                    }

                    ListDialog(
                        state = rememberUseCaseState(
                            visible = true,
                            onFinishedRequest = {
                                if (selectedIndex >= 0 && selectedIndex < allOptions.size) {
                                    val newLocale = allOptions[selectedIndex].first
                                    prefs.edit { putString(AxeronSettings.LANGUAGE, newLocale) }

                                    // Update local state immediately
                                    currentAppLocale = LocaleHelper.getCurrentAppLocale(context)

                                    // Apply locale change immediately for Android < 13
                                    if (context is Activity) {
                                        context.recreate()
                                    }
                                }
                                dismiss()
                            },
                            onCloseRequest = {
                                dismiss()
                            }
                        ),
                        header = Header.Default(
                            title = settingsLanguage,
                        ),
                        selection = ListSelection.Single(
                            showRadioButtons = true,
                            options = options
                        ) { index, _ ->
                            selectedIndex = index
                        }
                    )
                }
            }

            // Compute display name based on current app locale (similar to the reference implementation)
            val currentLanguageDisplay = remember(currentAppLocale) {
                val locale = currentAppLocale
                if (locale != null) {
                    locale.getDisplayName(locale)
                } else {
                    systemDefault
                }
            }

            SettingsItem(
                iconVector = Icons.Filled.Translate,
                label = settingsLanguage,
                description = currentLanguageDisplay,
                onClick = {
                    languageDialog.show()
                }
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
    TopAppBar(
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
