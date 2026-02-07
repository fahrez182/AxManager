package frb.axeron.manager.ui.screen.advanced

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AdvancedTerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val terminalManager = AxeronTerminalManager(application)

    val terminalStatus = terminalManager.terminalStatus
    val terminalEmulator = TerminalEmulator()

    var isCtrlPressed by mutableStateOf(false)
    var isAltPressed by mutableStateOf(false)

    init {
        viewModelScope.launch {
            terminalManager.shellOutput.collect { data ->
                terminalEmulator.append(data)
            }
        }
        terminalManager.connect(viewModelScope)
    }

    fun sendInput(text: String) {
        terminalManager.sendShellRaw(text.toByteArray())
    }

    fun sendRaw(data: ByteArray) {
        terminalManager.sendShellRaw(data)
    }

    fun sendSpecialKey(key: String) {
        try {
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
            terminalManager.sendShellRaw(data)
        } catch (e: Exception) {
            Log.e("AdvancedTerminalViewModel", "Failed to send special key", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        terminalManager.disconnect()
    }
}
