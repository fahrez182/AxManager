package frb.axeron.adb.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemProperties
import androidx.annotation.ChecksSdkIntAtLeast
import frb.axeron.api.core.AxeronSettings
import java.io.File

object AdbEnvironment {
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun isTlsSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    /**
     * A Wi-Fi network is up, not just the switch on. Asked of the connectivity service: since
     * Android 12 WifiManager.connectionInfo is redacted without the location permission (its
     * networkId is -1 even when connected), so it can't tell.
     */
    @Suppress("DEPRECATION")
    fun isWifiConnected(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.allNetworks.any {
            cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    fun getAdbTcpPort(): Int {
        var port = SystemProperties.getInt("service.adb.tcp.port", -1)
        if (port <= 0) port = SystemProperties.getInt("persist.adb.tcp.port", -1)
        if (port <= 0 && !isTlsSupported()) {
            // The props above may be unreadable here, so the configured port is a guess:
            // only trust it when something really listens on it.
            val configured = AxeronSettings.getTcpPort()
            if (isListening(configured) != false) port = configured
        }
        return port
    }

    /**
     * Whether a TCP socket listens on [port], from /proc/net/tcp{,6}; null when the kernel
     * doesn't let this app read them (Android 10+).
     */
    private fun isListening(port: Int): Boolean? {
        val hexPort = ":%04X".format(port)
        var readable = false
        for (path in listOf("/proc/net/tcp", "/proc/net/tcp6")) {
            val lines = runCatching { File(path).readLines() }.getOrNull() ?: continue
            readable = true
            // Columns: sl local_address rem_address st … — st 0A is LISTEN.
            if (lines.drop(1).any { line ->
                    val cols = line.trim().split(Regex("\\s+"))
                    cols.size > 3 && cols[1].endsWith(hexPort) && cols[3] == "0A"
                }) return true
        }
        return if (readable) false else null
    }
}