package frb.axeron.api

import android.os.Parcelable
import frb.axeron.shared.AxeronConstant
import frb.axeron.shared.ServerInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class AxeronInfo(
    val serverInfo: ServerInfo = ServerInfo()
) : Parcelable {

    fun getActualVersion(): Long {
        return serverInfo.getActualVersion()
    }

    fun isRunning(): Boolean {
        return Axeron.pingBinder() && AxeronConstant.server.getActualVersion() <= getActualVersion()
    }

    fun isNeedUpdate(): Boolean {
        return AxeronConstant.server.getActualVersion() > getActualVersion() && Axeron.pingBinder()
    }

    fun isNeedExtraStep(): Boolean {
        return isRunning() && !serverInfo.permission
    }

}