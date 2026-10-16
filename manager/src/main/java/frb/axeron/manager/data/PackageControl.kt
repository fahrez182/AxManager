package frb.axeron.manager.data

import frb.axeron.api.Axeron
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Package actions run through the server, as shell or root. What shell may not do comes back as
 * an [Outcome] with [Outcome.needsRoot] rather than silently doing less.
 */
object PackageControl {

    data class Outcome(
        val ok: Boolean,
        /** Done, but only the part this mode allows (e.g. external cache only). */
        val partial: Boolean = false,
        val needsRoot: Boolean = false,
        val serverOff: Boolean = false,
        /** What the command printed when it failed, trimmed for display. */
        val error: String? = null
    )

    private class Exec(val code: Int, val out: String, val err: String)

    /** Android package names: letters, digits, `_` and `.` — safe to put in a script. */
    private val PACKAGE_NAME = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$")

    fun userIdOf(uid: Int): Int = uid / 100_000

    suspend fun forceStop(pkg: String, userId: Int): Outcome =
        command("am", "force-stop", "--user", "$userId", pkg)

    suspend fun clearData(pkg: String, userId: Int): Outcome =
        command("pm", "clear", "--user", "$userId", pkg)

    /**
     * Runtime and development permissions only; `pm` refuses normal, signature and
     * system-fixed ones, and that refusal comes back as the error.
     */
    suspend fun setPermission(pkg: String, userId: Int, permission: String, grant: Boolean): Outcome =
        command("pm", if (grant) "grant" else "revoke", "--user", "$userId", pkg, permission)

    /** [mode] as `cmd appops` takes it: allow, ignore, deny, default, foreground. */
    suspend fun setAppOp(pkg: String, userId: Int, op: String, mode: String): Outcome =
        command("cmd", "appops", "set", "--user", "$userId", pkg, op, mode)

    /** Battery optimization exemption (the Doze allowlist), device-wide rather than per user. */
    suspend fun setBatteryExempt(pkg: String, exempt: Boolean): Outcome =
        command("cmd", "deviceidle", "whitelist", (if (exempt) "+" else "-") + pkg)

    /** `disable-user` hides the app for [userId] until enabled again; nothing is deleted. */
    suspend fun setEnabled(pkg: String, userId: Int, enabled: Boolean): Outcome =
        command("pm", if (enabled) "enable" else "disable-user", "--user", "$userId", pkg)

    /**
     * A user app is removed for good; a system app only for [userId] (it stays on the system
     * partition and `cmd package install-existing` brings it back).
     */
    suspend fun uninstall(pkg: String, userId: Int, systemApp: Boolean, keepData: Boolean): Outcome {
        val args = buildList {
            addAll(listOf("pm", "uninstall"))
            if (keepData) add("-k")
            if (systemApp) addAll(listOf("--user", "$userId"))
            add(pkg)
        }
        return command(*args.toTypedArray())
    }

    /**
     * Shell can only reach the external cache (Android/data/<pkg>/cache); the internal
     * `cache` / `code_cache` folders are private to the app and need root.
     */
    suspend fun clearCache(pkg: String, userId: Int): Outcome {
        if (!PACKAGE_NAME.matches(pkg)) return Outcome(false, error = "Invalid package name")
        val root = isRoot() ?: return Outcome(false, serverOff = true)
        val dirs = buildList {
            if (root) {
                listOf("/data/user/$userId", "/data/user_de/$userId").forEach { base ->
                    add("$base/$pkg/cache")
                    add("$base/$pkg/code_cache")
                }
            }
            add("/storage/emulated/$userId/Android/data/$pkg/cache")
        }
        // Empty each folder but keep it: apps expect their cache dir to exist.
        val script = dirs.joinToString("; ") { "[ -d '$it' ] && find '$it' -mindepth 1 -delete" } + "; exit 0"
        val outcome = command("sh", "-c", script)
        return if (outcome.ok && !root) outcome.copy(partial = true) else outcome
    }

    /** `OP_NAME: mode; time=…`, optionally prefixed `Uid mode: ` for a uid-wide override. */
    // MIUI/HyperOS print their own ops as MIUIOP(10008); `cmd appops set` takes those by number.
    private val APP_OP_LINE = Regex("^\\s*(Uid mode: )?([A-Z0-9_]+|MIUIOP\\(\\d+\\)): ([a-z]+)")
    private val MIUI_OP_NAME = Regex("^MIUIOP\\((\\d+)\\)$")

    /**
     * An app-op is set in two places: for the package, and uid-wide. The system enforces a
     * uid-wide mode other than `default` over the package's, so writing only the package
     * mode can look like it did nothing.
     */
    data class AppOpState(val packageMode: String?, val uidMode: String?) {
        val uidOverrides: Boolean get() = uidMode != null && uidMode != "default"
        val effective: String? get() = if (uidOverrides) uidMode else packageMode
    }

    /**
     * App-op state of [pkg] by op name ("SYSTEM_ALERT_WINDOW"). Null without the server: an app
     * can't read another app's app-ops itself (that needs UPDATE_APP_OPS_STATS).
     */
    suspend fun appOps(pkg: String, userId: Int): Map<String, AppOpState>? {
        if (!Axeron.pingBinder()) return null
        val exec = run("cmd", "appops", "get", "--user", "$userId", pkg) ?: return null
        if (exec.code != 0) return null
        return parseAppOps(exec.out)
    }

    internal fun parseAppOps(output: String): Map<String, AppOpState> {
        val packageModes = HashMap<String, String>()
        val uidModes = HashMap<String, String>()
        output.lineSequence().forEach { line ->
            val m = APP_OP_LINE.find(line) ?: return@forEach
            val (uid, name, mode) = m.destructured
            // Keyed as it is written back: "10008" rather than "MIUIOP(10008)".
            val op = MIUI_OP_NAME.find(name)?.groupValues?.get(1) ?: name
            (if (uid.isNotEmpty()) uidModes else packageModes)[op] = mode
        }
        return (packageModes.keys + uidModes.keys).associateWith {
            AppOpState(packageModes[it], uidModes[it])
        }
    }

    /**
     * Sets the package mode of [op]. With [clearUidOverride], also resets a uid-wide mode to
     * `default`, otherwise that one keeps winning and the change has no effect.
     */
    suspend fun setAppOpMode(
        pkg: String,
        userId: Int,
        op: String,
        mode: String,
        clearUidOverride: Boolean
    ): Outcome {
        val outcome = setAppOp(pkg, userId, op, mode)
        if (!outcome.ok || !clearUidOverride) return outcome
        return setUidAppOp(pkg, userId, op, "default")
    }

    /**
     * Sets the uid-wide mode of [op]: every package of [pkg]'s uid, ahead of their package
     * modes. `default` hands the decision back to the package mode.
     */
    suspend fun setUidAppOp(pkg: String, userId: Int, op: String, mode: String): Outcome =
        command("cmd", "appops", "set", "--user", "$userId", "--uid", pkg, op, mode)

    private suspend fun isRoot(): Boolean? = withContext(Dispatchers.IO) {
        if (!Axeron.pingBinder()) null
        else runCatching { Axeron.getAxeronInfo().isRoot() }.getOrNull()
    }

    private suspend fun command(vararg argv: String): Outcome {
        if (!Axeron.pingBinder()) return Outcome(false, serverOff = true)
        val exec = run(*argv) ?: return Outcome(false, serverOff = true)
        // pm/am report some failures on stdout with exit code 0 ("Failure [...]", "Failed").
        val printedFailure = exec.out.lineSequence().any {
            it.startsWith("Failure") || it.startsWith("Failed") || it.startsWith("Error")
        }
        if (exec.code == 0 && !printedFailure) return Outcome(true)

        val message = exec.err.ifBlank { exec.out }.trim()
        val denied = message.contains("SecurityException") ||
                message.contains("Permission Denial") ||
                message.contains("Permission denied") ||
                message.contains("not allowed", ignoreCase = true)
        return Outcome(
            ok = false,
            needsRoot = denied && isRoot() == false,
            error = message.lines().takeLast(3).joinToString("\n").take(300).ifBlank { "exit ${exec.code}" }
        )
    }

    private suspend fun run(vararg argv: String): Exec? = withContext(Dispatchers.IO) {
        runCatching {
            val process = Axeron.newProcess(arrayOf(*argv))
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
