package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronCommandSession
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.utils.AnsiFilter
import frb.axeron.axerish.R
import frb.axeron.manager.ui.util.TerminalBuffer
import java.io.File

class QuickShellViewModel(application: Application) : AndroidViewModel(application) {

    var session: AxeronCommandSession = AxeronCommandSession()

    /**
     * The command being typed. A TextFieldState (not a TextFieldValue) so the input can show
     * its scroll position. Edit it on the main thread only: see [onMain].
     */
    val commandInput = TextFieldState()

    /** The command that started the running process, put back in the input when it ends. */
    private var savedCommand: String? = null

    /** Process callbacks come from session threads; the input may only change on main. */
    private fun onMain(block: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main.immediate) { block() }
    }

    // Lives here, not in the screen, so the log survives leaving and re-entering QuickShell.
    val terminal = TerminalBuffer<OutputType>(
        spoolDir = File(application.cacheDir, "terminal"),
        unlimited = true
    )

    init {
        session.setProcessListener(object : AxeronCommandSession.ProcessListener {
            override fun onProcessCreated(pid: Int, command: String) {
                Log.i("QuickShellViewModel", "onProcessCreated: $pid")
                if (command.lines().size > 1) {
                    append(OutputType.TYPE_COMMAND, "[command]")
                    append(OutputType.TYPE_COMMAND, command.trim())
                } else append(OutputType.TYPE_COMMAND, "[command] ${command.trim()}")
                append(OutputType.TYPE_START, "[start] pid=$pid")
                // `clear`/`top` redraws wipe this command's output only, not the session log.
                terminal.markScreenStart()
                savedCommand = command
                onMain { commandInput.clearText() } // now it takes the process's input
                execMode = "Inputs"
                isRunning = true
            }

            override fun onProcessRunning(input: String) {
                Log.i("QuickShellViewModel", "onProcessRunning: $input")
                val tagInput = if (input.lines().size > 1) "[input]\n" else "[input] "
                append(OutputType.TYPE_STDIN, tagInput + input.trim())
                onMain { commandInput.clearText() }
            }

            override fun onProcessFinished(exitCode: Int, lastOutput: String) {
                Log.i("QuickShellViewModel", "onProcessFinished: $exitCode")
                terminal.endStreams()
                if (!AnsiFilter.isScreenControl(lastOutput)) {
                    append(OutputType.TYPE_EXIT, "[exit] code=$exitCode")
                    append(OutputType.TYPE_SPACE, "")
                }
                execMode = "Commands"
                savedCommand?.let { saved -> onMain { commandInput.setTextAndPlaceCursorAtEnd(saved) } }
                savedCommand = null
                isRunning = false
            }
        })

        session.setResultListener(object : AxeronCommandSession.ResultListener {
            override fun output(output: CharSequence?) {
                output?.let { terminal.write(OutputType.TYPE_STDOUT, it.toString()) }
            }

            override fun onError(error: CharSequence?) {
                error?.let { terminal.write(OutputType.TYPE_STDERR, it.toString()) }
            }

        })
    }

    private val prefs = AxeronSettings.getPreferences()

    enum class OutputType(val labelId: Int) {
        TYPE_COMMAND(R.string.type_command),
        TYPE_START(R.string.type_start),
        TYPE_STDIN(R.string.type_stdin),
        TYPE_STDOUT(R.string.type_stdout),
        TYPE_STDERR(R.string.type_stderr),
        TYPE_THROW(R.string.type_throw),
        TYPE_SPACE(R.string.type_space),
        TYPE_EXIT(R.string.type_exit)
    }

    enum class KeyEventType(val labelId: Int) {
        VOLUME_UP(R.string.volume_up),
        VOLUME_DOWN(R.string.volume_down)
    }


    var isShellRestrictionEnabled: Boolean by mutableStateOf(
        prefs.getBoolean("shell_restriction", true)
    )
        private set

    fun setShellRestriction(enable: Boolean) {
        isShellRestrictionEnabled = enable
        prefs.edit {
            putBoolean("shell_restriction", enable)
        }
    }

    var isCompatModeEnabled: Boolean by mutableStateOf(
        prefs.getBoolean("shell_compat_mode", true)
    )
        private set

    fun setCompatMode(enable: Boolean) {
        isCompatModeEnabled = enable
        prefs.edit {
            putBoolean("shell_compat_mode", enable)
        }
    }


    var isRunning by mutableStateOf(false)
        private set

    var execMode by mutableStateOf("Commands")
        private set

    /** Replaces the whole input, cursor at the end. Main thread. */
    fun setCommand(text: String) = commandInput.setTextAndPlaceCursorAtEnd(text)

    /** Puts [text] in place of the selection (or at the cursor), cursor after it. Main thread. */
    fun insertCommand(text: String) = commandInput.edit {
        val start = selection.min
        replace(start, selection.max, text)
        selection = TextRange(start + text.length)
    }

    fun clear() {
        terminal.clear()
    }

    fun stop() {
        session.killSession()
    }

    fun runShell() {
        val cmd = commandInput.text.toString().ifBlank { return }
            .replace(Regex("(?i)adb\\s+shell\\s+"), "")
            .replace(Regex("[^\\p{Print}\\n]"), "") // sanitize

        session.runCommand(cmd, isCompatModeEnabled)
    }

    private fun append(type: OutputType, output: String) {
        terminal.appendLine(type, output)
    }

    override fun onCleared() {
        super.onCleared()
        if (Axeron.pingBinder()) stop()
        terminal.close()
    }
}


