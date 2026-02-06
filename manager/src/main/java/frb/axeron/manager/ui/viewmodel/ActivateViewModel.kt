package frb.axeron.manager.ui.viewmodel

import android.app.AppOpsManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import frb.axeron.manager.adb.AdbStarter
import frb.axeron.manager.adb.AdbStarter.stopTcp
import frb.axeron.manager.adb.AdbStateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ActivateViewModel : ViewModel() {

    companion object {
        const val TAG = "AdbViewModel"
        const val ACTIVATE_FAILED = -1
        const val ACTIVATE_PROCESS = 0
        const val ACTIVATE_SUCCESS = 1

    }

    var activateStatus by mutableStateOf<ActivateStatus>(run {
        if (Axeron.pingBinder() && Axeron.getAxeronInfo().isNeedUpdate()) {
            ActivateStatus.Updating(Axeron.getAxeronInfo())
        }
        ActivateStatus.Disable
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


    sealed class ActivateStatus {
        object Disable : ActivateStatus()
        object NeedExtraStep : ActivateStatus()
        class Updating(val axeronInfo: AxeronInfo) : ActivateStatus()
        class Running(val axeronInfo: AxeronInfo) : ActivateStatus()
    }

    fun axeronObserve(): Flow<ActivateStatus> = callbackFlow {
        if (Axeron.pingBinder()) {
            Log.i("AxManagerBinder", "binderHasReceived")
            val axeronInfo = Axeron.getAxeronInfo()
            when {
                axeronInfo.isNeedUpdate() -> {
                    trySend(ActivateStatus.Updating(axeronInfo))
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

                axeronInfo.isRunning() -> {
                    trySend(ActivateStatus.Running(axeronInfo))
                }

                axeronInfo.isNeedExtraStep() -> {
                    trySend(ActivateStatus.NeedExtraStep)
                }
            }
        }
        val receivedListener = Axeron.OnBinderReceivedListener {
            Log.i("AxManagerBinder", "onBinderReceived")
            val axeronInfo = Axeron.getAxeronInfo()
            when {
                axeronInfo.isRunning() -> {
                    trySend(ActivateStatus.Running(axeronInfo))
                }

                axeronInfo.isNeedExtraStep() -> {
                    trySend(ActivateStatus.NeedExtraStep)
                }
            }
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
                        status.axeronInfo
                    }

                    is ActivateStatus.Updating -> {
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

    fun startRoot(result: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (tryActivate) return@launch result(ACTIVATE_PROCESS)
                setTryToActivate(true)

                if (!Shell.getShell().isRoot) {
                    Shell.getCachedShell()?.close()
                    result(ACTIVATE_FAILED)
                    return@launch
                }

                Shell.cmd(Starter.internalCommand).submit {
                    if (it.isSuccess) {
                        AxeronSettings.setLastLaunchMode(AxeronSettings.LaunchMethod.ROOT)
                        result(ACTIVATE_SUCCESS)
                    } else {
                        result(ACTIVATE_FAILED)
                    }
                    Shell.getCachedShell()?.close()
                }
            }.onFailure {
                it.printStackTrace()
                result(ACTIVATE_FAILED)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun updateNotificationState(context: Context) {
        viewModelScope.launch {
            isNotificationEnabled = checkNotificationEnabled(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun startAdbWireless(
        context: Context, result: (AdbStateInfo) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (AdbEnvironment.isWifiRequired() && !isWifiEnabled(context)) return@launch requestEnableWifi(context)
            if (tryActivate) return@launch result(AdbStateInfo.Process("Trying to activate"))
            setTryToActivate(true)

            AdbStarter.startAdbWireless(context, result)
        }
    }

    fun startAdbTcp(
        context: Context, result: (AdbStateInfo) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (tryActivate) return@launch result(AdbStateInfo.Process("Trying to activate"))
            setTryToActivate(true)

            val tcpPort = AdbEnvironment.getAdbTcpPort()

            AdbStarter.startAdbClient(context, tcpPort, result)
        }
    }

    fun stopAdbTcp(
        context: Context, result: (AdbStateInfo) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (tryActivate) return@launch result(AdbStateInfo.Process("Trying to activate"))
            setTryToActivate(true)

            val tcpPort = AdbEnvironment.getAdbTcpPort()
            if (tcpPort > 0 && !AxeronSettings.getTcpMode()) {
                stopTcp(context, tcpPort)
            }
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
    fun startPairingService(context: Context) = runBlocking(Dispatchers.IO) {
        if (!isNotificationEnabled) return@runBlocking
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
                    Toast.makeText(
                        context,
                        "OP_START_FOREGROUND is denied. What are you doing?",
                        Toast.LENGTH_LONG
                    ).show()
                }
                context.startService(intent)
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