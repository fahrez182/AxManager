package frb.axeron.server.util

fun flattenOneLevel(map: Map<String, Any?>, separator: String = "."): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()

    for ((key, value) in map) {
        if (value is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            for ((innerKey, innerValue) in value as Map<String, Any?>) {
                result[innerKey] = innerValue
            }
        } else {
            result[key] = value
        }
    }

    return result
}
