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
import frb.axeron.adb.PreferenceAdbKeyStore
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronInfo
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Engine
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

    fun axeronObserve(): Flow<AxeronInfo> = callbackFlow {
        if (Axeron.pingBinder()) {
            trySend(Axeron.getAxeronInfo())
        }
        val receivedListener = Axeron.OnBinderReceivedListener {
            Log.i("AxManagerBinder", "onBinderReceived")
            trySend(Axeron.getAxeronInfo())
        }
        val deadListener = Axeron.OnBinderDeadListener {
            Log.i("AxManagerBinder", "onBinderDead")
            trySend(AxeronInfo())
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
            axeronObserve().collect {
                axeronInfo = it
                if (axeronInfo.isNeedUpdate()) {
                    tryActivate = true
                    Axeron.newProcess(
                        QuickShellViewModel.getQuickCmd(Starter.internalCommand),
                        null,
                        null
                    )
                    return@collect
                }
                tryActivate = false
            }
        }

        viewModelScope.launch {
            shizukuObserve().collect {
                isShizukuActive = it
            }
        }
    }

    fun startRoot(): Boolean = runBlocking(Dispatchers.IO) {
        if (tryActivate) return@runBlocking true
        tryActivate = true

        var result = false

        if (!Shell.getShell().isRoot) {
            Shell.getCachedShell()?.close()
            tryActivate = false
            return@runBlocking result
        }


        Shell.cmd(Starter.internalCommand).submit {
            if (it.isSuccess) {
                AxeronSettings.setLastLaunchMode(AxeronSettings.LaunchMethod.ROOT)
                result = true
            }
        }

        tryActivate = false
        return@runBlocking result
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun updateNotificationState(context: Context) {
        viewModelScope.launch {
            isNotificationEnabled = checkNotificationEnabled(context)
        }
    }

    var adbMdns: AdbMdns? = null

    @RequiresApi(Build.VERSION_CODES.R)
    fun startAdb(context: Context, tryConnect: Boolean = false): Boolean =
        runBlocking(Dispatchers.IO) {
            if (tryActivate) return@runBlocking true
            tryActivate = true

            val cr = Engine.application.contentResolver
            if (Engine.application.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED &&
                AxeronSettings.getLastLaunchMode() == AxeronSettings.LaunchMethod.ADB
            ) {
                Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
            }

            val adbWifiEnabled = Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 1

            if (!adbWifiEnabled && !tryConnect) {
                tryActivate = false
                setLaunchDevSettings(true)
                startPairingService(context)
                return@runBlocking false
            }

            AdbMdns(
                context,
                AdbMdns.TLS_CONNECT
            ) { data ->
                Log.d(TAG, "AdbMdns ${data.host} ${data.port}")
                AdbClient(
                    data.host,
                    data.port,
                    AdbKey(PreferenceAdbKeyStore(AxeronSettings.getPreferences()), "axeron")
                ).runCatching {
                    Log.d(TAG, "AdbClient running")
                    Log.d(TAG, Starter.internalCommand)
                    connect()
                    shellCommand(Starter.internalCommand, null)
                    close()
                }.onSuccess {
                    Log.d(TAG, "AdbClient success")
                    AxeronSettings.setLastLaunchMode(AxeronSettings.LaunchMethod.ADB)
                }.onFailure {
                    Log.e(TAG, "AdbClient failed", it)
                    if (!tryConnect) {
                        setLaunchDevSettings(true)
                        startPairingService(context)
                        adbMdns?.run {
                            this.stop()
                        }
                    }
                }

                tryActivate = false
            }.runCatching {
                Log.d(TAG, "AdbMdns running")
                adbMdns = this
                adbMdns?.run {
                    this.start()
                }
            }.onFailure {
                Log.e(TAG, "AdbMdns failed", it)
                it.printStackTrace()
                if (!tryConnect) {
                    setLaunchDevSettings(true)
                    startPairingService(context)
                    adbMdns?.run {
                        this.stop()
                    }
                }
            }
            tryActivate = false
            return@runBlocking false
        }

    @RequiresApi(Build.VERSION_CODES.R)
    fun startPairingService(context: Context) = runBlocking(Dispatchers.IO) {
        if (!isNotificationEnabled) return@runBlocking
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
        val channel = nm.getNotificationChannel(AdbPairingService.notificationChannel)
        return nm.areNotificationsEnabled() &&
                (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
    }
}