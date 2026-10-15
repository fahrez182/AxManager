package frb.axeron.manager.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import frb.axeron.manager.R
import frb.axeron.manager.data.CommandBookmarks

/**
 * "Save snippet" in the text selection toolbar (Android's PROCESS_TEXT), so a command can be
 * saved from anything selectable: QuickShell's output as well as its input. Not exported, so
 * the entry shows inside AxManager only and other apps can't plant commands into the list.
 */
class SaveSnippetActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (!text.isNullOrBlank()) {
            CommandBookmarks.add(text)
            Toast.makeText(this, R.string.quickshell_bookmark_saved, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
