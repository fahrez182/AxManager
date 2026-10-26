package frb.axeron.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.manager.data.SettingsRepository
import frb.axeron.manager.data.SettingsRepository.SettingType
import frb.axeron.shared.AxeronApiConstant.server.APPEND_ONLY_ENV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data for the settings editor. Every repository call spawns a process (`settings list`,
 * `resetprop`), so loading and writing run on IO; entries arrive sorted with their display
 * text prepared, so scrolling only lays out text.
 */
class SettingsEditorViewModel(application: Application) : AndroidViewModel(application) {

    @Immutable
    class Entry(val type: SettingType, val key: String, val value: String) {
        val id = "${type.name}/$key"

        /** Key with break points after symbols, so long dotted keys wrap cleanly. */
        val keyDisplay = smartWrap(key)

        /** The preview below drops the rest of the value. */
        val isPreviewTruncated = value.length > PREVIEW_CHARS

        /** Start of the value only: a huge value would cost a full text layout per frame. */
        val valuePreview = smartWrap(
            if (isPreviewTruncated) value.take(PREVIEW_CHARS) + "…" else value
        )

        /** Fast-scroll section: first letter or digit of the key, `#` for anything else. */
        val section = key.firstOrNull()?.uppercaseChar()
            ?.takeIf { it.isLetterOrDigit() }?.toString() ?: "#"
    }

    private val repository = SettingsRepository(application.contentResolver)

    private val entries = mutableStateMapOf<SettingType, List<Entry>>()
    private val loading = mutableStateMapOf<SettingType, Boolean>()

    var query by mutableStateOf("")

    fun entries(type: SettingType): List<Entry> = entries[type].orEmpty()

    /** Every type merged, sorted by key (each list is already sorted, so this is cheap). */
    val allEntries: List<Entry> by derivedStateOf {
        SettingType.entries.flatMap { entries[it].orEmpty() }.sortedWith(ORDER)
    }

    /** [allEntries] matching [query] by key or value; everything when the query is blank. */
    val filteredAll: List<Entry> by derivedStateOf {
        val q = query.trim()
        if (q.isEmpty()) allEntries
        else allEntries.filter { it.key.contains(q, true) || it.value.contains(q, true) }
    }

    /** [filteredAll] split by source, each still sorted. */
    private val filteredByType: Map<SettingType, List<Entry>> by derivedStateOf {
        filteredAll.groupBy { it.type }
    }

    /** What a page shows: one source, or every source for null ("All"). */
    fun filtered(type: SettingType?): List<Entry> =
        if (type == null) filteredAll else filteredByType[type].orEmpty()

    val isSearching: Boolean get() = query.isNotBlank()

    fun isLoading(type: SettingType?): Boolean =
        if (type == null) SettingType.entries.any { loading[it] == true } else loading[type] == true

    fun isLoaded(type: SettingType): Boolean = entries.containsKey(type)

    /** Loads [type] (null = every type) unless it is already loaded or loading. */
    fun ensureLoaded(type: SettingType?) {
        types(type).filterNot { isLoaded(it) }.forEach(::load)
    }

    fun refresh(type: SettingType?) = types(type).forEach(::load)

    /** How the user's env may set a key; mirrors the server's merge (APPEND_ONLY_ENV). */
    enum class EnvLock {
        /** The user's own variable. */
        NONE,

        /** A default path list: the user's value keeps `$KEY` and only adds entries. */
        APPEND,

        /** The server's or the system's: can't be changed. */
        LOCKED
    }

    // As loaded with AXERON_ENV: the default env's keys, and the user's own env.
    private var envDefaults by mutableStateOf(emptySet<String>())
    private var envUser by mutableStateOf(emptyMap<String, String>())

    fun envLock(key: String): EnvLock = when (key) {
        !in envDefaults -> EnvLock.NONE
        in APPEND_ONLY_ENV -> EnvLock.APPEND
        else -> EnvLock.LOCKED
    }

    /** Whether the user's env holds [key]; only then is there something of the user's to delete. */
    fun hasUserEnv(key: String): Boolean = key in envUser

    /**
     * What the editor starts from for env [key]: the user's own value as saved, never the merged
     * one (saving that would copy the default into the user's env); `$KEY:` for an empty list.
     */
    fun envEditValue(key: String): String =
        envUser[key] ?: if (envLock(key) == EnvLock.APPEND) "\$$key:" else ""

    suspend fun put(type: SettingType, key: String, value: String): Boolean =
        withContext(Dispatchers.IO) { repository.putValue(type, key, value) }
            .also { if (it) load(type) }

    suspend fun delete(type: SettingType, key: String): Boolean =
        withContext(Dispatchers.IO) { repository.deleteValue(type, key) }
            .also { if (it) load(type) }

    private fun types(type: SettingType?) = if (type == null) SettingType.entries else listOf(type)

    private fun load(type: SettingType) {
        if (loading[type] == true) return
        loading[type] = true
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                if (type == SettingType.AXERON_ENV) {
                    val defaults = runCatching { repository.getDefaultEnvKeys() }.getOrDefault(emptySet())
                    val user = repository.getUserEnv()
                    withContext(Dispatchers.Main) {
                        envDefaults = defaults
                        envUser = user
                    }
                }
                runCatching { repository.getAll(type) }.getOrDefault(emptyMap())
                    .map { (key, value) -> Entry(type, key, value) }
                    .sortedWith(ORDER)
            }
            entries[type] = list
            loading[type] = false
        }
    }

    private companion object {
        const val PREVIEW_CHARS = 280
        val ORDER: Comparator<Entry> =
            compareBy<Entry, String>(String.CASE_INSENSITIVE_ORDER) { it.key }.thenBy { it.type }
        val WRAP_AFTER = Regex("(?<=[.,=_()\\[\\]{}<>:;+\\-*/|\\\\])")

        fun smartWrap(text: String): String = text.replace(WRAP_AFTER, "​")
    }
}
