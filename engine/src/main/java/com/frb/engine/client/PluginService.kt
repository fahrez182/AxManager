package com.frb.engine.client

import android.content.Context
import android.net.Uri
import android.util.Log
import com.frb.engine.core.ConstantEngine
import com.frb.engine.core.Engine.Companion.application
import com.frb.engine.implementation.AxeronService
import com.frb.engine.utils.PathHelper
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CompletableFuture


object PluginService {
    const val TAG = "PluginService"

    val BUSYBOX: String
        get() = "${application.applicationInfo.nativeLibraryDir}/libbusybox.so"
    val RESETPROP: String
        get() = "${application.applicationInfo.nativeLibraryDir}/libresetprop.so"
    val BASEAPK: String
        get() = application.applicationInfo.sourceDir

    val AXERONBIN: String
        get() = PathHelper.getShellPath(ConstantEngine.folder.PARENT_BINARY).absolutePath
    val PLUGINDIR: String
        get() = PathHelper.getShellPath(ConstantEngine.folder.PARENT_PLUGIN).absolutePath
    val PLUGINUPDATEDIR: String
        get() = PathHelper.getShellPath(ConstantEngine.folder.PARENT_PLUGIN_UPDATE).absolutePath

    val axFS = Axeron.newFileService()!!

    data class FlashResult(val code: Int, val err: String, val showReboot: Boolean) {
        constructor(result: ResultExec, showReboot: Boolean) : this(
            result.code,
            result.err,
            showReboot
        )

        constructor(result: ResultExec) : this(result, result.isSuccess())
    }

    fun flashPlugin(
        uri: Uri,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit
    ): FlashResult {
        val resolver = application.contentResolver
        with(resolver.openInputStream(uri)) {
            val file = File(PathHelper.getTmpPath(ConstantEngine.folder.PARENT_ZIP), "module.zip")

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

            val cmd = "ZIPFILE=${file.absolutePath}; . functions.sh; install_plugin; exit 0"
            val result = execWithIO(cmd, onStdout, onStderr, standAlone = true)

            Log.i(TAG, "install module $uri result: $result")

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

    fun execWithIO(
        cmd: String,
        onStdout: (String) -> Unit = {},
        onStderr: (String) -> Unit = {},
        useSetsid: Boolean = false,
        useBusybox: Boolean = true,
        standAlone: Boolean = false,
        hideStderr: Boolean = true
    ): ResultExec {
        Log.d(TAG, "execWithIO: $cmd")
        val scope = CoroutineScope(Dispatchers.IO)

        return runCatching {
            val process = Axeron.newProcess(
                if (useSetsid) arrayOf(BUSYBOX, "setsid", "sh")
                else arrayOf("sh"),
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
            val stdout = process.inputStream
            val stderr = process.errorStream

            scope.launch {
                val buf = ByteArray(1024 * 4)
                generateSequence { stdout.read(buf).takeIf { it > 0 } }
                    .forEach { len ->
                        val chunk = String(buf, 0, len)
                        onStdout(chunk)
                        builderOut.append(chunk)
                    }
            }

            scope.launch {
                val buf = ByteArray(1024 * 4)
                generateSequence { stderr.read(buf).takeIf { it > 0 } }
                    .forEach { len ->
                        val chunk = String(buf, 0, len)
                        onStderr(chunk)
                        builderErr.append(chunk)
                    }
            }

            val exitCode = process.waitFor()
            process.destroy()

            ResultExec(
                code = exitCode,
                out = builderOut.toString(),
                err = if (!hideStderr) builderErr.toString() else ""
            )
        }.getOrElse { e ->
            ResultExec(-1, err = e.toString())
        }
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
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch(Dispatchers.IO) {
            runCatching {
                val process = Axeron.newProcess(
                    arrayOf("sh"),
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
                val stdout = process.inputStream
                val stderr = process.errorStream

                launch {
                    val buf = ByteArray(4096)
                    generateSequence { stdout.read(buf).takeIf { it > 0 } }
                        .forEach { len ->
                            val chunk = String(buf, 0, len)
                            onStdout(chunk)
                            builderOut.append(chunk)
                        }
                }

                launch {
                    val buf = ByteArray(4096)
                    generateSequence { stderr.read(buf).takeIf { it > 0 } }
                        .forEach { len ->
                            val chunk = String(buf, 0, len)
                            onStderr(chunk)
                            builderErr.append(chunk)
                        }
                }

                val exitCode = process.waitFor()
                process.destroy()

                val result = ResultExec(
                    code = exitCode,
                    out = builderOut.toString(),
                    err = if (!hideStderr) builderErr.toString() else ""
                )

                future.complete(result)
            }.onFailure {
                future.complete(ResultExec(-1, err = it.toString()))
            }
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

    @JvmStatic
    fun igniteService() = runBlocking {
        igniteSuspendService()
    }

    suspend fun igniteSuspendService(): Boolean = withContext(Dispatchers.IO) {
        if (AxeronService.getActualVersion() > Axeron.getInfo().getActualVersion()) {
            Log.i(
                TAG,
                "Updating.. ${Axeron.getInfo().versionCode} < ${AxeronService.VERSION_CODE}"
            )
            return@withContext false
        }

        if (Axeron.isFirstInit(true)) {
            Log.i(TAG, "First Init: Removing old bin")
            removeScripts()
            removeLibrary()
        }

        if (!ensureLibrary()) return@withContext false
        if (!ensureScripts()) return@withContext false

        val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val cmd =
            "$AXERONBIN/ignite_plugins.sh ${prefs.getBoolean("enable_developer_options", false)}"
        Log.d(TAG, "Start Init Service: $cmd")

        return@withContext runCatching {
            val process = Axeron.newProcess(
                arrayOf(BUSYBOX, "sh", "-c", cmd),
                Axeron.getEnvironment(),
                null
            )

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()

            val exitCode = process.waitFor()
            process.destroy()

            if (stdout.isNotEmpty()) Log.i(TAG, "STDOUT:\n$stdout")
            if (stderr.isNotEmpty()) Log.e(TAG, "STDERR:\n$stderr")

            exitCode == 0
        }.getOrElse {
            Log.e("StartService", "Error: ${it.message}", it)
            false
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    suspend fun removeScripts() {
        val files = application.assets.list("scripts") ?: return
        if (files.isEmpty()) return

        withContext(Dispatchers.IO) {
            for (filename in files) {
                val dstFile = File(AXERONBIN, filename)
                if (axFS.exists(dstFile.absolutePath)) {
                    axFS.delete(dstFile.absolutePath)
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun removeLibrary() {
        withContext(Dispatchers.IO) {
            val dstBusybox = File(AXERONBIN, "busybox")
            if (axFS.exists(dstBusybox.absolutePath)) {
                if (axFS.delete(dstBusybox.absolutePath)) {
                    val removeBBSymlink = "find $AXERONBIN -type l -delete"
                    val result = execWithIO(removeBBSymlink, useBusybox = false, hideStderr = false)

                    if (result.isSuccess()) {
                        Log.i(TAG, "symlink from busybox removed")
                    } else {
                        Log.e(TAG, "remove symlink failed: ${result.err}")
                    }
                }
            }
            val dstResetprop = File(AXERONBIN, "resetprop")
            if (axFS.exists(dstResetprop.absolutePath)) {
                axFS.delete(dstResetprop.absolutePath)
            }
        }
    }

    private fun ensureScripts(): Boolean {
        val assetManager = application.assets
        val files = assetManager.list("scripts") ?: return false
        if (files.isEmpty()) return false

        if (!axFS.exists(AXERONBIN)) {
            if (!axFS.mkdirs(AXERONBIN)) return false
        }

        for (filename in files) {
            val inPath = "assets/scripts/$filename"
            val dstFile = File(AXERONBIN, filename)

            if (axFS.exists(dstFile.absolutePath)) continue

            val cmd =
                "$BUSYBOX unzip -p $BASEAPK $inPath > ${dstFile.absolutePath} && chmod 755 ${dstFile.absolutePath} && chown 2000:2000 ${dstFile.absolutePath}; dos2unix ${dstFile.absolutePath}"
            val result = execWithIO(cmd, {}, {}, hideStderr = false)

            if (result.isSuccess()) {
                Log.i(TAG, "$inPath extracted to: ${dstFile.absolutePath}")
            } else {
                Log.e(TAG, "$inPath failed: ${result.err}")
            }
        }
        return true
    }

    fun ensureLibrary(): Boolean {
        return try {
            val dstBusyBox = File(AXERONBIN, "busybox")
            val dstResetProp = File(AXERONBIN, "resetprop")
            if (axFS.exists(dstBusyBox.absolutePath) && axFS.exists(dstResetProp.absolutePath)) return true

            if (!axFS.exists(AXERONBIN)) {
                if (!axFS.mkdirs(AXERONBIN)) return false
            }

            val cmdBB =
                "cp $BUSYBOX ${dstBusyBox.absolutePath} && chmod 755 ${dstBusyBox.absolutePath} && chown 2000:2000 ${dstBusyBox.absolutePath} && ${dstBusyBox.absolutePath} --install -s $AXERONBIN"
            val resultBB = execWithIO(cmdBB, useBusybox = false, hideStderr = false)

            if (!resultBB.isSuccess()) {
                Log.e(TAG, "ensureBusybox failed: ${resultBB.err}")
                return false
            }
            Log.i(TAG, "busybox extracted & installed to: ${dstBusyBox.absolutePath}")

            val cmdRP =
                "cp $RESETPROP ${dstResetProp.absolutePath} && chmod 755 ${dstResetProp.absolutePath} && chown 2000:2000 ${dstResetProp.absolutePath}"
            val resultRP = execWithIO(cmdRP, useBusybox = false, hideStderr = false)

            if (!resultRP.isSuccess()) {
                Log.e(TAG, "ensureResetprop failed: ${resultRP.err}")
                return false
            }
            Log.i(TAG, "resetprop extracted to: ${dstResetProp.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "ensureLibrary failed: ${e.message}", e)
            false
        }
    }

}