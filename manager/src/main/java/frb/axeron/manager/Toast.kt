package frb.axeron.manager

import android.app.Activity
import android.os.Bundle
import android.view.Gravity

class Toast : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var text = intent.getStringExtra("text")
        if (text == null || text.trim().isEmpty()) text = "Toasted!"

        val toast = android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
        toast.show()

        finish()
    }
}
