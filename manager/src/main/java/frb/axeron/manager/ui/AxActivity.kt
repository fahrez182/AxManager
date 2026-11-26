package frb.axeron.manager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ramcosta.composedestinations.destinations.DestinationsNavHost
import frb.axeron.manager.ui.navigation.RootNavGraph // حتما چک کن این نام درست باشد
import frb.axeron.manager.viewmodel.ViewModelGlobal

class AxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // این فقط ناوبری اصلی را راه اندازی میکند
            DestinationsNavHost(
                navGraph = RootNavGraph,
                startDestination = "home" // نام صفحه اصلی تو
            )
        }
    }
}
