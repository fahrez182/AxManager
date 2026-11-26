package frb.axeron.manager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.ramcosta.composedestinations.DestinationsNavHost
import frb.axeron.manager.ui.screen.LocalViewModelGlobal
import frb.axeron.manager.ui.generated.NavGraphs // یا پکیج تولیدی گراف‌های شما
import frb.axeron.manager.viewmodel.ViewModelGlobal

class AxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val viewModelGlobal = ViewModelGlobal()

        setContent {
            CompositionLocalProvider(
                LocalViewModelGlobal provides viewModelGlobal
            ) {
                // استفاده از ساختار استاندارد کتابخانه برای ناوبری
                DestinationsNavHost(navGraph = NavGraphs.root)
            }
        }
    }
}
