package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import frb.axeron.manager.ui.util.TerminalPrefs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.api.Axeron
import frb.axeron.manager.ui.util.TerminalBuffer
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Runs one command for an output screen (plugin action, flash). The job and its output live
 * here, so they survive rotation and the command never runs twice for the same screen.
 */
class CommandRunViewModel(application: Application) : AndroidViewModel(application) {

    enum class Stream { STDOUT, STDERR, NOTICE }

    sealed interface State {
        data object Idle : State
        data object Running : State
        data class Finished(val exitCode: Int) : State {
            val success get() = exitCode == 0
        }
    }

    val terminal = TerminalBuffer<Stream>(
        spoolDir = File(application.cacheDir, "terminal"),
        unlimited = TerminalPrefs.unlimited
    )

    override fun onCleared() {
        super.onCleared()
        terminal.close()
    }

    var state by mutableStateOf<State>(State.Idle)
        private set

    private var startedAt = 0L
    private var finishedAt = 0L

    val isRunning get() = state == State.Running

    /** Run time in ms; live while running. */
    fun elapsedMillis(): Long = when (state) {
        State.Idle -> 0L
        State.Running -> SystemClock.elapsedRealtime() - startedAt
        is State.Finished -> finishedAt - startedAt
    }

    /** Starts [block] once; it returns the exit code. Later calls are ignored. */
    fun start(block: suspend (TerminalBuffer<Stream>) -> Int) {
        if (state != State.Idle) return
        state = State.Running
        startedAt = SystemClock.elapsedRealtime()

        viewModelScope.launch(Dispatchers.IO) {
            val code = try {
                block(terminal)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                terminal.appendLine(Stream.NOTICE, "! ${e.message ?: e.javaClass.simpleName}")
                -1
            }
            terminal.endStreams()
            withContext(Dispatchers.Main) {
                finishedAt = SystemClock.elapsedRealtime()
                state = State.Finished(code)
            }
        }
    }

    /** Writes the plain-text log to the log folder and returns its path. */
    suspend fun saveLog(prefix: String): String = withContext(Dispatchers.IO) {
        val date = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(Date())
        val baseDir = PathHelper.getPath(AxeronApiConstant.folder.PARENT_LOG)
        if (!baseDir.exists()) baseDir.mkdirs()

        val file = File(baseDir, "${prefix}_${date}.log")
        // Streamed line by line: unlimited output can be far bigger than the heap.
        Axeron.newFileService().getStreamSession(file.absolutePath, true, false).outputStream.use {
            terminal.writeTo(it)
        }
        file.absolutePath
    }
}
