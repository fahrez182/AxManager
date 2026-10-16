package frb.axeron.manager.data

import android.content.Context
import android.util.Log
import frb.axeron.api.Axeron
import java.io.File

/**
 * `/sdcard/axeron/axsh`: a tiny `sh` wrapper around our libaxsh.so, so a terminal has one name
 * to run however often AxManager updates (and its /data/app path with it).
 *
 * /sdcard is noexec, so it is run as `sh /sdcard/axeron/axsh`, or copied once into a terminal's
 * own bin. Either way it heals itself: when the path it was written with is gone, `pm path`
 * finds the current one. It is rewritten whenever its content differs (on the server binder and
 * when the QuickShell card is opened); the manager can't write /sdcard itself, the server does.
 */
object AxshLauncher {

    private const val TAG = "AxshLauncher"

    const val PATH = "/sdcard/axeron/axsh"

    fun binaryPath(context: Context): String =
        File(context.applicationInfo.nativeLibraryDir, "libaxsh.so").absolutePath

    /** `sh` + the launcher, for any terminal that can read /sdcard. */
    const val RUN_COMMAND = "sh $PATH"

    /** Once in Termux, then `axsh` is a command like any other. */
    const val TERMUX_INSTALL_COMMAND = "install -m 700 $PATH \$PREFIX/bin/axsh"

    fun script(context: Context): String {
        val binary = binaryPath(context)
        // nativeLibraryDir ends in the APK's lib folder name for this device: arm64, arm, x86_64.
        val abi = File(context.applicationInfo.nativeLibraryDir).name
        val pkg = context.packageName
        return """
            |#!/system/bin/sh
            |# axsh launcher, written by AxManager. Safe to copy anywhere.
            |# The path changes with every AxManager update; pm then finds the new one.
            |AXSH='$binary'
            |[ -x "${'$'}AXSH" ] || AXSH="${'$'}(pm path $pkg | sed -n 's/^package://;s/base\.apk${'$'}//p')lib/$abi/libaxsh.so"
            |exec "${'$'}AXSH" "${'$'}@"
            |""".trimMargin()
    }

    /**
     * Writes [PATH] when it is missing or differs from [script]. Blocking (binder + file I/O):
     * call it off the main thread. False when the server is out of reach or the write failed.
     */
    fun ensure(context: Context): Boolean {
        val fs = Axeron.newFileService() ?: return false
        val wanted = script(context)
        val current = runCatching {
            if (!fs.exists(PATH)) null
            else fs.setFileInputStream(PATH).use { it.readBytes().decodeToString() }
        }.getOrNull()
        if (current == wanted) return true

        val session = fs.getStreamSession(PATH, true, false) ?: return false
        return try {
            session.outputStream.use { it.write(wanted.toByteArray()) }
            Log.i(TAG, "wrote $PATH")
            true
        } catch (e: Exception) {
            Log.w(TAG, "can't write $PATH", e)
            false
        }
    }
}
