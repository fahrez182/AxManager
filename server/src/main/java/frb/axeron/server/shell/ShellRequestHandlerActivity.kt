package frb.axeron.server.shell

import android.app.Activity
import android.content.Context
import android.os.Bundle
import frb.axeron.api.core.Engine

class ShellRequestHandlerActivity : Activity() {

    override fun attachBaseContext(newBase: Context?) {
        ShellBinderRequestHandler.handleRequest(Engine.application, intent)
        super.attachBaseContext(newBase)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
