package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.api.Axeron
import frb.axeron.manager.AxeronApplication.Companion.axeronApp
import frb.axeron.manager.ui.util.HanziToPinyin
import frb.axeron.server.PluginInfo
import frb.axeron.server.PluginInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

class PluginViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("plugin_settings", Context.MODE_PRIVATE)

    companion object {
        const val TAG = "PluginsViewModel"
    }

    var search by mutableStateOf("")

    var getSelectedAsc by mutableIntStateOf(
        prefs.getInt("selected_asc", 0)
    )
        private set
    var getSelectedSort by mutableIntStateOf(
        prefs.getInt("selected_sort", 0)
    )
        private set

    fun setSelectedAsc(value: Int) {
        getSelectedAsc = value
        prefs.edit { putInt("selected_asc", value) }
    }

    fun setSelectedSort(value: Int) {
        getSelectedSort = value
        prefs.edit { putInt("selected_sort", value) }
    }

    val pluginList by derivedStateOf {
        val comparator: Comparator<PluginInfo> = if (getSelectedAsc == 0) {
            when (getSelectedSort) {
                1 -> compareBy { it.size }
                2 -> compareBy { it.enabled }
                3 -> compareBy { it.hasActionScript }
                4 -> compareBy { it.hasWebUi }
                else -> compareBy<PluginInfo> { it.prop.name }
            }
        } else {
            when (getSelectedSort) {
                1 -> compareByDescending { it.size }
                2 -> compareByDescending { it.enabled }
                3 -> compareByDescending { it.hasActionScript }
                4 -> compareByDescending { it.hasWebUi }
                else -> compareByDescending<PluginInfo> { it.prop.name }
            }
        }.thenBy { it.prop.id }

        plugins.filter {
            it.prop.id.contains(search, ignoreCase = true) ||
                    it.prop.name.contains(search, ignoreCase = true) ||
                    HanziToPinyin.getInstance().toPinyinString(it.prop.name)
                        .contains(search, ignoreCase = true)
        }.sortedWith(comparator).also {
            isRefreshing = false
        }
    }

    var plugins by mutableStateOf<List<PluginInfo>>(ArrayList())
        private set

    var isNeedReignite by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var isNeedRefresh by mutableStateOf(false)
        private set

    var pluginUpdateCount by mutableIntStateOf(0)
        private set

    var pluginEnabledCount by mutableIntStateOf(0)
        private set

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    var pluginInstalers by mutableStateOf<List<PluginInstaller>>(emptyList())

    fun updateZipUris(installers: List<PluginInstaller>) {
        pluginInstalers = installers
    }

    fun clearZipUris() {
        pluginInstalers = emptyList()
    }

    fun fetchModuleList() {
        viewModelScope.launch {
            isRefreshing = true

            withContext(Dispatchers.IO) {
                val start = SystemClock.elapsedRealtime()
                val oldPluginList = plugins

                runCatching {
                    isNeedReignite = false

                    plugins = Axeron.getPlugins()

                    isNeedReignite = plugins.any { it.update }

                    isNeedRefresh = false
                }.onFailure { e ->
                    Log.e(TAG, "fetchModuleList: ", e)
                    isRefreshing = false
                }

                withContext(Dispatchers.IO) {
                    pluginEnabledCount = plugins.count {
                        it.enabled
                    }
                    pluginUpdateCount = plugins.count {
                        checkUpdate(it).first.isNotEmpty()
                    }
                }

                val loadCost = SystemClock.elapsedRealtime() - start
                Log.i(TAG, "load cost: $loadCost, modules: $plugins")

                // when both old and new is kotlin.collections.EmptyList
                // moduleList update will don't trigger
                delay(if (loadCost < 100) loadCost + 80 else 0)
                if (oldPluginList === plugins) {
                    isRefreshing = false
                }
            }

        }
    }

    private fun sanitizeVersionString(version: String): String {
        return version.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
    }

    fun checkUpdate(pluginInfo: PluginInfo): Triple<String, String, String> {
        val empty = Triple("", "", "")
        if (pluginInfo.prop.updateJson.isEmpty() || pluginInfo.remove || pluginInfo.update || !pluginInfo.enabled) {
            return empty
        }
        // download updateJson
        val result = kotlin.runCatching {
            val url = pluginInfo.prop.updateJson
            Log.i(TAG, "checkUpdate url: $url")
            val response = axeronApp.okhttpClient.newCall(
                Request.Builder().url(url).build()
            ).execute()
            Log.d(TAG, "checkUpdate code: ${response.code}")
            if (response.isSuccessful) {
                response.body?.string() ?: ""
            } else {
                ""
            }
        }.getOrDefault("")
        Log.i(TAG, "checkUpdate result: $result")

        if (result.isEmpty()) {
            return empty
        }

        val updateJson = kotlin.runCatching {
            JSONObject(result)
        }.getOrNull() ?: return empty

        var version = updateJson.optString("version", "")
        version = sanitizeVersionString(version)
        val versionCode = updateJson.optInt("versionCode", 0)
        val zipUrl = updateJson.optString("zipUrl", "")
        val changelog = updateJson.optString("changelog", "")
        if (versionCode <= pluginInfo.prop.versionCode || zipUrl.isEmpty()) {
            return empty
        }

        return Triple(zipUrl, version, changelog)
    }

}