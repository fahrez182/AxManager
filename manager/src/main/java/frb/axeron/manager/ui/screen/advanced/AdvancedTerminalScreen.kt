package frb.axeron.manager.ui.screen.advanced

import androidx.compose.animation.core.LinearEasing
import android.util.Log
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.ui.component.TerminalControlPanel
import frb.axeron.manager.ui.component.TerminalInputView

@Destination<RootGraph>
@Composable
fun AdvancedTerminalScreen(navigator: DestinationsNavigator) {
    val viewModel: AdvancedTerminalViewModel = viewModel()
    val terminalStatus by viewModel.terminalStatus.collectAsState()

    AdvancedTerminalView(viewModel, terminalStatus) {
        navigator.popBackStack()
    }
}

@Composable
fun AdvancedTerminalView(
    viewModel: AdvancedTerminalViewModel,
    terminalStatus: String,
    onBack: () -> Unit
) {
    val emulator = viewModel.terminalEmulator
    val lines = emulator.outputLines
    val cursorRow = emulator.cursorRow
    val cursorCol = emulator.cursorCol

    var terminalInputView by remember { mutableStateOf<TerminalInputView?>(null) }
    val scrollState = rememberScrollState()

    LaunchedEffect(terminalInputView) {
        terminalInputView?.requestTerminalFocus()
    }

    // Auto-scroll to bottom when content changes
    LaunchedEffect(emulator.revision, cursorRow) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .imePadding()
    ) {
        // Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Axeron: $terminalStatus",
                color = if (terminalStatus == "Connected") Color.Green else Color.Red,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Filled.Terminal, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    terminalInputView?.requestTerminalFocus()
                }
        ) {
            val density = LocalDensity.current
            // Use a more precise character measurement if possible, or stick to monospace assumptions
            val charWidth = with(density) { 7.2.sp.toDp() }
            val charHeight = with(density) { 14.sp.toDp() }

            val cols = (maxWidth / charWidth).toInt().coerceAtLeast(10)
            val rows = (maxHeight / charHeight).toInt().coerceAtLeast(10)

            LaunchedEffect(cols, rows) {
                viewModel.terminalEmulator.resize(rows, cols)
            }

            // Real input view instead of hidden TextField
            AndroidView(
                factory = { ctx ->
                    TerminalInputView(ctx).apply {
                        onTextInput = { text ->
                            Log.d("AdvancedTerminal", "LOG: Keyboard input received: $text")
                            viewModel.sendInput(text)
                        }
                        onActionKey = { keyCode ->
                            Log.d("AdvancedTerminal", "LOG: Action key received: $keyCode")
                            when (keyCode) {
                                android.view.KeyEvent.KEYCODE_ENTER -> viewModel.sendInput("\n")
                                android.view.KeyEvent.KEYCODE_DEL -> viewModel.sendRaw(byteArrayOf(0x7f))
                                android.view.KeyEvent.KEYCODE_FORWARD_DEL -> viewModel.sendInput("\u001b[3~")
                                android.view.KeyEvent.KEYCODE_TAB -> viewModel.sendInput("\t")
                                android.view.KeyEvent.KEYCODE_DPAD_UP -> viewModel.sendInput("\u001b[A")
                                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> viewModel.sendInput("\u001b[B")
                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> viewModel.sendInput("\u001b[D")
                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> viewModel.sendInput("\u001b[C")
                                android.view.KeyEvent.KEYCODE_MOVE_HOME -> viewModel.sendInput("\u001b[H")
                                android.view.KeyEvent.KEYCODE_MOVE_END -> viewModel.sendInput("\u001b[F")
                                android.view.KeyEvent.KEYCODE_PAGE_UP -> viewModel.sendInput("\u001b[5~")
                                android.view.KeyEvent.KEYCODE_PAGE_DOWN -> viewModel.sendInput("\u001b[6~")
                                android.view.KeyEvent.KEYCODE_ESCAPE -> viewModel.sendInput("\u001b")
                            }
                        }
                        setOnFocusChangeListener { _, hasFocus ->
                            Log.d("AdvancedTerminal", "LOG: Terminal input focus: $hasFocus")
                        }
                        terminalInputView = this
                    }
                },
                update = {
                    // Focus is handled by LaunchedEffect and click listener
                },
                modifier = Modifier.size(1.dp).alpha(0.01f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                lines.forEachIndexed { index, line ->
                    TerminalLine(
                        line = line,
                        isCursorLine = index == cursorRow,
                        cursorCol = cursorCol
                    )
                }
            }
        }

        // Control Panel
        TerminalControlPanel(
            onKeyPress = { key ->
                viewModel.sendSpecialKey(key)
            },
            onHistoryNavigate = { up ->
                // History navigate via special keys or handled in ViewModel
            },
            isCtrlPressed = viewModel.isCtrlPressed,
            isAltPressed = viewModel.isAltPressed,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        )
    }
}

@Composable
fun TerminalLine(line: AnnotatedString, isCursorLine: Boolean, cursorCol: Int) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box {
        BasicText(
            text = line,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE5E5E5),
                fontSize = 12.sp,
                lineHeight = 14.sp
            ),
            onTextLayout = { textLayoutResult = it }
        )
        if (isCursorLine) {
            val cursorOffset = if (textLayoutResult != null) {
                if (cursorCol in 0 until line.length) {
                    textLayoutResult?.getHorizontalPosition(cursorCol, true) ?: 0f
                } else {
                    textLayoutResult?.getLineRight(0) ?: 0f
                }
            } else 0f

            val density = LocalDensity.current
            BlinkingCursor(
                offset = with(density) { cursorOffset.toDp() },
                height = with(density) { 14.sp.toDp() }
            )
        }
    }
}

@Composable
fun BlinkingCursor(offset: Dp, height: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        Modifier
            .padding(start = offset)
            .width(8.dp)
            .height(height)
            .background(Color.White.copy(alpha = alpha))
    )
}
