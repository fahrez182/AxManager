package frb.axeron.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.api.RootNavGraph
import frb.axeron.manager.ui.screen.LocalViewModelGlobal

class AxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(LocalViewModelGlobal provides null) {
                        DestinationsNavHost(navGraph = RootNavGraph)
                    }
                }
            }
        }
    }
}
