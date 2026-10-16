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

        /** Keeps disabled apps in the list instead of dropping them as if uninstalled. */
        private const val MATCH_DISABLED = PackageManager.MATCH_DISABLED_COMPONENTS or
                PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
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


    // Requested permissions are needed for the "Shizuku supported" filter. Disabled apps stay
    // listed, otherwise one turned off could no longer be turned back on from here.
    private fun getApplications(): List<PackageInfo> {
        return application.packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS or MATCH_DISABLED)
    }

    /** Uids the full load would list; compared on resume to spot installs and uninstalls. */
    private fun installedUids(): Set<Int> =
        application.packageManager.getInstalledPackages(MATCH_DISABLED)
            .mapNotNullTo(HashSet()) { it.applicationInfo?.uid }
            .apply { remove(android.os.Process.myUid()) }

    fun loadInstalledApps(refresh: Boolean = true) {
        // Avoid stacking loads: resume triggers a non-refresh load, pull-to-refresh forces one.
        if (isRefreshing) return
        if (!refresh && privileges.isNotEmpty()) {
            // The list may still be good, but apps can be (un)installed and grants changed
            // elsewhere (e.g. the Shizuku app writes the same config): check, then update.
            reloadFlags()
            return
        }
        loadAll(showRefreshing = refresh)
    }

    private fun loadAll(showRefreshing: Boolean) {
        viewModelScope.launch {
            isRefreshing = showRefreshing
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

    private fun reloadFlags() {
        viewModelScope.launch {
            val current = privileges
            val uids = current.keys.toIntArray()
            val installedChanged = withContext(Dispatchers.IO) { installedUids() != current.keys }
            if (installedChanged) {
                // An app was installed or uninstalled meanwhile: rebuild the list quietly.
                if (privileges === current && !isRefreshing) loadAll(showRefreshing = false)
                return@launch
            }
            val flags = withContext(Dispatchers.IO) {
                // Without the server the call answers all zeros, which would read as "revoked".
                if (!Axeron.pingBinder()) null
                else Axeron.getFlagsForUids(uids, MASK_PERMISSION)
            } ?: return@launch
            // Skip the update when a full load replaced the list meanwhile, or nothing changed.
            if (privileges !== current) return@launch

            var changed = false
            val updated = current.toMutableMap()
            uids.forEachIndexed { index, uid ->
                val isAdded = (flags.getOrElse(index) { 0 } and FLAG_ALLOWED) == FLAG_ALLOWED
                val app = updated[uid] ?: return@forEachIndexed
                if (app.isAdded != isAdded) {
                    updated[uid] = app.copy(isAdded = isAdded)
                    changed = true
                }
            }
            if (changed) privileges = updated
        }
    }
}