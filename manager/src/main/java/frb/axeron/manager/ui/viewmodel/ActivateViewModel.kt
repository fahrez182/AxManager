package frb.axeron.manager.ui.viewmodel

import android.app.AppOpsManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
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
import frb.axeron.api.AxeronCommandSession
import frb.axeron.api.AxeronInfo
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.manager.R
import frb.axeron.manager.adb.ActivationLog
import frb.axeron.manager.adb.AdbStarter
import frb.axeron.manager.adb.AdbStarter.stopTcp
import frb.axeron.manager.adb.AdbStateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ActivateViewModel : ViewModel() {

    companion object {
        const val TAG = "AdbViewModel"
        const val ACTIVATE_FAILED = -1
        const val ACTIVATE_PROCESS = 0
        const val ACTIVATE_SUCCESS = 1

    }

    var activateStatus by mutableStateOf<ActivateStatus>(run {
        if (Axeron.pingBinder()) {
            val axeronInfo = Axeron.getAxeronInfo()
            val isNeedExtraStep = axeronInfo.isNeedExtraStep() || (axeronInfo.isRunning() && Axeron.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") != PackageManager.PERMISSION_GRANTED)
            if (axeronInfo.isNeedUpdate()) {
                ActivateStatus.Updating(axeronInfo)
            } else if (isNeedExtraStep) {
                ActivateStatus.NeedExtraStep(axeronInfo)
            } else if (axeronInfo.isRunning()) {
                ActivateStatus.Running(axeronInfo)
            } else {
                ActivateStatus.Disable
            }
        } else {
            ActivateStatus.Disable
        }
    })
    private set

    var axeronInfo by mutableStateOf(AxeronInfo())
        private set

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
            axeronInfo.isNeedUpdate() -> {
                ActivateStatus.Updating(axeronInfo)
            }

            isNeedExtraStep -> {
                ActivateStatus.NeedExtraStep(axeronInfo)
            }

            axeronInfo.isRunning() -> {
                ActivateStatus.Running(axeronInfo)
            }

            else -> ActivateStatus.Disable
        }
    }

    fun axeronObserve(): Flow<ActivateStatus> = callbackFlow {
        if (Axeron.pingBinder()) {
            val axeronInfo = Axeron.getAxeronInfo()
            val status = getStatus(axeronInfo)
            Log.i("AxManagerBinder", "Initial status determined: ${status::class.simpleName}")
            trySend(status)
            if (status is ActivateStatus.Updating) {
                Log.i("AxManagerBinder", "Server needs update, starting update process")
                setTryToActivate(true)
                Axeron.newProcess(
                    AxeronCommandSession.getQuickCmd(
                        Starter.internalCommand,
                        true,
                        false
                    ),
                    null,
                    null
                )
            }
        }
        val receivedListener = Axeron.OnBinderReceivedListener {
            Log.i("AxManagerBinder", "onBinderReceived listener triggered")
            val axeronInfo = Axeron.getAxeronInfo()
            val status = getStatus(axeronInfo)
            Log.i("AxManagerBinder", "New status after binder received: ${status::class.simpleName}")
            trySend(status)
        }
        val deadListener = Axeron.OnBinderDeadListener {
            Log.i("AxManagerBinder", "onBinderDead")
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
                val isStillUpdating =
                    status is ActivateStatus.Disable && activateStatus is ActivateStatus.Updating
                axeronInfo = when (status) {
                    is ActivateStatus.Running -> {
                        checkShizukuIntercept()
                        // Re-apply the PATH preference on every (re)connect.
                        Axeron.setPreferSystemBinaries(AxeronSettings.getPreferSystemBinaries())
                        status.axeronInfo
                    }

                    is ActivateStatus.Updating -> {
                        status.axeronInfo
                    }

                    is ActivateStatus.NeedExtraStep -> {
                        status.axeronInfo
                    }

                    else -> {
                        if (isStillUpdating) {
                            (activateStatus as ActivateStatus.Updating).axeronInfo
                        } else {
                            AxeronInfo()
                        }
                    }
                }
                if (isStillUpdating) return@collect
                Log.i("AxManagerBinder", "status: $status")
                activateStatus = status
                setTryToActivate(false)
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
                ActivationLog.error("Root access wasn't granted (no su, or the request was denied)")
                Shell.getCachedShell()?.close()
                return@withContext ACTIVATE_FAILED
            }
            ActivationLog.success("Root access granted")

            val command = if (mode == AxeronSettings.LaunchMethod.ROOT_AS_SHELL) {
                "su 2000 -c \"${Starter.internalCommand}\""
            } else {
                Starter.internalCommand
            }

            ActivationLog.info(
                if (mode == AxeronSettings.LaunchMethod.ROOT_AS_SHELL) "Running the starter as shell (uid 2000)…"
                else "Running the starter as root…"
            )
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
    suspend fun startAdbWireless(
        context: Context
    ): AdbStateInfo = withContext(Dispatchers.IO) {
        if (AdbEnvironment.isWifiRequired() && !isWifiEnabled(context)) {
            ActivationLog.error("Wi-Fi is off — wireless debugging needs Wi-Fi (opening Wi-Fi settings)")
            requestEnableWifi(context)
            return@withContext AdbStateInfo.Failed("WiFi is required")
        }
        if (tryActivate) return@withContext AdbStateInfo.Process("Trying to activate")
        setTryToActivate(true)
        resetStatus()

        val resultChannel = kotlinx.coroutines.channels.Channel<AdbStateInfo>(1)
        val job = launch {
            AdbStarter.startAdbWireless(context) {
                resultChannel.trySend(it)
            }
        }

        val result = withTimeoutOrNull(15000) {
            resultChannel.receive()
        } ?: AdbStateInfo.Failed("Timeout waiting for connection").also {
            ActivationLog.error("Timed out after 15 s waiting for wireless debugging")
        }

        job.cancel()
        result
    }

    suspend fun startAdbTcp(
        context: Context
    ): AdbStateInfo = withContext(Dispatchers.IO) {
        if (tryActivate) return@withContext AdbStateInfo.Process("Trying to activate")
        setTryToActivate(true)
        try {
            resetStatus()

            val tcpPort = AdbEnvironment.getAdbTcpPort()

            val resultChannel = kotlinx.coroutines.channels.Channel<AdbStateInfo>(1)
            AdbStarter.startAdbClient(context, tcpPort) {
                resultChannel.trySend(it)
            }
            withTimeoutOrNull(15000) { resultChannel.receive() }
                ?: AdbStateInfo.Failed("Timeout waiting for connection").also {
                    ActivationLog.error("Timed out after 15 s waiting for adbd on TCP")
                }
        } finally {
            setTryToActivate(false)
        }
    }

    suspend fun stopAdbTcp(
        context: Context, result: (AdbStateInfo) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        if (tryActivate) return@withContext result(AdbStateInfo.Process("Trying to activate"))
        setTryToActivate(true)
        try {
            // An explicit Stop always stops (TCP mode only decides what activation switches to),
            // and the outcome is always reported back.
            val tcpPort = AdbEnvironment.getAdbTcpPort()
            if (tcpPort > 0) {
                stopTcp(context, tcpPort, result)
            } else {
                result(AdbStateInfo.Failed(context.getString(R.string.adb_tcp_not_active)))
            }
        } finally {
            setTryToActivate(false)
        }
    }

    fun isWifiEnabled(context: Context): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    fun requestEnableWifi(context: Context) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
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
                Log.e("AxManager", "startForegroundService", e)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && e is ForegroundServiceStartNotAllowedException
                ) {
                    val mode = context.getSystemService(AppOpsManager::class.java)
                        .noteOpNoThrow(
                            "android:start_foreground",
                            android.os.Process.myUid(),
                            context.packageName,
                            null,
                            null
                        )
                    if (mode == AppOpsManager.MODE_ERRORED) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "OP_START_FOREGROUND is denied. What are you doing?",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    context.startService(intent)
                }
            }
        }
    }


    /**
     * Cek notifikasi aktif atau tidak
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkNotificationEnabled(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = nm.getNotificationChannel(AdbPairingService.NOTIFICATION_CHANNEL)
        return nm.areNotificationsEnabled() &&
                (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
    }
}