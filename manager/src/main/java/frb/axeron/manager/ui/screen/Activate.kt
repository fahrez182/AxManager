package frb.axeron.manager.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.AutoResolutionManager
import frb.axeron.manager.api.InjectionResult
import kotlinx.coroutines.launch

private val Bg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1E)
private val AxYellow = Color(0xFFFFD60A)
private val AxGray = Color(0xFF9A9A9A)

@Destination<RootGraph>
@Composable
fun ActivateScreen(navigator: DestinationsNavigator) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val injector = remember { AxBladeInjector(context) }
  val manager = remember { AutoResolutionManager(context, injector) }
  var customDpi by remember { mutableStateOf("360") }
  var mode by remember { mutableStateOf("ULTRA_FPS") }
  var status by remember { mutableStateOf("Status: Ready") }
  var curDpi by remember { mutableStateOf("...") }
  LaunchedEffect(Unit) { curDpi = manager.currentDpi() }
  Column(Modifier.fillMaxSize().background(Bg).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = { navigator.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AxYellow) }
      Text("AXMANAGER", color = AxYellow, fontSize = 28.sp, fontWeight = FontWeight.Black)
    }
    Text("CODM Blade & DPI Booster", color = AxGray, fontSize = 14.sp)
    Spacer(Modifier.height(16.dp))
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp)) {
        Text(status, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text("Current DPI: $curDpi", color = AxGray, fontSize = 12.sp)
      }
    }
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(value = customDpi, onValueChange = { customDpi = it.filter(Char::isDigit).take(4) }, label = { Text("Custom DPI (e.g. 360)", color = AxYellow) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    Text("Performance Mode", color = AxYellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      listOf("SMOOTH" to "Smooth", "HIGH_FPS" to "High", "ULTRA_FPS" to "Ultra").forEach { (id, label) ->
        val sel = mode == id
        Button(onClick = { mode = id }, colors = ButtonDefaults.buttonColors(containerColor = if (sel) AxYellow else CardBg, contentColor = if (sel) Color.Black else Color.White), modifier = Modifier.weight(1f)) { Text(label) }
      }
    }
    Spacer(Modifier.height(20.dp))
    Button(onClick = {
      scope.launch {
        status = "Status: Working..."
        val dpi = customDpi.toIntOrNull() ?: 360
        val result = if (customDpi.isNotEmpty()) manager.applyCustomBoost(dpi) else manager.applyProBoost(mode)
        status = when (result) { is InjectionResult.Success -> "Status: Boosted / $mode"; is InjectionResult.Failure -> "Error: ${result.reason}" }
        curDpi = manager.currentDpi()
      }
    }, colors = ButtonDefaults.buttonColors(containerColor = AxYellow, contentColor = Color.Black), modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("APPLY BOOST", fontWeight = FontWeight.Black) }
    Spacer(Modifier.height(12.dp))
    Button(onClick = { scope.launch { manager.reset(); status = "Status: Ready"; curDpi = manager.currentDpi() } }, colors = ButtonDefaults.buttonColors(containerColor = CardBg, contentColor = Color.White), modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("RESET DEFAULTS") }
  }
}
