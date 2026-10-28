package frb.axeron.server

import android.content.pm.PackageInfo
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import frb.axeron.server.util.Logger
import moe.shizuku.server.IShizukuApplication
import moe.shizuku.server.IShizukuService
import rikka.parcelablelist.ParcelableListSlice

/**
 * The server as one app sees it: minted for [uid] alone. The manager holds the server itself;
 * every other app gets one of these: pushed once it is allowed, or from the manager's relay to
 * ask for that (see [ASKING]).
 *
 * Every transaction must come from [uid] (one handed on is no use to anyone else) and, past
 * asking, find its grant still there: a revoke in the manager, a denial, or another app on the
 * uid (see AxeronConfigManager.find) ends it at the next call. Calls go on to [service] on the
 * same thread, so its own gates still see the caller. The manager's own methods aren't served.
 */
class ClientCapability(private val service: AxeronService, val uid: Int) : IAxeronService.Stub() {

    companion object {
        private val LOGGER = Logger("ClientCapability")

        /**
         * What a uid not allowed yet may call: attach (for the answer), ask, and see where it
         * stands. Only the manager's relay hands out such a capability, to a client that already
         * named the manager; nothing else reaches an app before it is allowed.
         */
        private val ASKING = setOf(
            TRANSACTION_attachApplication,
            TRANSACTION_requestPermission,
            TRANSACTION_checkSelfPermission,
            TRANSACTION_shouldShowRequestPermissionRationale,
            TRANSACTION_getServerInfo,
        )
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        // INTERFACE_TRANSACTION only reports the descriptor.
        if (code != INTERFACE_TRANSACTION) {
            val caller = Binder.getCallingUid()
            if (caller != uid) {
                LOGGER.w("capability of uid=%d called by uid=%d, refused", uid, caller)
                throw SecurityException("This capability belongs to another app")
            }
            if (code !in ASKING && !isAllowed()) {
                throw SecurityException("Not allowed by AxManager")
            }
        }
        return super.onTransact(code, data, reply, flags)
    }

    /**
     * Allowed for good, or once: a one-time grant lives on the uid's attached clients, until
     * their process dies.
     */
    private fun isAllowed(): Boolean =
        service.configManager.find(uid)?.isAllowed == true ||
            service.clientManager.findClients(uid).any { it.allowed }

    private fun managerOnly(func: String): Nothing =
        throw SecurityException("$func is the manager's")

    // ---- served ----

    override fun getServerInfo(): ServerInfo = service.serverInfo

    override fun getFileService(): IFileService? = service.fileService

    override fun getPackages(flags: Int): ParcelableListSlice<PackageInfo?>? = service.getPackages(flags)

    override fun getPlugins(): ParcelableListSlice<PluginInfo?>? = service.plugins

    override fun getPluginById(id: String): PluginInfo? = service.getPluginById(id)

    override fun getShizukuService(): IShizukuService? = service.shizukuService

    override fun getEnvironment(envType: Int): Environment = service.getEnvironment(envType)

    override fun requestPermission(requestCode: Int) = service.requestPermission(requestCode)

    override fun checkSelfPermission(): Boolean = service.checkSelfPermission()

    override fun shouldShowRequestPermissionRationale(): Boolean =
        service.shouldShowRequestPermissionRationale()

    override fun attachApplication(application: IShizukuApplication?, args: Bundle?) =
        service.attachApplication(application, args)

    override fun checkPermission(permission: String?): Int = service.checkPermission(permission)

    override fun isWakeLockHeld(): Boolean = service.isWakeLockHeld

    override fun newAxshProcess(
        cmd: Array<out String?>?,
        env: Array<out String?>?,
        dir: String?,
        stdin: ParcelFileDescriptor?,
        stdout: ParcelFileDescriptor?,
        stderr: ParcelFileDescriptor?,
        pty: Boolean,
        size: Long,
        status: ParcelFileDescriptor?
    ): IAxshProcess = service.newAxshProcess(cmd, env, dir, stdin, stdout, stderr, pty, size, status)

    override fun openAxshChannel(): ParcelFileDescriptor = service.openAxshChannel()

    override fun getAxshSocketName(): String? = service.axshSocketName

    // ---- the manager's ----

    override fun isFirstInit(markAsFirstInit: Boolean): Boolean = managerOnly("isFirstInit")

    override fun enableShizukuService(enable: Boolean) = managerOnly("enableShizukuService")

    override fun setNewEnvironment(env: Environment?) = managerOnly("setNewEnvironment")

    override fun attachUserService(binder: IBinder?, options: Bundle?) = managerOnly("attachUserService")

    override fun dispatchPermissionConfirmationResult(
        requestUid: Int, requestPid: Int, requestCode: Int, data: Bundle?
    ) = managerOnly("dispatchPermissionConfirmationResult")

    override fun getFlagsForUid(uid: Int, mask: Int): Int = managerOnly("getFlagsForUid")

    override fun getFlagsForUids(uids: IntArray?, mask: Int): IntArray = managerOnly("getFlagsForUids")

    override fun updateFlagsForUid(uid: Int, mask: Int, value: Int) = managerOnly("updateFlagsForUid")

    override fun setPreferSystemBinaries(prefer: Boolean) = managerOnly("setPreferSystemBinaries")

    override fun setWakeLock(enabled: Boolean) = managerOnly("setWakeLock")

    override fun getClientCapability(uid: Int): IBinder? = managerOnly("getClientCapability")

    override fun exit() = managerOnly("exit")
}
