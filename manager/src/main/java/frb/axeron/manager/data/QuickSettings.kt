package frb.axeron.manager.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import frb.axeron.api.core.AxeronSettings
import frb.axeron.manager.data.SettingsRepository.SettingType
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import kotlin.math.roundToLong

/** A setting pinned to the Quick Settings sheet with a ready-made control. */
@Immutable
data class QuickSetting(
    val type: SettingType,
    val key: String,
    val label: String,
    val control: Control
) {
    /** One quick setting per setting: adding the same key again replaces it. */
    val id: String get() = "${type.name}/$key"

    sealed interface Control {
        /** Switch: [on] when checked, [off] when not. */
        data class Toggle(val on: String, val off: String) : Control

        /** Bookmark: shows the current value and takes any new one typed in. */
        data object Custom : Control

        /** One chip per option; picking one writes its value. */
        data class Choice(val options: List<Option>) : Control

        /** Number in [min, max] moving by [step]; [decimal] false writes whole numbers. */
        data class Slider(
            val decimal: Boolean,
            val min: Double,
            val max: Double,
            val step: Double
        ) : Control {
            /** Decimal places the step needs (0.001 → 3); integers have none. */
            val decimals: Int =
                if (!decimal) 0
                else BigDecimal(step.toString()).stripTrailingZeros().scale().coerceAtLeast(0)

            /** [value] clamped and snapped to the step grid from [min]. */
            fun snap(value: Double): Double {
                val steps = ((value - min) / step).roundToLong()
                return (min + steps * step).coerceIn(min, max)
            }

            /** How a value is written to the setting. */
            fun format(value: Double): String =
                if (!decimal) snap(value).roundToLong().toString()
                else BigDecimal(snap(value)).setScale(decimals, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString()
        }
    }

    @Immutable
    data class Option(val label: String, val value: String) {
        val displayLabel: String get() = label.ifBlank { value }
    }
}

/** The saved quick settings, in the order the user added them; persisted as JSON. */
object QuickSettingsStore {
    private const val KEY = "quick_settings"

    private val prefs get() = AxeronSettings.getPreferences()

    var items: List<QuickSetting> by mutableStateOf(load())
        private set

    fun find(type: SettingType, key: String): QuickSetting? =
        items.firstOrNull { it.type == type && it.key == key }

    /** Adds [item], or replaces the one for the same setting in place. */
    fun save(item: QuickSetting) {
        val index = items.indexOfFirst { it.id == item.id }
        items = if (index < 0) items + item else items.toMutableList().also { it[index] = item }
        persist()
    }

    /** Moves [item] by [delta] places (-1 = up); ignored past either end. */
    fun move(item: QuickSetting, delta: Int) {
        val from = items.indexOfFirst { it.id == item.id }
        val to = from + delta
        if (from < 0 || to !in items.indices) return
        items = items.toMutableList().apply { add(to, removeAt(from)) }
        persist()
    }

    fun remove(item: QuickSetting) {
        items = items.filterNot { it.id == item.id }
        persist()
    }

    private fun persist() = prefs.edit { putString(KEY, encode(items)) }

    private fun load(): List<QuickSetting> =
        runCatching { decode(prefs.getString(KEY, null) ?: return emptyList()) }
            .getOrDefault(emptyList())

    private fun encode(list: List<QuickSetting>): String = JSONArray().apply {
        list.forEach { item ->
            put(JSONObject().apply {
                put("type", item.type.name)
                put("key", item.key)
                put("label", item.label)
                when (val c = item.control) {
                    QuickSetting.Control.Custom -> put("mode", "custom")

                    is QuickSetting.Control.Toggle -> {
                        put("mode", "toggle")
                        put("on", c.on)
                        put("off", c.off)
                    }

                    is QuickSetting.Control.Choice -> {
                        put("mode", "choice")
                        put("options", JSONArray().apply {
                            c.options.forEach { o ->
                                put(JSONObject().put("label", o.label).put("value", o.value))
                            }
                        })
                    }

                    is QuickSetting.Control.Slider -> {
                        put("mode", "slider")
                        put("decimal", c.decimal)
                        put("min", c.min)
                        put("max", c.max)
                        put("step", c.step)
                    }
                }
            })
        }
    }.toString()

    /** Entries that no longer parse (e.g. a removed setting type) are skipped, not fatal. */
    private fun decode(json: String): List<QuickSetting> {
        val array = JSONArray(json)
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                val control = when (o.getString("mode")) {
                    "custom" -> QuickSetting.Control.Custom
                    "toggle" -> QuickSetting.Control.Toggle(o.getString("on"), o.getString("off"))
                    "choice" -> {
                        val options = o.getJSONArray("options")
                        QuickSetting.Control.Choice((0 until options.length()).map { j ->
                            val opt = options.getJSONObject(j)
                            QuickSetting.Option(opt.getString("label"), opt.getString("value"))
                        })
                    }

                    else -> QuickSetting.Control.Slider(
                        o.getBoolean("decimal"),
                        o.getDouble("min"),
                        o.getDouble("max"),
                        o.getDouble("step")
                    )
                }
                QuickSetting(SettingType.valueOf(o.getString("type")), o.getString("key"), o.getString("label"), control)
            }.getOrNull()
        }
    }
}
