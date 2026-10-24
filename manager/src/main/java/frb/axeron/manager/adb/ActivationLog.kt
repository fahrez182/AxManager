package frb.axeron.manager.adb

import android.os.Build
import android.util.Log
import frb.axeron.manager.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Trace of the current activation attempt (wireless, TCP or root), shown live in the activation
 * output sheet and copyable as a bug report. Every line is mirrored to logcat.
 */
object ActivationLog {

    private const val TAG = "ActivationLog"
    private const val MAX_LINES = 500

    enum class Level { INFO, SUCCESS, WARN, ERROR, OUTPUT }

    data class Line(val time: Long, val level: Level, val text: String)

    private val _lines = MutableStateFlow<List<Line>>(emptyList())
    val lines: StateFlow<List<Line>> = _lines.asStateFlow()

    /** Starts a new attempt: clears the log and records what is being tried and on what. */
    fun begin(method: String) {
        _lines.value = emptyList()
        info("Method: $method")
        info("AxManager ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        info("${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
    }

    fun info(text: String) = add(Level.INFO, text)
    fun success(text: String) = add(Level.SUCCESS, text)
    fun warn(text: String) = add(Level.WARN, text)

    fun error(text: String, cause: Throwable? = null) {
        add(Level.ERROR, text)
        // The cause chain is usually what explains an ADB failure (refused, timeout, TLS…).
        var current = cause
        var depth = 0
        while (current != null && depth < 4) {
            add(Level.ERROR, "  ${current.javaClass.simpleName}: ${current.message.orEmpty()}")
            current = current.cause.takeIf { it !== current }
            depth++
        }
    }

    /** Raw process output (starter, su); split into lines, blanks dropped. */
    fun output(chunk: String) {
        chunk.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .forEach { add(Level.OUTPUT, it) }
    }

    val errorCount: Int get() = _lines.value.count { it.level == Level.ERROR }

    /** Plain-text copy for sharing, one timestamped line per entry. */
    /** [lines] defaults to this log; the OTG sheets pass their own. */
    fun asText(lines: List<Line> = _lines.value): String {
        val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        return lines.joinToString("\n") { line ->
            "${format.format(Date(line.time))} ${line.level.name.padEnd(7)} ${line.text}"
        }
    }

    private fun add(level: Level, text: String) {
        when (level) {
            Level.ERROR -> Log.e(TAG, text)
            Level.WARN -> Log.w(TAG, text)
            else -> Log.i(TAG, text)
        }
        _lines.update { (it + Line(System.currentTimeMillis(), level, text)).takeLast(MAX_LINES) }
    }
}
