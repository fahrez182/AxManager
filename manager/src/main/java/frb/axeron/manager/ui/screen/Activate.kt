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
import frb.axeron.manager.viewmodel.ViewModelGlobal
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.AutoResolutionManager
import frb.axeron.manager.api.InjectionResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ActivateScreen(
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal
) {
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
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Text(
                    text = statusText,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }

            Text(
                text = "PERFORMANCE ENGINE",
                color = goldColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CODM Blade & DPI Booster", color = goldColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customDpi,
                        onValueChange = { customDpi = it },
                        label = { Text("Custom DPI", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = goldColor,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ULTRA_FPS" to "Ultra", "HIGH_FPS" to "High").forEach { (modeId, label) ->
                            val isSelected = selectedMode == modeId
                            Button(
                                onClick = { selectedMode = modeId },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) goldColor else Color.DarkGray)
                            ) {
                                Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
