package frb.axeron.adb

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AdbWifiGate(
    private val ctx: Context,
    private val timeoutMs: Long = 5_000L
) {

    private val startAt = SystemClock.uptimeMillis()
    private val handler = Handler(Looper.getMainLooper())

    @Throws(RuntimeException::class)
    fun await(th: Throwable) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        fun ready() {
            latch.countDown()
        }

        fun fail() {
            error = th
            latch.countDown()
        }

        check(::ready, ::fail)

        val finished = latch.await(timeoutMs + 500, TimeUnit.MILLISECONDS)
        if (!finished) {
            throw th
        }

        error?.let { throw it }
    }

    private fun check(
        onReady: () -> Unit,
        onFail: () -> Unit
    ) {
        val enabled = Settings.Global.getInt(ctx.contentResolver, "adb_wifi_enabled", 0) == 1

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

        handler.postDelayed({ check(onReady, onFail) }, 300)
    }
}
