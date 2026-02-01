package frb.axeron.server

import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import frb.axeron.server.util.Logger
import frb.axeron.server.util.UserHandleCompat
import frb.axeron.shared.AxeronApiConstant.server.TYPE_ENV
import moe.shizuku.server.IRemoteProcess
import moe.shizuku.server.IShizukuApplication
import moe.shizuku.server.IShizukuService
import moe.shizuku.server.IShizukuServiceConnection
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.PackageManagerApis

class ShizukuServiceIntercept(val axeronService: IAxeronService) : IShizukuService.Stub() {
    companion object {
        val LOGGER = Logger("ShizukuServiceIntercept")
    }

    override fun getVersion(): Int {
        return 13
    }

    override fun getUid(): Int {
        return axeronService.serverInfo.uid
    }

    override fun checkPermission(permission: String?): Int {
        return axeronService.checkPermission(permission)
    }

    override fun newProcess(
        cmd: Array<out String?>?,
        env: Array<out String?>?,
        dir: String?
    ): IRemoteProcess {
        return axeronService.newProcess(cmd, env ?: axeronService.getEnvironment(TYPE_ENV).env, dir)
    }

    override fun getSELinuxContext(): String {
        return axeronService.serverInfo.selinuxContext
    }

    override fun getSystemProperty(
        name: String?,
        defaultValue: String?
    ): String {
        return axeronService.getSystemProperty(name, defaultValue)
    }

    override fun setSystemProperty(name: String?, value: String?) {
        axeronService.setSystemProperty(name, value)
    }

    override fun addUserService(
        conn: IShizukuServiceConnection?,
        args: Bundle?
    ): Int {
        return axeronService.addUserService(conn, args)
    }

    override fun removeUserService(
        conn: IShizukuServiceConnection?,
        args: Bundle?
    ): Int {
        return axeronService.removeUserService(conn, args)
    }

    override fun requestPermission(requestCode: Int) {
        return axeronService.requestPermission(requestCode)
    }

    override fun checkSelfPermission(): Boolean {
        return axeronService.checkSelfPermission()
    }

    override fun shouldShowRequestPermissionRationale(): Boolean {
        return axeronService.shouldShowRequestPermissionRationale()
    }

    override fun attachApplication(
        application: IShizukuApplication?,
        args: Bundle?
    ) {
        axeronService.attachApplication(application, args)
    }

    override fun exit() {
        val callingUid = getCallingUid()
        val userId = UserHandleCompat.getUserId(callingUid)
        LOGGER.i("exit: CallingUid:%s, UserId:%s", callingUid, userId)
        val packages = PackageManagerApis.getPackagesForUidNoThrow(callingUid)
        axeronService.enableShizukuService(false)
        if (packages.size == 1) {
            LOGGER.i("exit: Force Stop %s", packages[0])
            ActivityManagerApis.forceStopPackageNoThrow(packages[0], userId)
        }
    }

    override fun attachUserService(binder: IBinder?, options: Bundle) {
        axeronService.attachUserService(binder, options)
    }

    override fun dispatchPackageChanged(intent: Intent?) {
        axeronService.dispatchPackageChanged(intent)
    }

    override fun isHidden(uid: Int): Boolean {
        return false
    }

    override fun dispatchPermissionConfirmationResult(
        requestUid: Int,
        requestPid: Int,
        requestCode: Int,
        data: Bundle?
    ) {
        axeronService.dispatchPermissionConfirmationResult(requestUid, requestPid, requestCode, data)
    }

    override fun getFlagsForUid(uid: Int, mask: Int): Int {
        return axeronService.getFlagsForUid(uid, mask)
    }

    override fun updateFlagsForUid(uid: Int, mask: Int, value: Int) {
        axeronService.updateFlagsForUid(uid, mask, value)
    }
}