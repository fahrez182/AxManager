package com.frb.axmanager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.os.Parcel
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frb.axmanager.ui.util.HanziToPinyin
import com.frb.axmanager.ui.viewmodel.AppsViewModel.AppInfo
import com.frb.engine.client.Axeron
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.parcelablelist.ParcelableListSlice
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants
import rikka.shizuku.ktx.workerHandler
import rikka.shizuku.manager.ServerConstants

class PermissionViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val FLAG_ALLOWED = 1 shl 1
        private const val FLAG_DENIED = 1 shl 2
        private const val MASK_PERMISSION = FLAG_ALLOWED or FLAG_DENIED
    }

    var search by mutableStateOf("")

    val needPermissionList by derivedStateOf {
        needPermissionApps.filter {
            it.label.contains(search, ignoreCase = true) ||
                    it.packageName.contains(search, ignoreCase = true) ||
                    HanziToPinyin.getInstance().toPinyinString(it.label)
                        .contains(search, ignoreCase = true)
        }
    }

    var isPermissionManagerEnabled by mutableStateOf(
        Axeron.pingBinder() && Axeron.getShizukuService() != null
    )
        private set

    fun attachListener() {
        Shizuku.addBinderReceivedListener(listenerReceived, workerHandler)
        Shizuku.addBinderDeadListener(listenerDead, workerHandler)
    }

    val listenerReceived = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            isPermissionManagerEnabled = true
            Shizuku.removeBinderReceivedListener(this)
        }
    }

    val listenerDead = object : Shizuku.OnBinderDeadListener {
        override fun onBinderDead() {
            isPermissionManagerEnabled = false
            Shizuku.removeBinderDeadListener(this)
        }
    }

    fun enablePermissionManager(enabled: Boolean) {
        attachListener()

        Axeron.enableShizukuService(enabled)
    }

    var needPermissionApps: List<AppInfo> by mutableStateOf(emptyList())
        private set

    fun granted(uid: Int): Boolean {
        return (Shizuku.getFlagsForUid(uid, MASK_PERMISSION) and FLAG_ALLOWED) == FLAG_ALLOWED
    }

    fun grant(uid: Int) {
        Shizuku.updateFlagsForUid(uid, MASK_PERMISSION, FLAG_ALLOWED)
    }

    fun revoke(uid: Int) {
        Shizuku.updateFlagsForUid(uid, MASK_PERMISSION, 0)
    }

    private fun getApplications(): List<PackageInfo> {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(ShizukuApiConstants.BINDER_DESCRIPTOR)
            data.writeInt(-1)
            try {
                Shizuku.getBinder()!!.transact(ServerConstants.BINDER_TRANSACTION_getApplications, data, reply, 0)
            } catch (e: Throwable) {
                throw RuntimeException(e)
            }
            reply.readException()
            @Suppress("UNCHECKED_CAST")
            (ParcelableListSlice.CREATOR.createFromParcel(reply) as ParcelableListSlice<PackageInfo>).list!!
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager

            // Ambil packageName yang sudah tersimpan
            val packages = getApplications()

            needPermissionApps = packages.map {
                val appInfo = it.applicationInfo!!
                Log.d("ShizukuViewModel", "loadInstalledApps: ${granted(appInfo.uid)}")
                AppInfo(
                    label = appInfo.loadLabel(pm).toString(),
                    packageInfo = it,
                    isAdded = granted(appInfo.uid)
                )
            }
        }
    }
}