package frb.axeron.manager.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    // موتورهای Blade و رزولوشن
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
            // Status Card
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

            // --- بخش AX-BOOST & BLADE ---
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
                    Text(
                        text = "CODM Blade & DPI Booster",
                        color = goldColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Safe Non-Root FPS & Resolution Scaling",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom DPI Input
                    OutlinedTextField(
                        value = customDpi,
                        onValueChange = { customDpi = it },
                        label = { Text("Custom DPI (e.g. 360)", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = goldColor,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = goldColor,
                            cursorColor = goldColor
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ULTRA_FPS" to "Ultra", "HIGH_FPS" to "High", "SMOOTH" to "Smooth").forEach { (modeId, label) ->
                            val isSelected = selectedMode == modeId
                            Button(
                                onClick = { selectedMode = modeId },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) goldColor else Color.DarkGray
                                )
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Apply Button
                    Button(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        onClick = {
                            coroutineScope.launch {
                                statusText = "Applying Blade Boost..."
                                val result = if (customDpi.isNotEmpty()) {
                                    val dpi = customDpi.toIntOrNull() ?: 360
                                    resolutionManager.applyCustomBoost(dpi)
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reset Button
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        onClick = {
                            coroutineScope.launch {
                                statusText = "Resetting defaults..."
                                val result = resolutionManager.reset()
                                statusText = when (result) {
                                    is InjectionResult.Success -> "Reset Successful"
                                    is InjectionResult.Failure -> "Reset Error: ${result.reason}"
                                }
                                customDpi = ""
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("RESET DEFAULTS", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
