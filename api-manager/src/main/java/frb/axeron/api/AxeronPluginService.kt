package frb.axeron.api

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.annotations.SerializedName
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Engine.Companion.application
import frb.axeron.shared.AxeronConstant
import frb.axeron.shared.Environment
import frb.axeron.shared.PathHelper
import frb.axeron.shared.PluginInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CompletableFuture


object AxeronPluginService {
    const val TAG = "PluginService"

    val BUSYBOX: String
        get() = "${application.applicationInfo.nativeLibraryDir}/libbusybox.so"
    val RESETPROP: String
        get() = "${application.applicationInfo.nativeLibraryDir}/libresetprop.so"
    val BASEAPK: String
        get() = application.applicationInfo.sourceDir

    val AXERONBIN: String
        get() = PathHelper.getShellPath(AxeronConstant.folder.PARENT_BINARY).absolutePath
    val PLUGINDIR: String
        get() = PathHelper.getShellPath(AxeronConstant.folder.PARENT_PLUGIN).absolutePath
    val PLUGINUPDATEDIR: String
        get() = PathHelper.getShellPath(AxeronConstant.folder.PARENT_PLUGIN_UPDATE).absolutePath

    val axFS
        get() = Axeron.newFileService()!!

    fun getUid(context: Context, packageName: String): Int? =
        try {
            context.packageManager
                .getApplicationInfo(packageName, 0)
                .uid
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun checkManageExternalStorageUid(
        context: Context,
        uid: Int,
        packageName: String
    ): Int {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        @Suppress("DEPRECATION")
        return appOps.unsafeCheckOpNoThrow(
            "android:manage_external_storage",
            uid,
            packageName
        )
    }

    fun allowManageExternalStorageUid(uid: Int): Int {
        return Axeron.newProcess(
            arrayOf(
                "sh",
                "-c",
                "cmd appops set --uid $uid MANAGE_EXTERNAL_STORAGE allow"
            )
        ).waitFor()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun ensureManageExternalStorageAllowed(
        context: Context,
        packageNames: List<String> = listOf(
            "com.android.externalstorage",
            "com.android.providers.downloads",
            "com.google.android.storagemanager"
        ),
        onResult: (Boolean) -> Unit
    ) {
        Thread {
            var allAllowed = true

            packageNames.forEach { pkg ->
                val uid = getUid(context, pkg) ?: return@forEach

                val mode = checkManageExternalStorageUid(
                    context = context,
                    uid = uid,
                    packageName = pkg
                )

                if (mode != AppOpsManager.MODE_DEFAULT && mode != AppOpsManager.MODE_ALLOWED) {
                    val exitCode = allowManageExternalStorageUid(uid)
                    if (exitCode != 0) {
                        allAllowed = false
                        return@forEach
                    }

                    // re-check (wajib)
                    val recheck = checkManageExternalStorageUid(
                        context = context,
                        uid = uid,
                        packageName = pkg
                    )

                    if (recheck != AppOpsManager.MODE_ALLOWED) {
                        allAllowed = false
                    }
                }
            }

            // balik ke main thread
            Handler(Looper.getMainLooper()).post {
                onResult(allAllowed)
            }
        }.start()
    }


    data class FlashResult(val code: Int, val err: String, val showReboot: Boolean) {
        constructor(result: ResultExec, showReboot: Boolean) : this(
            result.code,
            result.err,
            showReboot
        )

        constructor(result: ResultExec) : this(result, result.isSuccess())
    }

    suspend fun flashPlugin(
        installer: PluginInstaller,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit
    ): FlashResult {
        val resolver = application.contentResolver
        with(resolver.openInputStream(installer.uri)) {
            val file = File(PathHelper.getTmpPath(AxeronConstant.folder.PARENT_ZIP), "module.zip")

            val fos = axFS.getStreamSession(file.absolutePath, true, false).outputStream

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            while (this?.read(buffer).also {
                    bytesRead = it!!
                } != -1) {
                fos.write(buffer, 0, bytesRead)
            }
            fos.flush()
            this?.close()

            val cmd =
                "ZIPFILE=${file.absolutePath}; . functions.sh; install_plugin ${installer.autoEnable}; exit 0"
            val result = execWithIO(cmd, onStdout, onStderr, standAlone = true)

            Log.i(TAG, "install module ${installer.uri} result: $result")

            axFS.delete(file.absolutePath)

            return FlashResult(result)
        }
    }

    data class ResultExec(
        @SerializedName("errno")
        val code: Int,
        @SerializedName("stdout")
        val out: String = "",
        @SerializedName("stderr")
        val err: String = ""
    ) {
        fun isSuccess(): Boolean {
            return code == 0
        }
    }

    suspend fun execWithIO(
        cmd: String,
        onStdout: (String) -> Unit = {},
        onStderr: (String) -> Unit = {},
        useSetsid: Boolean = false,
        useBusybox: Boolean = true,
        standAlone: Boolean = false,
        hideStderr: Boolean = true
    ): ResultExec = runCatching {

        Log.d(TAG, "execWithIO: $cmd")

        val process = Axeron.newProcess(
            if (useSetsid) arrayOf(BUSYBOX, "setsid", "sh")
            else arrayOf(BUSYBOX, "sh"),
            Axeron.getEnvironment(),
            null
        )

        process.outputStream.use { os ->
            val cmdLine = when {
                useBusybox && !standAlone -> "$BUSYBOX sh -c \"$cmd\"\n"
                useBusybox && standAlone -> "$BUSYBOX sh -o standalone -c \"$cmd\"\n"
                else -> "sh -c \"$cmd\"\n"
            }
            os.write(cmdLine.toByteArray())
            os.flush()
        }

        val builderOut = StringBuilder()
        val builderErr = StringBuilder()

        coroutineScope {

            val jobStdout = async(Dispatchers.IO) {
                val buf = ByteArray(4096)
                val stream = process.inputStream

                while (true) {
                    val len = stream.read(buf)
                    if (len <= 0) break
                    val chunk = String(buf, 0, len)

                    synchronized(builderOut) { builderOut.append(chunk) }
                    onStdout(chunk)
                }
            }

            val jobStderr = async(Dispatchers.IO) {
                val buf = ByteArray(4096)
                val stream = process.errorStream

                while (true) {
                    val len = stream.read(buf)
                    if (len <= 0) break
                    val chunk = String(buf, 0, len)

                    synchronized(builderErr) { builderErr.append(chunk) }
                    onStderr(chunk)
                }
            }

            // Tunggu keduanya selesai
            jobStdout.await()
            jobStderr.await()
        }

        val exit = process.waitFor()
        process.destroy()

        ResultExec(
            code = exit,
            out = builderOut.toString(),
            err = if (!hideStderr) builderErr.toString() else ""
        )
    }.getOrElse { e ->
        if (e is kotlinx.coroutines.CancellationException || e.toString()
                .contains("CancellationException")
        ) throw e
        ResultExec(-1, err = e.toString())
    }

    fun execWithIOFuture(
        cmd: String,
        onStdout: (String) -> Unit = {},
        onStderr: (String) -> Unit = {},
        useBusybox: Boolean = true,
        standAlone: Boolean = false,
        hideStderr: Boolean = true
    ): CompletableFuture<ResultExec> {

        val future = CompletableFuture<ResultExec>()

        CoroutineScope(Dispatchers.IO).launch {

            val result = runCatching {

                val process = Axeron.newProcess(
                    arrayOf("sh"),
                    Axeron.getEnvironment(),
                    null
                )

                // KIRIM COMMAND
                process.outputStream.use { os ->
                    val cmdLine = when {
                        useBusybox && !standAlone -> "$BUSYBOX sh -c \"$cmd\"\n"
                        useBusybox && standAlone -> "$BUSYBOX sh -o standalone -c \"$cmd\"\n"
                        else -> "sh -c \"$cmd\"\n"
                    }
                    os.write(cmdLine.toByteArray())
                    os.flush()
                }

                val builderOut = StringBuilder()
                val builderErr = StringBuilder()

                supervisorScope {

                    val jobOut = async(Dispatchers.IO) {
                        val buf = ByteArray(4096)
                        val s = process.inputStream

                        while (true) {
                            val len = s.read(buf)
                            if (len <= 0) break

                            val chunk = String(buf, 0, len)
                            synchronized(builderOut) { builderOut.append(chunk) }
                            onStdout(chunk)
                        }
                    }

                    val jobErr = async(Dispatchers.IO) {
                        val buf = ByteArray(4096)
                        val s = process.errorStream

                        while (true) {
                            val len = s.read(buf)
                            if (len <= 0) break

                            val chunk = String(buf, 0, len)
                            synchronized(builderErr) { builderErr.append(chunk) }
                            onStderr(chunk)
                        }
                    }

                    jobOut.await()
                    jobErr.await()
                }

                val exit = process.waitFor()
                process.destroy()

                ResultExec(
                    code = exit,
                    out = builderOut.toString(),
                    err = if (!hideStderr) builderErr.toString() else ""
                )
            }

            future.complete(
                result.getOrElse { e ->
                    ResultExec(-1, err = e.toString())
                }
            )
        }

        return future
    }


    fun togglePlugin(dirId: String, enable: Boolean): Boolean {
        val path = "$PLUGINDIR/$dirId"
        val updatePath = "$PLUGINUPDATEDIR/$dirId"

        if (enable) {
            // hapus disable di plugin folder
            axFS.delete("$path/disable")

            //buat file jika memang dari awal gak ada keduanya
            if (!axFS.exists("$updatePath/update_disable") && !axFS.exists("$updatePath/update_enable")) {
                return axFS.createNewFile("$updatePath/update_enable")
            }

            // hapus update_disable jika ada
            axFS.delete("$updatePath/update_disable")
            // buat update_enable kalau belum ada

        } else {
            axFS.createNewFile("$path/disable")

            // kalau update_enable ada, hapus update_enable
            if (!axFS.exists("$updatePath/update_enable") && !axFS.exists("$updatePath/update_disable")) {
                return axFS.createNewFile("$updatePath/update_disable")
            }

            axFS.delete("$updatePath/update_enable")
        }

        // kalau semua file sudah sesuai kondisi, return true
        return true
    }


    fun uninstallPlugin(dirId: String): Boolean {
        val path = "$PLUGINDIR/$dirId"
        val updatePath = "$PLUGINUPDATEDIR/$dirId"

        return axFS.createNewFile("$path/remove") && axFS.createNewFile("$updatePath/update_remove")
    }

    fun restorePlugin(dirId: String): Boolean {
        val path = "$PLUGINDIR/$dirId"
        val updatePath = "$PLUGINUPDATEDIR/$dirId"

        return axFS.delete("$path/remove") && axFS.delete("$updatePath/update_remove")
    }

    //===================================
    // IGNITER
    //===================================

    data class ExecResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        fun isSuccess() = exitCode == 0
    }

    suspend fun execProcessSafe(
        cmd: Array<String>,
        env: Environment? = null
    ): ExecResult = withContext(Dispatchers.IO) {

        val process = Axeron.newProcess(cmd, env, null)

        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val outJob = launch {
            process.inputStream.bufferedReader().useLines {
                it.forEach { line -> stdout.appendLine(line) }
            }
        }

        val errJob = launch {
            process.errorStream.bufferedReader().useLines {
                it.forEach { line -> stderr.appendLine(line) }
            }
        }

        val exitCode = process.waitFor()

        outJob.join()
        errJob.join()

        process.destroy()

        ExecResult(exitCode, stdout.toString(), stderr.toString())
    }

    suspend fun fsBarrier() {
        withContext(Dispatchers.IO) {
            // opsi minimal & portable
            delay(10)
        }
    }


    @JvmStatic
    fun igniteService(): Boolean {
        return runBlocking(Dispatchers.IO) {
            igniteSuspendService()
        }
    }

    suspend fun igniteSuspendService(): Boolean = withContext(Dispatchers.IO) {

        val localVer = Axeron.getAxeronInfo().getActualVersion()
        val serverVer = AxeronConstant.server.getActualVersion()

        if (serverVer > localVer) {
            Log.i(TAG, "Updating.. $localVer < $serverVer")
            return@withContext false
        }

        if (Axeron.isFirstInit(true)) {
            Log.i(TAG, "First Init: Removing old bin")
            removeScripts()
            removeLibrary()
            fsBarrier()
        }

        if (!ensureLibrary()) return@withContext false
        fsBarrier()

        if (!ensureScripts()) return@withContext false
        fsBarrier()

        val cmd =
            "CLASSPATH=$AXERONBIN/ax_reignite.dex; app_process / frb.axeron.reignite.Igniter ${AxeronSettings.getEnableDeveloperOptions()}"

        Log.d(TAG, "Start Init Service")

        val result = execProcessSafe(
            arrayOf(BUSYBOX, "sh", "-c", cmd),
            Axeron.getEnvironment()
        )

        if (result.stdout.isNotBlank()) Log.i(TAG, "STDOUT:\n${result.stdout}")
        if (result.stderr.isNotBlank()) Log.e(TAG, "STDERR:\n${result.stderr}")

        result.isSuccess()
    }


    suspend fun removeScripts() = withContext(Dispatchers.IO) {
        val files = application.assets.list("scripts") ?: return@withContext
        if (files.isEmpty()) return@withContext

        for (filename in files) {
            val dstFile = File(AXERONBIN, filename)
            if (!axFS.exists(dstFile.absolutePath)) continue

            if (!axFS.delete(dstFile.absolutePath)) {
                Log.e(TAG, "failed to remove ${dstFile.absolutePath}")
                continue
            }

            Log.i(TAG, "removed ${dstFile.absolutePath}")
        }
    }

    suspend fun removeLibrary() = withContext(Dispatchers.IO) {
        val dstBusybox = File(AXERONBIN, "busybox")

        if (axFS.exists(dstBusybox.absolutePath)) {
            if (!axFS.delete(dstBusybox.absolutePath)) {
                return@withContext
            }

            val cmd = "find $AXERONBIN -type l -delete"
            val result = execWithIO(cmd, useBusybox = false, hideStderr = false)

            if (!result.isSuccess()) {
                Log.e(TAG, "remove symlink failed: ${result.err}")
                return@withContext
            }

            Log.i(TAG, "symlink from busybox removed")
        }
        val dstResetprop = File(AXERONBIN, "resetprop")
        if (axFS.exists(dstResetprop.absolutePath)) {
            axFS.delete(dstResetprop.absolutePath)
        }
    }

    private suspend fun ensureScripts(): Boolean = withContext(Dispatchers.IO) {
        val files = application.assets.list("scripts") ?: return@withContext  false
        if (files.isEmpty()) return@withContext false

        if (!axFS.exists(AXERONBIN) && !axFS.mkdirs(AXERONBIN)) return@withContext false

        for (filename in files) {
            val inPath = "assets/scripts/$filename"
            val dstFile = File(AXERONBIN, filename)

            if (axFS.exists(dstFile.absolutePath)) continue

            val cmd =
                "$BUSYBOX unzip -p $BASEAPK assets/scripts/$filename > ${dstFile.absolutePath} " +
                        "&& chmod 755 ${dstFile.absolutePath} " +
                        "&& chown 2000:2000 ${dstFile.absolutePath}; dos2unix ${dstFile.absolutePath}"

            val result = execWithIO(cmd, hideStderr = false)

            if (!result.isSuccess()) {
                Log.e(TAG, "$inPath failed: ${result.err}")
                return@withContext false
            }

            Log.i(TAG, "$filename extracted")
        }
        return@withContext true
    }

    suspend fun ensureLibrary(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!axFS.exists(AXERONBIN) && !axFS.mkdirs(AXERONBIN)) return@withContext false

            val dstBusyBox = File(AXERONBIN, "busybox")
            val dstResetProp = File(AXERONBIN, "resetprop")

            if (axFS.exists(dstBusyBox.absolutePath) && axFS.exists(dstResetProp.absolutePath)) return@withContext true

            val cmdBB =
                "cp $BUSYBOX ${dstBusyBox.absolutePath} && chmod 755 ${dstBusyBox.absolutePath} " +
                        "&& chown 2000:2000 ${dstBusyBox.absolutePath} " +
                        "&& ${dstBusyBox.absolutePath} --install -s $AXERONBIN"

            val rBB = execWithIO(cmdBB, useBusybox = false, hideStderr = false)
            if (!rBB.isSuccess()) {
                Log.e(TAG, "Failed to ensure busybox: ${rBB.err}")
                return@withContext false
            }

            val cmdRP =
                "cp $RESETPROP ${dstResetProp.absolutePath} && chmod 755 ${dstResetProp.absolutePath} " +
                        "&& chown 2000:2000 ${dstResetProp.absolutePath}"

            val rRP = execWithIO(cmdRP, useBusybox = false, hideStderr = false)
            if (!rRP.isSuccess()) {
                Log.e(TAG, "Failed to ensure resetprop: ${rRP.err}")
                return@withContext false
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure library", e)
            false
        }
    }

}
