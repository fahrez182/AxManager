package frb.axeron.manager.ui.viewmodel

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.AppOpsManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
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
import frb.axeron.adb.AdbClient
import frb.axeron.adb.AdbKey
import frb.axeron.adb.AdbMdns
import frb.axeron.adb.AdbPairingService
import frb.axeron.adb.AdbWifiGate
import frb.axeron.adb.PreferenceAdbKeyStore
import frb.axeron.adb.WifiReadyGate
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronCommandSession
import frb.axeron.api.AxeronInfo
import frb.axeron.api.core.AxeronSettings
import frb.axeron.server.utils.Starter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import rikka.shizuku.Shizuku

class ActivateViewModel : ViewModel() {

    companion object {
        const val TAG = "AdbViewModel"
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

    var isShizukuActive by mutableStateOf(false)
        private set

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

    fun shizukuObserve(): Flow<Boolean> = callbackFlow {
        if (Shizuku.pingBinder()) {
            Log.i("AxManagerBinder", "shizukuBinderHasReceived")
            trySend(true)
        }
        val shizukuReceived = Shizuku.OnBinderReceivedListener {
            Log.i("AxManagerBinder", "onShizukuBinderReceived")
            trySend(true)
        }

        val shizukuDead = Shizuku.OnBinderDeadListener {
            Log.i("AxManagerBinder", "onShizukuBinderDead")
            trySend(false)
        }
        Shizuku.addBinderReceivedListener(shizukuReceived)
        Shizuku.addBinderDeadListener(shizukuDead)

        awaitClose {
            Shizuku.removeBinderReceivedListener(shizukuReceived)
            Shizuku.removeBinderDeadListener(shizukuDead)
        }
    }

    init {
        viewModelScope.launch {
            axeronObserve().collect { status ->
                val isStillUpdating = status is ActivateStatus.Disable && activateStatus is ActivateStatus.Updating
                axeronInfo = when (status) {
                    is ActivateStatus.Running -> {
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

        viewModelScope.launch {
            shizukuObserve().collect {
                isShizukuActive = it
            }
        }
    }

//    init {
//        viewModelScope.launch {
//            axeronObserve().collect { status ->
//                setTryToActivate(false)
//                if (status is ActivateStatus.Running) {
//                    axeronInfo = status.axeronInfo
//                    when {
//                        axeronInfo.isNeedUpdate() -> {
//                            activateStatus = ActivateStatus.Updating
//                            setTryToActivate(true)
//                            Axeron.newProcess(
//                                AxeronCommandSession.getQuickCmd(
//                                    Starter.internalCommand,
//                                    true,
//                                    false
//                                ),
//                                null,
//                                null
//                            )
//                            return@collect
//                        }
//
//                        axeronInfo.isNeedExtraStep() -> {
//                            activateStatus = ActivateStatus.NeedExtraStep
//                            return@collect
//                        }
//                    }
//                    activateStatus = status
//                    return@collect
//                }
//                axeronInfo = AxeronInfo()
//                activateStatus = status
//            }
//        }
//
//        viewModelScope.launch {
//            shizukuObserve().collect {
//                isShizukuActive = it
//            }
//        }
//    }
    @Throws(ActivateException::class)
    fun startRoot() {
        if (tryActivate) return
        setTryToActivate(true)

        if (!Shell.getShell().isRoot) {
            Shell.getCachedShell()?.close()
            throw ActivateException.FailedToConnectToClient("Failed to connect to root")
        }


        Shell.cmd(Starter.internalCommand).submit {
            if (it.isSuccess) {
                AxeronSettings.setLastLaunchMode(AxeronSettings.LaunchMethod.ROOT)
            }
        }

        setTryToActivate(false)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun updateNotificationState(context: Context) {
        viewModelScope.launch {
            isNotificationEnabled = checkNotificationEnabled(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Throws(ActivateException::class)
    fun startAdb(context: Context) {
        if (tryActivate) throw ActivateException.TryToActivate("Already trying to activate")
        setTryToActivate(true)

        WifiReadyGate(context).await(ActivateException.FailedToConnectToWifi("Failed to connect to Wifi"))

        val cr = context.contentResolver

        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
            Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
        }

        AdbWifiGate(context).await(ActivateException.FailedToAutoActivateAdb("Failed to auto activate ADB"))

        AdbMdns(
            context,
            AdbMdns.TLS_CONNECT
        ) { data ->
            Log.d(TAG, "AdbMdns ${data.host} ${data.port}")
            if (data.port <= 0) throw ActivateException.FailedToGetHostAndPort("Failed to get Host and Port")

            AdbClient(
                data.host,
                data.port,
                AdbKey(
                    PreferenceAdbKeyStore(AxeronSettings.getPreferences()),
                    "axeron"
                )
            ).runCatching {
                Log.d(TAG, "AdbClient running")
                Log.d(TAG, Starter.internalCommand)
                connect()
                shellCommand(Starter.internalCommand, null)
                close()
            }.onSuccess {
                Log.d(TAG, "AdbClient success")
                context.startService(AdbPairingService.stopIntent(context))
                AxeronSettings.setLastLaunchMode(AxeronSettings.LaunchMethod.ADB)
                setTryToActivate(false)
            }.onFailure {
                throw ActivateException.FailedToConnectToClient("Failed to connect to ADB Client")
            }
        }.runCatching {
            Log.d(TAG, "AdbMdns running")
            start()
            setTryToActivate(false)
        }.onFailure {
            throw ActivateException.FailedToStartAdbMdns("Failed to start ADB mDNS (Multicast DNS)")
        }
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

sealed class ActivateException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class TryToActivate(message: String, cause: Throwable? = null) :
        ActivateException(message, cause)

    class FailedToAutoActivateAdb(message: String, cause: Throwable? = null) :
        ActivateException(message, cause)

    class FailedToConnectToWifi(message: String, cause: Throwable? = null) :
        ActivateException(message, cause)

    class FailedToStartAdbMdns(message: String, cause: Throwable? = null) :
        ActivateException(message, cause)

    class FailedToGetHostAndPort(message: String, cause: Throwable? = null) :
        ActivateException(message, cause)

    class FailedToConnectToClient(message: String, cause: Throwable? = null) :
        ActivateException(message, cause)

}