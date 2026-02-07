package frb.axeron.manager.ui.screen.advanced

import android.app.Application
import android.util.Log
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronNewProcess
import frb.axeron.server.Environment
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
            try {
                if (!Axeron.pingBinder()) {
                    _terminalStatus.value = "Axeron environment not ready"
                    return@launch
                }

                val currentEnv = Axeron.getEnvironment()
                val env = Environment.Builder(false)
                    .putAll(currentEnv.envMap)
                    .put("TERM", "xterm-256color")
                    .build()

                val proc = withContext(Dispatchers.IO) {
                    Axeron.newProcess(arrayOf("setsid", "sh", "-i"), env, null)
                }
                process = proc

                _terminalStatus.value = "Connected"

                // Output Collection
                launch(Dispatchers.IO) {
                    val stdoutJob = launch {
                        val inputStream = proc.inputStream
                        val buffer = ByteArray(8192)
                        try {
                            while (isActive) {
                                val read = withContext(Dispatchers.IO) { inputStream.read(buffer) }
                                if (read == -1) break
                                if (read > 0) {
                                    val data = buffer.copyOfRange(0, read)
                                    val filteredData = filterShellWarnings(data)
                                    if (filteredData.isNotEmpty()) {
                                        _shellOutput.emit(filteredData)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AxeronTerminalManager", "Stdout read error: ${e.message}")
                        }
                    }

                    val stderrJob = launch {
                        val errorStream = proc.errorStream
                        val buffer = ByteArray(8192)
                        try {
                            while (isActive) {
                                val read = withContext(Dispatchers.IO) { errorStream.read(buffer) }
                                if (read == -1) break
                                if (read > 0) {
                                    val data = buffer.copyOfRange(0, read)
                                    val filteredData = filterShellWarnings(data)
                                    if (filteredData.isNotEmpty()) {
                                        _shellOutput.emit(filteredData)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AxeronTerminalManager", "Stderr read error: ${e.message}")
                        }
                    }

                    // Wait for process to exit and cleanup
                    try {
                        withContext(Dispatchers.IO) {
                            proc.waitFor()
                        }
                    } catch (e: Exception) {
                        Log.e("AxeronTerminalManager", "Process wait error: ${e.message}")
                    } finally {
                        stdoutJob.cancel()
                        stderrJob.cancel()
                        _terminalStatus.value = "Disconnected"
                        Log.i("AxeronTerminalManager", "Terminal session ended")
                    }
                }

            } catch (t: Throwable) {
                Log.e("AxeronTerminalManager", "Error in terminal connection", t)
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
                }
            } catch (e: Exception) {
                Log.e("AxeronTerminalManager", "Write error: ${e.message}")
            }
        }
    }

    private fun filterShellWarnings(data: ByteArray): ByteArray {
        val text = String(data)
        if (text.contains("sh: can't find tty fd") || text.contains("sh: warning: won't have full job control")) {
            val filteredText = text.replace("sh: can't find tty fd: No such device or address\n", "")
                .replace("sh: warning: won't have full job control\n", "")
                .replace("sh: can't find tty fd: No such device or address", "")
                .replace("sh: warning: won't have full job control", "")
            return filteredText.toByteArray()
        }
        return data
    }
}
