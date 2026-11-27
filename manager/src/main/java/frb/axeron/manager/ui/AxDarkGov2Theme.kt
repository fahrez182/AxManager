package com.axdubai.manager.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val Gold = Color(0xFFF5D97A)
private val Navy = Color(0xFF0B1628)
private val Black = Color(0xFF07070A)
private val Card = Color(0xFF12141C)
private val Rgb = Color(0xFF00E5FF)
private val Dark = darkColorScheme(primary = Gold, secondary = Rgb, background = Black, surface = Card, onSurface = Color.White)
private val Light = lightColorScheme(primary = Color(0xFF8A6D00), secondary = Color(0xFF00695C), background = Color(0xFFF2F2F2), surface = Color.White, onSurface = Color.Black)
private val Official = darkColorScheme(primary = Color.White, secondary = Gold, background = Navy, surface = Color(0xFF111E33), onSurface = Color.White)
@Composable
fun AxDarkGov2Theme(mode: String = "dark", content: @Composable () -> Unit) {
  val s = when(mode) { "light" -> Light; "official" -> Official; else -> Dark }
  MaterialTheme(colorScheme = s, typography = Typography(), content = content)
}
