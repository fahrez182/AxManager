package frb.axeron.manager.data

import androidx.compose.runtime.Immutable
import frb.axeron.shell.Axsh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Processes as the server sees them, through toybox `ps` and `/proc`. Nothing is checked up
 * front: every read and signal is just run, and what the kernel or SELinux refuses comes back
 * as the command's own error, so shell and root each get whatever they can reach.
 */
object ProcessControl {

    @Immutable
    data class Proc(
        val pid: Int,
        val ppid: Int,
        val pgid: Int,
        val uid: Int,
        val user: String,
        /** R, S, D, Z, T, … */
        val state: String,
        /** Android scheduling policy (fg, bg, ta, …); null when `ps` doesn't know PCY. */
        val policy: String?,
        val priority: Int?,
        val nice: Int?,
        /** KiB. */
        val rss: Long,
        /** KiB. */
        val vsz: Long,
        /** Average over the process's whole life, as `ps` reports it, not the current load. */
        val cpu: Float?,
        /** CPU time used, `[DD-]HH:MM:SS`. */
        val time: String?,
        /** Since start, `[DD-]HH:MM:SS`. */
        val elapsed: String?,
        /** SELinux context. */
        val label: String?,
        /** The kernel's name for it (comm, 15 chars at most); null when `ps` doesn't know CMD. */
        val comm: String?,
        /** Full command line; kernel threads show as `[name]`, or empty on older toybox. */
        val args: String,
    ) {
        /** argv[0] without its path, or the bracketed kernel thread name. */
        val name: String = args.substringBefore(' ').let {
            if (it.startsWith('[')) args else it.substringAfterLast('/')
        }.ifEmpty { comm?.let { "[$it]" } ?: "?" }
    }

    /** A read or signal: [ok] with what it printed, or the error it printed (raw). */
    data class Outcome(val ok: Boolean, val text: String)

    enum class Signal { TERM, KILL, INT, HUP, QUIT, STOP, CONT, USR1, USR2 }

    /** One row of `/proc/[pid]/limits`; [unit] empty when the limit has none. */
    data class Limit(val name: String, val soft: String, val hard: String, val unit: String)

    /** One open fd: its number and what it points at (a path, `socket:[…]`, `pipe:[…]`, …). */
    data class Fd(val fd: Int, val target: String)

    // Every field but the last has no spaces, so a row splits on whitespace with the command
    // line left whole at the end. Older toybox lacks some fields: BASIC is the fallback.
    // CMD (comm) is the one that may hold a space, rarely: it sits just before CMDLINE, so such
    // a row only gets the rest of its name prepended to its command line.
    // No TID or TCNT: asking for either makes toybox print a row per thread, all with the
    // process's PID. The thread count is in /proc/[pid]/status.
    internal val FULL = listOf(
        "PID", "PPID", "PGID", "UID", "USER", "S", "PCY", "PRI", "NI",
        "RSS", "VSZ", "%CPU", "TIME", "ELAPSED", "LABEL", "CMD", "CMDLINE"
    )
    internal val BASIC = listOf("PID", "PPID", "PGID", "UID", "USER", "S", "RSS", "VSZ", "CMDLINE")

    private val WHITESPACE = Regex("\\s+")

    private class Exec(val code: Int, val out: String, val err: String)

    /** Every process; what each `ps` printed when none of them worked. */
    suspend fun list(): Result<List<Proc>> {
        val errors = LinkedHashSet<String>()
        // `ps` from PATH first, so "prefer system binaries" decides. AxManager's BusyBox ps
        // (first on PATH otherwise) takes none of these options; then Android's own toybox,
        // by name, which BusyBox has no applet for.
        for (ps in listOf(arrayOf("ps"), arrayOf("toybox", "ps"))) {
            for (fields in listOf(FULL, BASIC)) {
                val exec = run(*ps, "-A", "-w", "-o", fields.joinToString(","))
                    ?: return Result.failure(IllegalStateException("Server unreachable"))
                val procs = parse(exec.out, fields)
                if (exec.code == 0 && procs.isNotEmpty()) return Result.success(procs)
                errors += ps.joinToString(" ") + ": " +
                        exec.err.ifBlank { exec.out }.trim().ifBlank { "exit ${exec.code}" }
            }
        }
        return Result.failure(IllegalStateException(errors.joinToString("\n")))
    }

    internal fun parse(output: String, fields: List<String>): List<Proc> =
        output.lineSequence().drop(1).mapNotNull { line ->
            val parts = line.trimStart().split(WHITESPACE, limit = fields.size)
            if (parts.size < fields.size - 1) return@mapNotNull null
            val col = fun(name: String): String? =
                fields.indexOf(name).takeIf { it >= 0 }?.let(parts::getOrNull)
            Proc(
                pid = col("PID")?.toIntOrNull() ?: return@mapNotNull null,
                ppid = col("PPID")?.toIntOrNull() ?: 0,
                pgid = col("PGID")?.toIntOrNull() ?: 0,
                uid = col("UID")?.toIntOrNull() ?: -1,
                user = col("USER").orEmpty(),
                state = col("S").orEmpty(),
                policy = col("PCY")?.takeIf { it != "-" },
                priority = col("PRI")?.toIntOrNull(),
                nice = col("NI")?.toIntOrNull(),
                rss = col("RSS")?.toLongOrNull() ?: 0,
                vsz = col("VSZ")?.toLongOrNull() ?: 0,
                cpu = col("%CPU")?.toFloatOrNull(),
                time = col("TIME"),
                elapsed = col("ELAPSED"),
                label = col("LABEL"),
                comm = col("CMD"),
                args = col("CMDLINE")?.trim().orEmpty(),
            )
        }.toList()

    /**
     * `/proc/[pid]/[entry]` as text. NUL-separated entries (cmdline, environ) come back one
     * item per line.
     */
    suspend fun readProc(pid: Int, entry: String): Outcome =
        outcome(run("cat", "/proc/$pid/$entry"))
            .let { if (it.ok) it.copy(text = it.text.trimEnd('\u0000').replace('\u0000', '\n')) else it }

    /** Target of a `/proc/[pid]` link: exe, cwd, root, fd/N. */
    suspend fun readLink(pid: Int, link: String): Outcome =
        outcome(run("readlink", "/proc/$pid/$link"))

    /** Open fds, one `N -> target` per line. */
    suspend fun fds(pid: Int): Outcome =
        outcome(run("ls", "-l", "/proc/$pid/fd"))

    suspend fun signal(pid: Int, signal: Signal): Outcome =
        outcome(run("kill", "-s", signal.name, "$pid"))

    /** [signal] to every process in group [pgid]. */
    // Through sh: its kill takes `-- -pgid` for a group, toybox's reads that as an option.
    suspend fun signalGroup(pgid: Int, signal: Signal): Outcome =
        outcome(run("sh", "-c", "kill -s ${signal.name} -- -$pgid"))

    /** `/proc/[pid]/status`: `Key:<tab>value` per line, in kernel order. */
    fun parseStatus(text: String): Map<String, String> =
        text.lineSequence().mapNotNull { line ->
            val i = line.indexOf(':')
            if (i <= 0) null else line.substring(0, i).trim() to line.substring(i + 1).trim()
        }.toMap(LinkedHashMap())

    /**
     * `/proc/[pid]/limits` is a fixed-width table; the header says where each column starts,
     * since names ("Max file size") and values hold spaces.
     */
    fun parseLimits(text: String): List<Limit> {
        val lines = text.lines().filter { it.isNotBlank() }
        val header = lines.firstOrNull() ?: return emptyList()
        val soft = header.indexOf("Soft Limit")
        val hard = header.indexOf("Hard Limit")
        val units = header.indexOf("Units")
        if (soft < 0 || hard < 0 || units < 0) return emptyList()
        fun String.cut(from: Int, to: Int) =
            if (from >= length) "" else substring(from, minOf(to, length)).trim()
        return lines.drop(1).map {
            Limit(it.cut(0, soft), it.cut(soft, hard), it.cut(hard, units), it.cut(units, it.length))
        }
    }

    /** `environ` as [readProc] returns it (one `NAME=value` per line). */
    fun parseEnviron(text: String): List<Pair<String, String>> =
        text.lines().filter { it.isNotEmpty() }.map {
            val i = it.indexOf('=')
            if (i < 0) it to "" else it.substring(0, i) to it.substring(i + 1)
        }

    /** [fds] output: `ls -l` lines ending in `N -> target`. */
    fun parseFds(text: String): List<Fd> =
        text.lines().mapNotNull { line ->
            val arrow = line.indexOf(" -> ")
            if (arrow < 0) return@mapNotNull null
            val fd = line.substring(0, arrow).trimEnd().substringAfterLast(' ').toIntOrNull()
                ?: return@mapNotNull null
            Fd(fd, line.substring(arrow + 4))
        }.sortedBy { it.fd }

    /** `cgroup`: `id:controllers:path`; cgroup v2 has no controllers. */
    fun parseCgroup(text: String): List<Pair<String, String>> =
        text.lines().filter { it.isNotBlank() }.map {
            val parts = it.split(':', limit = 3)
            if (parts.size < 3) it to "" else parts[1].ifEmpty { "v2" } to parts[2]
        }

    private fun outcome(exec: Exec?): Outcome = when {
        exec == null -> Outcome(false, "Server unreachable")
        exec.code == 0 -> Outcome(true, exec.out)
        else -> Outcome(false, exec.err.ifBlank { exec.out }.trim().ifBlank { "exit ${exec.code}" })
    }

    private suspend fun run(vararg argv: String): Exec? = withContext(Dispatchers.IO) {
        runCatching {
            val process = Axsh.exec(arrayOf(*argv))
            process.outputStream.close()
            // Both streams at once: a full pipe on one would stall the process.
            coroutineScope {
                val out = async { process.inputStream.bufferedReader().readText() }
                val err = async { process.errorStream.bufferedReader().readText() }
                Exec(process.waitFor(), out.await(), err.await())
            }
        }.getOrNull()
    }
}
