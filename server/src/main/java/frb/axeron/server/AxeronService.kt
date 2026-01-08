package frb.axeron.server

import android.content.Context
import android.content.IContentProvider
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.ddm.DdmHandleAppName
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.IPowerManager
import android.os.Looper
import android.os.PowerManager
import android.os.SELinux
import android.os.ServiceManager
import android.os.SystemClock
import android.system.Os
import frb.axeron.api.utils.PathHelper
import frb.axeron.data.AxeronConstant
import frb.axeron.data.AxeronConstant.server.PATCH_CODE
import frb.axeron.data.AxeronConstant.server.TYPE_DEFAULT_ENV
import frb.axeron.data.AxeronConstant.server.TYPE_ENV
import frb.axeron.data.AxeronConstant.server.TYPE_NEW_ENV
import frb.axeron.data.AxeronConstant.server.VERSION_CODE
import frb.axeron.data.AxeronConstant.server.VERSION_NAME
import frb.axeron.data.Environment
import frb.axeron.data.ServerInfo
import frb.axeron.server.manager.EnvironmentManager
import frb.axeron.server.utils.ApkChangedListener
import frb.axeron.server.utils.ApkChangedObservers
import frb.axeron.server.utils.BinderSender
import frb.axeron.server.utils.IContentProviderCompat
import moe.shizuku.api.BinderContainer
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.DeviceIdleControllerApis
import rikka.hidden.compat.PackageManagerApis
import rikka.hidden.compat.UserManagerApis
import rikka.hidden.compat.util.SystemServiceBinder
import rikka.shizuku.manager.ServerConstants
import rikka.shizuku.manager.ServerConstants.MANAGER_APPLICATION_ID
import rikka.shizuku.manager.ServerConstants.PERMISSION
import rikka.shizuku.manager.ServerConstants.SHIZUKU_MANAGER_APPLICATION_ID
import rikka.shizuku.manager.ShizukuUserServiceManager
import rikka.shizuku.server.ShizukuService
import java.io.File
import kotlin.system.exitProcess


open class AxeronService() : Service() {

    companion object {
        @Suppress("DEPRECATION")
        @JvmStatic
        fun main(args: Array<String>) {
            DdmHandleAppName.setAppName("axeron_server", 0)

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

        fun getManagerApplicationInfo(): ApplicationInfo? {
            return PackageManagerApis.getApplicationInfoNoThrow(MANAGER_APPLICATION_ID, 0, 0)
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
                IContentProviderCompat.callCompat(provider, null, name, "sendBinder", null, extra)
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
                .put(
                    "AXERONDIR",
                    PathHelper.getShellPath(AxeronConstant.folder.PARENT).absolutePath
                )
                .put(
                    "AXERONBIN",
                    PathHelper.getShellPath(AxeronConstant.folder.PARENT_BINARY).absolutePath
                )
                .put(
                    "AXERONXBIN",
                    PathHelper.getShellPath(AxeronConstant.folder.PARENT_EXTERNAL_BINARY).absolutePath
                )
                .put("AXERONLIB", getManagerApplicationInfo()?.nativeLibraryDir)
                .put("AXERONVER", VERSION_CODE.toString())
                .put(
                    "TMPDIR",
                    PathHelper.getTmpPath(AxeronConstant.folder.PARENT_CACHE).absolutePath
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

    override fun destroy() {
        release()
        super.destroy()
    }

    private val starting: Long = SystemClock.elapsedRealtime()
    private val environmentManager by lazy { EnvironmentManager() }
    private val shizukuUserServiceManager by lazy {
        ShizukuUserServiceManager(getEnvironment(TYPE_ENV)?.getEnv())
    }

    private val axCompanion =
        File(PathHelper.getShellPath(AxeronConstant.folder.PARENT), "ax_perm_companion")

    init {
        waitSystemService("package")
        waitSystemService(Context.ACTIVITY_SERVICE)
        waitSystemService(Context.USER_SERVICE)
        waitSystemService(Context.APP_OPS_SERVICE)
        waitSystemService(Context.POWER_SERVICE)

        val ai: ApplicationInfo =
            getManagerApplicationInfo() ?: exitProcess(ServerConstants.MANAGER_APP_NOT_FOUND)

        if (axCompanion.exists()) {
            // use lazy initialization via property above
            shizuku = ShizukuService(mainHandler, shizukuUserServiceManager, this)
        }

        // make ApkChangedObservers lazy or start on-demand; and keep reference to listener so you can stop it
        val apkObserver = ApkChangedListener {
            if (getManagerApplicationInfo() == null) exitProcess(ServerConstants.MANAGER_APP_NOT_FOUND)
        }
        ApkChangedObservers.start(ai.sourceDir, apkObserver)

        BinderSender.register(this)

        mainHandler.post {
            sendBinderToClient()
            sendBinderToManager()
        }

        acquire()
    }

    fun sendBinderToClient() {
        for (userId in UserManagerApis.getUserIdsNoThrow()) {
            shizukuService?.let {
                sendBinderToClient(it.asBinder(), userId)
            }
        }
    }

    fun sendBinderToManager() {
        sendBinderToManager(this)
        shizuku?.let {
            sendBinderToShizukuManager(it)
        }
    }

    override fun enableShizukuService(enable: Boolean) {
        if (enable) {
            if (axCompanion.createNewFile()) {
                LOGGER.i("AX-Scope")
                shizuku = ShizukuService(
                    mainHandler,
                    shizukuUserServiceManager,
                    this
                )
            }
        } else {
            if (axCompanion.delete()) {
                shizukuUserServiceManager.removeAllUserService()
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


    override fun getEnvironment(envType: Int): Environment? {
        return when (envType) {
            TYPE_DEFAULT_ENV -> getDefaultEnvironment()
            TYPE_ENV -> getMergedEnvironment()
            TYPE_NEW_ENV -> getNewEnvironment()
            else -> null
        }
    }

    override fun setNewEnvironment(env: Environment) {
        environmentManager.replaceAllBlocking(HashMap(env.envMap)) // manager needs mutability
        // invalidate cached refs deterministically
        cachedMergedEnv = null
        cachedNewEnv = null
        if (shizuku != null) shizukuUserServiceManager.environment =
            getEnvironment(TYPE_ENV)?.getEnv()
    }

    fun checkRuntime(): Boolean {
        return checkPermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED
    }

    override fun getServerInfo(): ServerInfo? {
        return ServerInfo(
            VERSION_NAME,
            VERSION_CODE,
            PATCH_CODE,
            Os.getuid(),
            Os.getpid(),
            SELinux.getContext(),
            starting,
            checkRuntime()
        )
    }
}