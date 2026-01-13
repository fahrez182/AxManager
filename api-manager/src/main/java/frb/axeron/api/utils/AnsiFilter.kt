package frb.axeron.api.utils

object AnsiFilter {
    val ANSI_CLEAR_REGEX =
        Regex("\u001B\\[(2J|J|H|\\d+;\\d+H)")

    val ANSI_REGEX =
        Regex("\u001B\\[[0-9;?]*[ -/]*[@-~]")

    fun isScreenControl(text: String): Boolean =
        ANSI_CLEAR_REGEX.containsMatchIn(text)

    fun stripAnsi(text: String): String =
        ANSI_REGEX.replace(text, "")
}