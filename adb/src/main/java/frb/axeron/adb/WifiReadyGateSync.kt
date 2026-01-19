package frb.axeron.adb

import android.content.Context
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WifiReadyGateSync(
    private val ctx: Context,
    private val timeoutMs: Long = 15_000L
) {
    fun awaitOrThrow(th: Throwable) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        fun ready() {
            latch.countDown()
        }

        fun fail() {
            error = th
            latch.countDown()
        }

        WifiReadyGate(ctx, timeoutMs,::ready, ::fail).start()

        val finished = latch.await(timeoutMs + 500, TimeUnit.MILLISECONDS)
        if (!finished) {
            throw th
        }

        error?.let { throw it }
    }
}
