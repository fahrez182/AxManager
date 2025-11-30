package frb.axeron.manager.ui.webui

data class Insets(
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int,
) {
    val css
        get() = buildString {
            appendLine(":root {")
            appendLine("\t--safe-area-inset-top: ${top}px;")
            appendLine("\t--safe-area-inset-right: ${right}px;")
            appendLine("\t--safe-area-inset-bottom: ${bottom}px;")
            appendLine("\t--safe-area-inset-left: ${left}px;")
            appendLine("\t--window-inset-top: var(--safe-area-inset-top, 0px);")
            appendLine("\t--window-inset-bottom: var(--safe-area-inset-bottom, 0px);")
            appendLine("\t--window-inset-left: var(--safe-area-inset-left, 0px);")
            appendLine("\t--window-inset-right: var(--safe-area-inset-right, 0px);")
            appendLine("\t--f7-safe-area-top: var(--window-inset-top, 0px) !important;")
            appendLine("\t--f7-safe-area-bottom: var(--window-inset-bottom, 0px) !important;")
            appendLine("\t--f7-safe-area-left: var(--window-inset-left, 0px) !important;")
            appendLine("\t--f7-safe-area-right: var(--window-inset-right, 0px) !important;")
            append("}")
        }
}