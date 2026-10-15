package frb.axeron.manager.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import frb.axeron.api.core.AxeronSettings

/** View options shared by every command-output screen; persisted and observable. */
object TerminalPrefs {
    const val MIN_SCALE = 0.6f
    const val MAX_SCALE = 1.8f

    private const val KEY_LINE_NUMBERS = "terminal_line_numbers"
    private const val KEY_TEXT_SCALE = "terminal_text_scale"

    private val prefs get() = AxeronSettings.getPreferences()

    private var _lineNumbers by mutableStateOf(prefs.getBoolean(KEY_LINE_NUMBERS, true))
    private var _textScale by mutableFloatStateOf(
        prefs.getFloat(KEY_TEXT_SCALE, 1f).coerceIn(MIN_SCALE, MAX_SCALE)
    )

    var lineNumbers: Boolean
        get() = _lineNumbers
        set(value) {
            _lineNumbers = value
            prefs.edit { putBoolean(KEY_LINE_NUMBERS, value) }
        }

    var textScale: Float
        get() = _textScale
        set(value) {
            val v = value.coerceIn(MIN_SCALE, MAX_SCALE)
            _textScale = v
            prefs.edit { putFloat(KEY_TEXT_SCALE, v) }
        }
}
