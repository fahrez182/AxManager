package frb.axeron.manager.ui.screen

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import frb.axeron.manager.api.AutoResolutionManager
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.InjectionResult
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var injector: AxBladeInjector
    private lateinit var resolutionManager: AutoResolutionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        injector = AxBladeInjector(this)
        resolutionManager = AutoResolutionManager(this, injector)

        setContent {
            AxManagerTheme {
                MainScreen(resolutionManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(resolutionManager: AutoResolutionManager) {
    val coroutineScope = rememberCoroutineScope()
    var customDpi by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("ULTRA_FPS") }
    var statusText by remember { mutableStateOf("Status: Ready") }

    val goldColor = Color(0xFFFFD700)
    val darkBackground = Color(0xFF121212)
    val cardBackground = Color(0xFF1E1E1E)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = darkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "AXMANAGER",
                color = goldColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "CODM Blade & DPI Booster",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 30.dp)
            )

            // Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Text(
                    text = statusText,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }

            // Custom DPI Input
            OutlinedTextField(
                value = customDpi,
                onValueChange = { customDpi = it },
                label = { Text("Custom DPI (e.g. 360)", color = goldColor) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = goldColor,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = goldColor,
                    cursorColor = goldColor
                ),
                singleLine = true
            )

            // Mode Selection
            Text(
                text = "Performance Mode",
                color = goldColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("ULTRA_FPS" to "Ultra", "HIGH_FPS" to "High", "SMOOTH" to "Smooth").forEach { (mode, label) ->
                    Button(
                        onClick = { selectedMode = mode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == mode) goldColor else cardBackground
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (selectedMode == mode) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Apply Boost Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        statusText = "Applying Boost..."
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = goldColor)
            ) {
                Text(
                    text = "APPLY BOOST",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            // Reset Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        statusText = "Resetting..."
                        val result = resolutionManager.reset()
                        statusText = when (result) {
                            is InjectionResult.Success -> "Reset Successful"
                            is InjectionResult.Failure -> "Reset Error: ${result.reason}"
                        }
                        customDpi = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cardBackground)
            ) {
                Text(
                    text = "RESET DEFAULTS",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AxManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFFD700),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        ),
        content = content
    )
}
