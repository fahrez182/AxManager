package frb.axeron.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import rikka.shizuku.shell.ShellBinderRequestHandler

class BinderRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "rikka.shizuku.intent.action.REQUEST_BINDER") return
        ShellBinderRequestHandler.handleRequest(context, intent)
    }
}
