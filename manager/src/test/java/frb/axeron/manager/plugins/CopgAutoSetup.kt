package frb.axeron.manager.plugins

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object CopgAutoSetup {
    private const val TAG = "CopgAutoSetup"
    private const val ASSET_PATH = "builtin_plugins/COPG.zip"

    fun init(context: Context) {
        try {
            val outDir = File(context.filesDir, "builtin_plugins")
            if (!outDir.exists()) outDir.mkdirs()
            val outFile = File(outDir, "COPG.zip")
            if (outFile.exists() && outFile.length() > 0) {
                return
            }
            context.assets.open(ASSET_PATH).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "COPG.zip copied to " + outFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "COPG auto-setup failed: " + e.message)
        }
    }

    fun getZipFile(context: Context): File {
        return File(File(context.filesDir, "builtin_plugins"), "COPG.zip")
    }
}
