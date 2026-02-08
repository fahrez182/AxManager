package frb.axeron.server

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.ActivityThread
import android.content.Context
import android.content.ContextHidden
import android.content.IContentProvider
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.UserInfo
import android.ddm.DdmHandleAppName
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.IPowerManager
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.os.PowerManager
import android.os.RemoteException
import android.os.SELinux
import android.os.ServiceManager
import android.os.SystemClock
import android.os.UserHandle
import android.os.UserHandleHidden
import android.system.Os
import dev.rikka.tools.refine.Refine
import frb.axeron.server.ServerConstants.MANAGER_APPLICATION_ID
import frb.axeron.server.ServerConstants.PERMISSION
import frb.axeron.server.ServerConstants.SHIZUKU_MANAGER_APPLICATION_ID
import frb.axeron.server.api.RemoteProcessHolder
import frb.axeron.server.util.HandlerUtil
import frb.axeron.server.util.IContentProviderCompat
import frb.axeron.server.util.OsUtils
import frb.axeron.server.util.UserHandleCompat
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.AxeronApiConstant.server.BINDER_DESCRIPTOR
import frb.axeron.shared.AxeronApiConstant.server.TYPE_ENV
import frb.axeron.shared.AxeronApiConstant.server.TYPE_NEW_ENV
import frb.axeron.shared.AxeronApiConstant.server.VERSION_CODE
import frb.axeron.shared.AxeronApiConstant.server.VERSION_NAME
import frb.axeron.shared.PathHelper
import frb.axeron.shared.ShizukuApiConstant.ATTACH_APPLICATION_API_VERSION
import frb.axeron.shared.ShizukuApiConstant.ATTACH_APPLICATION_PACKAGE_NAME
import frb.axeron.shared.ShizukuApiConstant.BIND_APPLICATION_PERMISSION_GRANTED
import frb.axeron.shared.ShizukuApiConstant.BIND_APPLICATION_SERVER_PATCH_VERSION
import frb.axeron.shared.ShizukuApiConstant.BIND_APPLICATION_SERVER_SECONTEXT
import frb.axeron.shared.ShizukuApiConstant.BIND_APPLICATION_SERVER_UID
import frb.axeron.shared.ShizukuApiConstant.BIND_APPLICATION_SERVER_VERSION
import frb.axeron.shared.ShizukuApiConstant.BIND_APPLICATION_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE
import frb.axeron.shared.ShizukuApiConstant.REQUEST_PERMISSION_REPLY_ALLOWED
import frb.axeron.shared.ShizukuApiConstant.REQUEST_PERMISSION_REPLY_IS_ONETIME
import frb.axeron.shared.ShizukuApiConstant.SHIZUKU_SERVER_PATCH_VERSION
import frb.axeron.shared.ShizukuApiConstant.SHIZUKU_SERVER_VERSION
import moe.shizuku.api.BinderContainer
import moe.shizuku.server.IRemoteProcess
import moe.shizuku.server.IShizukuApplication
import moe.shizuku.server.IShizukuService
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.DeviceIdleControllerApis
import rikka.hidden.compat.PackageManagerApis
import rikka.hidden.compat.PermissionManagerApis
import rikka.hidden.compat.UserManagerApis
import rikka.hidden.compat.util.SystemServiceBinder
import rikka.parcelablelist.ParcelableListSlice
import rikka.rish.RishConfig
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.system.exitProcess


open class AxeronService() :
    Service<AxeronUserServiceManager, AxeronClientManager, AxeronConfigManager>() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DdmHandleAppName.setAppName("axeron_server", 0)
            RishConfig.setLibraryPath(System.getProperty("axeron.library.path"))

            @Suppress("DEPRECATION")
            Looper.prepareMainLooper()
            AxeronService()
            Looper.loop()
        }

        fun waitSystemService(name: String) {
            while (ServiceManager.getService(name) == null) {
                try {
                    LOGGER.i("service $name is not started, wait 1s.")
                    Thread.sleep(1000)
                } catch (it: Throwable) {
                    LOGGER.e("waitSystemService failed", it)
                }
            }
        }

        val powerManager: SystemServiceBinder<IPowerManager> by lazy {
            SystemServiceBinder<IPowerManager>(
                "power", IPowerManager.Stub::asInterface
            )
        }

        @JvmStatic
        fun getManagerApplicationInfo(): ApplicationInfo? {
            return PackageManagerApis.getApplicationInfoNoThrow(MANAGER_APPLICATION_ID, 0, 0)
        }

        private fun getShizukuManagerApplicationInfo(): ApplicationInfo? {
            return PackageManagerApis.getApplicationInfoNoThrow(
                SHIZUKU_MANAGER_APPLICATION_ID,
                0,
                0
            )
        }

        fun sendBinderToClient(binder: IBinder, userId: Int) {
            try {
                for (pi in PackageManagerApis.getInstalledPackagesNoThrow(
                    PackageManager.GET_PERMISSIONS.toLong(),
                    userId
                )) {
                    if (pi == null || pi.requestedPermissions == null)
                        continue

                    if ((pi.requestedPermissions as Array<out Any?>).contains(PERMISSION)) {
                        sendBinderToUserApp(binder, pi.packageName, userId)
                    }
                }
            } catch (it: Throwable) {
                LOGGER.e("sendBinderToClient failed", it)
            }
        }

        fun sendBinderToManager(binder: IBinder) {
            for (userId in UserManagerApis.getUserIdsNoThrow()) {
                sendBinderToManager(binder, userId)
            }
        }

        fun sendBinderToShizukuManager(binder: IBinder) {
            for (userId in UserManagerApis.getUserIdsNoThrow()) {
                sendBinderToShizukuManager(binder, userId)
            }
        }

        @JvmStatic
        fun sendBinderToManager(binder: IBinder, userId: Int) {
            sendBinderToUserApp(binder, MANAGER_APPLICATION_ID, userId)
        }

        @JvmStatic
        fun sendBinderToShizukuManager(binder: IBinder, userId: Int) {
            sendBinderToUserApp(binder, SHIZUKU_MANAGER_APPLICATION_ID, userId)
        }

        @JvmStatic
        fun sendBinderToUserApp(binder: IBinder, packageName: String, userId: Int) {
            sendBinderToUserApp(binder, packageName, userId, true)
        }

        fun sendBinderToUserApp(binder: IBinder, packageName: String, userId: Int, retry: Boolean) {
            try {
                DeviceIdleControllerApis.addPowerSaveTempWhitelistApp(
                    packageName, 30 * 1000, userId,
                    316/* PowerExemptionManager#REASON_SHELL */, "shell"
                )
                LOGGER.v("Add %d:%s to power save temp whitelist for 30s", userId, packageName)
            } catch (it: Throwable) {
                LOGGER.e(
                    it,
                    "Failed to add %d:%s to power save temp whitelist",
                    userId,
                    packageName
                )
            }

            var name: String
            var extraBinder: String
            if (packageName == MANAGER_APPLICATION_ID) {
                name = "$packageName.server"
                extraBinder = "AxServer.BINDER"
            } else {
                name = "$packageName.shizuku"
                extraBinder = "moe.shizuku.privileged.api.intent.extra.BINDER"
            }
            var provider: IContentProvider?
            val token: IBinder? = null

            try {
                provider =
                    ActivityManagerApis.getContentProviderExternal(name, userId, token, name)
                if (provider == null) {
                    LOGGER.e("provider is null %s %d", name, userId)
                    return
                }
                if (!provider.asBinder().pingBinder()) {
                    LOGGER.e("provider is dead %s %d", name, userId)

                    if (retry) {
                        ActivityManagerApis.forceStopPackageNoThrow(packageName, userId)
                        LOGGER.e("kill %s in user %d and try again", packageName, userId)
                        Thread.sleep(1000)
                        sendBinderToUserApp(binder, packageName, userId, false)
                    }
                    return
                }

                if (!retry) {
                    LOGGER.e("retry works")
                }

                val extra = Bundle().apply {
                    putParcelable(extraBinder, BinderContainer(binder))
                }
                IContentProviderCompat.call(provider, null, null, name, "sendBinder", null, extra)
                LOGGER.i("send binder to user app %s in user %d", packageName, userId)
            } catch (it: Throwable) {
                LOGGER.e(it, "failed send binder to user app %s in user %d", packageName, userId)
            } finally {
                try {
                    ActivityManagerApis.removeContentProviderExternal(name, token)
                } catch (tr: Throwable) {
                    LOGGER.w(tr, "removeContentProviderExternal")
                }
            }
        }

        var cachedDefaultEnv: Environment? = null

        fun getDefaultEnvironment(): Environment {
            val cached = cachedDefaultEnv
            if (cached != null) return cached

            val built = Environment.Builder(false)
                .put("AXERON", "true")
                .put("HOSTNAME", "axeron")
                .put(
                    "AXERONDIR",
                    PathHelper.getWorkingPath(isRoot, AxeronApiConstant.folder.PARENT).absolutePath
                )
                .put(
                    "AXERONBIN",
                    PathHelper.getWorkingPath(
                        isRoot,
                        AxeronApiConstant.folder.PARENT_BINARY
                    ).absolutePath
                )
                .put(
                    "AXERONXBIN",
                    PathHelper.getWorkingPath(
                        isRoot,
                        AxeronApiConstant.folder.PARENT_EXTERNAL_BINARY
                    ).absolutePath
                )
                .put("AXERONLIB", getManagerApplicationInfo()?.nativeLibraryDir)
                .put("AXERONVER", VERSION_CODE.toString())
                .put(
                    "TMPDIR",
                    PathHelper.getTmpPath(AxeronApiConstant.folder.PARENT_CACHE).absolutePath
                )
                .put("PATH", $$"$AXERONXBIN:$PATH:$AXERONBIN")
                .build()
            cachedDefaultEnv = built
            return built
        }
    }

    val lockToken = Binder()

    fun acquire() {
        LOGGER.i("Acquire wakelock")
        try {
            val flags = PowerManager.PARTIAL_WAKE_LOCK
            val tag = "axeron::wakelock"
            val pkg = "axeron_server"
            val uid = Os.getuid()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                powerManager.get().acquireWakeLockWithUid(lockToken, flags, tag, pkg, uid, 0, null)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                powerManager.get().acquireWakeLockWithUid(lockToken, flags, tag, pkg, uid, 0)
            } else {
                powerManager.get().acquireWakeLockWithUid(lockToken, flags, tag, pkg, uid)
            }
            LOGGER.i("Acquire wakelock success")
        } catch (e: Exception) {
            LOGGER.e("Acquire wakelock failed", e)
        }
    }

    fun release() {
        LOGGER.i("Release wakelock")
        try {
            powerManager.get().releaseWakeLock(lockToken, 0)
            LOGGER.i("Release wakelock success")
        } catch (e: Exception) {
            LOGGER.e("Release wakelock failed", e)
        }
    }

    override fun onCreateUserServiceManager(): AxeronUserServiceManager {
        return AxeronUserServiceManager(getEnvironment(TYPE_ENV)?.getEnv())
    }

    override fun onCreateClientManager(): AxeronClientManager {
        return AxeronClientManager(configManager)
    }

    override fun onCreateConfigManager(): AxeronConfigManager {
        return AxeronConfigManager()
    }

    @Synchronized
    fun checkCaller(callingUid: Int): Boolean {
        val managerAppUid: Int =
            getManagerApplicationInfo()?.uid ?: exitProcess(ServerConstants.MANAGER_APP_NOT_FOUND)

        val shizukuManagerUid: Int = getShizukuManagerApplicationInfo()?.uid ?: managerAppUid
        return UserHandleCompat.getAppId(callingUid) == managerAppUid || UserHandleCompat.getAppId(
            callingUid
        ) == shizukuManagerUid
    }

    override fun checkCallerManagerPermission(
        func: String?, callingUid: Int, callingPid: Int
    ): Boolean {
        return checkCaller(callingUid)
    }

    override fun checkCallerPermission(
        func: String?,
        callingUid: Int,
        callingPid: Int,
        clientRecord: ClientRecord?
    ): Boolean {
        if (checkCaller(callingUid)) {
            return true
        }
        return clientRecord == null
    }

    private val starting: Long = SystemClock.elapsedRealtime()

    var shizuku: ShizukuServiceIntercept? = null

    val axCompanion =
        File(
            PathHelper.getWorkingPath(isRoot, AxeronApiConstant.folder.PARENT),
            "ax_perm_companion"
        )


    init {

        HandlerUtil.setMainHandler(mainHandler)

        waitSystemService("package")
        waitSystemService(Context.ACTIVITY_SERVICE)
        waitSystemService(Context.USER_SERVICE)
        waitSystemService(Context.APP_OPS_SERVICE)
        waitSystemService(Context.POWER_SERVICE)

        getManagerApplicationInfo() ?: exitProcess(ServerConstants.MANAGER_APP_NOT_FOUND)

        if (axCompanion.exists()) {
            shizuku = ShizukuServiceIntercept(this)
        }

        // make ApkChangedObservers lazy or start on-demand; and keep reference to listener so you can stop it
//        val apkObserver = ApkChangedListener {
//            if (getManagerApplicationInfo() == null) exitProcess(ServerConstants.MANAGER_APP_NOT_FOUND)
//        }
//        ApkChangedObservers.start(ai.sourceDir, mainHandler, apkObserver)


        BinderSender.register(asInterface(this))

        mainHandler.post {
            sendBinderToClient()
            sendBinderToManager()
        }

        acquire()
    }

    fun sendBinderToClient() {
        shizukuService?.let {
            for (userId in UserManagerApis.getUserIdsNoThrow()) {
                sendBinderToClient(it.asBinder(), userId)
            }
        }
    }

    fun sendBinderToManager() {
        sendBinderToManager(this)
        shizukuService?.let {
            sendBinderToShizukuManager(it.asBinder())
        }
    }

    override fun enableShizukuService(enable: Boolean) {
        if (enable) {
            if (axCompanion.createNewFile()) {
                LOGGER.i("AX-Scope")
                shizuku = ShizukuServiceIntercept(this)
            }
        } else {
            if (axCompanion.delete()) {
                userServiceManager.removeAllUserService()
                shizuku = null
            }
        }
        sendBinderToManager()
    }

    @Volatile
    private var cachedMergedEnv: Environment? = null

    @Volatile
    private var cachedNewEnv: Environment? = null
    private fun getMergedEnvironment(): Environment {
        val cached = cachedMergedEnv
        if (cached != null) return cached

        // avoid extra temporary HashMap allocations — rely on Builder to copy if needed
        val merged = Environment.Builder(true)
            .putAll(getDefaultEnvironment().envMap) // prefer to pass the map directly
            .putAll(environmentManager.getAll())    // avoid new HashMap(...)
            .build()

        cachedMergedEnv = merged
        return merged
    }

    private fun getNewEnvironment(): Environment {
        val cached = cachedNewEnv
        if (cached != null) return cached

        // if Environment accepts a Map and you can keep it read-only, pass asMap() / toMap()
        val newEnv = Environment(HashMap(environmentManager.getAll()), true)
        cachedNewEnv = newEnv
        return newEnv
    }


    override fun getEnvironment(envType: Int): Environment {
        return when (envType) {
            TYPE_ENV -> getMergedEnvironment()
            TYPE_NEW_ENV -> getNewEnvironment()
            else -> getDefaultEnvironment()
        }
    }

    override fun setNewEnvironment(env: Environment) {
        environmentManager.replaceAllBlocking(HashMap(env.envMap)) // manager needs mutability
        // invalidate cached refs deterministically
        cachedMergedEnv = null
        cachedNewEnv = null
        userServiceManager.environment =
            getEnvironment(TYPE_ENV)?.getEnv()
    }

    fun checkRuntime(): Boolean {
        return checkPermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED
    }

    @Throws(RemoteException::class)
    override fun getShizukuService(): IShizukuService? {
        return shizuku
    }

    private var context: WeakReference<Context>? = null

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun getContext(): Context {
        if (context == null || context!!.get() == null) {
            val activityThread = ActivityThread.systemMain()
            val systemContext: Context? = activityThread.systemContext

            val userHandle = Refine.unsafeCast<UserHandle?>(
                UserHandleHidden.of(0)
            )
            try {
                context = WeakReference<Context>(
                    Refine.unsafeCast<ContextHidden>(systemContext).createPackageContextAsUser(
                        MANAGER_APPLICATION_ID,
                        Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY,
                        userHandle
                    )
                )
            } catch (e: PackageManager.NameNotFoundException) {
                throw RuntimeException(e)
            }
        }
        return context!!.get()!!
    }

    override fun newProcess(
        cmd: Array<out String?>?,
        env: Array<out String?>?,
        dir: String?
    ): IRemoteProcess {
        enforceCallingPermission("newProcess")

        LOGGER.d(
            "newProcess: uid=%d, cmd=%s",
            getCallingUid(),
            cmd.contentToString()
        )

        val process: Process?
        try {
            process = Runtime.getRuntime().exec(cmd, env, if (dir != null) File(dir) else null)
        } catch (e: IOException) {
            throw IllegalStateException(e.message)
        }

        val clientRecord = clientManager.findClient(getCallingUid(), getCallingPid())
        val token = clientRecord?.client?.asBinder()

        return RemoteProcessHolder(process, token)
    }

    override fun getServerInfo(): ServerInfo {
        return ServerInfo(
            VERSION_NAME,
            VERSION_CODE,
            Os.getuid(),
            Os.getpid(),
            SELinux.getContext(),
            starting,
            checkRuntime()
        )
    }

    override fun showPermissionConfirmation(
        requestCode: Int,
        clientRecord: ClientRecord?,
        callingUid: Int,
        callingPid: Int,
        userId: Int
    ) {

        LOGGER.i("showPermissionConfirmation")
        val ai = if (clientRecord != null) {
            PackageManagerApis.getApplicationInfoNoThrow(clientRecord.packageName, 0, userId)
                ?: return
        } else {
            val packageEntry = configManager.findOrUpdate(callingUid)
            LOGGER.d(
                "showPermissionConfirmation pe:%s, pkgs:%s",
                packageEntry,
                packageEntry?.packages
            )
            if (packageEntry != null && !packageEntry.packages.isNullOrEmpty()) {
                if (packageEntry.packages.size > 1) throw IllegalStateException("This uid should not have multiple packages")
                PackageManagerApis.getApplicationInfoNoThrow(
                    packageEntry.packages.get(0),
                    0,
                    userId
                )
                    ?: return
            } else return
        }
        LOGGER.i("showPermissionConfirmation: %s", ai)

        val pi = PackageManagerApis.getPackageInfoNoThrow(MANAGER_APPLICATION_ID, 0, userId)
        val userInfo = UserManagerApis.getUserInfo(userId)
        var isWorkProfileUser = (userInfo.flags and UserInfo.FLAG_MANAGED_PROFILE) != 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isWorkProfileUser = "android.os.usertype.profile.MANAGED" == userInfo.userType
        }
        if (pi == null && !isWorkProfileUser) {
            LOGGER.w(
                "Manager not found in non work profile user %d. Revoke permission",
                userId
            )
            clientRecord?.dispatchRequestPermissionResult(requestCode, false)
            return
        }

        LOGGER.i("Requesting Permission")
        val intent = Intent(ServerConstants.REQUEST_PERMISSION_ACTION)
            .setPackage(MANAGER_APPLICATION_ID)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            .putExtra("uid", callingUid)
            .putExtra("pid", callingPid)
            .putExtra("requestCode", requestCode)
            .putExtra("applicationInfo", ai)
        ActivityManagerApis.startActivityNoThrow(intent, null, if (isWorkProfileUser) 0 else userId)
    }

    @Throws(RemoteException::class)
    override fun dispatchPermissionConfirmationResult(
        requestUid: Int,
        requestPid: Int,
        requestCode: Int,
        data: Bundle?
    ) {
        if (!checkCaller(getCallingUid())) {
            LOGGER.w("dispatchPermissionConfirmationResult called not from the manager package")
            return
        }

        if (data == null) {
            return
        }

        val allowed = data.getBoolean(REQUEST_PERMISSION_REPLY_ALLOWED)
        val onetime = data.getBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME)

        LOGGER.i(
            "dispatchPermissionConfirmationResult: uid=%d, pid=%d, requestCode=%d, allowed=%s, onetime=%s",
            requestUid, requestPid, requestCode, allowed.toString(), onetime.toString()
        )

        val records: MutableList<ClientRecord> =
            clientManager.findClients(requestUid)
        val packages: MutableList<String?> = ArrayList()
        if (records.isEmpty()) {
            LOGGER.w(
                "dispatchPermissionConfirmationResult: no client for uid %d was found",
                requestUid
            )
        } else {
            for (record in records) {
                packages.add(record.packageName)
                record.allowed = allowed
                if (record.pid == requestPid) {
                    record.dispatchRequestPermissionResult(requestCode, allowed)
                    if (!allowed) {
                        onPermissionRevoked(record.packageName)
                    }
                }
            }
        }


        if (!onetime) {
            configManager.update(
                requestUid,
                packages,
                ConfigManager.MASK_PERMISSION,
                if (allowed) ConfigManager.FLAG_ALLOWED else ConfigManager.FLAG_DENIED
            )
        }
    }

    @Synchronized
    private fun onPermissionRevoked(packageName: String?) {
        userServiceManager.removeUserServicesForPackage(packageName)
    }

    override fun attachApplication(application: IShizukuApplication?, args: Bundle?) {
        if (application == null || args == null) {
            return
        }

        val requestPackageName: String =
            args.getString(ATTACH_APPLICATION_PACKAGE_NAME) ?: return
        val apiVersion = args.getInt(ATTACH_APPLICATION_API_VERSION, -1)

        val callingPid = getCallingPid()
        val callingUid = getCallingUid()
        var clientRecord: ClientRecord? = null

        val packages = PackageManagerApis.getPackagesForUidNoThrow(callingUid)
        if (!packages.contains(requestPackageName)) {
            LOGGER.w("Request package " + requestPackageName + "does not belong to uid " + callingUid)
            throw SecurityException("Request package " + requestPackageName + "does not belong to uid " + callingUid)
        }

        val isManager: Boolean = MANAGER_APPLICATION_ID == requestPackageName

        if (clientManager.findClient(callingUid, callingPid) == null) {
            synchronized(this) {
                clientRecord = clientManager.addClient(
                    callingUid,
                    callingPid,
                    application,
                    requestPackageName,
                    apiVersion
                )
            }
            if (clientRecord == null) {
                LOGGER.w("Add client failed")
                return
            }
        }

        LOGGER.d(
            "attachApplication: %s %d %d",
            requestPackageName,
            callingUid,
            callingPid
        )

        var replyServerVersion = SHIZUKU_SERVER_VERSION
        if (apiVersion == -1) {
            // ShizukuBinderWrapper has adapted API v13 in dev.rikka.shizuku:api 12.2.0, however
            // attachApplication in 12.2.0 is still old, so that server treat the client as pre 13.
            // This finally cause transactRemote fails.
            // So we can pass 12 here to pretend we are v12 server.
            replyServerVersion = 12
        }

        val reply = Bundle()
        reply.putInt(BIND_APPLICATION_SERVER_UID, OsUtils.getUid())
        reply.putInt(BIND_APPLICATION_SERVER_VERSION, replyServerVersion)
        reply.putString(
            BIND_APPLICATION_SERVER_SECONTEXT,
            OsUtils.getSELinuxContext()
        )
        reply.putInt(
            BIND_APPLICATION_SERVER_PATCH_VERSION,
            SHIZUKU_SERVER_PATCH_VERSION
        )
        if (!isManager) {
            reply.putBoolean(
                BIND_APPLICATION_PERMISSION_GRANTED,
                clientRecord?.allowed ?: false
            )
            reply.putBoolean(
                BIND_APPLICATION_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE,
                false
            )
        } else {
            try {
                PermissionManagerApis.grantRuntimePermission(
                    MANAGER_APPLICATION_ID,
                    WRITE_SECURE_SETTINGS, UserHandleCompat.getUserId(callingUid)
                )
            } catch (e: RemoteException) {
                LOGGER.w(e, "grant WRITE_SECURE_SETTINGS")
            }
        }
        try {
            application.bindApplication(reply)
        } catch (e: Throwable) {
            LOGGER.w(e, "attachApplication")
        }
    }

    @Throws(RemoteException::class)
    override fun dispatchPackageChanged(intent: Intent?) {
    }

    override fun exit() {
        release()
        super.exit()
    }

    override fun getApplications(userId: Int): ParcelableListSlice<PackageInfo?> {
        val list = ArrayList<PackageInfo?>()

        val users: ArrayList<Int> = ArrayList()
        if (userId == -1) {
            users.addAll(UserManagerApis.getUserIdsNoThrow())
        } else {
            users.add(userId)
        }

        for (user in users) {
            val packages = PackageManagerApis.getInstalledPackagesNoThrow(
                (PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS).toLong(),
                user
            )

            for (pi in packages) {
                if (pi.packageName == MANAGER_APPLICATION_ID) continue

                val appInfo = pi.applicationInfo ?: continue
                val uid = appInfo.uid

                val entry = configManager.find(uid)
                val flags = entry?.let {
                    if (it.packages != null && !it.packages.contains(pi.packageName)) {
                        return@let 0 // skip by flags=0
                    }
                    it.flags and ConfigManager.MASK_PERMISSION
                } ?: 0

                when {
                    flags != 0 -> {
                        list.add(pi)
                    }

                    appInfo.metaData?.getBoolean("moe.shizuku.client.V3_SUPPORT", false) == true &&
                            pi.requestedPermissions?.contains(PERMISSION) == true -> {
                        list.add(pi)
                    }
                }
            }
        }

        return ParcelableListSlice(list)
    }


    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == ServerConstants.BINDER_TRANSACTION_getApplications) {
            data.enforceInterface(BINDER_DESCRIPTOR)
            val userId = data.readInt()
            val result: ParcelableListSlice<PackageInfo?> = getApplications(userId)
            reply!!.writeNoException()
            result.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE)
            return true
        }
        return super.onTransact(code, data, reply, flags)
    }

    @Synchronized
    private fun getFlagsForUidInternal(uid: Int, mask: Int): Int {
        val entry: AxeronConfig.PackageEntry? = configManager.find(uid)
        if (entry != null) {
            return entry.flags and mask
        }
        return 0
    }

    override fun getFlagsForUid(uid: Int, mask: Int): Int {
        if (!checkCaller(getCallingUid())) {
            LOGGER.w("updateFlagsForUid is allowed to be called only from the manager")
            return 0
        }
        return getFlagsForUidInternal(uid, mask)
    }

    @Throws(RemoteException::class)
    override fun updateFlagsForUid(uid: Int, mask: Int, value: Int) {
        if (!checkCaller(getCallingUid())) {
            LOGGER.w("updateFlagsForUid is allowed to be called only from the manager")
            return
        }

        if ((mask and ConfigManager.MASK_PERMISSION) != 0) {
            val allowed = (value and ConfigManager.FLAG_ALLOWED) != 0
            (value and ConfigManager.FLAG_DENIED) != 0

            val records: MutableList<ClientRecord> =
                clientManager.findClients(uid)
            for (record in records) {
                if (allowed) {
                    record.allowed = true
                } else {
                    record.allowed = false
                    ActivityManagerApis.forceStopPackageNoThrow(
                        record.packageName,
                        UserHandleCompat.getUserId(record.uid)
                    )
                    onPermissionRevoked(record.packageName)
                }
            }
        }

        configManager.update(uid, null, mask, value)
    }
}