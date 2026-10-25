package frb.axeron.reignite

import android.ddm.DdmHandleAppName
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit

object Igniter {

    private var VERSION = "v1.1"

    private val AXERONDIR = System.getenv("AXERONDIR")
    private val PLUGINS_DIR = File("$AXERONDIR/plugins")
    private val PLUGINS_UPDATE_DIR = File("$AXERONDIR/plugins_update")
    // From the env that also builds PATH, so links land where PATH looks for them.
    private val AXERONXBIN = File(System.getenv("AXERONXBIN") ?: "$AXERONDIR/xbin")

    /**
     * Env var every process of a plugin's service carries (AXERON_PLUGIN=<id>). Children inherit
     * it even when they leave the service's session, so stopping can still find them.
     */
    private const val MARKER = "AXERON_PLUGIN"

    /** How long a service gets to exit on SIGTERM before what's left is SIGKILLed. */
    private const val STOP_GRACE_MS = 3000L

    /**
     * How long post-fs-data.sh / uninstall.sh may block the igniter, like Magisk's limit for
     * post-fs-data. A script still running after it is left running; the next plugin goes on.
     */
    private const val SCRIPT_TIMEOUT_S = 40L

    /** Output of post-fs-data.sh / uninstall.sh, one file per plugin and script. */
    private val LOG_DIR = File("$AXERONDIR/logs")

    /** Never treated as a plugin's process, even if it carries a leaked marker. */
    private const val SERVER_PROCESS = "axeron_server"

    private var DEBUG = false

    // ===============================
    // ENTRY
    // ===============================
    /** Argument of the server's death watchdog: stop every plugin's processes, nothing else. */
    private const val ARG_STOP_ALL = "--stop-all"

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.firstOrNull() == ARG_STOP_ALL) {
            DdmHandleAppName.setAppName("axeron_plugin_stopper", 0)
            println("AXERON Plugin Manager ($VERSION)")
            println("- Stopping all plugins")
            if (!stopAllPlugins()) println(" - none running")
            println("- Completed")
            kotlin.system.exitProcess(0)
        }

        val debug = args.firstOrNull() == "true"
        DEBUG = debug

        DdmHandleAppName.setAppName("axeron_plugin_igniter", 0)
        Log.i("test", "AXERON Plugin Manager ($VERSION)")
        println("AXERON Plugin Manager ($VERSION)")

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
                    return@forEach
                }

                disable.exists() -> {
                    println("- Disable $name")
                    stopPlugin(name, bin)
                    return@forEach
                }

                update.exists() -> {
                    println("- Updating $name")
                    stopPlugin(name, bin)
                    if (update.delete()) {
                        println(" - Update $name Complete")
                    } else {
                        println(" - Failed to update $name")
                    }
                }

                isServiceRunning(name) -> {
                    println("- $name:${servicePids(name).joinToString(",")} is already running, skip.")
                    return@forEach
                }
            }

            println("- Starting $name")
            startPlugin(name, fsData, sProp, service, bin)
        }
    }

    // ===============================
    // START PLUGIN (subshell + &)
    // ===============================
    // Each flag is its own argv entry: "sh -o standalone" as one entry is looked up as an applet name.
    private fun shellCommand(standalone: Boolean, script: File): Array<String> =
        if (standalone) arrayOf("busybox", "sh", "-o", "standalone", script.absolutePath)
        else arrayOf("busybox", "sh", script.absolutePath)

    private fun isStandalone(script: File): Boolean {
        if (!script.exists()) return false
        return script.useLines { lines ->
            lines.any { it.trim() == "ASH_STANDALONE=1" }
        }
    }

    private fun startPlugin(
        name: String,
        fsData: File,
        sProp: File,
        service: File,
        bin: File
    ) {
        if (bin.exists()) {
            linkBin(bin)
        }
        if (fsData.exists()) {
            postFsData(name, fsData)
        }
        if (sProp.exists()) {
            applySetprop(name, sProp)
        }
        if (service.exists()) {
            startService(name, service)
        }
    }

    // ===============================
    // SERVICE (setsid + echo $$ + exec)
    // ===============================

    private fun logPipe(tag: String): String {
//        return if (DEBUG)
//            "2>&1 | busybox log -t $tag"
//        else
        return ">/dev/null 2>&1"
    }

    private fun applySetprop(name: String, sProp: File) {
        val tag = "log.tag.props.$name"

        if (SystemProp.get(tag) != "-1") {
            println(" - Applying system.prop of $name")
            SystemProp.set("-f", sProp.absolutePath)
            SystemProp.set(tag, "1")
        }
    }

    private fun postFsData(name: String, fsData: File) {
        val tag = "log.tag.fs.$name"

        if (SystemProp.get(tag) != "1") {
            runScript(name, fsData, "post-fs-data")
            SystemProp.set(tag, "1")
        }
    }

    /**
     * Runs a plugin's blocking script (post-fs-data.sh, uninstall.sh) with its [MARKER], so
     * anything it leaves running can be stopped later. Its output goes to a log file: an unread
     * pipe fills up and hangs the script, and a daemon left holding it dies on SIGPIPE once the
     * igniter exits. stdin is /dev/null. Waits at most [SCRIPT_TIMEOUT_S].
     */
    private fun runScript(name: String, script: File, kind: String) {
        val log = File(LOG_DIR, "$name.$kind.log")
        val out = if (LOG_DIR.isDirectory || LOG_DIR.mkdirs()) log else File("/dev/null")
        // Before it runs: the line stays the current one (and its time runs) while it blocks.
        println(" - Running $kind.sh of $name (up to ${SCRIPT_TIMEOUT_S}s)")
        try {
            val process = ProcessBuilder(*shellCommand(isStandalone(script), script))
                .apply { environment()[MARKER] = name }
                .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
                .redirectErrorStream(true)
                .redirectOutput(out)
                .start()
            if (!process.waitFor(SCRIPT_TIMEOUT_S, TimeUnit.SECONDS)) {
                println(" - $kind of $name still running after ${SCRIPT_TIMEOUT_S}s, moving on")
            }
        } catch (_: Exception) {
            println(" - failed to run $kind of $name")
        }
    }

    private fun startService(name: String, service: File) {
        val tag = "axeron.plugin.$name"
        val log = logPipe(tag)
        val standalone = isStandalone(service)

        val execLine = if (standalone)
            "exec busybox sh -o standalone \"${service.absolutePath}\""
        else
            "exec busybox sh \"${service.absolutePath}\""

        // The plugin id reaches the shell through the env, never pasted into the script text.
        // stdin is /dev/null so nothing waits on a pipe the igniter closes when it exits.
        val service = $$"""
            busybox setsid sh -c '
              $$execLine
            ' $$log </dev/null &
            pid=$!
            resetprop "log.tag.service.$$$MARKER" "$pid"
        """.trimIndent()

        println(" - startService $name")
        try {
            ProcessBuilder("busybox", "sh", "-c", service)
                .apply { environment()[MARKER] = name }
                .start()
        } catch (_: Exception) {
            println("ERROR: startService $name")
        }
    }


    // ===============================
    // STOP / UNINSTALL
    // ===============================
    private fun stopPlugin(name: String, bin: File) {
        SystemProp.set("log.tag.fs.$name", "0")
        SystemProp.set("log.tag.props.$name", "0")

        stopService(name)
        SystemProp.set("log.tag.service.$name", "-1")

        unlinkBin(bin)
    }

    /**
     * SIGTERM to every process of the service, a grace period to exit, then SIGKILL to whatever
     * is left, re-scanned so children forked meanwhile are caught too.
     */
    private fun stopService(name: String) = stopProcesses("service $name") { servicePids(name) }

    /**
     * Stops every plugin's processes at once; the server's death watchdog runs this. A new
     * server may already be starting plugins meanwhile (a restart), so it is confined to what
     * was running when it began and what descends from or joined those, never fresh services.
     */
    private fun stopAllPlugins() = stopProcesses("all plugins", confine = true) { allPluginPids() }

    /** False when there was nothing to stop. */
    private fun stopProcesses(label: String, confine: Boolean = false, scan: () -> List<Int>): Boolean {
        var pids = scan()
        if (pids.isEmpty()) return false
        val known = HashSet(pids)
        println(" - stopping $label: ${pids.joinToString(" ")}")
        pids.forEach { signal(it, OsConstants.SIGTERM) }

        val deadline = System.currentTimeMillis() + STOP_GRACE_MS
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
            pids = if (confine) confined(scan(), known) else scan()
            if (pids.isEmpty()) return true
        }

        println(" - killing $label: ${pids.joinToString(" ")}")
        pids.forEach { signal(it, OsConstants.SIGKILL) }
        return true
    }

    /**
     * The part of [current] that is in [known], or is a child or session/group member of one of
     * them, repeated until nothing new joins; [known] grows with what is kept.
     */
    private fun confined(current: List<Int>, known: MutableSet<Int>): List<Int> {
        val keep = current.filterTo(HashSet()) { it in known }
        do {
            var grew = false
            for (pid in current) {
                if (pid in keep) continue
                val stat = readStat(pid) ?: continue
                if (stat.ppid in known || stat.session in known || stat.pgrp in known) {
                    keep.add(pid)
                    known.add(pid)
                    grew = true
                }
            }
        } while (grew)
        return keep.toList()
    }

    private fun signal(pid: Int, sig: Int) {
        try {
            Os.kill(pid, sig)
        } catch (_: ErrnoException) {
            // Already gone, or not ours to signal.
        }
    }

    /**
     * Live processes of a plugin: those carrying its [MARKER], plus every member of a session or
     * process group led by one of them. The second part catches children that cleared their env
     * but stayed with their daemon (e.g. crond's jobs, which start with a clean env).
     *
     * The igniter, its ancestors and the server are never included, nor taken as leaders, even
     * with a leaked marker (a plugin that restarted AxManager or ran the igniter itself).
     */
    private fun servicePids(name: String): List<Int> = pluginPids { it == "$MARKER=$name" }

    /** Processes of any plugin, by the same rule as [servicePids]. */
    private fun allPluginPids(): List<Int> = pluginPids { it.startsWith("$MARKER=") }

    /** [servicePids] with [isMarker] deciding which env entries count as a plugin's marker. */
    private fun pluginPids(isMarker: (String) -> Boolean): List<Int> {
        val protected = selfAndAncestors()

        val live = File("/proc").list()
            ?.mapNotNull { it.toIntOrNull() }
            ?.filter { it !in protected }
            ?.mapNotNull { pid -> readStat(pid)?.let { pid to it } }
            ?.filter { (_, stat) -> stat.state != 'Z' } // already dead, waiting to be reaped
            .orEmpty()

        val marked = live
            .filter { (pid, _) -> hasMarker(pid, isMarker) && !isServer(pid) }
            .mapTo(HashSet()) { it.first }

        return live
            .filter { (pid, stat) -> pid in marked || stat.session in marked || stat.pgrp in marked }
            .map { it.first }
    }

    /** The igniter and every process above it: stopping a plugin must never kill its caller. */
    private fun selfAndAncestors(): Set<Int> {
        val chain = HashSet<Int>()
        var pid = Os.getpid()
        while (pid > 1 && chain.add(pid)) {
            pid = readStat(pid)?.ppid ?: break
        }
        return chain
    }

    private fun isServer(pid: Int): Boolean = try {
        File("/proc/$pid/cmdline").readBytes()
            .toString(Charsets.UTF_8)
            .substringBefore('\u0000') == SERVER_PROCESS
    } catch (_: Exception) {
        false
    }

    private class ProcStat(val state: Char, val ppid: Int, val pgrp: Int, val session: Int)

    /** Fields of /proc/<pid>/stat; the name field can hold spaces and ')'. */
    private fun readStat(pid: Int): ProcStat? = try {
        val stat = File("/proc/$pid/stat").readText()
        val fields = stat.substring(stat.lastIndexOf(')') + 2).split(' ')
        // After the name: state, ppid, pgrp, session, ...
        ProcStat(fields[0][0], fields[1].toInt(), fields[2].toInt(), fields[3].toInt())
    } catch (_: Exception) {
        null
    }

    /** Whether an env entry of the process passes [isMarker]; unreadable envs (other uids) count as no. */
    private fun hasMarker(pid: Int, isMarker: (String) -> Boolean): Boolean = try {
        File("/proc/$pid/environ").readBytes()
            .toString(Charsets.UTF_8)
            .split('\u0000')
            .any(isMarker)
    } catch (_: Exception) {
        false
    }

    private fun uninstallPlugin(name: String, uninstall: File, bin: File) {
        if (uninstall.exists()) {
            runScript(name, uninstall, "uninstall")
            // uninstall.sh may itself have started something; it goes with the plugin.
            stopService(name)
        }
        unlinkBin(bin)
        File("$AXERONDIR/plugins/$name").deleteRecursively()
    }

    // ===============================
    // LINK BIN
    // ===============================
    private fun linkBin(bin: File) {
        if (!bin.isDirectory) return

        AXERONXBIN.mkdirs()
        bin.listFiles()?.forEach { src ->
            val dst = File(AXERONXBIN, src.name)

            try {
                if (dst.exists()) {
                    if (!dst.delete()) {
                        println(" - Failed to remove: ${dst.absolutePath}")
                    }
                }
                // buat symlink baru
                Os.symlink(src.absolutePath, dst.absolutePath)
                println(" - Linked : ${dst.absolutePath}")
            } catch (_: ErrnoException) {
                println(" - Failed Linking : ${src.absolutePath}")
            }
        }
    }

    private fun unlinkBin(bin: File) {
        if (!bin.exists()) return
        bin.listFiles()?.forEach { src ->
            val dst = File(AXERONXBIN, src.name)
            if (!dst.exists()) return@forEach
            if (Os.readlink(dst.absolutePath) == src.absolutePath) {
                println(" - Unlinked : ${dst.absolutePath}")
                File(AXERONXBIN, src.name).delete()
            }
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

    /** Running means a live process of the service exists, so a crashed one is started again. */
    private fun isServiceRunning(name: String): Boolean = servicePids(name).isNotEmpty()

}