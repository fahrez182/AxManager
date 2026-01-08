package frb.axeron.reignite

object SystemProp {

    fun get(key: String): String {
        return try {
            val proc = Runtime.getRuntime().exec(
                arrayOf("resetprop", key)
            )
            proc.inputStream.bufferedReader().readLine() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun set(key: String, value: String) {
        try {
            Runtime.getRuntime().exec(
                arrayOf("resetprop", key, value)
            ).waitFor()
        } catch (_: Exception) {}
    }
}
