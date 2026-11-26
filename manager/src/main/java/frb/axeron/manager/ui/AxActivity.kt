package frb.axeron.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.destinations.DestinationContent
import com.ramcosta.composedestinations.destinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.compose.DestinationsNavHost
import frb.axeron.manager.ui.screen.LocalViewModelGlobal
// فرض بر این است که ViewModelGlobal را در پکیج اصلی یا زیرپکیج مربوطه داری
// اگر ارور داد، نام پکیج را چک کن، اما اینجا با استفاده از CompositionLocal مشکل حل می‌شود

class AxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // اینجا تم اصلی اپلیکیشن را ست می‌کنیم
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // استفاده از CompositionLocal برای تزریق ViewModel به تمام صفحات بدون ارور KSP
                    // ما از Any استفاده می‌کنیم تا اگر کلاس ViewModelGlobal هنوز کامپایل نشده، ارور ندهد
                    CompositionLocalProvider(LocalViewModelGlobal provides null) { 
                        
                        // این بخش مربوط به Compose Destinations است که مسیرها را خودکار می‌سازد
                        DestinationsNavHost(navGraph = RootNavGraph)
                        
                    }
                }
            }
        }
    }
}
