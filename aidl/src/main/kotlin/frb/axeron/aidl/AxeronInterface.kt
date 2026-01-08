package frb.axeron.aidl

import frb.axeron.data.ServerInfo

interface AxeronInterface {
    fun enableShizukuService(enable: Boolean)
    fun getServerInfo(): ServerInfo?
}