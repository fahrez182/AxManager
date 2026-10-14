package frb.axeron.manager.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.widget.Toast
import frb.axeron.manager.R

/**
 * Callback for ShortcutManager.requestPinShortcut: the system sends it only once the shortcut
 * is actually on the home screen. Some launchers (HyperOS) pin without any dialog, so this
 * toast is the user's only confirmation.
 */
class ShortcutPinnedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        Toast.makeText(
            context.applicationContext,
            context.getString(R.string.shortcut_added, label),
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        private const val EXTRA_LABEL = "label"

        fun callback(context: Context, shortcutId: String, label: String): IntentSender {
            val intent = Intent(context, ShortcutPinnedReceiver::class.java)
                .putExtra(EXTRA_LABEL, label)
            return PendingIntent.getBroadcast(
                context,
                shortcutId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ).intentSender
        }
    }
}
