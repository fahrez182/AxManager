package frb.axeron.adb

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WifiReadyGate(
    private val ctx: Context,
    private val timeoutMs: Long = 15_000L
) {
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
        val cm =
            ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun isUsable(network: Network): Boolean {
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (isUsable(network)) {
                    cleanup()
                    onReady()
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                caps: NetworkCapabilities
            ) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                ) {
                    cleanup()
                    onReady()
                }
            }

            fun cleanup() {
                try {
                    cm.unregisterNetworkCallback(this)
                } catch (_: Exception) {
                }
            }
        }

        val current = cm.activeNetwork
        if (current == null || !isUsable(current)) {
            onFail()
        }

        cm.registerDefaultNetworkCallback(callback)
    }
}
