package frb.axeron.adb.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log

class WifiReadyGate(
    context: Context,
    private val timeoutMs: Long = 15_000L,
    private val onReady: (Network?) -> Unit,
    private val onFail: () -> Unit
) {

    companion object {
        private const val TAG = "WifiReadyGate"
    }

    private val cm =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val wm =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val handler = Handler(Looper.getMainLooper())

    private var fired = false
    private var started = false

    fun start() {
        if (started) return
        started = true

        Log.d(TAG, "start()")

        if (isWifiLinkUp()) {
            Log.d(TAG, "Wi-Fi link already up (link-layer)")
            fire(null)
            return
        }

//        val network = cm.activeNetwork
//        if (network == null || !isUsable(network)) {
//            Log.d(TAG, "Fast: No Wi-Fi network available")
//            cleanup()
//            onFail()
//            return
//        }

        cm.registerDefaultNetworkCallback(callback)

        handler.postDelayed({
            if (!fired) {
                Log.e(TAG, "timeout")
                cleanup()
                onFail()
            }
        }, timeoutMs)
    }

    private fun fire(network: Network?) {
        if (fired) return
        fired = true

        Log.d(TAG, "fire() network=$network")

        // Bind process if possible (critical for OEMs)
        if (network != null) {
            try {
                cm.bindProcessToNetwork(network)
                Log.d(TAG, "process bound to network")
            } catch (e: Exception) {
                Log.w(TAG, "bindProcessToNetwork failed", e)
            }
        }

        cleanup()
        onReady(network)
    }

    private fun cleanup() {
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
        handler.removeCallbacksAndMessages(null)
    }

    private fun isWifiLinkUp(): Boolean {
        val info = wm.connectionInfo ?: return false
        return info.networkId != -1 &&
                info.supplicantState == SupplicantState.COMPLETED
    }

    private fun isUsable(network: Network): Boolean {
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private val callback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            Log.d(TAG, "onAvailable($network)")
            if (isUsable(network)) {
                fire(network)
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            caps: NetworkCapabilities
        ) {
            Log.d(TAG, "onCapabilitiesChanged($network, $caps)")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                fire(network)
            }
        }

        override fun onUnavailable() {
            Log.w(TAG, "onUnavailable()")
            cleanup()
            onFail()
        }

        override fun onLost(network: Network) {
            Log.w(TAG, "onLost($network)")
            cleanup()
            onFail()
        }
    }
}
