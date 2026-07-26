package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
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

    // Di PrivilegeViewModel.kt
    val privilegeList by derivedStateOf {
        val currentSearch = search
        val allPrivileges = privileges.values

        if (currentSearch.isEmpty()) {
            allPrivileges.filter { it.isNotSystemOrSelf() }.toList()
        } else {
            allPrivileges.asSequence()
                .filter { it.isNotSystemOrSelf() }
                .filter { app ->
                    app.label.contains(currentSearch, true) ||
                            app.packageName.contains(currentSearch, true) ||
                            //Panggil hanya jika perlu
                            (currentSearch.any { it.code > 128 } &&
                                    HanziToPinyin.getInstance().toPinyinString(app.label)
                                        .contains(currentSearch, true))
                }
                .toList()
        }
            .also { isRefreshing = false }
    }

    // Helper Extension
    private fun AppsViewModel.AppInfo.isNotSystemOrSelf(): Boolean {
        val isSystem = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isSelf = uid == android.os.Process.myUid()
        return !isSystem && !isSelf
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
        if (isRefreshing && refresh) return

        viewModelScope.launch {
            isRefreshing = refresh
            val pm = application.packageManager

            val result = withContext(Dispatchers.IO) {
                val packages = getApplications()

                // Gunakan chunked atau parallel map untuk mempercepat loadLabel
                packages.associate { packageInfo ->
                    val appInfo = packageInfo.applicationInfo!!
                    val uid = appInfo.uid
                    val label = appInfo.loadLabel(pm).toString()

                    uid to AppsViewModel.AppInfo(
                        label = label,
                        packageInfo = packageInfo,
                        isAdded = granted(uid)
                    )
                }
            }

            privileges = result
            isRefreshing = false
        }
    }
}