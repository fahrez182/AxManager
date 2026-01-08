package frb.axeron.server

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.ActivityThread
import android.content.Context
import android.content.ContextHidden
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.RemoteException
import android.os.UserHandle
import android.os.UserHandleHidden
import android.system.Os
import dev.rikka.tools.refine.Refine
import frb.axeron.aidl.AxeronInterface
import frb.axeron.api.utils.PathHelper
import frb.axeron.data.AxeronConstant
import frb.axeron.data.Environment
import frb.axeron.data.ModuleProp
import frb.axeron.data.ParcelableMapJson
import frb.axeron.data.PluginInfo
import frb.axeron.server.utils.Logger
import moe.shizuku.server.IShizukuService
import rikka.hidden.compat.PackageManagerApis
import rikka.hidden.compat.PermissionManagerApis
import rikka.parcelablelist.ParcelableListSlice
import rikka.shizuku.manager.ServerConstants.MANAGER_APPLICATION_ID
import rikka.shizuku.server.ShizukuService
import rikka.shizuku.server.util.UserHandleCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Collections
import kotlin.system.exitProcess

abstract class Service : IAxeronService.Stub(), AxeronInterface {

    companion object {
        protected const val TAG: String = "AxeronService"

        @JvmStatic
        protected val LOGGER: Logger = Logger(TAG)

        @JvmStatic
        val mainHandler by lazy {
            Handler(Looper.getMainLooper())
        }
    }

    private var context: WeakReference<Context>? = null
    var shizuku: ShizukuService? = null
    private var firstInitFlag = true

    override fun getFileService(): IFileService? {
        return FileService()
    }

    @Throws(RemoteException::class)
    override fun getRuntimeService(
        command: Array<out String?>?,
        env: Environment?,
        dir: String?
    ): IRuntimeService? {
        var process: Process
        try {
            process = Runtime.getRuntime().exec(
                command,
                env?.env,
                if (dir != null) File(dir) else null
            )
        } catch (e: IOException) {
            LOGGER.e(e.message)
            return null
        }
        val token: IBinder = this.asBinder()

        return RuntimeService(process, token)
    }

    override fun bindAxeronApplication(app: IAxeronApplication) {
        val callingUid = getCallingUid()
        try {
            PermissionManagerApis.grantRuntimePermission(
                MANAGER_APPLICATION_ID,
                WRITE_SECURE_SETTINGS, UserHandleCompat.getUserId(callingUid)
            )
        } catch (e: Exception) {
            LOGGER.w(e, "grant WRITE_SECURE_SETTINGS")
        }
        app.bindApplication(Bundle())
    }

    @Throws(RemoteException::class)
    override fun getPackages(flags: Int): ParcelableListSlice<PackageInfo?>? {
        val list = PackageManagerApis.getInstalledPackagesNoThrow(flags.toLong(), 0)
        LOGGER.i(TAG, "getPackages: " + list.size)
        return ParcelableListSlice<PackageInfo?>(list)
    }

    @Throws(RemoteException::class)
    override fun getPlugins(): ParcelableListSlice<PluginInfo?>? {
        val pluginsPath =
            PathHelper.getShellPath(AxeronConstant.folder.PARENT_PLUGIN).absolutePath
        val plugins = readAllPlugin(pluginsPath)
        return ParcelableListSlice<PluginInfo?>(plugins)
    }

    @Throws(RemoteException::class)
    override fun getPluginById(id: String): PluginInfo? {
        val dir =
            File(PathHelper.getShellPath(AxeronConstant.folder.PARENT_PLUGIN).absolutePath, id)
        return getPluginByDir(dir)
    }

    override fun isFirstInit(markAsFirstInit: Boolean): Boolean {
        val firstInitFlag = this.firstInitFlag
        if (markAsFirstInit) {
            this.firstInitFlag = false
        }
        return firstInitFlag
    }

    @Throws(RemoteException::class)
    override fun getShizukuService(): IShizukuService? {
        val isActive = File(
            PathHelper.getShellPath(AxeronConstant.folder.PARENT),
            "ax_perm_companion"
        ).exists()
        if (!isActive) {
            shizuku = null
        }
        return shizuku
    }

    private fun readAllPlugin(pluginsDirPath: String): MutableList<PluginInfo?> {
        val pluginsDir = File(pluginsDirPath)
        val result: MutableList<PluginInfo?> = ArrayList()
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) return result

        val subDirs = pluginsDir.listFiles { obj: File? -> obj!!.isDirectory() }
        if (subDirs == null) return result

        for (dir in subDirs) {
            val pluginInfo: PluginInfo = getPluginByDir(dir) ?: continue
            result.add(pluginInfo)
        }

        return result
    }

    private fun getPluginByDir(dir: File): PluginInfo? {
        if (!dir.isDirectory) return null

        val propFile = File(dir, "module.prop")
        val moduleProp = if (propFile.exists() && propFile.isFile) readFileProp(propFile) else null
        if (moduleProp == null) return null

        val pluginId = moduleProp.id
        val updateDir =
            File(PathHelper.getShellPath(AxeronConstant.folder.PARENT_PLUGIN_UPDATE), pluginId)
        val updateFiles = updateDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
        val isUpdate = updateFiles.isNotEmpty()

        // List semua file/folder di plugin dir
        val dirFiles = dir.listFiles()?.map { it.name }?.toSet() ?: emptySet()

        val pluginInfo: MutableMap<String, Any?> = HashMap()

        pluginInfo["prop"] = moduleProp.toMap()
        pluginInfo["update"] = isUpdate
        pluginInfo["update_install"] = "update_install" in updateFiles
        pluginInfo["update_remove"] = "update_remove" in updateFiles
        pluginInfo["update_enable"] = "update_enable" in updateFiles
        pluginInfo["update_disable"] = "update_disable" in updateFiles

        pluginInfo["enabled"] = "disable" !in dirFiles
        pluginInfo["remove"] = "remove" in dirFiles
        pluginInfo["action"] = "action.sh" in dirFiles
        pluginInfo["web"] = "webroot" in dirFiles && File(dir, "webroot/index.html").exists()
        pluginInfo["size"] = getFolderSize(dir)
        pluginInfo["dir_id"] = pluginId

        return ParcelableMapJson.fromMap<PluginInfo>(Collections.unmodifiableMap(pluginInfo))
    }


    private fun getFolderSize(folder: File?): Long {
        var length: Long = 0

        if (folder != null && folder.exists()) {
            val files = folder.listFiles()
            if (files != null) {
                for (file in files) {
                    length += if (file.isFile()) {
                        file.length() // ukuran file
                    } else {
                        getFolderSize(file) // rekursif ke subfolder
                    }
                }
            }
        }

        return length
    }

    private fun readFileProp(file: File): ModuleProp? {
        val map: MutableMap<String?, Any?> = java.util.HashMap<String?, Any?>()

        try {
            BufferedReader(FileReader(file)).use { br ->
                var line: String?
                while ((br.readLine().also { line = it }) != null) {
                    if (line!!.trim { it <= ' ' }.isEmpty() || line.trim { it <= ' ' }
                            .startsWith("#")) continue

                    val parts: Array<String?> =
                        line.split("=".toRegex(), limit = 2).toTypedArray()
                    if (parts.size == 2) {
                        val key = parts[0]!!.trim { it <= ' ' }
                        val rawValue = parts[1]!!.trim { it <= ' ' }

                        val value: Any = when {
                            // Boolean
                            rawValue.equals("true", ignoreCase = true) -> true
                            rawValue.equals("false", ignoreCase = true) -> false

                            // Integer/Long (tidak diawali 0 kecuali 0 sendiri)
                            rawValue.matches(Regex("0|[1-9]\\d*")) -> {
                                try {
                                    rawValue.toLong()
                                } catch (e: NumberFormatException) {
                                    rawValue
                                }
                            }

                            // Float/Double (tidak diawali 0 kecuali 0.x)
                            rawValue.matches(Regex("0\\.\\d+|[1-9]\\d*\\.\\d+")) -> {
                                try {
                                    rawValue.toDouble()
                                } catch (e: NumberFormatException) {
                                    rawValue
                                }
                            }

                            // Fallback string
                            else -> rawValue
                        }

                        map[key] = value
                    }
                }
            }
        } catch (e: IOException) {
            LOGGER.e(TAG, "Error reading file: " + file.absolutePath, e)
        }

        map["id"] ?: return null

        return ParcelableMapJson.fromMap<ModuleProp>(Collections.unmodifiableMap(map))
    }

    @Throws(RemoteException::class)
    override fun destroy() {
        exitProcess(0)
    }

    @Throws(RemoteException::class)
    fun transactRemote(data: Parcel, reply: Parcel?) {
        val targetBinder = data.readStrongBinder()
        val targetCode = data.readInt()
        val targetFlags = data.readInt()

        LOGGER.d(
            "transact: uid=%d, descriptor=%s, code=%d",
            getCallingUid(),
            targetBinder.interfaceDescriptor,
            targetCode
        )
        val newData = Parcel.obtain()
        try {
            newData.appendFrom(data, data.dataPosition(), data.dataAvail())
        } catch (tr: Throwable) {
            LOGGER.w(tr, "appendFrom")
            return
        }
        try {
            val id = clearCallingIdentity()
            targetBinder.transact(targetCode, newData, reply, targetFlags)
            restoreCallingIdentity(id)
        } finally {
            newData.recycle()
        }
    }

    @Throws(RemoteException::class)
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == 1) {
            data.enforceInterface(DESCRIPTOR)
            transactRemote(data, reply)
            return true
        }
        return super.onTransact(code, data, reply, flags)
    }

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
                    Refine.unsafeCast<ContextHidden?>(systemContext).createPackageContextAsUser(
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

    fun checkPermission(permission: String?): Int {
        val uid = Os.getuid()
        if (uid == 0) return PackageManager.PERMISSION_GRANTED
        return PermissionManagerApis.checkPermission(permission, uid)
    }
}