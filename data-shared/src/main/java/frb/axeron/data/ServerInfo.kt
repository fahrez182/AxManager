package frb.axeron.shared

import android.os.Parcelable
import android.os.SystemClock
import kotlinx.parcelize.Parcelize

enum class Mode(val label: String) {
    NOT_ACTIVATED("Not Activated"), ROOT("Root"), SHELL("Shell"), USER("User")
}


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

    fun getMode(): Mode {
        return when (uid) {
            -1 -> Mode.NOT_ACTIVATED
            0 -> Mode.ROOT
            2000 -> Mode.SHELL
            else -> Mode.ROOT
        }
    }
}