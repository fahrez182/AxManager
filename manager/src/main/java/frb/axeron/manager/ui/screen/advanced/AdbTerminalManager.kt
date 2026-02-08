package frb.axeron.manager.ui.screen.advanced

import android.app.Application
import android.provider.Settings
import android.util.Log
import frb.axeron.adb.AdbClient
import frb.axeron.adb.AdbKey
import frb.axeron.adb.PreferenceAdbKeyStore
import frb.axeron.adb.util.AdbEnvironment
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdbTerminalManager(private val application: Application) {
    private var adbClient: AdbClient? = null
    private var adbJob: Job? = null
    private var isConnecting = false

    private val _adbStatus = MutableStateFlow("Disconnected")
    val adbStatus: StateFlow<String> = _adbStatus

    private val _shellOutput = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val shellOutput: SharedFlow<ByteArray> = _shellOutput

    fun connect(scope: CoroutineScope) {
        if (isConnecting) return
        isConnecting = true
        adbJob?.cancel()
        adbJob = scope.launch {
            Log.i("AdbTerminalManager", "LOG: Starting ADB connection")
            try {
                val port = withContext(Dispatchers.IO) { AdbEnvironment.getAdbTcpPort() }
                if (port <= 0) {
                    _adbStatus.value = "ADB Port not found"
                    isConnecting = false
                    return@launch
                }

                val keyStore = PreferenceAdbKeyStore(
                    AxeronSettings.getPreferences(),
                    Settings.Global.getString(application.contentResolver, Starter.KEY_PAIR)
                )
                val key = AdbKey(keyStore, "axeron")

                adbClient = AdbClient(key, port)

                // Status Collection
                launch {
                    adbClient?.connectionStatus?.collect { status ->
                        _adbStatus.value = status
                    }
                }

                // Output Collection
                launch {
                    adbClient?.shellOutput?.collect { data ->
                        _shellOutput.emit(data)
                    }
                }

                withContext(Dispatchers.IO) {
                    adbClient?.connect()
                    adbClient?.startShell()
                }
            } catch (t: Throwable) {
                Log.e("AdbTerminalManager", "LOG: ADB connect failed", t)
                _adbStatus.value = "Failed: ${t.message}"
            } finally {
                isConnecting = false
            }
        }
    }

    fun disconnect() {
        adbJob?.cancel()
        adbJob = null
        adbClient?.close()
        adbClient = null
        _adbStatus.value = "Disconnected"
    }

    fun sendShellCommand(command: String) {
        adbClient?.sendShellCommand(command)
    }

    fun sendShellRaw(data: ByteArray) {
        adbClient?.sendShellRaw(data)
    }
}
