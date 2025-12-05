package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronNewProcess
import frb.axeron.api.AxeronPluginService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class QuickShellViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    companion object {
        fun getQuickCmd(cmd: String, useBusybox: Boolean = true, withPid: Boolean = false): Array<String> {
            val execCmd = if (withPid) {
                "export PARENT_PID=$$; echo \"\\r\$PARENT_PID\\r\"; exec -a \"QuickShell\" sh -c \"$0\""
            } else {
                "exec -a \"QuickShell\" sh -c \"$0\""
            }
            return when {
                useBusybox -> arrayOf(
                    AxeronPluginService.BUSYBOX, "setsid", "sh", "-c",
                    execCmd,
                    cmd
                )

                else -> arrayOf(
                    "setsid", "sh", "-c",
                    execCmd,
                    cmd
                )
            }
        }
    }

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
        if (isShellRestrictionEnabled) Axeron.newProcess("kill -TERM -$pid")
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
        val cmd = commandText.text.ifBlank { return }
            .replace(Regex("[^\\p{Print}\\n]"), "") // sanitize

        // Jika shell sudah jalan → kirim input saja
        if (isRunning && writer != null) {
            try {
                writer!!.write(cmd)
                writer!!.newLine()
                writer!!.flush()

                val tagInput = if (cmd.lines().size > 1) "[input]\n" else "[input] "
                append(OutputType.TYPE_STDIN, tagInput + cmd.trim())
            } catch (t: Throwable) {
                append(OutputType.TYPE_THROW, "[error write] ${t.message}")
            }
            return
        }

        // Shell belum jalan → buat baru
        launchSafely(cmd) {

            // tampilkan command
            if (cmd.lines().size > 1) {
                appendRaw(OutputType.TYPE_COMMAND, "[command]")
                appendRaw(OutputType.TYPE_COMMAND, cmd.trim())
            } else appendRaw(OutputType.TYPE_COMMAND, "[command] ${cmd.trim()}")

            // start process
            process = Axeron.newProcess(
                getQuickCmd(
                    cmd = cmd,
                    useBusybox = isCompatModeEnabled,
                    withPid = true
                ),
                Axeron.getEnvironment(),
                null
            )

            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))

            val stdout = process!!.inputStream
            val stderr = process!!.errorStream

            // regex pid
            val pidRegex = Regex("\r(\\d+)\r")

            // baca stdout
            launch {
                val buf = ByteArray(4096)
                while (isRunning) {
                    val available = stdout.available()
                    if (available > 0) {
                        val len = stdout.read(buf, 0, minOf(buf.size, available))
                        if (len <= 0) continue

                        val text = String(buf, 0, len)

                        // Parse PID sekali saja
                        if (pid == null) {
                            pidRegex.find(text)?.let { m ->
                                pid = m.groupValues[1].toInt()
                                appendRaw(OutputType.TYPE_START, "[start] pid=$pid")
                                continue
                            }
                        }

                        appendRaw(OutputType.TYPE_STDOUT, text)
                    }
                }
            }

            // baca stderr
            launch {
                val buf = ByteArray(4096)
                generateSequence { stderr.read(buf).takeIf { it > 0 } }
                    .forEach { len ->
                        appendRaw(OutputType.TYPE_STDERR, String(buf, 0, len))
                    }
            }

            // tunggu exit
            launch {
                val code = process?.waitFor() ?: -1
                appendRaw(OutputType.TYPE_EXIT, "[exit] code=$code")
                appendRaw(OutputType.TYPE_SPACE, "")
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
            delay(200)
            savedCommand = TextFieldValue(text = cmd, selection = TextRange(cmd.length))
            commandText = TextFieldValue("") // clear input
            execMode = "Inputs"
        }

        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (ce: CancellationException) {
                append(OutputType.TYPE_THROW, "[Cancelled]")
            } catch (t: Throwable) {
                append(OutputType.TYPE_THROW, "[throw] ${t.message}")
            }
        }
    }


    private fun append(type: OutputType, output: String) = runBlocking(Dispatchers.IO) {
        appendRaw(type, output)
    }

    private var pendingOutput: Output? = null

    private suspend fun appendRaw(type: OutputType, output: String) {
        if (type == OutputType.TYPE_STDOUT || type == OutputType.TYPE_STDERR) {
            val endsWithNewline = output.endsWith("\n") || output.endsWith("\r")
            if (!endsWithNewline) {
                if (pendingOutput == null) {
                    pendingOutput = Output(type, output)
                } else {
                    pendingOutput!!.output += output.trimEnd()
                }
                return // belum emit karena belum newline
            } else {
                // ada newline → flush pending
                pendingOutput?.let {
                    it.output += output.trimEnd()
                    _output.emit(it)
                    pendingOutput = null
                    return
                }
            }
        }

        // tipe lain → langsung emit tanpa buffering
        _output.emit(Output(type, output.trimEnd()))
    }

//    private suspend fun appendRaw(type: OutputType, output: String) {
//        _output.emit(Output(type, output)) // langsung emit chunk
//    }

    override fun onCleared() {
        super.onCleared()
        if (Axeron.pingBinder()) stop()
    }
}


