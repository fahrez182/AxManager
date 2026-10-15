package frb.axeron.manager.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import frb.axeron.api.Axeron
import frb.axeron.manager.data.SettingsRepository.SettingType

/**
 * What can be written without the server. The server grants WRITE_SECURE_SETTINGS and allows
 * WRITE_SETTINGS whenever the manager binds, and both stay granted after it stops.
 */
object SettingsAccess {

    fun canWriteSecure(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED

    fun canWriteSystem(context: Context): Boolean = Settings.System.canWrite(context)

    /** Both granted: Global, Secure and System are all writable without the server. */
    fun canWriteWithoutServer(context: Context): Boolean =
        canWriteSecure(context) && canWriteSystem(context)

    /** Whether [type] can be written right now: through the server, or the app's own grants. */
    fun canWrite(context: Context, type: SettingType): Boolean =
        Axeron.pingBinder() || when (type) {
            SettingType.GLOBAL, SettingType.SECURE -> canWriteSecure(context)
            SettingType.SYSTEM -> canWriteSystem(context)
            SettingType.ANDROID_PROP, SettingType.AXERON_ENV -> false
        }
}
