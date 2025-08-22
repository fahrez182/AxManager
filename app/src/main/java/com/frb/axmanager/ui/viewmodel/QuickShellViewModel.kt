package com.frb.axmanager.ui.viewmodel

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class QuickShellViewModel : ViewModel() {

    private val _output = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val output: SharedFlow<String> = _output

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _commandText = MutableStateFlow("")
    val commandText: StateFlow<String> = _commandText

    private var job: Job? = null
    private var process: AxeronNewProcess? = null
    private var writer: BufferedWriter? = null

    fun setCommand(text: String) {
        _commandText.value = text
    }

    fun clear() {
        _output.tryEmit("__CLEAR__")
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
        _isRunning.value = false
        appendLine("\n[stopped]")
    }

    fun runDummy() {
        launchSafely {
            appendLine("[dummy] start")
            repeat(5) { i ->
                delay(400)
                appendLine("dummy line ${i + 1}")
            }
            appendLine("[dummy] done")
        }
    }

    fun runShell() {
        val cmd = commandText.value.ifBlank { "echo 'no command'" }

        // kalau sudah jalan → tulis command ke stdin
        if (_isRunning.value && writer != null) {
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
        launchSafely(io = true) {
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
                    appendRaw("[err] $chunk")
                }
            }

            // tunggu proses selesai (misal user stop)
            withContext(Dispatchers.IO) {
                val code = process?.waitFor() ?: -1
                appendLine("[exit] code=$code")
            }
        }
    }

    private fun launchSafely(io: Boolean = false, block: suspend CoroutineScope.() -> Unit) {
        if (_isRunning.value) return
        _isRunning.value = true
        job = viewModelScope.launch(if (io) Dispatchers.IO else Dispatchers.Default) {
            try {
                block()
            } catch (ce: CancellationException) { /* ignore */
            } catch (t: Throwable) {
                appendLine("[error] ${t.message}")
            } finally {
                withContext(Dispatchers.Main) { _isRunning.value = false }
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
