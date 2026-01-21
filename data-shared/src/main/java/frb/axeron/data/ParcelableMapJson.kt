package frb.axeron.shared

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import kotlin.properties.ReadOnlyProperty

@SuppressLint("ParcelCreator")
open class ParcelableMapJson(
    private val json: String = ""
) : Parcelable {

    // Lazy deserialize
    private val map: Map<String, Any?> by lazy {
        if (json.isEmpty()) emptyMap()
        else gson.fromJson(json, mapType)
    }

    fun toMap(): Map<String, Any?> {
        // Convert nested ParcelableMapJson ke Map juga
        val result = mutableMapOf<String, Any?>()
        map.forEach { (key, value) ->
            result[key] = when (value) {
                is ParcelableMapJson -> value.toMap()
                else -> value
            }
        }
        return result
    }

    fun toJson(): String = gson.toJson(toMap())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(toJson())
    }

    override fun describeContents(): Int = 0

    /**
     * Delegasi property
     * @param key JSON key
     * @param default default value
     * @param clazz optional: class target jika nested ParcelableMapJson
     */
    protected fun <T> field(
        key: String,
        default: T,
        clazz: Class<out ParcelableMapJson>? = null
    ): ReadOnlyProperty<ParcelableMapJson, T> = ReadOnlyProperty { _, _ ->
        val value = map[key]

        @Suppress("UNCHECKED_CAST")
        when {
            value == null -> default
            clazz != null && value is Map<*, *> -> fromMap(clazz, value as Map<String, Any?>) as T
            clazz != null && clazz.isInstance(value) -> value as T
            else -> try {
                    when (default) {
                        is Int -> when (value) {
                            is Number -> value.toInt() as T
                            is String -> value.toIntOrNull()?.let { it as T } ?: default
                            else -> default
                        }

                        is Long -> when (value) {
                            is Number -> value.toLong() as T
                            is String -> value.toLongOrNull()?.let { it as T } ?: default
                            else -> default
                        }

                        is Float -> when (value) {
                            is Number -> value.toFloat() as T
                            is String -> value.toFloatOrNull()?.let { it as T } ?: default
                            else -> default
                        }

                        is Double -> when (value) {
                            is Number -> value.toDouble() as T
                            is String -> value.toDoubleOrNull()?.let { it as T } ?: default
                            else -> default
                        }

                        is Boolean -> when (value) {
                            is Boolean -> value as T
                            is String -> {
                                when (value.lowercase()) {
                                    "true", "1", "yes", "on" -> true as T
                                    "false", "0", "no", "off" -> false as T
                                    else -> default
                                }
                            }
                            is Number -> (value.toInt() != 0) as T
                            else -> default
                        }

                        is String -> value.toString() as T

                        else -> value as? T ?: default
                    }
                } catch (_: Exception) {
                    default
                }
        }
    }

    companion object {
        private val gson = GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create()
        private val mapType = object : TypeToken<Map<String, Any?>>() {}.type

        /**
         * Convert Map -> instance subclass
         */
        @JvmStatic
        fun <T : ParcelableMapJson> fromMap(clazz: Class<T>, map: Map<String, Any?>): T {
            val json = gson.toJson(map)
            val ctor = clazz.getConstructor(String::class.java)
            return ctor.newInstance(json)
        }

        @JvmStatic
        inline fun <reified T : ParcelableMapJson> fromMap(map: Map<String, Any?>): T {
            return fromMap(T::class.java, map)
        }
    }
}
