package frb.axeron.manager.ui.util

import android.util.Log
import com.topjohnwu.superuser.Shell
import frb.axeron.manager.AxeronApplication.Companion.axeronApp
import java.io.File

private const val TAG = "AxeronCli"

/** The manager's own axsh client; it reaches the server through the in-process AxshBroker. */
private val axshPath: String
    get() = File(axeronApp.applicationInfo.nativeLibraryDir, "libaxsh.so").absolutePath

fun createShellBuilder(globalMnt: Boolean = false): Shell.Builder {
    return Shell.Builder.create().run {
        val cmd = buildString {
            append("'$axshPath'")
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
