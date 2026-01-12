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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class QuickShellViewModel(application: Application) : AndroidViewModel(application) {

    var session: AxeronCommandSession = AxeronCommandSession()
    private var savedCommand: TextFieldValue? = null

    init {
        session.setProcessListener(object : AxeronCommandSession.ProcessListener {
            override fun onProcessCreated(pid: Int, command: String) {
                Log.i("QuickShellViewModel", "onProcessCreated: $pid")
                if (command.lines().size > 1) {
                    append(OutputType.TYPE_COMMAND, "[command]")
                    append(OutputType.TYPE_COMMAND, command.trim())
                } else append(OutputType.TYPE_COMMAND, "[command] ${command.trim()}")
                append(OutputType.TYPE_START, "[start] pid=$pid")
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

            override fun onProcessFinished(exitCode: Int) {
                Log.i("QuickShellViewModel", "onProcessFinished: $exitCode")
                append(OutputType.TYPE_EXIT, "[exit] code=$exitCode")
                append(OutputType.TYPE_SPACE, "")
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
                output?.let {
                    append(OutputType.TYPE_STDOUT, it.toString())
                }
            }

            override fun onError(error: CharSequence?) {
                error?.let {
                    append(OutputType.TYPE_STDERR, it.toString())
                }
            }

        })
    }

    private val prefs = AxeronSettings.getPreferences()

    enum class OutputType {
        TYPE_COMMAND,
        TYPE_START,
        TYPE_STDIN,
        TYPE_STDOUT,
        TYPE_STDERR,
        TYPE_THROW,
        TYPE_SPACE,
        TYPE_EXIT
    }

    enum class KeyEventType {
        VOLUME_UP,
        VOLUME_DOWN
    }

    data class Output(val type: OutputType, var output: String)

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


    private val _output = MutableSharedFlow<Output>(extraBufferCapacity = 64)
    val output: SharedFlow<Output> = _output

    var isRunning by mutableStateOf(false)
        private set

    var commandText by mutableStateOf(TextFieldValue(""))
        private set

    var clear by mutableStateOf(false)
        private set

    var execMode by mutableStateOf("Commands")
        private set

    fun setCommand(text: TextFieldValue) {
        commandText = text
    }

    fun clear() {
        //make a toggle state
        clear = !clear
    }

    fun stop() {
        session.killSession()
    }

    fun runShell() {
        val cmd = commandText.text.ifBlank { return }
            .replace(Regex("[^\\p{Print}\\n]"), "") // sanitize

        session.runCommand(cmd, isCompatModeEnabled)
    }

    private fun append(type: OutputType, output: String) {
        viewModelScope.launch {
            _output.emit(Output(type, output.trimEnd()))
        }
    }

    private suspend fun appendRaw(type: OutputType, output: String) {
        _output.emit(Output(type, output.trimEnd()))
    }


    override fun onCleared() {
        super.onCleared()
        if (Axeron.pingBinder()) stop()
    }
}


