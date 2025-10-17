package com.frb.axmanager.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frb.engine.client.Axeron
import com.frb.engine.client.AxeronNewProcess
import com.frb.engine.client.PluginService
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
        fun getQuickCmd(cmd: String, useBusybox: Boolean = true): Array<String> {
            val execCmd =
                "export PARENT_PID=$$; echo \"\\r\$PARENT_PID\\r\"; exec -a \"QuickShell\" sh -c \"$0\""
            return when {
                useBusybox -> arrayOf(
                    PluginService.BUSYBOX, "setsid", "sh", "-c",
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

    class PrefsHelper(
        private val keyPrefix: String
    ) {
        private val cache = mutableMapOf<String, Boolean>() // cache sementara

        private fun getPrefs(context: Context): SharedPreferences =
            context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        fun saveState(context: Context, type: OutputType, value: Boolean) {
            val key = "$keyPrefix${type.name}"
            cache[key] = value
            getPrefs(context).edit {
                putBoolean(key, value)
            }
        }

        fun loadState(context: Context, type: OutputType, default: Boolean = false): Boolean {
            val key = "$keyPrefix${type.name}"

            // kalau sudah pernah dimuat, ambil dari cache
            if (cache.containsKey(key)) return cache[key] ?: default

            // kalau belum, ambil dari SharedPreferences lalu simpan ke cache
            val value = getPrefs(context).getBoolean(key, default)
            cache[key] = value
            return value
        }

        fun clearAll(context: Context) {
            cache.clear()
            getPrefs(context).edit { clear() }
        }

        fun getAll(context: Context): Map<String, *> {
            // sync cache dan prefs
            val all = getPrefs(context).all
            all.forEach { (k, v) ->
                if (v is Boolean) cache[k] = v
            }
            return all
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

    data class Output(val type: OutputType, val output: String)

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
        val cmd = commandText.text.ifBlank {
            return
        }.replace(Regex("[^\\p{Print}\\n]"), "") //Sanitize

        // kalau sudah jalan → tulis command ke stdin
        if (isRunning && writer != null) {
            try {
                writer!!.write(cmd)
                writer!!.newLine()
                writer!!.flush()
                if (cmd.lines().size > 1) {
                    append(OutputType.TYPE_STDIN, "[input]")
                    append(OutputType.TYPE_STDIN, cmd.trim())
                } else {
                    append(OutputType.TYPE_STDIN, "[input] ${cmd.trim()}")
                }
            } catch (t: Throwable) {
                append(OutputType.TYPE_THROW, "[error write] ${t.message}")
            }
            return
        }

        // kalau belum jalan → buat shell baru
        launchSafely(cmd) {
            if (cmd.lines().size > 1) {
                appendRaw(OutputType.TYPE_COMMAND, "[command]")
                appendRaw(OutputType.TYPE_COMMAND, cmd.trim())
            } else {
                appendRaw(OutputType.TYPE_COMMAND, "[command] ${cmd.trim()}")
            }

            process = Axeron.newProcess(
                getQuickCmd(
                    cmd,
                    isCompatModeEnabled
                ),
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
                            appendRaw(OutputType.TYPE_STDOUT, chunk.trimEnd())
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
        runBlocking { appendRaw(type, output) }
    }

    private suspend fun appendRaw(type: OutputType, output: String) {
        _output.emit(Output(type, output)) // langsung emit chunk
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}


