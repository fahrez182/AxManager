package frb.axeron.adb.util

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.TileService

/**
 * Intents that open developer options, best first. Each is pinned to the Settings app: left
 * open, any app answering the same action turns the launch into an "Open with" chooser, which
 * counts as a successful launch and never reaches the next candidate. Pinned, a Settings app
 * that can't take one throws ActivityNotFoundException, so the caller moves on.
 */
object DeveloperSettings {

    private const val SETTINGS_PACKAGE = "com.android.settings"

    fun candidates(flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK): List<Intent> = listOf(
        // Developer options, scrolled to the wireless debugging switch.
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            .setPackage(SETTINGS_PACKAGE)
            .putExtra(":settings:fragment_args_key", "toggle_adb_wireless")
            .addFlags(flags),
        // The wireless debugging tile's own settings.
        Intent(TileService.ACTION_QS_TILE_PREFERENCES)
            .setPackage(SETTINGS_PACKAGE)
            .putExtra(
                Intent.EXTRA_COMPONENT_NAME,
                ComponentName(
                    SETTINGS_PACKAGE,
                    "com.android.settings.development.qstile.DevelopmentTiles\$WirelessDebugging"
                )
            )
            .addFlags(flags),
        Intent(Settings.ACTION_SETTINGS)
            .setPackage(SETTINGS_PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
