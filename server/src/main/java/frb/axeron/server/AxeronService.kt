package frb.axeron.server

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
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
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.os.PowerManager
import android.os.RemoteException
import android.os.SELinux
import android.os.SystemProperties
import android.os.ServiceManager
import android.os.SystemClock
import android.system.Os
import frb.axeron.server.ServerConstants.MANAGER_APPLICATION_ID
import frb.axeron.server.ServerConstants.PERMISSION
import frb.axeron.server.ServerConstants.SHIZUKU_MANAGER_APPLICATION_ID
import frb.axeron.server.api.RemoteProcessHolder
import frb.axeron.server.axsh.AxshServer
import frb.axeron.server.util.HandlerUtil
import frb.axeron.server.util.IContentProviderCompat
import frb.axeron.server.util.OsUtils
import frb.axeron.server.util.UserHandleCompat
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.AxeronApiConstant.server.BINDER_DESCRIPTOR
import frb.axeron.shared.AxeronApiConstant.server.TYPE_ENV
import frb.axeron.shared.MiuiAppOps
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
import frb.axeron.shell.KeepAlive
import frb.axeron.shell.host.AxshProcess
import frb.axeron.shell.host.PtyHost
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess


open class AxeronService :
    Service<AxeronUserServiceManager, AxeronClientManager, AxeronConfigManager>() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DdmHandleAppName.setAppName("axeron_server", 0)
            PtyHost.setLibraryPath(System.getProperty("axeron.library.path"))

            @Suppress("DEPRECATION")
            Looper.prepareMainLooper()
            // Before the service exists: its constructor already reads the root world's configs.
            if (Os.getuid() == 0) migrateRootWorld()
            ensureCacheDir(Os.getuid() == 0)
            val service = AxeronService()
            service.startAxsh()
            Looper.loop()
        }

        /**
         * $TMPDIR (cache/) is handed to every plugin process, but nothing else creates it: made
         * here so it exists from the first start, owned by the server's uid.
         */
        private fun ensureCacheDir(isRoot: Boolean) {
            val cache = PathHelper.getWorkingPath(isRoot, AxeronApiConstant.folder.PARENT_CACHE)
            if (!cache.isDirectory && !cache.mkdirs()) {
                LOGGER.w("can't create %s, plugins get a missing TMPDIR", cache)
            }
        }

        /**
         * One-time move of the root world from its old home (/data/user_de/0/android/axeron) to
         * /data/adb/axeron. Skipped when the new folder already exists, so nothing is overwritten;
         * the old one is then left as it is.
         *
         * bin, xbin, cache and zip are dropped instead of moved: they are recreated on every start,
         * and the symlinks in bin/xbin would still point into the old folder. Renamed files keep
         * their old SELinux label, so they are relabeled to /data/adb's.
         */
        private fun migrateRootWorld() {
            val old = File(AxeronApiConstant.folder.ROOT_DE, AxeronApiConstant.folder.PARENT).absoluteFile
            val new = PathHelper.getWorkingPath(true, AxeronApiConstant.folder.PARENT).absoluteFile
            if (!old.isDirectory) return
            if (new.exists()) {
                LOGGER.w("migrate: %s already exists, old root data left at %s", new, old)
                return
            }

            val parent = new.parentFile ?: return
            if (!parent.isDirectory && !parent.mkdirs()) {
                LOGGER.e("migrate: can't create %s", parent)
                return
            }
            // Same filesystem in practice (both on /data); mv covers the rare case where not.
            if (!old.renameTo(new) && exec("mv", old.path, new.path) != 0) {
                LOGGER.e("migrate: can't move %s to %s, root data left in place", old, new)
                return
            }

            listOf(
                AxeronApiConstant.folder.BINARY,
                AxeronApiConstant.folder.EXTERNAL_BINARY,
                AxeronApiConstant.folder.CACHE,
                AxeronApiConstant.folder.ZIP
            ).forEach {
                // rm -rf never follows symlinks, unlike File.deleteRecursively.
                exec("rm", "-rf", File(new, it).path)
            }

            val label = execOutput("stat", "-c", "%C", parent.path)?.takeIf { it.startsWith("u:") }
            if (label != null) exec("chcon", "-hR", label, new.path)
            else LOGGER.w("migrate: can't read the label of %s, %s keeps its old one", parent, new)

            LOGGER.i("migrate: root world moved from %s to %s", old, new)
        }

        private fun exec(vararg cmd: String): Int = try {
            Runtime.getRuntime().exec(cmd).waitFor()
        } catch (e: Throwable) {
            LOGGER.w(e, "exec %s", cmd.joinToString(" "))
            -1
        }

        private fun execOutput(vararg cmd: String): String? = try {
            val process = Runtime.getRuntime().exec(cmd)
            val out = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0) out else null
        } catch (e: Throwable) {
            LOGGER.w(e, "exec %s", cmd.joinToString(" "))
            null
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
                        // Later, not by sleeping: callers are the main thread and the uid and
                        // process observers, whose other events would wait behind it.
                        Service.mainHandler.postDelayed({
                            sendBinderToUserApp(binder, packageName, userId, false)
                        }, 1000)
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

        @Volatile
        var cachedDefaultEnv: Environment? = null

        @Volatile
        var preferSystemBinaries: Boolean = false

        fun getDefaultEnvironment(): Environment {
            val cached = cachedDefaultEnv
            if (cached != null) return cached

            val managerInfo = getManagerApplicationInfo()
            val builder = Environment.Builder(false)
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
                .put("AXERONVER", VERSION_CODE.toString())
                .put(
                    "TMPDIR",
                    PathHelper.getWorkingPath(isRoot, AxeronApiConstant.folder.PARENT_CACHE).absolutePath
                )

            // Pastikan PATH sistem tetap ada sebagai fallback
            val systemPath = System.getenv("PATH") ?: "/sbin:/system/bin:/system/xbin"
            val axeronPath = "\$AXERONXBIN:\$AXERONBIN"
            builder.put(
                "PATH",
                if (preferSystemBinaries) "$systemPath:$axeronPath" else "$axeronPath:$systemPath"
            )

            if (managerInfo != null) {
                builder.put("AXERONLIB", managerInfo.nativeLibraryDir)
                val systemLd = System.getenv("LD_LIBRARY_PATH")
                if (systemLd.isNullOrEmpty()) {
                    builder.put("LD_LIBRARY_PATH", "\$AXERONLIB")
                } else {
                    // Taruh AXERONLIB di depan agar binari kustom kita prioritas menemukan lib-nya sendiri
                    builder.put("LD_LIBRARY_PATH", "\$AXERONLIB:\$LD_LIBRARY_PATH")
                }
            }

            val built = builder.build()
            cachedDefaultEnv = built
            return built
        }
    }

    val lockToken = Binder()

    /** Whether [lockToken] currently holds the wakelock; guarded by [lockToken]. */
    @Volatile
    private var wakeLockHeld = false

    private fun acquire(): Boolean {
        LOGGER.i("Acquire wakelock")
        return try {
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
            true
        } catch (e: Exception) {
            LOGGER.e("Acquire wakelock failed", e)
            false
        }
    }

    private fun release(): Boolean {
        LOGGER.i("Release wakelock")
        return try {
            powerManager.get().releaseWakeLock(lockToken, 0)
            LOGGER.i("Release wakelock success")
            true
        } catch (e: Exception) {
            LOGGER.e("Release wakelock failed", e)
            false
        }
    }

    /** Manager only. Asking for the state it is already in does nothing. */
    override fun setWakeLock(enabled: Boolean) {
        enforceManagerPermission("setWakeLock")
        synchronized(lockToken) {
            if (enabled == wakeLockHeld) return
            if (enabled) {
                if (acquire()) wakeLockHeld = true
            } else {
                if (release()) wakeLockHeld = false
            }
        }
    }

    override fun isWakeLockHeld(): Boolean = wakeLockHeld

    override fun onCreateUserServiceManager(): AxeronUserServiceManager {
        return AxeronUserServiceManager(getEnvironment(TYPE_ENV).env)
    }

    override fun onCreateClientManager(): AxeronClientManager {
        return AxeronClientManager(configManager)
    }

    override fun onCreateConfigManager(): AxeronConfigManager {
        return AxeronConfigManager()
    }

    // Volatile, not locked: every gated call and every axsh auth passes here. Two threads filling
    // the cache at once look up, and store, the same uid.
    @Volatile
    private var cachedManagerUid: Int = -1

    @Volatile
    private var cachedShizukuManagerUid: Int = -1

    fun checkCaller(callingUid: Int): Boolean {
        var managerUid = cachedManagerUid
        if (managerUid == -1) {
            managerUid = getManagerApplicationInfo()?.uid
                ?: exitProcess(ServerConstants.MANAGER_APP_NOT_FOUND)
            cachedManagerUid = managerUid
        }
        var shizukuManagerUid = cachedShizukuManagerUid
        if (shizukuManagerUid == -1) {
            shizukuManagerUid = getShizukuManagerApplicationInfo()?.uid ?: managerUid
            cachedShizukuManagerUid = shizukuManagerUid
        }
        val appId = UserHandleCompat.getAppId(callingUid)
        return appId == managerUid || appId == shizukuManagerUid
    }

    override fun checkCallerManagerPermission(
        func: String?, callingUid: Int, callingPid: Int
    ): Boolean {
        return checkCaller(callingUid)
    }

    private var axshServer: AxshServer? = null

    /**
     * Native `axsh` clients (uid/pid from SO_PEERCRED) reach the same gate as binder callers:
     * root/shell always, the manager app always, everything else only once granted per-uid.
     */
    private fun checkAxshCaller(uid: Int): Boolean {
        if (uid == 0 || uid == 2000) return true
        if (checkCaller(uid)) return true
        return configManager.find(uid)?.isAllowed == true
    }

    fun startAxsh() {
        val server = AxshServer(
            server = { this },
            authorize = { uid, _ -> checkAxshCaller(uid) },
            // As requestPermission does for an unattached binder caller: found by uid alone, and
            // answered right away when the manager has it denied.
            requestPermission = { uid, pid ->
                if (configManager.find(uid)?.isDenied == true) {
                    axshServer?.onPermissionResult(uid, false)
                } else {
                    showPermissionConfirmation(0, null, uid, pid, UserHandleCompat.getUserId(uid))
                }
            }
        )
        axshServer = server
        server.start()
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
        // Attached clients are validated against clientRecord.allowed by Service.enforceCallingPermission.
        if (clientRecord != null) {
            return false
        }
        // Non-attached callers (axsh brokers, unattached Shizuku clients) are only allowed once
        // their uid has been granted permission, mirroring Service.checkSelfPermission().
        if (callingUid == 0 || callingUid == 2000) {
            return true
        }
        return configManager.find(callingUid)?.isAllowed == true
    }

    private val starting: Long = SystemClock.elapsedRealtime()

    var shizuku: ShizukuServiceIntercept? = null

    val axCompanion =
        File(
            PathHelper.getWorkingPath(isRoot, AxeronApiConstant.folder.PARENT),
            "ax_perm_companion"
        )

    /**
     * Waits on a pipe whose write end only this server holds and never writes to. When the
     * server dies for any reason (exit(), a crash, SIGKILL, replaced by a new server) the kernel
     * closes it, the watchdog's read returns, and if the user asked for it (flag file
     * [AxeronApiConstant.folder.STOP_PLUGINS_WITH_SERVER], read at that moment) it stops every
     * plugin's processes through the igniter. Kept in a field: a collected Process closes the
     * pipe, which would fire the watchdog while the server is still running.
     */
    private var deathWatchdog: Process? = null

    private fun startDeathWatchdog() {
        val flag = File(
            PathHelper.getWorkingPath(isRoot, AxeronApiConstant.folder.PARENT),
            AxeronApiConstant.folder.STOP_PLUGINS_WITH_SERVER
        ).absolutePath
        // The manager's :reignite dex. An update of the manager moves its lib dir, so a server
        // replaced after one finds it gone and leaves the plugins as they are.
        val libDir = getManagerApplicationInfo()?.nativeLibraryDir ?: run {
            LOGGER.w("no manager lib dir, no death watchdog")
            return
        }
        val dex = File(libDir, "libaxreignite.so").absolutePath
        val script = $$"""
            read -r _
            [ -f "$1" ] && [ -f "$2" ] || exit 0
            CLASSPATH="$2" exec /system/bin/app_process / frb.axeron.reignite.Igniter --stop-all
        """.trimIndent()

        try {
            deathWatchdog = ProcessBuilder("/system/bin/sh", "-c", script, "axeron_watchdog", flag, dex)
                .apply {
                    // The server's env, without a plugin marker (see getMergedEnvironment).
                    environment().clear()
                    environment().putAll(getEnvironment(TYPE_ENV).envMap)
                }
                .redirectErrorStream(true)
                .redirectOutput(File("/dev/null"))
                .start()
        } catch (e: Throwable) {
            LOGGER.w(e, "start death watchdog")
        }
    }

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

        BinderSender.register(this)

        mainHandler.post {
            sendBinderToClient()
            sendBinderToManager()
        }

        // Held from the start; the manager releases it on connect if the user turned it off.
        synchronized(lockToken) { wakeLockHeld = acquire() }

        startDeathWatchdog()
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

    /**
     * Reached from the manager's toggle and from Shizuku's own "stop" (IShizukuService.exit), so
     * only the manager or the Shizuku app may switch it; any other Shizuku client is refused.
     */
    override fun enableShizukuService(enable: Boolean) {
        enforceManagerPermission("enableShizukuService")
        if (enable) {
            if (axCompanion.createNewFile()) {
                LOGGER.i("AX-Scope")
                shizuku = ShizukuServiceIntercept(this)
            }
        } else {
            if (axCompanion.delete()) {
                userServiceManager.removeAllUserService()
                // Clients keep the old binder alive; make it refuse calls instead of serving on.
                shizuku?.revoke()
                shizuku = null
            }
        }
        mainHandler.post {
            // Push the Shizuku binder to already-running client apps, not only when they (re)start.
            sendBinderToClient()
            sendBinderToManager()
        }
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
            // A plugin's process marker set in the user's env would tag every process as that
            // plugin's, so stopping it would stop them all. Only plugin code gets it.
            .remove("AXERON_PLUGIN")
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
        enforceManagerPermission("setNewEnvironment")
        environmentManager.replaceAllBlocking(HashMap(env.envMap)) // manager needs mutability
        invalidateEnvironment()
    }

    override fun setPreferSystemBinaries(prefer: Boolean) {
        enforceManagerPermission("setPreferSystemBinaries")
        preferSystemBinaries = prefer
        invalidateEnvironment()
    }

    /** Drops every cached env, and hands user services the new one (the new PATH order too). */
    private fun invalidateEnvironment() {
        cachedDefaultEnv = null
        cachedMergedEnv = null
        cachedNewEnv = null
        userServiceManager.environment = getEnvironment(TYPE_ENV).env
    }

    /**
     * App-ops the manager needs, set like WRITE_SECURE_SETTINGS is granted: WRITE_SETTINGS, and on
     * MIUI/HyperOS its own ops. Without Autostart there, a manager swiped from recents (which MIUI
     * kills) can't be woken to hand axsh the server binder. They are app-ops, not runtime
     * permissions (`pm grant` refuses them), so they go through `cmd appops`, off the binder
     * thread as that spawns a process.
     */
    private fun allowWriteSettings(userId: Int) {
        Thread {
            allowManagerAppOp(userId, "WRITE_SETTINGS")
            val miui = runCatching { SystemProperties.get(MiuiAppOps.VERSION_PROPERTY) }.getOrNull()
            if (!miui.isNullOrEmpty()) {
                MiuiAppOps.MANAGER_OPS.forEach { allowManagerAppOp(userId, it.toString()) }
            }
        }.start()
    }

    private fun allowManagerAppOp(userId: Int, op: String) {
        try {
            val code = Runtime.getRuntime().exec(
                arrayOf(
                    "cmd", "appops", "set", "--user", userId.toString(),
                    MANAGER_APPLICATION_ID, op, "allow"
                )
            ).waitFor()
            if (code != 0) LOGGER.w("allow %s exited with %d", op, code)
        } catch (e: Throwable) {
            LOGGER.w(e, "allow %s", op)
        }
    }

    // Not through checkPermission(): that one is gated, and getServerInfo is open to any caller.
    fun checkRuntime(): Boolean {
        if (isRoot) return true
        return PermissionManagerApis.checkPermission(
            "android.permission.GRANT_RUNTIME_PERMISSIONS", Os.getuid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Throws(RemoteException::class)
    override fun getShizukuService(): IShizukuService? {
        return shizuku
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
    ): IAxshProcess {
        // The binder stub has already made these fds in this process. Until PtyHost takes them
        // they are ours to close: one left open (here a refusal, as before a permission prompt)
        // holds the client's pipe, so its relay never sees EOF and `axsh -c` never returns.
        try {
            enforceCallingPermission("newAxshProcess")

            LOGGER.d(
                "newAxshProcess: uid=%d, pty=%s, cmd=%s",
                getCallingUid(),
                pty.toString(),
                cmd.contentToString()
            )

            requireNotNull(stdin) { "stdin" }
            requireNotNull(stdout) { "stdout" }
            val argv = cmd?.filterNotNull()?.toTypedArray()
            require(!argv.isNullOrEmpty()) { "empty command" }

            // As newProcess: an attached app's shells end with it. A broker or bootstrap isn't
            // attached until it asks for permission; its sessions end with their socket instead.
            val owner = clientManager.findClient(getCallingUid(), getCallingPid())?.client?.asBinder()
            return AxshProcess.start(
                argv, env?.filterNotNull()?.toTypedArray(), dir, stdin, stdout, stderr, pty, size,
                status, owner
            )
        } catch (e: Throwable) {
            // Already detached ones (PtyHost failed later) close as a no-op. Closing status tells
            // the caller there is no exit code to wait for.
            for (fd in arrayOf(stdin, stdout, stderr, status)) {
                try {
                    fd?.close()
                } catch (_: Exception) {
                }
            }
            throw e
        }
    }

    override fun openAxshChannel(): ParcelFileDescriptor {
        enforceCallingPermission("openAxshChannel")
        val server = checkNotNull(axshServer) { "axsh is not running" }
        return server.openChannel(getCallingUid())
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
            val entryPackages = packageEntry?.packages
            if (!entryPackages.isNullOrEmpty()) {
                if (entryPackages.size > 1) throw IllegalStateException("This uid should not have multiple packages")
                PackageManagerApis.getApplicationInfoNoThrow(
                    entryPackages[0],
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

        // axsh sessions on our own socket wait on this uid's answer, not as attached clients.
        axshServer?.onPermissionResult(requestUid, allowed)
    }

    @Synchronized
    private fun onPermissionRevoked(packageName: String?) {
        userServiceManager.removeUserServicesForPackage(packageName)
    }

    private val managerGrantedUids: MutableSet<Int> = ConcurrentHashMap.newKeySet()

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
            // The Shizuku app shows the server as "<api>.<patch>" and can't show text, so the
            // patch carries AxManager's version code as a hint that AxManager is serving it
            // (e.g. "13.14800"). It no longer matches, so the app shows its "update" note.
            // Other clients keep the real patch: they never display it.
            if (requestPackageName == SHIZUKU_MANAGER_APPLICATION_ID) {
                VERSION_CODE.toInt()
            } else {
                SHIZUKU_SERVER_PATCH_VERSION
            }
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
            val userId = UserHandleCompat.getUserId(callingUid)
            // Once per manager uid for this server: they last, and every manager process
            // attaches. By uid, so a reinstalled manager (new uid) is granted again.
            if (managerGrantedUids.add(callingUid)) {
                try {
                    PermissionManagerApis.grantRuntimePermission(
                        MANAGER_APPLICATION_ID,
                        WRITE_SECURE_SETTINGS, userId
                    )
                } catch (e: Throwable) {
                    LOGGER.w(e, "grant WRITE_SECURE_SETTINGS")
                }
                allowWriteSettings(userId)
            }
        }
        try {
            application.bindApplication(reply)
        } catch (e: Throwable) {
            LOGGER.w(e, "attachApplication")
        }
    }

    // The caches are volatile, so a reset from BinderSender's thread is seen by binder threads.
    @Throws(RemoteException::class)
    override fun dispatchPackageChanged(intent: Intent?) {
        // Manager / Shizuku-manager uid may have changed on (re)install.
        cachedManagerUid = -1
        cachedShizukuManagerUid = -1
    }

    override fun exit() {
        // Before the release: a refused caller must not drop the wakelock on its way out.
        enforceManagerPermission("exit")
        synchronized(lockToken) {
            if (wakeLockHeld && release()) wakeLockHeld = false
        }
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

        // Only the Shizuku app reads this list (AxManager's Privilege screen lists apps itself),
        // so it follows upstream Shizuku: apps that already have a permission entry, plus apps
        // built on Shizuku-API (V3_SUPPORT meta-data and the API_V23 permission).
        for (user in users) {
            val packages = PackageManagerApis.getInstalledPackagesNoThrow(
                (PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS).toLong(),
                user
            )

            for (pi in packages) {
                if (pi.packageName == MANAGER_APPLICATION_ID) continue
                if (pi.packageName == SHIZUKU_MANAGER_APPLICATION_ID) continue

                val appInfo = pi.applicationInfo ?: continue
                val uid = appInfo.uid

                val entry = configManager.find(uid)
                val flags = entry?.let {
                    val entryPackages = it.packages
                    if (entryPackages != null && !entryPackages.contains(pi.packageName)) {
                        return@let 0 // entry belongs to another package of a shared uid
                    }
                    it.flags and ConfigManager.MASK_PERMISSION
                } ?: 0

                when {
                    flags != 0 -> list.add(pi)

                    appInfo.metaData?.getBoolean("moe.shizuku.client.V3_SUPPORT", false) == true &&
                            pi.requestedPermissions?.contains(PERMISSION) == true -> list.add(pi)
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

    // configManager.find() takes the config's own lock.
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

    override fun getFlagsForUids(uids: IntArray, mask: Int): IntArray {
        if (!checkCaller(getCallingUid())) {
            LOGGER.w("getFlagsForUids is allowed to be called only from the manager")
            return IntArray(uids.size)
        }
        val result = IntArray(uids.size)
        for (i in uids.indices) {
            result[i] = getFlagsForUidInternal(uids[i], mask)
        }
        return result
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

        val keepAlive = mask and KeepAlive.MASK
        if (keepAlive != 0) {
            // With its packages: an entry without them is dropped as stale on the next start, and
            // a terminal app like Termux has none until it asks for the permission.
            configManager.findOrUpdate(uid)
        }
        val keepAliveBefore = getFlagsForUidInternal(uid, KeepAlive.MASK)

        configManager.update(uid, null, mask, value)

        // The app's daemon runs with the setting it was started with: end it, and the next
        // command starts one with this.
        if (keepAlive != 0 && getFlagsForUidInternal(uid, KeepAlive.MASK) != keepAliveBefore) {
            axshServer?.closeChannels(uid)
        }
    }
}