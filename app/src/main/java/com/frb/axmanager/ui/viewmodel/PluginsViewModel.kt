package com.frb.axmanager.ui.viewmodel

import android.net.Uri
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frb.axmanager.axApp
import com.frb.axmanager.ui.util.HanziToPinyin
import com.frb.engine.client.Axeron
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

class PluginsViewModel : ViewModel() {

    companion object {
        const val TAG = "PluginsViewModel"

        fun convertPluginInfo(str : String): PluginInfo {
            val obj = JSONObject(str)
            return PluginInfo(
                obj.getString("id"),
                obj.optString("name"),
                obj.optString("author", "Unknown"),
                obj.optString("version", "Unknown"),
                obj.optInt("versionCode", 0),
                obj.optString("description"),
                obj.getBoolean("enabled"),
                obj.getBoolean("update"),
                obj.getBoolean("update_install"),
                obj.getBoolean("update_remove"),
                obj.getBoolean("update_enable"),
                obj.getBoolean("update_disable"),
                obj.getBoolean("remove"),
                obj.optString("updateJson"),
                obj.optBoolean("web"),
                obj.optBoolean("action"),
                obj.getString("dir_id"),
                obj.getLong("size"),
                obj.optString("banner")
            )
        }
    }

    var search by mutableStateOf("")

    val pluginList by derivedStateOf {
        plugins.filter {
            it.id.contains(search, ignoreCase = true) ||
                    it.name.contains(search, ignoreCase = true) ||
                    HanziToPinyin.getInstance().toPinyinString(it.name)
                        .contains(search, ignoreCase = true)
        }.also {
            isRefreshing = false
        }
    }

    @Parcelize
    data class PluginInfo(
        val id: String,
        val name: String,
        val author: String,
        val version: String,
        val versionCode: Int,
        val description: String,
        val enabled: Boolean,
        val update: Boolean,
        val update_install: Boolean,
        val update_remove: Boolean,
        val update_enable: Boolean,
        val update_disable: Boolean,
        val remove: Boolean,
        val updateJson: String,
        val hasWebUi: Boolean,
        val hasActionScript: Boolean,
        val dirId: String,
        val size: Long,
        val banner: String
    ) : Parcelable

    var plugins by mutableStateOf<List<PluginInfo>>(emptyList())
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var isNeedRefresh by mutableStateOf(false)
        private set

    var pluginUpdateCount by mutableIntStateOf(0)
        private set

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    var zipUris by mutableStateOf<List<Uri>>(emptyList())

    fun updateZipUris(uris: List<Uri>) {
        zipUris = uris
    }

    fun clearZipUris() {
        zipUris = emptyList()
    }

    fun fetchModuleList() {
        viewModelScope.launch {
            isRefreshing = true

            withContext(Dispatchers.IO) {
                val start = SystemClock.elapsedRealtime()
                val oldPluginList = plugins

                kotlin.runCatching {
                    val result = Axeron.getPlugins()
                    Log.i(TAG, "$result")

                    plugins = result.map {
                        convertPluginInfo(it)
                    }
                    isNeedRefresh = false
                }.onFailure { e ->
                    Log.e(TAG, "fetchModuleList: ", e)
                    isRefreshing = false
                }

                withContext(Dispatchers.IO) {
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

    fun checkUpdate(m: PluginInfo): Triple<String, String, String> {
        val empty = Triple("", "", "")
        if (m.updateJson.isEmpty() || m.remove || m.update || !m.enabled) {
            return empty
        }
        // download updateJson
        val result = kotlin.runCatching {
            val url = m.updateJson
            Log.i(TAG, "checkUpdate url: $url")
            val response = axApp.okhttpClient.newCall(
                okhttp3.Request.Builder().url(url).build()
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
        if (versionCode <= m.versionCode || zipUrl.isEmpty()) {
            return empty
        }

        return Triple(zipUrl, version, changelog)
    }

}