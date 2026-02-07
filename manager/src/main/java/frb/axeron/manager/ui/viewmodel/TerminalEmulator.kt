package frb.axeron.manager.ui.viewmodel

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

class TerminalEmulator(
    var numRows: Int = 40,
    var numCols: Int = 100,
    val maxScrollback: Int = 500
) {
    companion object {
        const val DEFAULT_FG = 7
        const val DEFAULT_BG = 0
        const val FLAG_BOLD = 1
        const val FLAG_UNDERLINE = 2
        const val FLAG_ITALIC = 4
    }

    class Cell(
        var char: Char = ' ',
        var fg: Int = DEFAULT_FG,
        var bg: Int = DEFAULT_BG,
        var flags: Int = 0
    ) {
        fun copyFrom(other: Cell) {
            this.char = other.char
            this.fg = other.fg
            this.bg = other.bg
            this.flags = other.flags
        }

        fun reset() {
            char = ' '
            fg = DEFAULT_FG
            bg = DEFAULT_BG
            flags = 0
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Cell) return false
            return char == other.char && fg == other.fg && bg == other.bg && flags == other.flags
        }

        override fun hashCode(): Int {
            var result = char.hashCode()
            result = 31 * result + fg
            result = 31 * result + bg
            result = 31 * result + flags
            return result
        }
    }

    private val scrollback = mutableListOf<Array<Cell>>()
    private var screen = Array(numRows) { Array(numCols) { Cell() } }

    var cursorRow by mutableIntStateOf(0)
    var cursorCol by mutableIntStateOf(0)

    private var currentFg = DEFAULT_FG
    private var currentBg = DEFAULT_BG
    private var currentFlags = 0

    private var state = State.NORMAL
    private val escapeBuffer = StringBuilder()

    var revision by mutableIntStateOf(0)
        private set

    enum class State { NORMAL, ESCAPE, CSI }

    // For rendering
    private val _outputLines = mutableStateListOf<AnnotatedString>()
    val outputLines: List<AnnotatedString> get() = _outputLines

    private val dirtyLines = mutableSetOf<Int>()

    init {
        updateOutputLines()
    }

    @Synchronized
    fun append(data: ByteArray) {
        for (b in data) {
            val c = b.toInt().toChar()
            processChar(c)
        }
        flushDirtyLines()
    }

    private fun processChar(c: Char) {
        when (state) {
            State.NORMAL -> {
                when (c) {
                    '\u001b' -> state = State.ESCAPE
                    '\n' -> newLine()
                    '\r' -> cursorCol = 0
                    '\b' -> if (cursorCol > 0) cursorCol--
                    '\t' -> repeat(8 - (cursorCol % 8)) { putChar(' ') }
                    '\u0007' -> { /* Bell - ignore */ }
                    else -> if (c.code >= 32) putChar(c)
                }
            }
            State.ESCAPE -> {
                if (c == '[') {
                    state = State.CSI
                    escapeBuffer.setLength(0)
                } else {
                    // Other escape sequences not supported yet
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

    private fun putChar(c: Char) {
        if (cursorCol >= numCols) {
            newLine()
        }
        if (cursorRow >= numRows) {
            newLine() // Should have been handled, but safety first
        }

        val cell = screen[cursorRow][cursorCol]
        cell.char = c
        cell.fg = currentFg
        cell.bg = currentBg
        cell.flags = currentFlags

        dirtyLines.add(cursorRow)
        cursorCol++
    }

    private fun newLine() {
        cursorCol = 0
        cursorRow++
        if (cursorRow >= numRows) {
            scrollDown()
            cursorRow = numRows - 1
        }
        dirtyLines.add(cursorRow)
    }

    private fun scrollDown() {
        // Add top line to scrollback
        val top = screen[0]
        scrollback.add(Array(numCols) { j -> Cell().apply { copyFrom(top[j]) } })
        if (scrollback.size > maxScrollback) {
            scrollback.removeAt(0)
        }

        // Shift screen up
        for (i in 0 until numRows - 1) {
            for (j in 0 until numCols) {
                screen[i][j].copyFrom(screen[i + 1][j])
            }
            dirtyLines.add(i)
        }

        // Clear last line
        for (j in 0 until numCols) {
            screen[numRows - 1][j].reset()
        }
        dirtyLines.add(numRows - 1)

        revision++ // Significant change
    }

    private fun handleCSI(cmd: Char, params: String) {
        val parts = params.split(';')
        val p = parts.map { it.toIntOrNull() ?: 0 }
        val p1 = p.getOrNull(0) ?: 1
        val p1Default0 = parts.getOrNull(0)?.toIntOrNull() ?: 0

        when (cmd) {
            'A' -> cursorRow = (cursorRow - p1.coerceAtLeast(1)).coerceAtLeast(0)
            'B' -> cursorRow = (cursorRow + p1.coerceAtLeast(1)).coerceAtMost(numRows - 1)
            'C' -> cursorCol = (cursorCol + p1.coerceAtLeast(1)).coerceAtMost(numCols - 1)
            'D' -> cursorCol = (cursorCol - p1.coerceAtLeast(1)).coerceAtLeast(0)
            'H', 'f' -> {
                val r = (p.getOrNull(0) ?: 1) - 1
                val c = (p.getOrNull(1) ?: 1) - 1
                cursorRow = r.coerceIn(0, numRows - 1)
                cursorCol = c.coerceIn(0, numCols - 1)
            }
            'J' -> {
                when (p1Default0) {
                    0 -> { // Clear from cursor to end of screen
                        clearInLine(cursorRow, cursorCol, numCols)
                        for (i in cursorRow + 1 until numRows) clearLine(i)
                    }
                    1 -> { // Clear from beginning of screen to cursor
                        for (i in 0 until cursorRow) clearLine(i)
                        clearInLine(cursorRow, 0, cursorCol + 1)
                    }
                    2, 3 -> { // Clear entire screen
                        for (i in 0 until numRows) clearLine(i)
                        cursorRow = 0
                        cursorCol = 0
                    }
                }
                revision++
            }
            'K' -> {
                when (p1Default0) {
                    0 -> clearInLine(cursorRow, cursorCol, numCols)
                    1 -> clearInLine(cursorRow, 0, cursorCol + 1)
                    2 -> clearLine(cursorRow)
                }
                dirtyLines.add(cursorRow)
            }
            'm' -> {
                if (parts.isEmpty() || (parts.size == 1 && parts[0] == "")) {
                    resetAttributes()
                } else {
                    var i = 0
                    while (i < p.size) {
                        val code = p[i]
                        when (code) {
                            0 -> resetAttributes()
                            1 -> currentFlags = currentFlags or FLAG_BOLD
                            3 -> currentFlags = currentFlags or FLAG_ITALIC
                            4 -> currentFlags = currentFlags or FLAG_UNDERLINE
                            22 -> currentFlags = currentFlags and FLAG_BOLD.inv()
                            23 -> currentFlags = currentFlags and FLAG_ITALIC.inv()
                            24 -> currentFlags = currentFlags and FLAG_UNDERLINE.inv()
                            in 30..37 -> currentFg = code - 30
                            38 -> { // Extended FG
                                if (p.getOrNull(i+1) == 5) {
                                    currentFg = p.getOrNull(i+2) ?: currentFg
                                    i += 2
                                } else if (p.getOrNull(i+1) == 2) {
                                    // Truecolor not supported yet, ignore
                                    i += 4
                                }
                            }
                            39 -> currentFg = DEFAULT_FG
                            in 40..47 -> currentBg = code - 40
                            48 -> { // Extended BG
                                if (p.getOrNull(i+1) == 5) {
                                    currentBg = p.getOrNull(i+2) ?: currentBg
                                    i += 2
                                } else if (p.getOrNull(i+1) == 2) {
                                    i += 4
                                }
                            }
                            49 -> currentBg = DEFAULT_BG
                            in 90..97 -> currentFg = code - 90 + 8
                            in 100..107 -> currentBg = code - 100 + 8
                        }
                        i++
                    }
                }
            }
        }
    }

    private fun resetAttributes() {
        currentFg = DEFAULT_FG
        currentBg = DEFAULT_BG
        currentFlags = 0
    }

    private fun clearLine(row: Int) {
        if (row in 0 until numRows) {
            for (j in 0 until numCols) screen[row][j].reset()
            dirtyLines.add(row)
        }
    }

    private fun clearInLine(row: Int, start: Int, end: Int) {
        if (row in 0 until numRows) {
            for (j in start.coerceAtLeast(0) until end.coerceAtMost(numCols)) {
                screen[row][j].reset()
            }
            dirtyLines.add(row)
        }
    }

    private fun flushDirtyLines() {
        if (dirtyLines.isEmpty()) return

        dirtyLines.forEach { row ->
            if (row in 0 until numRows) {
                updateOutputLine(row)
            }
        }
        dirtyLines.clear()
        revision++
    }

    private fun updateOutputLines() {
        _outputLines.clear()
        for (i in 0 until numRows) {
            _outputLines.add(buildAnnotatedStringForLine(screen[i]))
        }
    }

    private fun updateOutputLine(row: Int) {
        _outputLines[row] = buildAnnotatedStringForLine(screen[row])
    }

    private fun buildAnnotatedStringForLine(line: Array<Cell>): AnnotatedString {
        return buildAnnotatedString {
            val len = getActualLineLength(line)
            var i = 0
            while (i < len) {
                val cell = line[i]
                val start = length

                // Find run of same style
                var j = i
                while (j < len && line[j].fg == cell.fg && line[j].bg == cell.bg && line[j].flags == cell.flags) {
                    append(line[j].char)
                    j++
                }

                addStyle(
                    SpanStyle(
                        color = getAnsiColor(cell.fg),
                        background = if (cell.bg != DEFAULT_BG) getAnsiColor(cell.bg) else Color.Transparent,
                        fontWeight = if (cell.flags and FLAG_BOLD != 0) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (cell.flags and FLAG_UNDERLINE != 0) TextDecoration.Underline else TextDecoration.None
                    ),
                    start, length
                )
                i = j
            }
        }
    }

    private fun getActualLineLength(line: Array<Cell>): Int {
        for (j in numCols - 1 downTo 0) {
            if (line[j].char != ' ' || line[j].bg != DEFAULT_BG) return j + 1
        }
        return 0
    }

    private fun getAnsiColor(code: Int): Color {
        if (code in 0..15) {
            return when (code) {
                0 -> Color(0xFF000000)
                1 -> Color(0xFFCD3131)
                2 -> Color(0xFF0DBC79)
                3 -> Color(0xFFE5E510)
                4 -> Color(0xFF2472C8)
                5 -> Color(0xFFBC3FBC)
                6 -> Color(0xFF11A8CD)
                7 -> Color(0xFFE5E5E5)
                8 -> Color(0xFF666666)
                9 -> Color(0xFFF14C4C)
                10 -> Color(0xFF23D18B)
                11 -> Color(0xFFF5F543)
                12 -> Color(0xFF3B8EEA)
                13 -> Color(0xFFD670D6)
                14 -> Color(0xFF29B8DB)
                15 -> Color(0xFFFFFFFF)
                else -> Color.Unspecified
            }
        }
        if (code in 16..231) {
            // 6x6x6 color cube
            val r = ((code - 16) / 36) * 51
            val g = (((code - 16) / 6) % 6) * 51
            val b = ((code - 16) % 6) * 51
            return Color(r, g, b)
        }
        if (code in 232..255) {
            // Grayscale ramp
            val gray = (code - 232) * 10 + 8
            return Color(gray, gray, gray)
        }
        return Color.Unspecified
    }

    fun resize(rows: Int, cols: Int) {
        if (rows == numRows && cols == numCols) return

        val newScreen = Array(rows) { Array(cols) { Cell() } }
        for (i in 0 until rows.coerceAtMost(numRows)) {
            for (j in 0 until cols.coerceAtMost(numCols)) {
                newScreen[i][j].copyFrom(screen[i][j])
            }
        }

        screen = newScreen
        numRows = rows
        numCols = cols
        cursorRow = cursorRow.coerceIn(0, numRows - 1)
        cursorCol = cursorCol.coerceIn(0, numCols - 1)

        updateOutputLines()
        revision++
    }

    fun clear() {
        scrollback.clear()
        for (i in 0 until numRows) clearLine(i)
        cursorRow = 0
        cursorCol = 0
        resetAttributes()
        flushDirtyLines()
    }

    fun getFullBuffer(): List<AnnotatedString> {
        val result = mutableListOf<AnnotatedString>()
        scrollback.forEach { result.add(buildAnnotatedStringForLine(it)) }
        _outputLines.forEach { result.add(it) }
        return result
    }
}
