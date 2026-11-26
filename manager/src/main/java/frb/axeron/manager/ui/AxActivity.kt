package frb.axeron.manager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ramcosta.composedestinations.destinations.DestinationsNavHost
import frb.axeron.manager.ui.navigation.RootNavGraph
import frb.axeron.manager.viewmodel.ViewModelGlobal

class AxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // فرض می‌کنیم ViewModelGlobal را اینجا ساخته‌ای یا از جای دیگری می‌آید
        val viewModelGlobal = ViewModelGlobal() 

        setContent {
            // با این خط، تمام زیرشاخه ها به ViewModelGlobal دسترسی پیدا می‌کنند
            androidx.compose.runtime.CompositionLocalProvider(
                LocalViewModelGlobal provides viewModelGlobal
            ) {
                DestinationsNavHost(
                    navGraph = RootNavGraph,
                    startDestination = "home"
                )
            }
        }
    }
}
