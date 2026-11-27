package YOUR_PACKAGE.util

object NativeBridge {
    fun load() {
        try { System.loadLibrary("codmmenu") } catch (e: Exception) { e.printStackTrace() }
        try { System.loadLibrary("XYImageRestore") } catch (e: Exception) { e.printStackTrace() }
    }
}
