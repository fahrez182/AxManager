package frb.axeron.manager.ui.viewmodel

import android.app.AppOpsManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import frb.axeron.adb.AdbPairingService
import frb.axeron.adb.util.AdbEnvironment
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronInfo
import frb.axeron.api.AxeronPluginService
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.manager.R
import frb.axeron.manager.adb.ActivationLog
import frb.axeron.manager.adb.AdbStarter
import frb.axeron.manager.adb.AdbStarter.stopTcp
import frb.axeron.manager.adb.AdbStateInfo
import frb.axeron.manager.ui.component.ActionLog
import frb.axeron.manager.ui.util.Stealth
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.withTimeoutOrNull

class ActivateViewModel : ViewModel() {

    companion object {
        const val TAG = "AdbViewModel"
        const val ACTIVATE_FAILED = -1
        const val ACTIVATE_PROCESS = 0
        const val ACTIVATE_SUCCESS = 1
        const val UPDATE_TIMEOUT = 15000L
    }

    var activateStatus by mutableStateOf<ActivateStatus>(
        if (Axeron.pingBinder() && !releaseIfRefused()) getStatus(Axeron.getAxeronInfo())
        else ActivateStatus.Disable
    )
    private set

    private var updateJob: Job? = null

    var axeronInfo by mutableStateOf(AxeronInfo())
        private set

    var wakeLockHeld by mutableStateOf(false)
        private set

    fun setWakeLock(enabled: Boolean) {
        AxeronSettings.setWakeLock(enabled)
        applyWakeLock(enabled)
    }

    var wakeLockUsable by mutableStateOf(true)
        private set

    private fun applyWakeLock(enabled: Boolean) {
        viewModelScope.launch {
            wakeLockHeld = withContext(Dispatchers.IO) {
                Axeron.setWakeLock(enabled)
                Axeron.isWakeLockHeld()
            }
            wakeLockUsable = !enabled || wakeLockHeld
        }
    }

    var isShizukuActive by mutableStateOf(
        Axeron.getShizukuService() != null
    )
        private set

    fun setShizukuIntercept(enable: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            isShizukuActive = enable
            Axeron.enableShizukuService(enable)
        }
    }

    fun checkShizukuIntercept() {
        viewModelScope.launch(Dispatchers.Main) {
            isShizukuActive = Axeron.pingBinder() && Axeron.getShizukuService() != null
        }
    }

    var isStopPluginsWithServer by mutableStateOf(false)
        private set

    private fun stopPluginsFlag(): String = File(
        PathHelper.getWorkingPath(Axeron.getAxeronInfo().isRoot(), AxeronApiConstant.folder.PARENT),
        AxeronApiConstant.folder.STOP_PLUGINS_WITH_SERVER
    ).absolutePath

    fun checkStopPluginsWithServer() {
        viewModelScope.launch {
            isStopPluginsWithServer = withContext(Dispatchers.IO) {
                runCatching { Axeron.newFileService()?.exists(stopPluginsFlag()) == true }
                    .getOrDefault(false)
            }
        }
    }

    private fun readStopPluginsFlag(): Boolean =
        runCatching { Axeron.newFileService()?.exists(stopPluginsFlag()) == true }.getOrDefault(false)

    suspend fun shutdownServer(log: ActionLog): Boolean = withContext(Dispatchers.IO) {
        if (readStopPluginsFlag() && !AxeronPluginService.stopAllPlugins(log::out, log::err)) {
            return@withContext false
        }
        log.setCancellable(false)
        log.out("- Stopping the server\n")
        Axeron.destroy()
        log.out("- Server stopped\n")
        true
    }

    suspend fun restartServer(log: ActionLog): Boolean = withContext(Dispatchers.IO) {
        if (readStopPluginsFlag() && !AxeronPluginService.stopAllPlugins(log::out, log::err)) {
            return@withContext false
        }

        log.setCancellable(false)
        val old = Axeron.getBinder()
        val received = CompletableDeferred<Unit>()
        val failed = CompletableDeferred<Unit>()
        val listener = Axeron.OnBinderReceivedListener {
            if (Axeron.getBinder() !== old) received.complete(Unit)
        }
        Axeron.addBinderReceivedListener(listener)
        Axeron.suppressAutoIgnite = true
        val up = try {
            Axeron.relaunchServer({ failed.complete(Unit) }) { line, error ->
                if (error) log.err(line) else log.out(line)
            }
            withTimeoutOrNull(UPDATE_TIMEOUT) {
                merge(flow { received.await(); emit(true) }, flow { failed.await(); emit(false) })
                    .first()
            } == true
        } finally {
            Axeron.removeBinderReceivedListener(listener)
        }
        if (!up) {
            Axeron.suppressAutoIgnite = false
            log.err(
                if (failed.isCompleted) "! The starter can't be run from here: start the server again\n"
                else "! No new server within ${UPDATE_TIMEOUT / 1000}s\n"
            )
            return@withContext false
        }
        log.out("- Server is up\n")

        log.setCancellable(true)
        AxeronPluginService.igniteSuspendService(onStdout = log::out, onStderr = log::err)
    }

    fun updateStopPluginsWithServer(enable: Boolean) {
        viewModelScope.launch {
            isStopPluginsWithServer = enable
            withContext(Dispatchers.IO) {
                runCatching {
                    val fs = Axeron.newFileService() ?: return@runCatching
                    val flag = stopPluginsFlag()
                    if (enable) fs.createNewFile(flag) else fs.delete(flag)
                }
            }
            checkStopPluginsWithServer()
        }
    }

    var isStealth by mutableStateOf(false)
        private set

    fun checkStealth(context: Context) {
        viewModelScope.launch {
            isStealth = withContext(Dispatchers.IO) { Stealth.isOn().also { Stealth.sync(context, it) } }
        }
    }

    fun updateStealth(context: Context, enable: Boolean) {
        viewModelScope.launch {
            isStealth = enable
            isStealth = withContext(Dispatchers.IO) { Stealth.set(context, enable) }
        }
    }

    var isNotificationEnabled by mutableStateOf(false)
        private set

    var devSettings by mutableStateOf(false)
        private set

    fun setLaunchDevSettings(launch: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            devSettings = launch
        }
    }

    var tryActivate by mutableStateOf(false)
        private set

    fun setTryToActivate(activate: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            tryActivate = activate
        }
    }

    fun resetStatus() {
        activateStatus = ActivateStatus.Disable
    }

    suspend fun awaitRunning(timeout: Long = 10000) {
        if (activateStatus is ActivateStatus.Running || activateStatus is ActivateStatus.NeedExtraStep) return
        withTimeoutOrNull(timeout) {
            snapshotFlow { activateStatus }.first {
                it is ActivateStatus.Running || it is ActivateStatus.NeedExtraStep
            }
        }
    }

    sealed class ActivateStatus {
        object Disable : ActivateStatus()
        class NeedExtraStep(val axeronInfo: AxeronInfo) : ActivateStatus()
        class Updating(val axeronInfo: AxeronInfo) : ActivateStatus()
        class Running(val axeronInfo: AxeronInfo) : ActivateStatus()
    }

    private fun getStatus(axeronInfo: AxeronInfo): ActivateStatus {
        val isNeedExtraStep = axeronInfo.isNeedExtraStep() || (axeronInfo.isRunning() && Axeron.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") != PackageManager.PERMISSION_GRANTED)
        return when {
            axeronInfo.isNeedUpdate() -> ActivateStatus.Updating(axeronInfo)
            isNeedExtraStep -> ActivateStatus.NeedExtraStep(axeronInfo)
            axeronInfo.isRunning() -> ActivateStatus.Running(axeronInfo)
            else -> ActivateStatus.Disable
        }
    }

    private fun releaseIfRefused(): Boolean {
        if (!Axeron.isRefusedByServer()) return false
        Log.w("AxManagerBinder", "server refuses the manager, taking it as stopped")
        Axeron.destroy()
        return true
    }

    fun axeronObserve(): Flow<ActivateStatus> = callbackFlow {
        if (Axeron.pingBinder() && releaseIfRefused()) {
            trySend(ActivateStatus.Disable)
        } else if (Axeron.pingBinder()) {
            val axeronInfo = Axeron.getAxeronInfo()
            val status = getStatus(axeronInfo)
            trySend(status)
        }
        val receivedListener = Axeron.OnBinderReceivedListener {
            if (releaseIfRefused()) return@OnBinderReceivedListener
            val axeronInfo = Axeron.getAxeronInfo()
            val status = getStatus(axeronInfo)
            trySend(status)
        }
        val deadListener = Axeron.OnBinderDeadListener {
            trySend(ActivateStatus.Disable)
        }
        Axeron.addBinderReceivedListener(receivedListener)
        Axeron.addBinderDeadListener(deadListener)
        awaitClose {
            Axeron.removeBinderReceivedListener(receivedListener)
            Axeron.removeBinderDeadListener(deadListener)
        }
    }

    init {
        viewModelScope.launch {
            axeronObserve().collect { status ->
                val isStillUpdating = status is ActivateStatus.Disable &&
                        activateStatus is ActivateStatus.Updating && updateJob?.isActive == true
                axeronInfo = when (status) {
                    is ActivateStatus.Running -> {
                        checkShizukuIntercept()
                        Axeron.setPreferSystemBinaries(AxeronSettings.getPreferSystemBinaries())
                        applyWakeLock(AxeronSettings.getWakeLock())
                        status.axeronInfo
                    }
                    is ActivateStatus.Updating -> status.axeronInfo
                    is ActivateStatus.NeedExtraStep -> status.axeronInfo
                    else -> {
                        if (isStillUpdating) (activateStatus as ActivateStatus.Updating).axeronInfo
                        else AxeronInfo()
                    }
                }
                if (isStillUpdating) return@collect
                activateStatus = status
                if (status is ActivateStatus.Updating) startUpdate() else setTryToActivate(false)
            }
        }
    }

    private fun startUpdate() {
        if (updateJob?.isActive == true) return
        setTryToActivate(true)
        updateJob = viewModelScope.launch {
            val failed = CompletableDeferred<Unit>()
            Axeron.relaunchServer { failed.complete(Unit) }
            val updated = withTimeoutOrNull(UPDATE_TIMEOUT) {
                merge(
                    snapshotFlow { activateStatus }
                        .filter { it is ActivateStatus.Running || it is ActivateStatus.NeedExtraStep }
                        .map { true },
                    flow { failed.await(); emit(false) }
                ).first()
            } == true
            if (updated) return@launch
            activateStatus = ActivateStatus.Disable
            axeronInfo = AxeronInfo()
            setTryToActivate(false)
            if (!failed.isCompleted) withContext(Dispatchers.IO) {
                if (Axeron.pingBinder() && !Axeron.isUpdated()) Axeron.destroy()
            }
        }
    }

    suspend fun startRoot(): Int = startRootInternal(AxeronSettings.LaunchMethod.ROOT)
    suspend fun startRootAsShell(): Int = startRootInternal(AxeronSettings.LaunchMethod.ROOT_AS_SHELL)

    private suspend fun startRootInternal(mode: Int): Int = withContext(Dispatchers.IO) {
        runCatching {
            if (tryActivate) return@withContext ACTIVATE_PROCESS
            setTryToActivate(true)

            ActivationLog.info("Requesting root access…")
            if (!Shell.getShell().isRoot) {
                ActivationLog.error("Root access wasn't granted")
                Shell.getCachedShell()?.close()
                return@withContext ACTIVATE_FAILED
            }
            ActivationLog.success("Root access granted")

            val command = if (mode == AxeronSettings.LaunchMethod.ROOT_AS_SHELL) {
                "su 2000 -c \"${Starter.internalCommand}\""
            } else {
                Starter.internalCommand
            }

            val result = Shell.cmd(command).exec()
            result.out.forEach(ActivationLog::output)
            result.err.forEach(ActivationLog::output)
            if (result.isSuccess) {
                ActivationLog.success("Starter finished")
                AxeronSettings.setLastLaunchMode(mode)
                ACTIVATE_SUCCESS
            } else {
                ActivationLog.error("Starter exited with code ${result.code}")
                ACTIVATE_FAILED
            }
        }.getOrElse {
            ActivationLog.error("Root activation failed", it)
            ACTIVATE_FAILED
        }.also {
            Shell.getCachedShell()?.close()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun updateNotificationState(context: Context) {
        viewModelScope.launch {
            isNotificationEnabled = checkNotificationEnabled(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun startAdbWireless(context: Context): AdbStateInfo = withContext(Dispatchers.IO) {
        if (tryActivate) return@withContext AdbStateInfo.Process("Trying to activate")
        setTryToActivate(true)
        resetStatus()

        if (!AdbEnvironment.isWifiConnected(context)) {
            ActivationLog.warn("Wi-Fi is off — turn it on")
            requestEnableWifi(context)
            val connected = withTimeoutOrNull(WIFI_WAIT_MS) {
                while (!AdbEnvironment.isWifiConnected(context)) delay(500)
                true
            }
            if (connected != true) {
                ActivationLog.error("Wi-Fi wasn't connected")
                return@withContext AdbStateInfo.Failed("WiFi is required")
            }
        }
        val resultChannel = kotlinx.coroutines.channels.Channel<AdbStateInfo>(1)
        val job = launch {
            AdbStarter.startAdbWireless(context) { resultChannel.trySend(it) }
        }

        val result = withTimeoutOrNull(15000) {
            resultChannel.receive()
        } ?: AdbStateInfo.Failed("Timeout waiting for connection")

        job.cancel()
        result
    }

    suspend fun startAdbTcp(context: Context): AdbStateInfo = withContext(Dispatchers.IO) {
        if (tryActivate) return@withContext AdbStateInfo.Process("Trying to activate")
        setTryToActivate(true)
        try {
            resetStatus()
            val tcpPort = AdbEnvironment.getAdbTcpPort()
            val resultChannel = kotlinx.coroutines.channels.Channel<AdbStateInfo>(1)
            AdbStarter.startAdbClient(context, tcpPort) { resultChannel.trySend(it) }
            withTimeoutOrNull(15000) { resultChannel.receive() }
                ?: AdbStateInfo.Failed("Timeout waiting for connection")
        } finally {
            setTryToActivate(false)
        }
    }

    suspend fun stopAdbTcp(context: Context, result: (AdbStateInfo) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (tryActivate) return@withContext result(AdbStateInfo.Process("Trying to activate"))
        setTryToActivate(true)
        try {
            val tcpPort = AdbEnvironment.getAdbTcpPort()
            if (tcpPort > 0) stopTcp(context, tcpPort, result)
            else result(AdbStateInfo.Failed(context.getString(R.string.adb_tcp_not_active)))
        } finally {
            setTryToActivate(false)
        }
    }

    private val WIFI_WAIT_MS = 60_000L

    fun requestEnableWifi(context: Context) {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_WIFI
        else Settings.ACTION_WIFI_SETTINGS
        val intent = Intent(action).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun startPairingService(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isNotificationEnabled) return@launch
            setLaunchDevSettings(true)
            val intent = AdbPairingService.startIntent(context)
            try {
                context.startForegroundService(intent)
            } catch (e: Throwable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                    context.startService(intent)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkNotificationEnabled(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = nm.getNotificationChannel(AdbPairingService.NOTIFICATION_CHANNEL)
        return nm.areNotificationsEnabled() &&
                (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
    }
}
