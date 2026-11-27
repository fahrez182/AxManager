package frb.axeron.manager.ui.viewmodel

import androidx.compose.runtime.staticCompositionLocalOf

// این کلاس همان ظرف شماست
class ViewModelGlobal(
    val settingsViewModel: SettingsViewModel,
    val appsViewModel: AppsViewModel,
    val privilegeViewModel: PrivilegeViewModel,
    val activateViewModel: ActivateViewModel,
    val pluginViewModel: PluginViewModel
)

// این بخش جادویی است: یک Local برای دسترسی جهانی بدون پاس دادن پارامتر
val LocalViewModelGlobal = staticCompositionLocalOf<ViewModelGlobal> {
    error("No ViewModelGlobal provided")
}
