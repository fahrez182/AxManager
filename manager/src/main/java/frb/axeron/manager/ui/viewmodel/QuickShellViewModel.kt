package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import android.provider.Settings
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.adb.AdbClient
import frb.axeron.adb.AdbKey
import frb.axeron.adb.PreferenceAdbKeyStore
import frb.axeron.adb.util.AdbEnvironment
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronCommandSession
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.api.utils.AnsiFilter
import frb.axeron.axerish.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

            override fun onProcessFinished(exitCode: Int, lastOutput: String) {
                Log.i("QuickShellViewModel", "onProcessFinished: $exitCode")
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

    data class Output(val type: OutputType, var output: String, var completed: Boolean = false)

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

    var isAdvancedMode by mutableStateOf(false)
        private set

    val commandHistory = mutableStateListOf<String>()
    var historyIndex by mutableIntStateOf(-1)
        private set

    var adbStatus by mutableStateOf("Disconnected")
        private set

    private var adbClient: AdbClient? = null

    val terminalEmulator = TerminalEmulator()
    var isCtrlPressed by mutableStateOf(false)
    var isAltPressed by mutableStateOf(false)

    fun toggleAdvancedMode() {
        isAdvancedMode = !isAdvancedMode
        if (isAdvancedMode) {
            connectAdb()
        } else {
            disconnectAdb()
        }
    }

    private fun connectAdb() {
        viewModelScope.launch(Dispatchers.IO) {
            adbStatus = "Connecting..."
            try {
                val port = AdbEnvironment.getAdbTcpPort()
                if (port <= 0) {
                    adbStatus = "ADB Port not found"
                    return@launch
                }

                val keyStore = PreferenceAdbKeyStore(
                    AxeronSettings.getPreferences(),
                    Settings.Global.getString(getApplication<Application>().contentResolver, Starter.KEY_PAIR)
                )
                val key = AdbKey(keyStore, "axeron")

                adbClient = AdbClient(key, port)
                adbClient?.onConnectionChanged = { status ->
                    adbStatus = status
                }
                adbClient?.connect()
                adbStatus = "Connected"

                adbClient?.startShell { data ->
                    viewModelScope.launch {
                        if (isAdvancedMode) {
                            terminalEmulator.append(data)
                        } else {
                            _output.emit(Output(OutputType.TYPE_STDOUT, String(data)))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("QuickShellViewModel", "ADB connect failed", e)
                adbStatus = "Failed: ${e.message}"
            }
        }
    }

    private fun disconnectAdb() {
        adbClient?.close()
        adbClient = null
        adbStatus = "Disconnected"
    }

    fun navigateHistory(up: Boolean) {
        if (commandHistory.isEmpty()) return
        if (up) {
            if (historyIndex < commandHistory.size - 1) {
                historyIndex++
                val cmd = commandHistory[commandHistory.size - 1 - historyIndex]
                commandText = TextFieldValue(cmd, selection = TextRange(cmd.length))
            }
        } else {
            if (historyIndex > 0) {
                historyIndex--
                val cmd = commandHistory[commandHistory.size - 1 - historyIndex]
                commandText = TextFieldValue(cmd, selection = TextRange(cmd.length))
            } else if (historyIndex == 0) {
                historyIndex = -1
                commandText = TextFieldValue("")
            }
        }
    }

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

        if (commandHistory.lastOrNull() != cmd) {
            commandHistory.add(cmd)
        }
        historyIndex = -1

        if (isAdvancedMode && adbStatus == "Connected") {
            append(OutputType.TYPE_COMMAND, "$ cmd")
            adbClient?.sendShellCommand(cmd)
            commandText = TextFieldValue("")
        } else {
            session.runCommand(cmd, isCompatModeEnabled)
        }
    }

    fun sendSpecialKey(key: String) {
        if (isAdvancedMode && adbStatus == "Connected") {
            if (key == "CTRL") {
                isCtrlPressed = !isCtrlPressed
                return
            }
            if (key == "ALT") {
                isAltPressed = !isAltPressed
                return
            }

            var data = key.toByteArray()
            if (isCtrlPressed) {
                if (key.length == 1) {
                    val c = key[0].uppercaseChar()
                    if (c in 'A'..'Z') {
                        data = byteArrayOf((c.code - 'A'.code + 1).toByte())
                    }
                }
                isCtrlPressed = false
            }
            if (isAltPressed) {
                val newData = ByteArray(data.size + 1)
                newData[0] = 0x1b
                System.arraycopy(data, 0, newData, 1, data.size)
                data = newData
                isAltPressed = false
            }
            adbClient?.sendShellRaw(data)
        }
    }

    private fun append(type: OutputType, output: String) {
        viewModelScope.launch {
            _output.emit(Output(type, output))
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (Axeron.pingBinder()) stop()
    }
}


