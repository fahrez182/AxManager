package frb.axeron.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import frb.axeron.manager.ui.theme.hexToColor
import kotlin.math.roundToInt

fun colorToHex(color: Color): String {
    val r = (color.red * 255).roundToInt()
    val g = (color.green * 255).roundToInt()
    val b = (color.blue * 255).roundToInt()
    return String.format("#%02X%02X%02X", r, g, b)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteDialog(
    initialColor: Color = Color(0xFFF56A1C),
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onReset: () -> Unit
) {
    val controller = rememberColorPickerController()
    var selectedColor by remember { mutableStateOf(initialColor) }
    var hexInput by remember { mutableStateOf(colorToHex(initialColor)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var redValue by remember { mutableFloatStateOf(initialColor.red * 255) }
    var greenValue by remember { mutableFloatStateOf(initialColor.green * 255) }
    var blueValue by remember { mutableFloatStateOf(initialColor.blue * 255) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Color Palette")
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Wheel") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("RGB") }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        HsvColorPicker(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            controller = controller,
                            initialColor = initialColor,
                            onColorChanged = { envelope ->
                                selectedColor = envelope.color
                                hexInput = colorToHex(envelope.color)
                                redValue = envelope.color.red * 255
                                greenValue = envelope.color.green * 255
                                blueValue = envelope.color.blue * 255
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        BrightnessSlider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            controller = controller,
                            initialColor = initialColor
                        )
                    }
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SliderRow("R", redValue, Color.Red) { value ->
                                redValue = value
                                selectedColor = Color(
                                    red = redValue / 255f,
                                    green = greenValue / 255f,
                                    blue = blueValue / 255f
                                )
                                hexInput = colorToHex(selectedColor)
                            }
                            SliderRow("G", greenValue, Color.Green) { value ->
                                greenValue = value
                                selectedColor = Color(
                                    red = redValue / 255f,
                                    green = greenValue / 255f,
                                    blue = blueValue / 255f
                                )
                                hexInput = colorToHex(selectedColor)
                            }
                            SliderRow("B", blueValue, Color.Blue) { value ->
                                blueValue = value
                                selectedColor = Color(
                                    red = redValue / 255f,
                                    green = greenValue / 255f,
                                    blue = blueValue / 255f
                                )
                                hexInput = colorToHex(selectedColor)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        hexToColor(input).let { color ->
                            selectedColor = color
                            redValue = color.red * 255
                            greenValue = color.green * 255
                            blueValue = color.blue * 255
                        }
                    },
                    label = { Text("Hex") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(selectedColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(colorToHex(selectedColor)) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) {
                    Text("Reset")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    tint: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
            modifier = Modifier.width(24.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.roundToInt().toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(36.dp)
        )
    }
}

// aaaa