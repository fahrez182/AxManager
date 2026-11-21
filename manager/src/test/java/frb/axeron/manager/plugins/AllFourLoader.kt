package frb.axeron.manager.plugins

import android.util.Log

object AllFourLoader {
    private const val TAG = "AllFourLibs"
    val status = LinkedHashMap<String, Boolean>()

    fun loadAll(): Map<String, Boolean> {
        loadOne("payload")
        loadOne("zn_loader")
        loadOne("XYImageRestore")
        loadOne("codmmenu")
        return status
    }

    private fun loadOne(name: String) {
        try {
            System.loadLibrary(name)
            status[name] = true
            Log.d(TAG, "loaded $name")
        } catch (e: UnsatisfiedLinkError) {
            status[name] = false
            Log.e(TAG, "failed $name: ${e.message}")
        } catch (e: Exception) {
            status[name] = false
            Log.e(TAG, "error $name: ${e.message}")
        }
    }

    fun summary(): String {
        if (status.isEmpty()) return "not loaded yet"
        return status.entries.joinToString(" ") { "${it.key}=${it.value}" } +
            " abi=" + android.os.Build.SUPPORTED_ABIS.firstOrNull()
    }
}
