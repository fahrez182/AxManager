package com.frb.engine.implementation

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
import android.os.UserHandle
import android.os.UserHandleHidden
import android.system.Os
import com.frb.engine.Environment
import com.frb.engine.IAxeronApplication
import com.frb.engine.IAxeronService
import com.frb.engine.IFileService
import com.frb.engine.IRuntimeService
import com.frb.engine.core.ConstantEngine
import com.frb.engine.utils.Logger
import com.frb.engine.utils.PathHelper
import dev.rikka.tools.refine.Refine
import moe.shizuku.server.IShizukuService
import org.json.JSONObject
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
import kotlin.system.exitProcess

abstract class ServiceImpl : IAxeronService.Stub() {

    companion object {
        protected const val TAG: String = "AxeronService"
        val LOGGER: Logger = Logger(TAG)
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
            PermissionManagerApis.grantRuntimePermission(MANAGER_APPLICATION_ID,
                WRITE_SECURE_SETTINGS, UserHandleCompat.getUserId(callingUid))
        } catch (e: Exception) {
            LOGGER.w(e, "grant WRITE_SECURE_SETTINGS")
        }
        app.bindApplication(Bundle())
    }

    override fun getPackages(flags: Int): ParcelableListSlice<PackageInfo?>? {
        val list = PackageManagerApis.getInstalledPackagesNoThrow(flags.toLong(), 0)
        LOGGER.i(TAG, "getPackages: " + list.size)
        return ParcelableListSlice<PackageInfo?>(list)
    }

    override fun getPlugins(): List<String?>? {
        val pluginsPath =
            PathHelper.getShellPath(ConstantEngine.folder.PARENT_PLUGIN).absolutePath
        return readAllPluginAsString(pluginsPath)
    }

    override fun getPluginById(id: String): String? {
        val dir =
            File(PathHelper.getShellPath(ConstantEngine.folder.PARENT_PLUGIN).absolutePath, id)
        return getPluginByDir(dir)
    }

    override fun isFirstInit(markAsFirstInit: Boolean): Boolean {
        val firstInitFlag = this.firstInitFlag
        if (markAsFirstInit) {
            this.firstInitFlag = false
        }
        return firstInitFlag
    }

    override fun getShizukuService(): IShizukuService? {
        val isActive = File(
            PathHelper.getShellPath(ConstantEngine.folder.PARENT),
            "ax_perm_companion"
        ).exists()
        if (!isActive) {
            shizuku = null
        }
        return shizuku
    }

    private fun readAllPluginAsString(pluginsDirPath: String): MutableList<String?> {
        val result: MutableList<String?> = ArrayList()
        val pluginsDir = File(pluginsDirPath)

        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            LOGGER.e(TAG, "Plugins dir not found: $pluginsDirPath")
            return result
        }

        val subDirs = pluginsDir.listFiles { obj: File? -> obj!!.isDirectory() }
        if (subDirs == null) return result

        for (dir in subDirs) {
            val pluginInfo: String? = getPluginByDir(dir)
            if (pluginInfo == null) continue
            result.add(pluginInfo)
        }

        return result
    }

    private fun getPluginByDir(dir: File): String? {
        if (dir.isFile()) return null
        var pluginInfo: MutableMap<String?, Any?>? = null
        val propFile = File(dir, "module.prop")
        if (propFile.exists() && propFile.isFile()) {
            pluginInfo = HashMap(readFileProp(propFile))
        }

        if (pluginInfo == null) return null
        val pluginId = pluginInfo["id"].toString()

        val updateDir =
            File(PathHelper.getShellPath(ConstantEngine.folder.PARENT_PLUGIN_UPDATE), pluginId)
        val folderUpdateChild =
            if (updateDir.exists() && updateDir.isDirectory()) updateDir.listFiles() else null
        val isUpdate = folderUpdateChild != null && folderUpdateChild.size > 0

        pluginInfo.put("dir_id", pluginId)
        pluginInfo.put("enabled", !(File(dir, "disable").exists()))
        pluginInfo.put("update", isUpdate.toString())
        pluginInfo.put("update_install", File(updateDir, "update_install").exists())
        pluginInfo.put("update_remove", File(updateDir, "update_remove").exists())
        pluginInfo.put("update_enable", File(updateDir, "update_enable").exists())
        pluginInfo.put("update_disable", File(updateDir, "update_disable").exists())
        pluginInfo.put("remove", File(dir, "remove").exists())
        pluginInfo.put("action", File(dir, "action.sh").exists())
        pluginInfo.put("web", File(dir, "webroot/index.html").exists())
        pluginInfo.put("size", getFolderSize(dir).toString())
        return JSONObject(pluginInfo).toString()
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

    private fun readFileProp(file: File): MutableMap<String?, String?> {
        val map: MutableMap<String?, String?> = java.util.HashMap<String?, String?>()

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
                        val value = parts[1]!!.trim { it <= ' ' }
                        map.put(key, value)
                    }
                }
            }
        } catch (e: IOException) {
            LOGGER.e(TAG, "Error reading file: " + file.absolutePath, e)
        }

        return map
    }

    override fun destroy() {
        exitProcess(0)
    }

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