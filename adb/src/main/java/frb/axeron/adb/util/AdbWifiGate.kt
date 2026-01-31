package frb.axeron.adb.util

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings

class AdbWifiGate(
    private val ctx: Context,
    private val timeoutMs: Long = 5_000,
    private val onReady: () -> Unit,
    private val onFail: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val startAt = SystemClock.uptimeMillis()

    fun start() {
        check()
    }

    private fun check() {
        val enabled =
            Settings.Global.getInt(
                ctx.contentResolver,
                "adb_wifi_enabled",
                0
            ) == 1

        if (enabled) {
            onReady()
            return
        }

        val permission =
            ctx.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

        if (!permission) {
            onFail()
            return
        }

        if (SystemClock.uptimeMillis() - startAt > timeoutMs) {
            onFail()
            return
        }

        handler.postDelayed({ check() }, 300)
    }
}
