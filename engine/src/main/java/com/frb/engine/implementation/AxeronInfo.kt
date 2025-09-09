package com.frb.engine.implementation

import android.os.Parcelable
import android.os.SystemClock
import com.frb.engine.client.Axeron
import kotlinx.parcelize.Parcelize

@Parcelize
data class AxeronInfo(
    val version: String = "Unknown",
    val versionCode: Long = -1,
    val uid: Int = -1,
    val pid: Int = -1,
    val selinuxContext: String = "Unknown",
    val starting: Long = SystemClock.elapsedRealtime(),
    val permission: Boolean = false
) : Parcelable {
    fun isRunning(): Boolean {
        return Axeron.pingBinder() && AxeronService.VERSION_CODE <= versionCode
    }

    fun isNeedUpdate(): Boolean {
        return AxeronService.VERSION_CODE > versionCode && Axeron.pingBinder()
    }

    fun isNeedExtraStep(): Boolean {
        return isRunning() && !permission
    }

    fun getMode(): String {
        return when (uid) {
            -1 -> "Not Activated"
            0 -> "Root"
            2000 -> "Shell"
            else -> "User"
        }
    }
}
