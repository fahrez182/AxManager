package frb.axeron.manager.ui.viewmodel

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

class TerminalEmulator(val numRows: Int = 40, val numCols: Int = 100) {
    data class Cell(val char: Char = ' ', val fg: Int = -1, val bg: Int = -1, val bold: Boolean = false)

    private val buffer = Array(numRows) { Array(numCols) { Cell() } }
    var cursorRow by mutableStateOf(0)
    var cursorCol by mutableStateOf(0)

    private var currentFg = -1
    private var currentBg = -1
    private var currentBold = false

    private var state = State.NORMAL
    private val escapeBuffer = StringBuilder()

    enum class State { NORMAL, ESCAPE, CSI }

    @Synchronized
    fun append(data: ByteArray) {
        for (b in data) {
            val c = b.toInt().toChar()
            when (state) {
                State.NORMAL -> {
                    when (c) {
                        '\u001b' -> state = State.ESCAPE
                        '\n' -> newLine()
                        '\r' -> cursorCol = 0
                        '\b' -> if (cursorCol > 0) cursorCol--
                        '\t' -> repeat(8 - (cursorCol % 8)) { putChar(' ') }
                        else -> putChar(c)
                    }
                }
                State.ESCAPE -> {
                    if (c == '[') {
                        state = State.CSI
                        escapeBuffer.setLength(0)
                    } else {
                        state = State.NORMAL
                    }
                }
                State.CSI -> {
                    if (c in '0'..'9' || c == ';' || c == '?' || c == '\"' || c == '\'') {
                        escapeBuffer.append(c)
                    } else {
                        handleCSI(c, escapeBuffer.toString())
                        state = State.NORMAL
                    }
                }
            }
        }
        triggerUpdate()
    }

    private fun putChar(c: Char) {
        if (cursorRow in 0 until numRows && cursorCol in 0 until numCols) {
            buffer[cursorRow][cursorCol] = Cell(c, currentFg, currentBg, currentBold)
            cursorCol++
            if (cursorCol >= numCols) {
                newLine()
            }
        }
    }

    private fun newLine() {
        cursorCol = 0
        cursorRow++
        if (cursorRow >= numRows) {
            // Scroll up
            for (i in 0 until numRows - 1) {
                for (j in 0 until numCols) {
                    buffer[i][j] = buffer[i + 1][j]
                }
            }
            for (j in 0 until numCols) {
                buffer[numRows - 1][j] = Cell()
            }
            cursorRow = numRows - 1
        }
    }

    private fun handleCSI(cmd: Char, params: String) {
        val parts = params.split(';')
        val p1 = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val p2 = parts.getOrNull(1)?.toIntOrNull() ?: 1

        when (cmd) {
            'A' -> cursorRow = (cursorRow - p1).coerceAtLeast(0)
            'B' -> cursorRow = (cursorRow + p1).coerceAtMost(numRows - 1)
            'C' -> cursorCol = (cursorCol + p1).coerceAtMost(numCols - 1)
            'D' -> cursorCol = (cursorCol - p1).coerceAtLeast(0)
            'H', 'f' -> {
                cursorRow = (p1 - 1).coerceIn(0, numRows - 1)
                cursorCol = (p2 - 1).coerceIn(0, numCols - 1)
            }
            'J' -> {
                if (p1 == 2) {
                    for (i in 0 until numRows) {
                        for (j in 0 until numCols) {
                            buffer[i][j] = Cell()
                        }
                    }
                    cursorRow = 0
                    cursorCol = 0
                }
            }
            'K' -> {
                if (cursorRow in 0 until numRows) {
                    for (j in cursorCol until numCols) {
                        buffer[cursorRow][j] = Cell()
                    }
                }
            }
            'm' -> {
                parts.forEach { p ->
                    val code = p.toIntOrNull() ?: 0
                    when (code) {
                        0 -> { currentFg = -1; currentBg = -1; currentBold = false }
                        1 -> currentBold = true
                        in 30..37 -> currentFg = code - 30
                        in 40..47 -> currentBg = code - 40
                        39 -> currentFg = -1
                        49 -> currentBg = -1
                    }
                }
            }
        }
    }

    // For rendering
    private val _outputLines = mutableStateListOf<AnnotatedString>()
    val outputLines: List<AnnotatedString> get() = _outputLines

    init {
        repeat(numRows) { _outputLines.add(AnnotatedString("")) }
    }

    private fun triggerUpdate() {
        for (i in 0 until numRows) {
            _outputLines[i] = buildAnnotatedString {
                val lastCol = getLineLength(i)
                for (j in 0 until lastCol) {
                    val cell = buffer[i][j]
                    val start = length
                    append(cell.char)
                    if (cell.fg != -1 || cell.bg != -1 || cell.bold) {
                        addStyle(SpanStyle(
                            color = if (cell.fg != -1) getAnsiColor(cell.fg) else Color.Unspecified,
                            fontWeight = if (cell.bold) FontWeight.Bold else FontWeight.Normal
                        ), start, length)
                    }
                }
            }
        }
    }

    private fun getLineLength(row: Int): Int {
        for (j in numCols - 1 downTo 0) {
            if (buffer[row][j].char != ' ') return j + 1
        }
        return 0
    }

    private fun getAnsiColor(code: Int): Color {
        return when (code) {
            0 -> Color(0xFF000000) // Black
            1 -> Color(0xFFCD3131) // Red
            2 -> Color(0xFF0DBC79) // Green
            3 -> Color(0xFFE5E510) // Yellow
            4 -> Color(0xFF2472C8) // Blue
            5 -> Color(0xFFBC3FBC) // Magenta
            6 -> Color(0xFF11A8CD) // Cyan
            7 -> Color(0xFFE5E5E5) // White
            else -> Color.Unspecified
        }
    }

    fun clear() {
        for (i in 0 until numRows) {
            for (j in 0 until numCols) {
                buffer[i][j] = Cell()
            }
        }
        cursorRow = 0
        cursorCol = 0
        triggerUpdate()
    }
}
