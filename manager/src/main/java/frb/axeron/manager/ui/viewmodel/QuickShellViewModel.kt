package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
    private var savedCommand: TextFieldValue? = null

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
                savedCommand = TextFieldValue(text = command, selection = TextRange(command.length))
                commandText = TextFieldValue("") // clear input
                execMode = "Inputs"
                isRunning = true
            }

            override fun onProcessRunning(input: String) {
                Log.i("QuickShellViewModel", "onProcessRunning: $input")
                val tagInput = if (input.lines().size > 1) "[input]\n" else "[input] "
                append(OutputType.TYPE_STDIN, tagInput + input.trim())
                commandText = TextFieldValue("")
            }

            override fun onProcessFinished(exitCode: Int, lastOutput: String) {
                Log.i("QuickShellViewModel", "onProcessFinished: $exitCode")
                terminal.endStreams()
                if (!AnsiFilter.isScreenControl(lastOutput)) {
                    append(OutputType.TYPE_EXIT, "[exit] code=$exitCode")
                    append(OutputType.TYPE_SPACE, "")
                }
                execMode = "Commands"
                if (savedCommand != null) {
                    commandText = savedCommand!!
                    savedCommand = null
                }
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

    var commandText by mutableStateOf(TextFieldValue(""))
        private set

    var execMode by mutableStateOf("Commands")
        private set

    fun setCommand(text: TextFieldValue) {
        commandText = text
    }

    fun clear() {
        terminal.clear()
    }

    fun stop() {
        session.killSession()
    }

    fun runShell() {
        val cmd = commandText.text.ifBlank { return }
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


