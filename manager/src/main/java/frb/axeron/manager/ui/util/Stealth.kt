package frb.axeron.manager.ui.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import frb.axeron.api.Axeron
import frb.axeron.manager.ui.RequestAccessActivity
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import java.io.File

/**
 * Stealth: an app the manager hasn't allowed can't tell AxManager from nothing. The setting is a
 * flag file the server reads when a request comes (no dialog then, only a no), written through
 * the server like the stop-plugins flag; the manager's own part (the request-access activity,
 * the relay) follows it. Allowed apps keep working.
 */
object Stealth {

    private fun flag(): String = File(
        PathHelper.getWorkingPath(Axeron.getAxeronInfo().isRoot(), AxeronApiConstant.folder.PARENT),
        AxeronApiConstant.folder.STEALTH
    ).absolutePath

    /** As on disk; off when the server is out of reach. */
    fun isOn(): Boolean =
        runCatching { Axeron.newFileService()?.exists(flag()) == true }.getOrDefault(false)

    /** Writes the flag, then has the activity follow what is really on disk. Off the main thread. */
    fun set(context: Context, enable: Boolean): Boolean {
        runCatching {
            val fs = Axeron.newFileService() ?: return@runCatching
            if (enable) fs.createNewFile(flag()) else fs.delete(flag())
        }
        return isOn().also { sync(context, it) }
    }

    /**
     * Disabled, the request-access activity resolves for no one: an app asking gets
     * ActivityNotFoundException, as with AxManager not installed, and `<queries>` find nothing.
     */
    fun sync(context: Context, on: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, RequestAccessActivity::class.java),
            if (on) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
            PackageManager.DONT_KILL_APP
        )
    }
}
