package com.frb.axmanager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frb.engine.client.Axeron
import com.frb.engine.client.AxeronNewProcess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class QuickShellViewModel : ViewModel() {

    enum class OutputType() {
        TYPE_COMMAND,
        TYPE_START,
        TYPE_STDIN,
        TYPE_STDOUT,
        TYPE_STDERR,
        TYPE_THROW,
        TYPE_SPACE,
        TYPE_EXIT,
    }

    data class Output(val type: OutputType, val output: String)

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

    private var job: Job? = null
    private var debounceJob: Job? = null
    private var process: AxeronNewProcess? = null
    private var writer: BufferedWriter? = null

    private var savedCommand: TextFieldValue? = null

    private var pid: Int? = null

    fun setCommand(text: TextFieldValue) {
        commandText = text
    }

    fun clear() {
        //make a toggle state
        clear = !clear
    }

    fun stop(fromUser: Boolean = true) {
        Axeron.newProcess("kill -TERM -$pid")
        debounceJob?.cancel()
        debounceJob = null
        job?.cancel()
        job = null
        try {
            writer?.close()
            process?.destroy()
        } catch (_: Throwable) {
        }
        writer = null
        pid = null
        process = null
        isRunning = false
        execMode = "Commands"
        if (savedCommand != null) {
            commandText = savedCommand!!
            savedCommand = null
        }
        if (fromUser) append(OutputType.TYPE_THROW, "[stopped]")
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
                append(OutputType.TYPE_STDIN, "[input] $cmd")
            } catch (t: Throwable) {
                append(OutputType.TYPE_THROW, "[error write] ${t.message}")
            }
            return
        }

        // kalau belum jalan → buat shell baru
        launchSafely(cmd) {
            appendRaw(OutputType.TYPE_COMMAND, "[command] $cmd")
            val execCmd = arrayOf(
                "setsid",
                "sh",
                "-c",
                "export PARENT_PID=$$; echo \"\\r\$PARENT_PID\\r\"; exec -a \"QuickShell\" sh -c \"$0\"",
                cmd
            )

            process = Axeron.newProcess(
                execCmd,
                Axeron.getEnvironment(), null
            )

            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))

            val stdout = process!!.inputStream
            val stderr = process!!.errorStream

            // baca stdout
            launch {
                val buf = ByteArray(1024 * 4)
                val regex = Regex("\r(\\d+)\r")
                generateSequence { stdout.read(buf).takeIf { it > 0 } }
                    .forEach { len ->
                        val chunk = String(buf, 0, len)
                        val match = regex.find(chunk)
                        if (match != null) {
                            pid = match.groupValues[1].toInt()
                            appendRaw(OutputType.TYPE_START, "[start] pid=$pid")
                        } else {
                            appendRaw(OutputType.TYPE_STDOUT, chunk.trim())
                        }
                    }
            }

            // baca stderr
            launch {
                val buf = ByteArray(1024 * 4)

                generateSequence { stderr.read(buf).takeIf { it > 0 } }
                    .forEach { len ->
                        val chunk = String(buf, 0, len)
                        appendRaw(OutputType.TYPE_STDERR, chunk.trimEnd())
                    }
            }

            launch {
                val code = process?.waitFor() ?: -1
                append(OutputType.TYPE_EXIT, "[exit] code=$code")
                append(OutputType.TYPE_SPACE, "")
                stop(false)
            }
        }
    }

    private fun launchSafely(
        cmd: String,
        block: suspend CoroutineScope.() -> Unit
    ) {
        if (isRunning) return
        isRunning = true
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(100)
            savedCommand = TextFieldValue(text = cmd, selection = TextRange(cmd.length))
            commandText = TextFieldValue(text = "")
            execMode = "Inputs"
        }

        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (ce: CancellationException) { /* ignore */
                append(OutputType.TYPE_THROW, "[Cancelled]")
            } catch (t: Throwable) {
                append(OutputType.TYPE_THROW, "[throw] ${t.message}")
            }
        }
    }

    private fun append(type: OutputType, output: String) {
        viewModelScope.launch { appendRaw(type, output) }
    }

    private suspend fun appendRaw(type: OutputType, output: String) {
        _output.emit(Output(type, output)) // langsung emit chunk
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}


