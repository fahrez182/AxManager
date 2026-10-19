package frb.axeron.manager.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardUtil {
    @JvmStatic
    fun put(context: Context, str: CharSequence?): Boolean {
        return put(context, ClipData.newPlainText("label", str))
    }

    @JvmStatic
    fun put(context: Context, clipData: ClipData): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(clipData)
            true
        } catch (e: Exception) {
            false
        }
    }
}
