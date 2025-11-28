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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

const val TAG = "AdbViewModel"

class AdbViewModel : ViewModel() {

    var axeronInfo by mutableStateOf(AxeronInfo())
        private set

    var isNotificationEnabled by mutableStateOf(false)
        private set

    var launchDevSettings by mutableStateOf(false)

    var tryActivate by mutableStateOf(false)
        private set

    var isUpdating by mutableStateOf(false)

//    var running by mutableStateOf(
//        Axeron.pingBinder() && AxeronConstant.server.getActualVersion() <= axeronInfo.getActualVersion()
//    )
//        private set
//
//    var needUpdate by mutableStateOf(
//        AxeronConstant.server.getActualVersion() > axeronInfo.getActualVersion() && Axeron.pingBinder()
//    )
//        private set
//
//    var needExtraStep by mutableStateOf(
//        running && !axeronInfo.permission
//    )
//        private set

    init {
        checkAxeronService()
    }

    fun checkAxeronService() {
        viewModelScope.launch {
            if (Axeron.pingBinder()) {
                tryActivate = false
                axeronInfo = Axeron.getAxeronInfo()
            } else {
                if (isUpdating) return@launch
                axeronInfo = AxeronInfo()
            }
        }
    }

    /**
     * Update state apakah notifikasi aktif atau tidak
     */
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
                launchDevSettings = true
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
                        launchDevSettings = true
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
                    launchDevSettings = true
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
    fun startPairingService(context: Context) {
        viewModelScope.launch {
            if (!isNotificationEnabled) return@launch
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