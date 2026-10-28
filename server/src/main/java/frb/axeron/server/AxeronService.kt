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
import frb.axeron.server.ServerConstants.PERMISSION
import frb.axeron.server.ServerConstants.SHIZUKU_MANAGER_APPLICATION_ID
import frb.axeron.server.api.RemoteProcessHolder
import frb.axeron.server.axsh.AxshServer
import frb.axeron.server.util.HandlerUtil
import frb.axeron.server.util.IContentProviderCompat
import frb.axeron.server.util.OsUtils
import frb.axeron.server.util.UserHandleCompat
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.AxeronApiConstant.server.APPEND_ONLY_ENV
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
import frb.axeron.shell.host.ShellProcess
import frb.axeron.shell.host.PtyHost
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess


open class AxeronService :
    Service<AxeronUserServiceManager, AxeronClientManager, AxeronConfigManager>() {

    companion object {
        // checkCaller's cache states. Both lookup results are impossible uids, so no caller's
        // appId can ever match them.
        private const val MANAGER_UID_UNKNOWN = -1
        private const val MANAGER_UID_MISSING = Int.MIN_VALUE
        private const val MANAGER_UID_LOOKUP_FAILED = Int.MIN_VALUE + 1

        // The manager's package is there, but signed by someone else than the manager this
        // server started from.
        private const val MANAGER_UID_FOREIGN = Int.MIN_VALUE + 2

        // How long init waits (a second each) for the package manager to name the manager.
        private const val MANAGER_RESOLVE_TRIES = 30

        // In AXERONDIR. Must match START_ENV_FILE in starter.cpp.
        private const val START_ENV_FILE = "server_env"

        // The sticky broadcast holding this server's binder (see publishBinder). Mirrored in
        // AxBroker.
        private const val SERVER_BINDER_ACTION = "AxRuntime.SERVER_BINDER"

        @JvmStatic
        fun main(args: Array<String>) {
            DdmHandleAppName.setAppName("axeron_server", 0)
            PtyHost.setLibraryPath(System.getProperty("axeron.library.path"))

            @Suppress("DEPRECATION")
            Looper.prepareMainLooper()
            // Before the service exists: its constructor already reads the root world's configs.
            if (Os.getuid() == 0) migrateRootWorld()
            ensureCacheDir(Os.getuid() == 0)
            saveStartEnv(Os.getuid() == 0)
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
         * The env this server was started with, for the starter: run again from inside AxManager
         * (a relaunch, the manager's root shell, a plugin's script) it has the env the server
         * gives its children, and takes this one instead. The boot id comes first, on a line of
         * its own, so one from before a reboot is left alone; then each `KEY=value`, NUL-ended.
         */
        private fun saveStartEnv(isRoot: Boolean) {
            val dir = PathHelper.getWorkingPath(isRoot, AxeronApiConstant.folder.PARENT)
            try {
                val bootId = File("/proc/sys/kernel/random/boot_id").readText().trim()
                val data = buildString {
                    append(bootId).append('\n')
                    for ((key, value) in System.getenv()) {
                        append(key).append('=').append(value).append('\u0000')
                    }
                }
                // Written whole, then renamed: a starter never reads half of one.
                val tmp = File(dir, "$START_ENV_FILE.tmp")
                tmp.writeText(data)
                if (!tmp.renameTo(File(dir, START_ENV_FILE))) {
                    LOGGER.w("can't save the start env in %s", dir)
                }
            } catch (e: Throwable) {
                LOGGER.w(e, "can't save the start env in %s", dir)
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

        /** The manager this server was started from, and the certificate it was signed with. */
        class RunningManager(val packageName: String, val uid: Int, val certDigest: String)

        /**
         * The manager is whoever started this server, not a package name: the starter runs the
         * server from that manager's APK, and a later manager that starts a server replaces this
         * one with it. Set in init once the package manager is up; a server that can't find its
         * manager exits instead, so after init this is never null.
         */
        @Volatile
        private var runningManager: RunningManager? = null

        /** The manager's package. Only once the server has found its manager (see init). */
        @get:JvmStatic
        val managerPackage: String
            get() = checkNotNull(runningManager) { "the manager isn't known yet" }.packageName

        /**
         * SHA-256 of [packageName]'s signing certificate(s), or null when it can't be read. A
         * package of the manager's name signed by someone else is not the manager.
         */
        fun certDigest(packageName: String, userId: Int = 0): String? {
            val signing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val info = PackageManagerApis.getPackageInfoNoThrow(packageName, signing.toLong(), userId)
                ?: return null
            val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            }
            if (signers.isNullOrEmpty()) return null
            return signers.joinToString(",") { signer ->
                MessageDigest.getInstance("SHA-256").digest(signer.toByteArray())
                    .joinToString("") { "%02x".format(it) }
            }
        }

        /**
         * The package owning the APK this server runs from (-Djava.class.path, set by the
         * starter), with its certificate. Null when no package owns the file or the certificate
         * can't be read.
         */
        fun resolveRunningManager(): RunningManager? {
            val apk = System.getProperty("java.class.path")?.substringBefore(':') ?: return null
            val owner = PackageManagerApis.getInstalledPackagesNoThrow(0, 0)
                .firstOrNull { it?.applicationInfo?.sourceDir == apk } ?: return null
            val digest = certDigest(owner.packageName) ?: return null
            return RunningManager(owner.packageName, owner.applicationInfo!!.uid, digest)
        }

        val powerManager: SystemServiceBinder<IPowerManager> by lazy {
            SystemServiceBinder<IPowerManager>(
                "power", IPowerManager.Stub::asInterface
            )
        }

        @JvmStatic
        fun getManagerApplicationInfo(): ApplicationInfo? {
            // Null before the manager is found: the env Service's init builds at boot.
            val manager = runningManager ?: return null
            return PackageManagerApis.getApplicationInfoNoThrow(manager.packageName, 0, 0)
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
            sendBinderToUserApp(binder, managerPackage, userId)
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
            if (packageName == runningManager?.packageName) {
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
            builder.put("PATH", orderPath((axeronBinDirs() + systemPath).joinToString(":")))

            if (managerInfo != null) {
                builder.put("AXERONLIB", managerInfo.nativeLibraryDir)
                val systemLd = System.getenv("LD_LIBRARY_PATH")
                if (systemLd.isNullOrEmpty()) {
                    // A set LD_LIBRARY_PATH becomes a child VM's whole java.library.path, so the
                    // linker defaults (this server's own java.library.path) must stay behind it:
                    // without them a boot-class System.loadLibrary fails (MIUI's libmiuinative
                    // on Android 9/10 kills every app_process child on its first binder call).
                    val defaultLd = System.getProperty("java.library.path").orEmpty()
                    builder.put(
                        "LD_LIBRARY_PATH",
                        if (defaultLd.isEmpty()) "\$AXERONLIB" else "\$AXERONLIB:$defaultLd"
                    )
                } else {
                    // Taruh AXERONLIB di depan agar binari kustom kita prioritas menemukan lib-nya sendiri
                    builder.put("LD_LIBRARY_PATH", "\$AXERONLIB:\$LD_LIBRARY_PATH")
                }
            }

            val built = builder.build()
            cachedDefaultEnv = built
            return built
        }

        /** AXERONXBIN and AXERONBIN, in their default order. */
        private fun axeronBinDirs(): List<String> = listOf(
            PathHelper.getWorkingPath(isRoot, AxeronApiConstant.folder.PARENT_EXTERNAL_BINARY).absolutePath,
            PathHelper.getWorkingPath(isRoot, AxeronApiConstant.folder.PARENT_BINARY).absolutePath
        )

        /**
         * [path] with AxManager's bin dirs in front of the rest, or behind it when system binaries
         * are preferred; each entry kept once. A system PATH inherited from a server started inside
         * an AxManager shell may already hold these dirs.
         */
        private fun orderPath(path: String): String {
            val ours = axeronBinDirs()
            val rest = path.split(':').filter { it.isNotEmpty() && it !in ours }.distinct()
            val present = ours.filter { it in path.split(':') }
            return (if (preferSystemBinaries) rest + present else present + rest).joinToString(":")
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

    // Volatile, not locked: every gated call and every axsh auth passes here. Two threads
    // filling the cache at once look up, and store, the same uid. attachApplication also seeds
    // the manager's entry from the caller's validated uid, so the manager's first gated call
    // normally needs no PackageManager look-up at all.
    @Volatile
    private var cachedManagerUid: Int = MANAGER_UID_UNKNOWN

    @Volatile
    private var cachedShizukuManagerUid: Int = MANAGER_UID_UNKNOWN

    /**
     * The manager's uid for [checkCaller], or [MANAGER_UID_MISSING] / [MANAGER_UID_LOOKUP_FAILED].
     * The "no throw" helper collapses both into null, but they mean different things: only a
     * manager that is really gone ends the server, while a look-up that failed (system_server
     * restarting, a package transaction in flight, a busy device) says nothing about the caller
     * and is retried on the next call.
     */
    private fun resolveManagerUid(): Int {
        val manager = runningManager ?: return MANAGER_UID_LOOKUP_FAILED
        return try {
            val uid = PackageManagerApis.getApplicationInfo(manager.packageName, 0, 0)?.uid
            when {
                uid == null -> MANAGER_UID_MISSING
                else -> when (certDigest(manager.packageName)) {
                    manager.certDigest -> uid
                    null -> MANAGER_UID_LOOKUP_FAILED
                    else -> MANAGER_UID_FOREIGN
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            MANAGER_UID_MISSING
        } catch (e: Throwable) {
            LOGGER.w(e, "can't look up the manager, denying this call")
            MANAGER_UID_LOOKUP_FAILED
        }
    }

    /** Whether the manager's package still has the certificate the server started under. */
    private fun managerCertMatches(): Boolean {
        val manager = runningManager ?: return false
        return certDigest(manager.packageName) == manager.certDigest
    }

    /**
     * The manager's appId from the cache or a look-up, or the MANAGER_UID_* state it couldn't be
     * had in (always negative, as no appId is). Only a real appId is cached.
     */
    private fun managerAppId(): Int {
        val cached = cachedManagerUid
        if (cached != MANAGER_UID_UNKNOWN) return cached
        val resolved = resolveManagerUid()
        if (resolved < 0) return resolved
        return UserHandleCompat.getAppId(resolved).also { cachedManagerUid = it }
    }

    /**
     * Whether [uid] is the manager's own. [checkCaller] lets the Shizuku app in too (through the
     * intercept only), and exits
     * the server when the manager is gone; this does neither, for callers that only ask.
     */
    fun isManagerUid(uid: Int): Boolean {
        val managerUid = managerAppId()
        return managerUid >= 0 && UserHandleCompat.getAppId(uid) == managerUid
    }

    fun checkCaller(callingUid: Int): Boolean {
        val managerUid = managerAppId()
        if (managerUid == MANAGER_UID_MISSING) {
            exitProcess(ServerConstants.MANAGER_APP_NOT_FOUND)
        }
        if (managerUid == MANAGER_UID_LOOKUP_FAILED) return false
        if (managerUid == MANAGER_UID_FOREIGN) {
            LOGGER.w("%s isn't signed like the manager this server started from, denying", managerPackage)
            return false
        }
        val appId = UserHandleCompat.getAppId(callingUid)
        if (appId == managerUid) return true
        // The Shizuku app manages its clients through the intercept only. Its package name proves
        // nothing, and the Axeron binder reaches any app (the sticky), so it is no manager there.
        if (!ShizukuServiceIntercept.isServing()) return false
        var shizukuManagerUid = cachedShizukuManagerUid
        if (shizukuManagerUid == MANAGER_UID_UNKNOWN) {
            // A failed look-up falls back to the manager's uid for this call; the next call
            // tries again.
            shizukuManagerUid = getShizukuManagerApplicationInfo()?.uid
                ?.let { UserHandleCompat.getAppId(it) }
                ?: managerUid
            cachedShizukuManagerUid = shizukuManagerUid
        }
        return appId == shizukuManagerUid
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

        // The manager must be there before anything is sent to it. A look-up that failed
        // (PackageManager still starting at boot, a package transaction) is retried: only a
        // manager that is really gone ends the server.
        var unresolved = 0
        while (true) {
            // The manager is the package this server's APK belongs to. A server that can't find
            // it doesn't guess a name: it exits, and the manager starts it again.
            if (runningManager == null) runningManager = resolveRunningManager()
            if (runningManager == null) {
                if (++unresolved > MANAGER_RESOLVE_TRIES) {
                    LOGGER.e("no package owns the APK this server runs from, exiting")
                    exitProcess(ServerConstants.MANAGER_APP_NOT_FOUND)
                }
                LOGGER.w("can't tell the manager yet, retrying in 1s")
                Thread.sleep(1000)
                continue
            }
            when (resolveManagerUid()) {
                MANAGER_UID_MISSING -> exitProcess(ServerConstants.MANAGER_APP_NOT_FOUND)
                MANAGER_UID_LOOKUP_FAILED -> {
                    LOGGER.w("can't look up the manager yet, retrying in 1s")
                    Thread.sleep(1000)
                }
                else -> break
            }
        }
        // Service's init already built the env, before PackageManager was waited for: at boot it
        // found no manager, so it lacks AXERONLIB and LD_LIBRARY_PATH. Built again now.
        invalidateEnvironment()

        LOGGER.i("manager: %s uid=%d cert=%s", managerPackage, runningManager!!.uid, runningManager!!.certDigest)

        if (axCompanion.exists()) {
            shizuku = ShizukuServiceIntercept(this)
        }

        BinderSender.register(this)
        publishBinder()

        mainHandler.post {
            sendBinderToClient()
            sendBinderToManager()
        }

        // Held from the start; the manager releases it on connect if the user turned it off.
        synchronized(lockToken) { wakeLockHeld = acquire() }

        startDeathWatchdog()
    }

    /**
     * Leaves this server's binder in system_server as a sticky broadcast, for every user, so an
     * app with a Context reads it with registerReceiver(null, …) — nothing is delivered and nobody
     * is woken — instead of waiting for BinderSender to push it. Each start replaces the last; a
     * stale sticky holds a dead binder the reader drops on pingBinder. Who reads it needn't be
     * known: every call on the binder is gated by the caller's uid. The binder can't be registered
     * as a receiver instead (AMS ignores receivers with no app process) nor added to ServiceManager
     * (the shell domain may not), and an app_process client (axsh in a bare terminal) can't read a
     * sticky either, so [BinderSender]'s push and the manager's AxRuntime.REQUEST_BINDER relay stay.
     */
    private fun publishBinder() {
        val intent = Intent(SERVER_BINDER_ACTION)
            .putExtra("data", Bundle().apply {
                putBinder("binder", this@AxeronService)
                putLong("version", VERSION_CODE)
                putString("libDir", getManagerApplicationInfo()?.nativeLibraryDir)
            })
        try {
            ActivityManagerApis.broadcastIntent(
                intent, null, null, 0, null, null, null, -1, null,
                false, true, -1 /* UserHandle.USER_ALL */
            )
            LOGGER.i("server binder published as sticky")
        } catch (e: Throwable) {
            LOGGER.w(e, "can't publish the server binder as sticky")
        }
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

        val defaults = getDefaultEnvironment().envMap
        // Raw: the defaults are built already, a `$` left in one isn't to be replaced again.
        val builder = Environment.Builder(true).putAllRaw(defaults)
        // The default env is locked: the user's env adds variables of its own, and only adds to
        // the path lists of APPEND_ONLY_ENV. A copy of a composed or system value saved in the
        // editor (an LD_LIBRARY_PATH without the system's paths, a stale AXERONLIB, a broken
        // BOOTCLASSPATH) can't replace it for every child.
        val user = HashMap<String, String>()
        for ((key, value) in environmentManager.getAll()) {
            when (key) {
                !in defaults -> user[key] = value
                in APPEND_ONLY_ENV -> user[key] = withDefault(key, value)
                else -> {} // the server's or the system's own
            }
        }
        builder.putAll(user)
        // A plugin's process marker set in the user's env would tag every process as that
        // plugin's, so stopping it would stop them all. Only plugin code gets it.
        val merged = HashMap(builder.remove("AXERON_PLUGIN").build().envMap)
        // Entries added twice (a copy of the default) kept once, at their first place. Not
        // reordered: the default PATH is already in preferSystemBinaries' order inside $PATH,
        // and the user's entries stay where they were put, before or after it.
        for (key in APPEND_ONLY_ENV) {
            merged[key]?.let { list ->
                merged[key] = list.split(':').filter { it.isNotEmpty() }.distinct().joinToString(":")
            }
        }
        val built = Environment(merged, true)

        cachedMergedEnv = built
        return built
    }

    /** [value] with `$key` for the default value, in front when the user's entries lack it. */
    private fun withDefault(key: String, value: String): String =
        if ("\$$key" in value.split(':')) value else "\$$key:$value"

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

    /**
     * A failed write reaches the caller as an IllegalStateException, which the binder carries
     * (a checked IOException it doesn't: the call returned as if it worked). The cached envs are
     * dropped either way, so what is served matches what the file holds.
     */
    override fun setNewEnvironment(env: Environment) {
        enforceManagerPermission("setNewEnvironment")
        try {
            environmentManager.replaceAllBlocking(HashMap(env.envMap)) // manager needs mutability
        } catch (e: Throwable) {
            LOGGER.w(e, "setNewEnvironment")
            throw IllegalStateException("can't save the env: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            invalidateEnvironment()
        }
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

    // Both levels: the mode of the uid is read before the mode of the package, so one set only
    // at the package can be shadowed by the system's own mode for the uid.
    private fun allowManagerAppOp(userId: Int, op: String) {
        for (byUid in listOf(true, false)) {
            try {
                val command = mutableListOf("cmd", "appops", "set", "--user", userId.toString())
                if (byUid) command.add("--uid")
                command.addAll(listOf(managerPackage, op, "allow"))
                val code = Runtime.getRuntime().exec(command.toTypedArray()).waitFor()
                if (code != 0) LOGGER.w("allow %s (%s) exited with %d", op, if (byUid) "uid" else "package", code)
            } catch (e: Throwable) {
                LOGGER.w(e, "allow %s (%s)", op, if (byUid) "uid" else "package")
            }
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
            return ShellProcess.start(
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
            checkRuntime(),
            runningManager?.certDigest.orEmpty(),
            // Open to any caller, like the rest of the info: it only says whether *this* caller is
            // the manager. A transaction's caller can't be forged; a local call (the Shizuku
            // intercept) is the server's own uid, which is not the manager's.
            isManagerUid(getCallingUid())
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

        val pi = PackageManagerApis.getPackageInfoNoThrow(managerPackage, 0, userId)
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
            .setPackage(managerPackage)
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

        // The package alone isn't enough: this seeds the gate's cache and grants the manager's
        // permissions, so it must be signed like the manager this server started from.
        val isManager: Boolean = runningManager?.packageName == requestPackageName && managerCertMatches()

        if (isManager) {
            // Validated above: this uid owns the manager package. Seed the gate's cache from it,
            // so the manager's first gated calls need no PackageManager look-up; a reinstalled
            // manager (new uid, new process) refreshes it through this same path.
            cachedManagerUid = UserHandleCompat.getAppId(callingUid)
            // A reinstall moves the manager's lib dir, even at the same version (no update, no
            // restart): AXERONLIB, and LD_LIBRARY_PATH with it, follow it for new processes.
            val libDir = getManagerApplicationInfo()?.nativeLibraryDir
            if (libDir != null && libDir != cachedDefaultEnv?.envMap?.get("AXERONLIB")) {
                invalidateEnvironment()
            }
        } else if (SHIZUKU_MANAGER_APPLICATION_ID == requestPackageName) {
            // Same for the Shizuku app, whose calls go through the intercept.
            cachedShizukuManagerUid = UserHandleCompat.getAppId(callingUid)
        }

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
        reply.putInt(AxeronApiConstant.server.BIND_APPLICATION_CLIENT_UID, callingUid)
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
                        managerPackage,
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
        // Manager / Shizuku-manager uid may have changed on (re)install. The manager's own uid is
        // seeded again by attachApplication, before it can make a gated call.
        cachedManagerUid = MANAGER_UID_UNKNOWN
        cachedShizukuManagerUid = MANAGER_UID_UNKNOWN
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
                if (pi.packageName == managerPackage) continue
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