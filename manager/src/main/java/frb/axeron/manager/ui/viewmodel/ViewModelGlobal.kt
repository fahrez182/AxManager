package frb.axeron.manager.ui.viewmodel

import androidx.compose.runtime.staticCompositionLocalOf

// این کلاس تمام ViewModelها را در یک جا نگه می‌دارد
class ViewModelGlobal(
    val settingsViewModel: SettingsViewModel,
    val appsViewModel: AppsViewModel,
    val privilegeViewModel: PrivilegeViewModel,
    val activateViewModel: ActivateViewModel,
    val pluginViewModel: PluginViewModel
)

// این خط بسیار مهم است! 
// این همان چیزی است که در فایل‌های دیگر (مثل Activate.kt) از آن استفاده می‌کنید
val LocalViewModelGlobal = staticCompositionLocalOf<ViewModelGlobal> {
    error("No ViewModelGlobal provided! Make sure you wrap your NavHost with CompositionLocalProvider in AxActivity.")
}
