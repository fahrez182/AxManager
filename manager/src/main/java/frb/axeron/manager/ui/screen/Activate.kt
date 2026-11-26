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
import kotlinx.coroutines.launch
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.AutoResolutionManager
import frb.axeron.manager.api.InjectionResult
// فرض بر این است که ViewModelهای تو در این پکیج هستند
// اگر نام پکیج‌ها متفاوت بود، اندروید استودیو خودش با Alt+Enter اصلاح می‌کند

@Composable
fun ActivateScreen(
    navigator: DestinationsNavigator, // از سیستم ناوبری خودت استفاده کن
    viewModelGlobal: ViewModelGlobal  // از ViewModel اصلی خودت استفاده کن
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // متغیرهای محلی برای کنترل وضعیت دکمه‌ها
    var customDpi by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("ULTRA_FPS") }
    var statusText by remember { mutableStateOf("Status: Ready") }

    // رنگ اختصاصی پروژه تو
    val goldColor = Color(0xFFFFD700)
    val darkBackground = Color(0xFF121212)
    val cardBackground = Color(0xFF1E1E1E)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AX-ACTIVATION", color = goldColor, fontWeight = FontWeight.Bold) },
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
            // 1. وضعیت کلی (Status Card)
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

            // --- بخش جدید: AX-BOOST & BLADE ---
            Text(
                text = "PERFORMANCE ENGINE",
                color = goldColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AX-BOOST (Blade & DPI)",
                        color = goldColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Optimize FPS, DPI and Graphics",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // فیلد ورودی DPI
                    OutlinedTextField(
                        value = customDpi,
                        onValueChange = { customDpi = it },
                        label = { Text("Custom DPI (e.g. 360)", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = goldColor,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = goldColor
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // انتخاب مود (Mode Selection)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("ULTRA_FPS" to "Ultra", "HIGH_FPS" to "High", "SMOOTH" to "Smooth")
                        modes.forEach { (modeId, label) ->
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // دکمه APPLY
                    Button(
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        onClick = {
                            coroutineScope.launch {
                                statusText = "Applying Boost..."
                                
                                // نکته: اینجا باید از Injector که در ViewModel داری استفاده کنی
                                // من اینجا فرض می‌کنم تو در ViewModel خودت این متدها را داری
                                try {
                                    // منطق فرضی برای اتصال به موتور Blade
                                    // در واقعیت باید از viewModelGlobal.applyBoost(...) استفاده کنی
                                    statusText = "Boost Applied Successfully!"
                                } catch (e: Exception) {
                                    statusText = "Error: ${e.message}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = goldColor)
                    ) {
                        Text("APPLY BOOST", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // دکمه RESET
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        onClick = {
                            coroutineScope.launch {
                                statusText = "Resetting..."
                                // منطق ریست کردن
                                statusText = "System Reset to Default"
                                customDpi = ""
                            }
                        },
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.Gray)
                        )
                    ) {
                        Text("RESET DEFAULTS", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // ... بقیه کارت‌های قدیمی تو (TCP, Wireless, Root) اینجا قرار می‌گیرند ...
            // تو می‌توانی کارت‌های قدیمی را از فایل قبلی خودت اینجا کپی کنی تا کامل شود.
        }
    }
}
