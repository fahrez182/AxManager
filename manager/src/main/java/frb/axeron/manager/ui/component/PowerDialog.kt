package frb.axeron.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun VerticalSlideSeekbar(
    onSlideUp: () -> Unit,
    onSlideDown: () -> Unit
) {
    val height = 320.dp
    val knobSize = 55.dp

    val heightPx = with(LocalDensity.current) { height.toPx() }
    val extraRange = with(LocalDensity.current) { -32.dp.toPx() } // thumb boleh keluar 40dp

    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .width(65.dp)
            .height(height)
            .background(Color(0xFF303030), RoundedCornerShape(46.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        offsetY = (offsetY + dragAmount)
                            .coerceIn(
                                -(heightPx / 2f + extraRange),
                                heightPx / 2f + extraRange
                            )
                    },
                    onDragEnd = {
                        val upThreshold = -heightPx * 0.38f
                        val downThreshold = heightPx * 0.38f

                        when {
                            offsetY <= upThreshold -> onSlideUp()
                            offsetY >= downThreshold -> onSlideDown()
                        }
                        offsetY = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {

        // === TRACK ATAS / BAWAH ===
        Box(
            modifier = Modifier
                .matchParentSize(),
            contentAlignment = Alignment.Center
        ) {

            // Ikon Shutdown di atas
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = Color(0xFFB0B0B0),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .size(28.dp)
            )

            // Ikon Restart di bawah
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                tint = Color(0xFFB0B0B0),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(28.dp)
            )
        }

        // THUMB BISA MELEBIHI BATAS BACKGROUND
        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.toInt()) }
                .size(knobSize)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PowerDialog(
    onDismiss: () -> Unit,
    onReignite: () -> Unit,
    onShutdown: () -> Unit,
    onRestart: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {

        // BACKGROUND — menerima tap untuk dismiss
        Box(
            contentAlignment = Alignment.Center
        ) {

            // CONTENT DI ATAS BACKGROUND (tidak ketutup gesture)
            Surface(
                color = Color.Transparent,
                tonalElevation = 4.dp,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    Button(
                        onClick = {
                            onDismiss()
                            onReignite()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        contentPadding = PaddingValues(16.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Reignite",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    VerticalSlideSeekbar(
                        onSlideUp = {
                            onDismiss()
                            onShutdown()
                        },
                        onSlideDown = {
                            onDismiss()
                            onRestart()
                        }
                    )
                }
            }
        }
    }
}