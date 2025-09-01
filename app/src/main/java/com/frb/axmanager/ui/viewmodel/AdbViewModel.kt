package com.frb.axmanager.ui.viewmodel

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
import com.frb.engine.adb.AdbClient
import com.frb.engine.adb.AdbKey
import com.frb.engine.adb.AdbMdns
import com.frb.engine.adb.AdbPairingService
import com.frb.engine.adb.PreferenceAdbKeyStore
import com.frb.engine.client.Axeron
import com.frb.engine.client.AxeronFile
import com.frb.engine.core.AxeronSettings
import com.frb.engine.core.Engine
import com.frb.engine.implementation.AxeronService
import com.frb.engine.utils.Starter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AdbViewModel : ViewModel() {

    data class AxeronServiceInfo(
        val versionName: String? = null,
        val versionCode: Long = -1,
        val uid: Int = -1,
        val pid: Int = -1,
        val selinuxContext: String? = null
    ) {
        fun isRunning(): Boolean {
            return Axeron.pingBinder() && AxeronService.VERSION_CODE <= versionCode
        }

        fun isNeedUpdate(): Boolean {
            return AxeronService.VERSION_CODE > versionCode && Axeron.pingBinder()
        }

        fun getMode(): String {
            AxeronSettings.setLastLaunchMode(if (uid == 0) AxeronSettings.LaunchMethod.ROOT else AxeronSettings.LaunchMethod.ADB)
            return when (uid) {
                -1 -> "Not Activated"
                0 -> "Root"
                2000 -> "Shell"
                else -> "User"
            }
        }
    }

    val _axeronServiceInfo = MutableStateFlow(AxeronServiceInfo())
    val axeronServiceInfo = _axeronServiceInfo

    var isNotificationEnabled by mutableStateOf(false)
        private set

    var launchDevSettings by mutableStateOf(false)
        private set

    var tryActivate by mutableStateOf(false)
        private set

    var isUpdating by mutableStateOf(false)

    init {
        checkAxeronService()
    }

    fun checkAxeronService() {
        viewModelScope.launch {
            if (Axeron.pingBinder()) {
                tryActivate = false
                _axeronServiceInfo.value = AxeronServiceInfo(
                    Axeron.getVersionName(),
                    Axeron.getVersionCode(),
                    Axeron.getUid(),
                    Axeron.getPid(),
                    Axeron.getSELinuxContext()
                )
                AxeronFile().extractBusyBoxFromSo()
//                allowPermission()
            } else {
                if (isUpdating) return@launch
                _axeronServiceInfo.value = AxeronServiceInfo()
            }
        }
    }

//    fun allowPermission() {
//        viewModelScope.launch {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                AxeronAppOpsManager.setMode(
//                    BuildConfig.APPLICATION_ID,
//                    ConstantEngine.permission.ops.OP_MANAGE_EXTERNAL_STORAGE,
//                    AppOpsManager.MODE_ALLOWED
//                ) {
//                    Environment.isExternalStorageManager()
//                }.apply {
//                    Log.d("AxManager", "MANAGE_EXTERNAL_STORAGE: $this")
//                }
//            }
//        }
//    }

    /**
     * Update state apakah notifikasi aktif atau tidak
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun updateNotificationState(context: Context) {
        viewModelScope.launch {
            isNotificationEnabled = checkNotificationEnabled(context)
        }
    }

//    var adbMdns: AdbMdns? = null

    @RequiresApi(Build.VERSION_CODES.R)
    fun startAdb(context: Context, tryConnect: Boolean = false) {
        viewModelScope.launch {
            val cr = Engine.application.contentResolver
            if (Engine.application.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED &&
                AxeronSettings.getLastLaunchMode() == AxeronSettings.LaunchMethod.ADB) {

                tryActivate = true
                Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
            }

            val adbWifiEnabled = Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 1

            if (!adbWifiEnabled && !tryConnect) {
                startPairingService(context)
                launchDevSettings = !launchDevSettings
                return@launch
            }

            val latch = CountDownLatch(1)
            val adbMdns = AdbMdns(
                context,
                AdbMdns.TLS_CONNECT,
                observer = { data ->
                    Log.d("AxManager", "startAdbTryConnect: ${data.host} ${data.port}")

                    runCatching {
                        val keystore = PreferenceAdbKeyStore(AxeronSettings.getPreferences())
                        val key = AdbKey(keystore, "axeron")
                        val client = AdbClient(data.host, data.port, key)
                        client.connect()
                        client.shellCommand(Starter.internalCommand, null)
                        client.close()
                    }.onFailure {
                        if (!tryConnect) {
                            startPairingService(context)
                            launchDevSettings = !launchDevSettings
                        }
                    }

                    tryActivate = false
                    latch.countDown()
                })
            adbMdns.start()
            latch.await(1, TimeUnit.SECONDS)
            adbMdns.stop()
        }
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