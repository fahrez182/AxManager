package frb.axeron.manager.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.AutoResolutionManager
import frb.axeron.manager.api.InjectionResult
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import kotlinx.coroutines.launch

private val Bg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1E)
private val AxYellow = Color(0xFFFFD60A)
private val AxGray = Color(0xFF9A9A9A)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ActivateScreen(
  navigator: DestinationsNavigator,
  viewModelGlobal: ViewModelGlobal
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val injector = remember { AxBladeInjector(context) }
  val resolutionManager = remember { AutoResolutionManager(context, injector) }

  var customDpi by remember { mutableStateOf("360") }
  var selectedMode by remember { mutableStateOf("ULTRA_FPS") }
  var statusText by remember { mutableStateOf("Status: Ready") }
  var curDpi by remember { mutableStateOf("...") }

  LaunchedEffect(Unit) { curDpi = resolutionManager.currentDpi() }

  Column(
    Modifier.fillMaxSize().background(Bg).padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = { navigator.popBackStack() }) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AxYellow)
      }
      Text("AXMANAGER", color = AxYellow, fontSize = 28.sp, fontWeight = FontWeight.Black)
    }
    Text("CODM Blade & DPI Booster", color = AxGray, fontSize = 14.sp)
    Spacer(Modifier.height(16.dp))

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp)) {
        Text(statusText, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text("Current DPI: $curDpi", color = AxGray, fontSize = 12.sp)
      }
    }
    Spacer(Modifier.height(14.dp))

    OutlinedTextField(
      value = customDpi, onValueChange = { customDpi = it.filter(Char::isDigit).take(4) },
      label = { Text("Custom DPI (e.g. 360)", color = AxYellow) },
      singleLine = true, keyboardOptions = KeyboardOptions(KeyboardType.Number),
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))
    Text("Performance Mode", color = AxYellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(10.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      listOf("SMOOTH" to "Smooth", "HIGH_FPS" to "High", "ULTRA_FPS" to "Ultra").forEach { (id, label) ->
        val sel = selectedMode == id
        Button(
          onClick = { selectedMode = id },
          colors = ButtonDefaults.buttonColors(containerColor = if (sel) AxYellow else CardBg, contentColor = if (sel) Color.Black else Color.White),
          modifier = Modifier.weight(1f), shape = RoundedCornerShape(22.dp)
        ) { Text(label) }
      }
    }
    Spacer(Modifier.height(20.dp))

    Button(
      onClick = {
        scope.launch {
          statusText = "Status: Working..."
          val dpi = customDpi.toIntOrNull() ?: 360
          val result = if (customDpi.isNotEmpty()) resolutionManager.applyCustomBoost(dpi)
          else resolutionManager.applyProBoost(selectedMode)
          statusText = when (result) {
            is InjectionResult.Success -> "Status: Boosted (${result.data} / $selectedMode)"
            is InjectionResult.Failure -> "Error: ${result.reason}"
          }
          curDpi = resolutionManager.currentDpi()
        }
      },
      colors = ButtonDefaults.buttonColors(containerColor = AxYellow, contentColor = Color.Black),
      modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(28.dp)
    ) { Text("APPLY BOOST", fontWeight = FontWeight.Black) }

    Spacer(Modifier.height(12.dp))
    Button(
      onClick = { scope.launch { resolutionManager.reset(); statusText = "Status: Ready"; curDpi = resolutionManager.currentDpi() } },
      colors = ButtonDefaults.buttonColors(containerColor = CardBg, contentColor = Color.White),
      modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(28.dp)
    ) { Text("RESET DEFAULTS") }

    Spacer(Modifier.height(10.dp))
    Text("بدون تزریق به پروسه بازی کار می‌کند. فقط سرویس باید Running باشد.", color = AxGray, fontSize = 11.sp, textAlign = TextAlign.Center)
  }
}
