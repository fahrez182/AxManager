package frb.axeron.manager.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import frb.axeron.manager.AxActivity

class OpenZipActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val intent = Intent(this, AxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}
