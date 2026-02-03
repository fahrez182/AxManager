package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.SystemClock
import android.system.Os
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import frb.axeron.api.Axeron
import frb.axeron.manager.ui.util.HanziToPinyin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        privileges.values
            .asSequence()
            .filter {
                it.label.contains(search, true) ||
                        it.packageName.contains(search, true) ||
                        HanziToPinyin.getInstance()
                            .toPinyinString(it.label)
                            .contains(search, true)
            }
            .filter {
                val isSystem =
                    it.packageInfo.applicationInfo!!
                        .flags.and(ApplicationInfo.FLAG_SYSTEM) != 0

                val isSelf = it.uid == Os.getuid()

                !isSystem && !isSelf
            }
            .toList()
            .also { isRefreshing = false }
    }


    var privileges by mutableStateOf<Map<Int, AppsViewModel.AppInfo>>(HashMap())
        private set

    val privilegedCount by derivedStateOf {
        privileges.values.count { it.isAdded }
    }

    fun granted(uid: Int): Boolean {
        return (Axeron.getFlagsForUid(uid, MASK_PERMISSION) and FLAG_ALLOWED) == FLAG_ALLOWED
    }

    fun grant(uid: Int) {
        Axeron.updateFlagsForUid(uid, MASK_PERMISSION, FLAG_ALLOWED)
        privileges = privileges.toMutableMap().apply {
            this[uid]?.let {
                this[uid] = it.copy(isAdded = true)
            }
        }
    }

    fun revoke(uid: Int) {
        Axeron.updateFlagsForUid(uid, MASK_PERMISSION, 0)
        privileges = privileges.toMutableMap().apply {
            this[uid]?.let {
                this[uid] = it.copy(isAdded = false)
            }
        }
    }


    private fun getApplications(): List<PackageInfo> {
        return application.packageManager.getInstalledPackages(0)
    }

    fun loadInstalledApps(refresh: Boolean = true) {
        viewModelScope.launch {
            isRefreshing = refresh

            withContext(Dispatchers.IO) {
                val start = SystemClock.elapsedRealtime()
                val oldPrivileges = privileges
                val pm = getApplication<Application>().packageManager

                // Ambil packageName yang sudah tersimpan
                runCatching {

                    privileges = getApplications().associate { packageInfo ->
                        val appInfo = packageInfo.applicationInfo!!
                        val uid = appInfo.uid
                        uid to AppsViewModel.AppInfo(
                            label = appInfo.loadLabel(pm).toString(),
                            packageInfo = packageInfo,
                            isAdded = granted(uid)
                        )
                    }
                }.onFailure {
                    isRefreshing = false
                }

                SystemClock.elapsedRealtime() - start
                if (oldPrivileges === privileges) {
                    isRefreshing = false
                }
            }
        }
    }
}