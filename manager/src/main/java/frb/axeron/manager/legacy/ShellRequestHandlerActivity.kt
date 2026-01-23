package frb.axeron.manager.legacy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.shell.ShellBinderRequestHandler

class ShellRequestHandlerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ShellBinderRequestHandler.handleRequest(this, intent)
        finish()
    }
}
