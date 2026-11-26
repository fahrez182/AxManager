package frb.axeron.manager.ui

import androidx.compose.runtime.compositionLocalOf
import frb.axeron.manager.viewmodel.ViewModelGlobal

// این خط به کامپوز می‌گوید که چطور ViewModelGlobal را در تمام صفحات پیدا کند
val LocalViewModelGlobal = compositionLocalOf<ViewModelGlobal> {
    error("No ViewModelGlobal provided")
}
