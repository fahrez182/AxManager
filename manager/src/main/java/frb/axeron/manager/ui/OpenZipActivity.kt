package frb.axeron.manager.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Receives a plugin zip opened from another app (VIEW) and hands it to [AxActivity] in
 * AxManager's own task.
 *
 * AxActivity can't take the VIEW itself: apps like Telegram open files with
 * startActivityForResult, and Android then ignores singleTask, so a second
 * AxActivity was created inside the sender's task. This one is started there
 * instead, invisible, and forwards with a plain startActivity in a new task,
 * passing on its read grant for the zip.
 */
class OpenZipActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent
        startActivity(
            Intent(source).apply {
                setClass(this@OpenZipActivity, AxActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        )
        finish()
    }
}
