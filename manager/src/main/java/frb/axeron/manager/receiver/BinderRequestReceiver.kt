package frb.axeron.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import frb.axeron.server.ServerConstants


class BinderRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ServerConstants.REQUEST_BINDER_AXRUNTIME) return
        // The reply may have to wait for the server's binder; keep the process alive until then.
        val pending = goAsync()
        ShellBinderRequestHandler.handleRequest(context.applicationContext, intent) { pending.finish() }
    }
}
