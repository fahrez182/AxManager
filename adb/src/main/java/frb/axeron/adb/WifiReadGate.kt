package frb.axeron.adb

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper

class WifiReadyGate(
    context: Context,
    private val timeoutMs: Long = 15_000L,
    private val onReady: () -> Unit,
    private val onFail: () -> Unit
) {

    private val cm =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val handler = Handler(Looper.getMainLooper())
    private var fired = false
    private var started = false

    fun await() {
        if (started) return
        started = true

        val current = cm.activeNetwork
        if (current == null || !isUsable(current)) {
            onFail()
        }

        cm.registerDefaultNetworkCallback(callback)

        // timeout hard-stop
        handler.postDelayed({
            if (!fired) {
                cleanup()
                onFail()
            }
        }, timeoutMs)
    }

    private fun fire() {
        if (fired) return
        fired = true
        cleanup()
        onReady()
    }

    private fun cleanup() {
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
        handler.removeCallbacksAndMessages(null)
    }

    private fun isUsable(network: Network): Boolean {
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
    }

    private val callback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            if (isUsable(network)) fire()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            caps: NetworkCapabilities
        ) {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            ) {
                fire()
            }
        }
    }
}
