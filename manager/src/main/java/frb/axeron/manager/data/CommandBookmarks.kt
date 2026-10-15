package frb.axeron.manager.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import frb.axeron.api.core.AxeronSettings
import org.json.JSONArray

/** Saved QuickShell commands, newest first; persisted as JSON in the app preferences. */
object CommandBookmarks {
    private const val KEY = "quickshell_bookmarks"

    private val prefs get() = AxeronSettings.getPreferences()

    var items: List<String> by mutableStateOf(load())
        private set

    /** Adds [command] at the top; saving one that exists moves it to the top instead. */
    fun add(command: String) {
        val clean = command.trim()
        if (clean.isEmpty()) return
        items = listOf(clean) + items.filterNot { it == clean }
        persist()
    }

    /**
     * Replaces [old] with [new] where it is, keeping the order. If [new] already exists as
     * another snippet, that other copy is dropped instead of keeping two.
     */
    fun update(old: String, new: String) {
        val clean = new.trim()
        if (clean.isEmpty() || clean == old) return
        items = items.filterNot { it == clean }.map { if (it == old) clean else it }
        persist()
    }

    fun remove(command: String) {
        items = items.filterNot { it == command }
        persist()
    }

    private fun persist() = prefs.edit { putString(KEY, JSONArray(items).toString()) }

    private fun load(): List<String> = runCatching {
        val array = JSONArray(prefs.getString(KEY, null) ?: return emptyList())
        (0 until array.length()).map { array.getString(it) }
    }.getOrDefault(emptyList())
}
