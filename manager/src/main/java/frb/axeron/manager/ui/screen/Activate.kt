package frb.axeron.manager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.ui.LocalViewModelGlobal // ایمپورت جدید
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.AutoResolutionManager
import frb.axeron.manager.api.InjectionResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ActivateScreen(
    navigator: DestinationsNavigator
    // پارامتر viewModelGlobal از اینجا حذف شد تا KSP ارور ندهد
) {
    // دریافت ViewModelGlobal از طریق CompositionLocal
    val viewModelGlobal = LocalViewModelGlobal.current 
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val injector = remember { AxBladeInjector(context) }
    val resolutionManager = remember { AutoResolutionManager(context, injector) }

    var customDpi by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("ULTRA_FPS") }
    var statusText by remember { mutableStateOf("Status: Ready for Blade Boost") }

    val goldColor = Color(0xFFFFD700)
    val darkBackground = Color(0xFF121212)
    val cardBackground = Color(0xFF1E1E1E)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AXMANAGER // BLADE", color = goldColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = goldColor)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = darkBackground)
            )
        },
        containerColor = darkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (بقیه کدهای مربوط به UI دقیقا مثل قبل است و تغییری نمی‌کند)
            // فقط مطمئن شو که در قسمت دکمه Apply، از همان viewModelGlobal استفاده می‌کنی
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                     Text("CODM Blade & DPI Booster", color = goldColor, fontWeight = FontWeight.Bold)
                     // ... (بقیه کدهای UI که در پیام قبل دادم را اینجا قرار بده)
                     // فقط دقت کن که دکمه APPLY دقیقا با همین منطق باشد:
                     
                     // دکمه Apply (بخش اصلی)
                     Button(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        onClick = {
                            coroutineScope.launch {
                                statusText = "Applying..."
                                val result = if (customDpi.isNotEmpty()) {
                                    resolutionManager.applyCustomBoost(customDpi.toIntOrNull() ?: 360)
                                } else {
                                    resolutionManager.applyProBoost(selectedMode)
                                }
                                statusText = when (result) {
                                    is InjectionResult.Success -> "Success: ${result.data}"
                                    is InjectionResult.Failure -> "Error: ${result.reason}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = goldColor)
                    ) {
                        Text("APPLY BOOST", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
