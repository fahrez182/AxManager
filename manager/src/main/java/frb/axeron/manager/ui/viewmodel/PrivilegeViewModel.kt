package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.edit
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import frb.axeron.api.Axeron
import frb.axeron.manager.ui.util.HanziToPinyin
import frb.axeron.server.ServerConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrivilegeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val FLAG_ALLOWED = 1 shl 1
        private const val FLAG_DENIED = 1 shl 2
        private const val MASK_PERMISSION = FLAG_ALLOWED or FLAG_DENIED
        private const val KEY_FILTER = "filter"
        private const val KEY_ASCENDING = "ascending"
    }

    var isRefreshing: Boolean by mutableStateOf(false)
        private set

    var search by mutableStateOf("")

    /** Which apps the privilege list shows. */
    enum class AppFilter { INSTALLED, SYSTEM, SHIZUKU }

    private val prefs = application.getSharedPreferences("privilege_prefs", Context.MODE_PRIVATE)

    var filter by mutableStateOf(
        AppFilter.entries.getOrElse(prefs.getInt(KEY_FILTER, 0)) { AppFilter.INSTALLED }
    )
        private set

    var ascending by mutableStateOf(prefs.getBoolean(KEY_ASCENDING, true))
        private set

    fun updateFilter(value: AppFilter) {
        filter = value
        prefs.edit { putInt(KEY_FILTER, value.ordinal) }
    }

    fun updateAscending(value: Boolean) {
        ascending = value
        prefs.edit { putBoolean(KEY_ASCENDING, value) }
    }

    fun resetFilter() {
        updateFilter(AppFilter.INSTALLED)
        updateAscending(true)
    }

    val isFilterDefault by derivedStateOf {
        filter == AppFilter.INSTALLED && ascending
    }

    /** How many apps each filter would show, for the filter sheet. */
    val filterCounts by derivedStateOf {
        val apps = privileges.values
        AppFilter.entries.associateWith { f -> apps.count { it.matches(f) } }
    }

    val privilegeList by derivedStateOf {
        val currentSearch = search
        val shown = privileges.values.filter { it.matches(filter) }

        val searched = if (currentSearch.isEmpty()) {
            shown
        } else {
            shown.filter { app ->
                app.label.contains(currentSearch, true) ||
                        app.packageName.contains(currentSearch, true) ||
                        app.pinyinLabel.contains(currentSearch, true)
            }
        }

        val sorted = searched.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        if (ascending) sorted else sorted.reversed()
    }

    private fun AppsViewModel.AppInfo.matches(filter: AppFilter): Boolean {
        val isSystem = ((packageInfo.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) != 0
        return when (filter) {
            AppFilter.INSTALLED -> !isSystem
            AppFilter.SYSTEM -> isSystem
            // Same rule the server uses to decide an app speaks the Shizuku API.
            AppFilter.SHIZUKU -> packageInfo.requestedPermissions
                ?.contains(ServerConstants.PERMISSION) == true
        }
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


    // Requested permissions are needed for the "Shizuku supported" filter.
    private fun getApplications(): List<PackageInfo> {
        return application.packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
    }

    fun loadInstalledApps(refresh: Boolean = true) {
        // Avoid stacking loads: resume triggers a non-refresh load, pull-to-refresh forces one.
        if (isRefreshing) return
        if (!refresh && privileges.isNotEmpty()) return

        viewModelScope.launch {
            isRefreshing = refresh
            val pm = application.packageManager

            val result = withContext(Dispatchers.IO) {
                // System apps are kept for the "System" filter; only the manager itself is skipped.
                val candidates = getApplications().mapNotNull { packageInfo ->
                    val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                    val uid = appInfo.uid
                    if (uid == android.os.Process.myUid()) return@mapNotNull null
                    uid to packageInfo
                }

                // Single binder call for all uids instead of one per app.
                val flags = Axeron.getFlagsForUids(
                    candidates.map { it.first }.toIntArray(),
                    MASK_PERMISSION
                )

                candidates.mapIndexed { index, (uid, packageInfo) ->
                    val appInfo = packageInfo.applicationInfo!!
                    val label = appInfo.loadLabel(pm).toString()
                    val isAdded = (flags.getOrElse(index) { 0 } and FLAG_ALLOWED) == FLAG_ALLOWED

                    uid to AppsViewModel.AppInfo(
                        label = label,
                        pinyinLabel = HanziToPinyin.getInstance().toPinyinString(label),
                        packageInfo = packageInfo,
                        isAdded = isAdded
                    )
                }.toMap()
            }

            privileges = result
            isRefreshing = false
        }
    }
}