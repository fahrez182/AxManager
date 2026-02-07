package frb.axeron.manager.ui.screen.advanced

import android.app.Application
import android.util.Log
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronNewProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AxeronTerminalManager(private val application: Application) {
    private var process: AxeronNewProcess? = null
    private var connectionJob: Job? = null
    private var isConnecting = false
    private var connectionScope: CoroutineScope? = null

    private val _terminalStatus = MutableStateFlow("Disconnected")
    val terminalStatus: StateFlow<String> = _terminalStatus

    private val _shellOutput = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val shellOutput: SharedFlow<ByteArray> = _shellOutput

    fun connect(scope: CoroutineScope) {
        if (isConnecting) return
        isConnecting = true
        connectionScope = scope
        connectionJob?.cancel()
        connectionJob = scope.launch {
            Log.i("AxeronTerminalManager", "LOG: Advanced Mode started")
            try {
                if (!Axeron.pingBinder()) {
                    Log.w("AxeronTerminalManager", "LOG: Axeron environment not detected")
                    _terminalStatus.value = "Axeron environment not ready"
                    return@launch
                }
                Log.i("AxeronTerminalManager", "LOG: Axeron environment detected")

                val proc = withContext(Dispatchers.IO) {
                    Axeron.newProcess(arrayOf("sh", "-i"))
                }
                process = proc
                Log.i("AxeronTerminalManager", "LOG: Axeron connected")
                Log.i("AxeronTerminalManager", "LOG: Terminal session started")

                _terminalStatus.value = "Connected"

                // Output Collection
                launch(Dispatchers.IO) {
                    Log.i("AxeronTerminalManager", "LOG: Output reader active")
                    val inputStream = proc.inputStream
                    val buffer = ByteArray(8192)
                    try {
                        while (isActive) {
                            val read = withContext(Dispatchers.IO) { inputStream.read(buffer) }
                            if (read == -1) break
                            if (read > 0) {
                                _shellOutput.emit(buffer.copyOfRange(0, read))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AxeronTerminalManager", "Read error: ${e.message}")
                    } finally {
                        _terminalStatus.value = "Disconnected"
                        Log.i("AxeronTerminalManager", "Output collection ended")
                    }
                }

            } catch (t: Throwable) {
                Log.e("AxeronTerminalManager", "LOG: Error handling if failure occurs", t)
                _terminalStatus.value = "Failed: ${t.message}"
            } finally {
                isConnecting = false
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        process?.destroy()
        process = null
        _terminalStatus.value = "Disconnected"
    }

    fun sendShellCommand(command: String) {
        sendShellRaw((command + "\n").toByteArray())
    }

    fun sendShellRaw(data: ByteArray) {
        connectionScope?.launch(Dispatchers.IO) {
            try {
                process?.outputStream?.let {
                    it.write(data)
                    it.flush()
                    Log.i("AxeronTerminalManager", "LOG: Data written to Axeron stdin")
                }
            } catch (e: Exception) {
                Log.e("AxeronTerminalManager", "Write error: ${e.message}")
            }
        }
    }
}
