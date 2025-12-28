package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.os.Parcel
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.manager.ui.util.HanziToPinyin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.parcelablelist.ParcelableListSlice
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants
import rikka.shizuku.manager.ServerConstants

class PrivilegeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val FLAG_ALLOWED = 1 shl 1
        private const val FLAG_DENIED = 1 shl 2
        private const val MASK_PERMISSION = FLAG_ALLOWED or FLAG_DENIED
    }

    var isRefreshing: Boolean by mutableStateOf(false)
        private set

    var search by mutableStateOf("")

    val privilegeList by derivedStateOf {
        privileges.filter {
            it.label.contains(search, ignoreCase = true) ||
                    it.packageName.contains(search, ignoreCase = true) ||
                    HanziToPinyin.getInstance().toPinyinString(it.label)
                        .contains(search, ignoreCase = true)
        }.also {
            isRefreshing = false
        }
    }

    var privileges by mutableStateOf<List<AppsViewModel.AppInfo>>(ArrayList())
        private set

    var privilegedCount by mutableIntStateOf(0)
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
                Shizuku.getBinder()!!
                    .transact(ServerConstants.BINDER_TRANSACTION_getApplications, data, reply, 0)
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
        viewModelScope.launch {
            isRefreshing = true

            withContext(Dispatchers.IO) {
                val start = SystemClock.elapsedRealtime()
                val oldPrivileges = privileges
                val pm = getApplication<Application>().packageManager

                // Ambil packageName yang sudah tersimpan
                runCatching {

                    privileges = getApplications().map {
                        val appInfo = it.applicationInfo!!
                        Log.d(
                            "ShizukuViewModel",
                            "loadInstalledApps(${it.packageName}): ${granted(appInfo.uid)}"
                        )
                        AppsViewModel.AppInfo(
                            label = appInfo.loadLabel(pm).toString(),
                            packageInfo = it,
                            isAdded = granted(appInfo.uid)
                        )
                    }
                }.onFailure {
                    isRefreshing = false
                }

                privilegedCount = privileges.count { it.isAdded }

                SystemClock.elapsedRealtime() - start
                if (oldPrivileges === privileges) {
                    isRefreshing = false
                }
            }
        }
    }
}