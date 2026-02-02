package frb.axeron.manager.ui.util

import android.util.Log
import com.topjohnwu.superuser.Shell
import frb.axeron.Axerish

private const val TAG = "AxeronCli"

fun createShellBuilder(globalMnt: Boolean = false): Shell.Builder {
    return Shell.Builder.create().run {
        val cmd = buildString {
            append("sh ${Axerish.axrun_path.absolutePath}")
            append(" || ")
            append("su")
            if (globalMnt) append(" --mount-master")
        }
        setCommands("sh", "-c", cmd)
    }
}

fun createShell(globalMnt: Boolean = false): Shell {
    return runCatching {
        createShellBuilder(globalMnt).build()
    }.getOrElse { e ->
        Log.w(TAG, "su failed: ", e)
        Shell.Builder.create().apply {
            if (globalMnt) setFlags(Shell.FLAG_MOUNT_MASTER)
        }.build()
    }
}

inline fun <T> withNewShell(
    globalMnt: Boolean = false,
    block: Shell.() -> T
): T {
    return createShell(globalMnt).use(block)
}