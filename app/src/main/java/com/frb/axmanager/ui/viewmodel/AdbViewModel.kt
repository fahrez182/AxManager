package com.frb.axmanager.ui.viewmodel

import android.app.AppOpsManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frb.engine.AxeronSettings
import com.frb.engine.adb.AdbClient
import com.frb.engine.adb.AdbKey
import com.frb.engine.adb.AdbMdns
import com.frb.engine.adb.AdbPairingService
import com.frb.engine.adb.PreferenceAdbKeyStore
import com.frb.engine.utils.Starter
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.R)
class AdbViewModel : ViewModel() {

    companion object {
        const val STATUS_FAILED = -1
        const val STATUS_LOST = 0
        const val STATUS_RESOLVED = 1
    }

    var isNotificationEnabled by mutableStateOf(false)
        private set

    var isWirelessActive by mutableStateOf(false)
        private set

    var status by mutableIntStateOf(0)
        private set

    /**
     * Update state apakah notifikasi aktif atau tidak
     */
    fun updateNotificationState(context: Context) {
        viewModelScope.launch {
            isNotificationEnabled = checkNotificationEnabled(context)
        }
    }

    var adbMdns: AdbMdns? = null

    fun startAdb(context: Context) {
        viewModelScope.launch {
            val key = try {
                AdbKey(PreferenceAdbKeyStore(AxeronSettings.getPreferences()), "axeron")
            } catch (e: Throwable) {
                Log.e("AxManager", "startAdb", e)
                return@launch
            }

            adbMdns = AdbMdns(
                context,
                AdbMdns.TLS_CONNECT,
                observer = { data ->
                    isWirelessActive = true
                    status = STATUS_LOST
                    Log.d("AxManager", "startAdbTryConnect: ${data.host} ${data.port}")

                    runCatching {
                        AdbClient(data.host, data.port, key).use { client ->
                            client.connect()
                            client.shellCommand(Starter.internalCommand) { byteArr ->
                                Log.d("AxManager", "startAdbTryConnect: ${String(byteArr)}")
                            }
                            client.close()
                            status = STATUS_RESOLVED
                        }
                    }.onFailure {
                        status = STATUS_FAILED
                        Log.e("AxManager", "startAdbFailure", it)
                        startPairingService(context)
                    }

                    adbMdns?.stop()
                    adbMdns = null
                })
            adbMdns?.start()
        }
    }

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
    private fun checkNotificationEnabled(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = nm.getNotificationChannel(AdbPairingService.notificationChannel)
        return nm.areNotificationsEnabled() &&
                (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
    }
}