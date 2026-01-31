package frb.axeron.adb.util

import android.os.Build
import android.os.SystemProperties
import androidx.annotation.ChecksSdkIntAtLeast
import frb.axeron.api.core.AxeronSettings

object AdbEnvironment {
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun isTlsSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    fun isWifiRequired(): Boolean {
        return (getAdbTcpPort() <= 0 || !AxeronSettings.getTcpMode())
    }

    fun getAdbTcpPort(): Int {
        var port = SystemProperties.getInt("service.adb.tcp.port", -1)
        if (port <= 0) port = SystemProperties.getInt("persist.adb.tcp.port", -1)
        if (port <= 0 && !isTlsSupported()) port = AxeronSettings.getTcpPort()
        return port
    }
}