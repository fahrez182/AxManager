package com.frb.axmanager.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Parcelable
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize


class AppsViewModel(application: Application) : AndroidViewModel(application) {

//    @Parcelize
//    data class AppInfo(
//        val uid: Int,
//        val label: String,
//        val packageName: String,
//        val isAdded: Boolean // Tambahan
//    ) : Parcelable

    @Parcelize
    data class AppInfo(
        val label: String,
        val packageInfo: PackageInfo,
        val isAdded: Boolean,
    ) : Parcelable {
        val packageName: String
            get() = packageInfo.packageName
        val uid: Int
            get() = packageInfo.applicationInfo!!.uid
    }

    private val prefs = application.getSharedPreferences("apps_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val mutableAddedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val addedApps: StateFlow<List<AppInfo>> = mutableAddedApps

    private val mutableInstalledApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = mutableInstalledApps

    init {
        loadInstalledApps()
    }
    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager

            // Ambil packageName yang sudah tersimpan
            val addedPackageNames = getSavedPackageNames()

            val apps = pm.getInstalledPackages(0)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map {
                    AppInfo(
                        label = it.applicationInfo!!.loadLabel(pm).toString(),
                        packageInfo = it,
                        isAdded = it.packageName in addedPackageNames
                    )
                }
                .sortedBy { it.label.lowercase() }

            mutableInstalledApps.value = apps
            mutableAddedApps.value = apps.filter { it.isAdded }
        }
    }

    fun addApp(app: AppInfo) {
        if (!mutableAddedApps.value.any { it.packageName == app.packageName }) {
            val updatedApp = app.copy(isAdded = true)
            mutableAddedApps.value = mutableAddedApps.value + updatedApp
            mutableInstalledApps.value = mutableInstalledApps.value.map {
                if (it.packageName == app.packageName) it.copy(isAdded = true) else it
            }
            saveAddedPackageNames(mutableAddedApps.value.map { it.packageName })
        }
    }

    fun removeApp(packageName: String) {
        mutableAddedApps.value = mutableAddedApps.value.filterNot { it.packageName == packageName }
        mutableInstalledApps.value = mutableInstalledApps.value.map {
            if (it.packageName == packageName) it.copy(isAdded = false) else it
        }
        saveAddedPackageNames(mutableAddedApps.value.map { it.packageName })
    }

    // ==== Penyimpanan hanya packageName ====

    private fun saveAddedPackageNames(packageNames: List<String>) {
        val json = gson.toJson(packageNames)
        prefs.edit { putString("added_apps", json) }
    }

    private fun getSavedPackageNames(): List<String> {
        val json = prefs.getString("added_apps", null)
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}


