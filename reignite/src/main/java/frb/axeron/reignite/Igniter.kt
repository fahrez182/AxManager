package frb.axeron.reignite

import android.ddm.DdmHandleAppName
import java.io.File

object Igniter {

    private val AXERONDIR = System.getenv("AXERONDIR")
    private val PLUGINS_DIR = File("$AXERONDIR/plugins")
    private val PLUGINS_UPDATE_DIR = File("$AXERONDIR/plugins_update")
    private val AXERONXBIN = File("$AXERONDIR/xbin")

    private var DEBUG = false

    // ===============================
    // ENTRY
    // ===============================
    @JvmStatic
    fun main(args: Array<String>) {
        val debug = args.firstOrNull() == "true"
        DEBUG = debug

        DdmHandleAppName.setAppName("axeron_plugin_igniter", 0)
        println("AXERON Plugin Manager (Kotlin, busybox-core)")

        cleanUpdateDir()
        mainLoop()

        println("- Completed")
        kotlin.system.exitProcess(0)
    }

    // ===============================
    // MAIN LOOP
    // ===============================
    private fun mainLoop() {
        PLUGINS_DIR.listFiles()?.forEach { plugin ->
            if (!plugin.isDirectory) return@forEach

            val name = plugin.name
            val bin = File(plugin, "system/bin")
            val fsData = File(plugin, "post-fs-data.sh")
            val sProp = File(plugin, "system.prop")
            val service = File(plugin, "service.sh")
            val uninstall = File(plugin, "uninstall.sh")

            val disable = File(plugin, "disable")
            val remove = File(plugin, "remove")
            val update = File(plugin, "update")

            when {
                remove.exists() -> {
                    println("- Disable and uninstalling $name")
                    stopPlugin(name, bin)
                    uninstallPlugin(name, uninstall, bin)
                }

                disable.exists() -> {
                    println("- Disable $name")
                    stopPlugin(name, bin)
                }

                update.exists() -> {
                    println("- Updating $name")
                    stopPlugin(name, bin)
                    update.delete()
                }

                isRunning(name) -> {
                    val pid = SystemProp.get("log.tag.service.$name")
                    println("- $name:$pid is already running, skip.")
                }

                else -> {
                    println("- Starting $name")
                    startPlugin(name, fsData, sProp, service, bin)
                }
            }
        }
    }

    // ===============================
    // START PLUGIN (subshell + &)
    // ===============================
    private fun isStandalone(script: File): Boolean {
        if (!script.exists()) return false
        return script.useLines { lines ->
            lines.any { it.trim() == "ASH_STANDALONE=1" }
        }
    }

    private fun shCmd(script: File): String {
        return if (isStandalone(script))
            "busybox sh -o standalone \"${script.absolutePath}\""
        else
            "busybox sh \"${script.absolutePath}\""
    }

    private fun startPlugin(
        name: String,
        fsData: File,
        sProp: File,
        service: File,
        bin: File
    ) {
        val script = buildString {
            append("(")

            // link bin
            append("mkdir -p ${AXERONXBIN.absolutePath};")
            if (bin.exists()) {
                append("for f in ${bin.absolutePath}/*; do ")
                append($$"busybox ln -sf \"$f\" \"$${AXERONXBIN.absolutePath}/$(basename \"$f\")\"; ")
                append("done;")
            }

            // post-fs-data (once)
            append(postFsDataShell(name, fsData))

            // system.prop (once)
            append(
                """
                if [ "$(resetprop log.tag.props.$name)" != "1" ] && [ -f "${sProp.absolutePath}" ]; then
                    resetprop -f "${sProp.absolutePath}" &&
                    resetprop log.tag.props.$name 1
                fi;
            """.trimIndent()
            )

            // service
            if (service.exists()) {
                append(startServiceShell(name, service))
            }

            append(") &")
        }

        exec(arrayOf("busybox", "sh", "-c", script))
    }

    // ===============================
    // SERVICE (setsid + echo $$ + exec)
    // ===============================

    private fun logPipe(tag: String): String {
        return if (DEBUG)
            "2>&1 | busybox log -t $tag"
        else
            ">/dev/null 2>&1"
    }

    private fun postFsDataShell(name: String, fsData: File): String {
        val standalone = isStandalone(fsData)
        val execLine = if (standalone)
            "busybox sh -o standalone \"${fsData.absolutePath}\""
        else
            "busybox sh \"${fsData.absolutePath}\""

        return """
            if [ "$(resetprop log.tag.fs.$name)" != "1" ] && [ -f "${fsData.absolutePath}" ]; then
                $execLine &&
                resetprop log.tag.fs.$name 1
            fi;
        """.trimIndent()
    }

    private fun startServiceShell(name: String, service: File): String {
        val tag = "axeron.plugin.$name"
        val log = logPipe(tag)
        val standalone = isStandalone(service)

        val execLine = if (standalone)
            "exec busybox sh -o standalone \"${service.absolutePath}\""
        else
            "exec busybox sh \"${service.absolutePath}\""

        return $$"""
            (
              busybox setsid busybox sh -c '
                echo $$;
                $$execLine
              ' $$log | {
                read pid
                resetprop log.tag.service.$$name "$pid"
              }
            );
        """.trimIndent()
    }


    // ===============================
    // STOP / UNINSTALL
    // ===============================
    private fun stopPlugin(name: String, bin: File) {
        reset("log.tag.fs.$name", "0")
        reset("log.tag.props.$name", "0")

        val pid = SystemProp.get("log.tag.service.$name")
        if (pid.isNotBlank() && pid != "-1") {
            exec(arrayOf("busybox", "kill", "-TERM", pid))
        }

        exec(arrayOf("busybox", "pkill", "-f", name))
        reset("log.tag.service.$name", "-1")

        unlinkBin(bin)
    }

    private fun uninstallPlugin(name: String, script: File, bin: File) {
        if (script.exists()) {
            exec(arrayOf("busybox", "sh", script.absolutePath))
        }
        unlinkBin(bin)
        File("$AXERONDIR/plugins/$name").deleteRecursively()
    }

    // ===============================
    // BIN CLEAN
    // ===============================
    private fun unlinkBin(bin: File) {
        if (!bin.exists()) return
        bin.listFiles()?.forEach {
            File(AXERONXBIN, it.name).delete()
        }
    }

    // ===============================
    // UTIL
    // ===============================
    private fun cleanUpdateDir() {
        PLUGINS_UPDATE_DIR.listFiles()?.forEach {
            if (it.isDirectory) it.deleteRecursively()
        }
    }

    private fun isRunning(name: String): Boolean {
        val pid = SystemProp.get("log.tag.service.$name")
        if (pid.isNotBlank() && pid != "-1") return true
        return exec(arrayOf("busybox", "pgrep", "-f", name)) == 0
    }

    private fun reset(key: String, value: String) {
        exec(arrayOf("resetprop", key, value))
    }

    private fun exec(cmd: Array<String>): Int =
        try {
            Runtime.getRuntime().exec(cmd).waitFor()
        } catch (_: Exception) {
            -1
        }
}
