package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.os.Parcelable
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import frb.axeron.api.Axeron
import frb.axeron.manager.AxeronApplication.Companion.axeronApp
import frb.axeron.manager.ui.util.HanziToPinyin
import frb.axeron.manager.ui.webui.AppIconUtil
import frb.axeron.server.util.AxWebLoader
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File


class AppsViewModel(application: Application) : AndroidViewModel(application) {
    @Parcelize
    data class AppInfo(
        val label: String,
        val packageInfo: PackageInfo,
        val isAdded: Boolean,
    ) : Parcelable {
        class Handler : AxWebLoader.PathHandler {
            override fun handle(
                context: Context,
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val packageName = request!!.url.path.toString().substring(1) // buang leading "/"
                val icon: Bitmap? = AppIconUtil.loadAppIconSync(packageName, 512)
                if (icon != null) {
                    val stream = ByteArrayOutputStream()
                    icon.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val inputStream = ByteArrayInputStream(stream.toByteArray())
                    return WebResourceResponse("image/png", null, inputStream)
                }
                return null
            }

        }

        val packageName: String
            get() = packageInfo.packageName
        val uid: Int
            get() = packageInfo.applicationInfo!!.uid
    }

    val file = File(
        PathHelper.getWorkingPath(
            Axeron.getAxeronInfo().isRoot(),
            AxeronApiConstant.folder.PARENT_BINARY
        ), "added_apps.txt"
    )
    var search by mutableStateOf("")

    val addedList by derivedStateOf {
        addedApps.filter {
            it.label.contains(search, ignoreCase = true) ||
                    it.packageName.contains(search, ignoreCase = true) ||
                    HanziToPinyin.getInstance().toPinyinString(it.label)
                        .contains(search, ignoreCase = true)
        }
    }

    val installedList by derivedStateOf {
        installedApps.filter {
            it.label.contains(search, ignoreCase = true) ||
                    it.packageName.contains(search, ignoreCase = true) ||
                    HanziToPinyin.getInstance().toPinyinString(it.label)
                        .contains(search, ignoreCase = true)
        }
    }

    private val prefs = application.getSharedPreferences("apps_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var addedPackageNames: List<String> by mutableStateOf(emptyList())
        private set

    var addedApps: List<AppInfo> by mutableStateOf(emptyList())
        private set

    var installedApps: List<AppInfo> by mutableStateOf(emptyList())
        private set

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager

            // Ambil packageName yang sudah tersimpan
            addedPackageNames = getSavedPackageNames()
            val packages = Axeron.getPackages(0)

            val apps = packages.map {
                val appInfo = it.applicationInfo!!
                AppInfo(
                    label = appInfo.loadLabel(pm).toString(),
                    packageInfo = it,
                    isAdded = it.packageName in addedPackageNames
                )
            }.filterNot {
                // jangan masukin ke list kalau system app atau app lo sendiri
                it.packageName == axeronApp.packageName ||
                        it.packageInfo.applicationInfo!!.flags.and(ApplicationInfo.FLAG_SYSTEM) != 0
            }

            installedApps = apps
            addedApps = apps.filter {
                it.isAdded
            }
        }
    }

//    fun addApp(app: AppInfo) {
//        if (!addedApps.any { it.packageName == app.packageName }) {
//            val updatedApp = app.copy(isAdded = true)
//            addedApps = addedApps + updatedApp
//            installedApps = installedApps.map {
//                if (it.packageName == app.packageName) it.copy(isAdded = true) else it
//            }
//            saveAddedPackageNames(addedApps.map { it.packageName })
//        }
//    }
//
//    fun removeApp(packageName: String) {
//        addedApps = addedApps.filterNot { it.packageName == packageName }
//        installedApps = installedApps.map {
//            if (it.packageName == packageName) it.copy(isAdded = false) else it
//        }
//        saveAddedPackageNames(addedApps.map { it.packageName })
//    }

    fun addApp(app: AppInfo) {
        if (!addedApps.any { it.packageName == app.packageName }) {
            val updatedApp = app.copy(isAdded = true)
            addedApps = addedApps + updatedApp
            installedApps = installedApps.map {
                if (it.packageName == app.packageName) it.copy(isAdded = true) else it
            }
            saveAddedAppsToFile(addedApps.map { it.packageName })
        }
    }

    fun removeApp(packageName: String) {
        addedApps = addedApps.filterNot { it.packageName == packageName }
        installedApps = installedApps.map {
            if (it.packageName == packageName) it.copy(isAdded = false) else it
        }
        saveAddedAppsToFile(addedApps.map { it.packageName })
    }

    // ==== Penyimpanan hanya packageName ====

    private fun saveAddedAppsToFile(packageNames: List<String>) {
        addedPackageNames = packageNames
        try {
            val fos = Axeron.newFileService()
                .getStreamSession(file.absolutePath, true, false).outputStream
            packageNames.forEach { pkg ->
                val app = installedApps.find { it.packageName == pkg }
                if (app != null) {
                    fos.write("${app.packageName}\n".toByteArray())
                }
            }
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSavedPackageNames(): List<String> {
        return try {
            val axFile = Axeron.newFileService()
            if (!axFile.exists(file.absolutePath)) return emptyList()
            val fis = axFile.setFileInputStream(file.absolutePath)
            fis.use {
                it?.bufferedReader()?.readLines()?.mapNotNull { line ->
                    line.split(",").firstOrNull()
                } ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

//    private fun saveAddedPackageNames(packageNames: List<String>) {
//        val json = gson.toJson(packageNames)
//        prefs.edit { putString("added_apps", json) }
//    }
//
//    private fun getSavedPackageNames(): List<String> {
//        val json = prefs.getString("added_apps", null)
//        return if (!json.isNullOrEmpty()) {
//            val type = object : TypeToken<List<String>>() {}.type
//            gson.fromJson(json, type)
//        } else {
//            emptyList()
//        }
//    }
}


