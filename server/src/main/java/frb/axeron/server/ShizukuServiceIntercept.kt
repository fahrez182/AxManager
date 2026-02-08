package frb.axeron.server

import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import frb.axeron.server.api.ShizukuIntercept
import frb.axeron.server.util.Logger
import frb.axeron.server.util.UserHandleCompat
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.AxeronApiConstant.server.TYPE_ENV
import frb.axeron.shared.ShizukuApiConstant.ATTACH_APPLICATION_API_VERSION
import frb.axeron.shared.ShizukuApiConstant.ATTACH_APPLICATION_PACKAGE_NAME
import frb.axeron.shared.ShizukuApiConstant.BINDER_TRANSACTION_transact
import moe.shizuku.server.IRemoteProcess
import moe.shizuku.server.IShizukuApplication
import moe.shizuku.server.IShizukuService
import moe.shizuku.server.IShizukuServiceConnection
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.PackageManagerApis
import rikka.parcelablelist.ParcelableListSlice

class ShizukuServiceIntercept(val shizukuIntercept: ShizukuIntercept) : IShizukuService.Stub() {
    companion object {
        val LOGGER = Logger("ShizukuServiceIntercept")
    }

    override fun getVersion(): Int {
        return 13
    }

    override fun getUid(): Int {
        return shizukuIntercept.getServerInfo().uid
    }

    override fun checkPermission(permission: String?): Int {
        return shizukuIntercept.checkPermission(permission)
    }

    override fun newProcess(
        cmd: Array<out String?>?,
        env: Array<out String?>?,
        dir: String?
    ): IRemoteProcess {
        return shizukuIntercept.newProcess(cmd, env ?: shizukuIntercept.getEnvironment(TYPE_ENV).env, dir)
    }

    override fun getSELinuxContext(): String {
        return shizukuIntercept.getServerInfo().selinuxContext
    }

    override fun getSystemProperty(
        name: String?,
        defaultValue: String?
    ): String {
        return shizukuIntercept.getSystemProperty(name, defaultValue)
    }

    override fun setSystemProperty(name: String?, value: String?) {
        shizukuIntercept.setSystemProperty(name, value)
    }

    override fun addUserService(
        conn: IShizukuServiceConnection?,
        args: Bundle?
    ): Int {
        return shizukuIntercept.addUserService(conn, args)
    }

    override fun removeUserService(
        conn: IShizukuServiceConnection?,
        args: Bundle?
    ): Int {
        return shizukuIntercept.removeUserService(conn, args)
    }

    override fun requestPermission(requestCode: Int) {
        return shizukuIntercept.requestPermission(requestCode)
    }

    override fun checkSelfPermission(): Boolean {
        return shizukuIntercept.checkSelfPermission()
    }

    override fun shouldShowRequestPermissionRationale(): Boolean {
        return shizukuIntercept.shouldShowRequestPermissionRationale()
    }

    override fun attachApplication(
        application: IShizukuApplication?,
        args: Bundle?
    ) {
        shizukuIntercept.attachApplication(application, args)
    }

    override fun exit() {
        val callingUid = getCallingUid()
        val userId = UserHandleCompat.getUserId(callingUid)
        LOGGER.i("exit: CallingUid:%s, UserId:%s", callingUid, userId)
        val packages = PackageManagerApis.getPackagesForUidNoThrow(callingUid)
        shizukuIntercept.enableShizukuService(false)
        if (packages.size == 1) {
            LOGGER.i("exit: Force Stop %s", packages[0])
            ActivityManagerApis.forceStopPackageNoThrow(packages[0], userId)
        }
    }

    override fun attachUserService(binder: IBinder?, options: Bundle) {
        shizukuIntercept.attachUserService(binder, options)
    }

    override fun dispatchPackageChanged(intent: Intent?) {
        shizukuIntercept.dispatchPackageChanged(intent)
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
        shizukuIntercept.dispatchPermissionConfirmationResult(requestUid, requestPid, requestCode, data)
    }

    override fun getFlagsForUid(uid: Int, mask: Int): Int {
        return shizukuIntercept.getFlagsForUid(uid, mask)
    }

    override fun updateFlagsForUid(uid: Int, mask: Int, value: Int) {
        shizukuIntercept.updateFlagsForUid(uid, mask, value)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        when (code) {
            ServerConstants.BINDER_TRANSACTION_getApplications -> {
                data.enforceInterface(AxeronApiConstant.server.SHIZUKU_BINDER_DESCRIPTOR)
                val userId = data.readInt()
                val result: ParcelableListSlice<PackageInfo?> = shizukuIntercept.getApplications(userId)
                reply!!.writeNoException()
                result.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE)
                return true
            }
            BINDER_TRANSACTION_transact -> {
                data.enforceInterface(AxeronApiConstant.server.SHIZUKU_BINDER_DESCRIPTOR)
                shizukuIntercept.transactRemote(data, reply, flags)
                return true
            }
            14 -> {
                data.enforceInterface(AxeronApiConstant.server.SHIZUKU_BINDER_DESCRIPTOR)
                val binder = data.readStrongBinder()
                val packageName = data.readString()
                val args = Bundle()
                args.putString(ATTACH_APPLICATION_PACKAGE_NAME, packageName)
                args.putInt(ATTACH_APPLICATION_API_VERSION, -1)
                attachApplication(IShizukuApplication.Stub.asInterface(binder), args)
                reply!!.writeNoException()
                return true
            }
            else -> return super.onTransact(code, data, reply, flags)
        }
    }
}