package frb.axeron.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.manager.data.QuickSetting
import frb.axeron.manager.data.QuickSettingsStore
import frb.axeron.manager.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Current values and writes for the Quick Settings sheet. Every read or write may spawn a
 * process (`settings`, `resetprop`), so all of it runs on IO.
 */
class QuickSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application.contentResolver)

    /** Current value per [QuickSetting.id]; absent until read, null when unset. */
    val values = mutableStateMapOf<String, String?>()

    /** Ids with a write in flight. */
    val writing = mutableStateMapOf<String, Boolean>()

    /** Re-reads every saved quick setting, in parallel. */
    fun refresh() {
        val items = QuickSettingsStore.items
        viewModelScope.launch {
            val read = withContext(Dispatchers.IO) {
                items.map { item ->
                    async { item.id to runCatching { repository.getValue(item.type, item.key) }.getOrNull() }
                }.awaitAll()
            }
            read.forEach { (id, value) -> values[id] = value }
        }
    }

    /**
     * Writes [value]. The shown value changes right away (a released slider must not jump
     * back while the process runs) and returns to the old one if the write fails.
     */
    suspend fun apply(item: QuickSetting, value: String): Boolean {
        val before = values[item.id]
        values[item.id] = value
        writing[item.id] = true
        val ok = withContext(Dispatchers.IO) {
            runCatching { repository.putValue(item.type, item.key, value) }.getOrDefault(false)
        }
        if (!ok) values[item.id] = before
        writing.remove(item.id)
        return ok
    }
}
