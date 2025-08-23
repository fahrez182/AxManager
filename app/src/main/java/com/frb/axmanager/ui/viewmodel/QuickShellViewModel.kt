package com.frb.axmanager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frb.engine.Axeron
import com.frb.engine.client.AxeronNewProcess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class QuickShellViewModel : ViewModel() {

    private val _output = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val output: SharedFlow<String> = _output

//    private val _isRunning = MutableStateFlow(false)
////    val isRunning: StateFlow<Boolean> = _isRunning

//    private val _commandText = MutableStateFlow("")
//    val commandText: StateFlow<String> = _commandText

    var isRunning by mutableStateOf(false)
        private set

    var commandText by mutableStateOf(TextFieldValue(""))
        private set

    var clear by mutableStateOf(false)
        private set

    var execMode by mutableStateOf("Commands")
        private set

    private var job: Job? = null
    private var debounceJob: Job? = null
    private var process: AxeronNewProcess? = null
    private var writer: BufferedWriter? = null

    private var savedCommand: TextFieldValue? = null

    fun setCommand(text: TextFieldValue) {
        commandText = text
    }

    fun clear() {
        //make a toggle state
        clear = !clear
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            writer?.close()
            process?.destroy()
        } catch (_: Throwable) {
        }
        writer = null
        process = null
        isRunning = false
        execMode = "Commands"
        if (savedCommand != null) {
            commandText = savedCommand!!
            savedCommand = null
        }
        appendLine("\n[stopped]")
    }

    fun runShell() {
        val cmd = commandText.text.ifBlank {
            return
        }

        // kalau sudah jalan → tulis command ke stdin
        if (isRunning && writer != null) {
            try {
                writer!!.write(cmd)
                writer!!.newLine()
                writer!!.flush()
                appendLine("[input] $cmd")
            } catch (t: Throwable) {
                appendLine("[error write] ${t.message}")
            }
            return
        }

        // kalau belum jalan → buat shell baru
        launchSafely(io = true, cmd) {
            process = Axeron.newProcess(arrayOf("sh", "-c", cmd), null, null)

            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))

            val stdout = process!!.inputStream
            val stderr = process!!.errorStream

            // baca stdout
            launch {
                val buf = ByteArray(4096)
                while (isActive) {
                    val len = stdout.read(buf)
                    if (len <= 0) break
                    val chunk = String(buf, 0, len) // ANSI escape preserved
                    appendRaw(chunk)
                }
            }

            // baca stderr
            launch {
                val buf = ByteArray(4096)
                while (isActive) {
                    val len = stderr.read(buf)
                    if (len <= 0) break
                    val chunk = String(buf, 0, len)
                    appendRaw(chunk)
                }
            }

            // tunggu proses selesai (misal user stop)
            withContext(Dispatchers.IO) {
                val code = process?.waitFor() ?: -1
                appendLine("[exit] code=$code")
            }
        }
    }

    private fun launchSafely(io: Boolean = false, cmd: String, block: suspend CoroutineScope.() -> Unit) {
        if (isRunning) return
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(80)
            isRunning = true
            savedCommand = TextFieldValue(text = cmd, selection = TextRange(cmd.length))
            commandText = TextFieldValue(text = "")
            execMode = "Inputs"
        }

        job = viewModelScope.launch(if (io) Dispatchers.IO else Dispatchers.Default) {
            try {
                block()
            } catch (ce: CancellationException) { /* ignore */
            } catch (t: Throwable) {
                appendLine("[throw] ${t.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    debounceJob?.cancel()
                    isRunning = false
                    execMode = "Commands"
                    if (savedCommand != null) {
                        commandText = savedCommand!!
                        savedCommand = null
                    }
                }
                try {
                    process?.destroy()
                } catch (_: Throwable) {
                }
                process = null
                writer = null
            }
        }
    }

    private fun appendLine(s: String) = appendRaw(s + "\n")

    private fun appendRaw(s: String) {
//        val spannable = AnsiParser.parseAsSpannable(s)
        _output.tryEmit(s) // langsung emit chunk
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
