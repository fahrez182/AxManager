package frb.axeron.server.shell

import android.app.Activity
import android.os.Bundle

class ShellRequestHandlerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ShellBinderRequestHandler.handleRequest(this.application, intent)
        finish()
    }
}
