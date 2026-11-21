package frb.axeron.manager.plugins
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
object CopgAutoSetup {
  private const val TAG = "CopgAuto"
  fun init(c: Context): String {
    val m = LinkedHashMap<String, Boolean>()
    listOf("payload","zn_loader","XYImageRestore","codmmenu").forEach {
      try { System.loadLibrary(it); m[it]=true } catch(e: UnsatisfiedLinkError) { m[it]=false }
    }
    var zp: String? = null
    try {
      val out = File(c.filesDir, "builtin_COPG.zip")
      if(!out.exists()) {
        c.assets.open("builtin_plugins/COPG.zip").use { ins -> FileOutputStream(out).use { o -> ins.copyTo(o) } }
      }
      zp = out.absolutePath
    } catch(e: Exception) { Log.e(TAG, e.message.toString()) }
    val r = "libs=$m copg=$zp"
    Log.d(TAG, r)
    return r
  }
}
