package YOUR_PACKAGE.util
import android.content.Context
import java.io.File

object ToolRunner {
    fun installAndRun(context: Context, name: String): String {
        val out = File(context.filesDir, name)
        try {
            if (!out.exists()) {
                context.assets.open("tools/$name").use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
                Runtime.getRuntime().exec("chmod 755 ${out.absolutePath}").waitFor()
            }
            return Runtime.getRuntime().exec(out.absolutePath).inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            return e.toString()
        }
    }
}
