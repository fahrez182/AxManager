package frb.axeron.data

import android.os.Parcelable
import android.os.SystemClock
//import com.frb.engine.client.Axeron
//import com.frb.engine.implementation.AxeronService
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServerInfo(
    val version: String = "Unknown",
    val versionCode: Long = -1,
    val patchCode: Long = 0,
    val uid: Int = -1,
    val pid: Int = -1,
    val selinuxContext: String = "Unknown",
    val starting: Long = SystemClock.elapsedRealtime(),
    val permission: Boolean = false
) : Parcelable {

    fun getActualVersion(): Long {
        return versionCode + patchCode
    }

//    fun isRunning(ping: Boolean, actualVersion: Long): Boolean {
//        return ping && actualVersion <= getActualVersion()
////        return Axeron.pingBinder() && AxeronService.getActualVersion() <= getActualVersion()
//    }
//
//    fun isNeedUpdate(ping: Boolean, actualVersion: Long): Boolean {
//        return actualVersion > getActualVersion() && ping
////        return false
////        return AxeronService.getActualVersion() > getActualVersion() && Axeron.pingBinder()
//    }
//
//    fun isNeedExtraStep(ping: Boolean, actualVersion: Long): Boolean {
//        return isRunning(ping, actualVersion) && !permission
//    }

    fun getMode(): String {
        return when (uid) {
            -1 -> "Not Activated"
            0 -> "Root"
            2000 -> "Shell"
            else -> "User"
        }
    }
}