package frb.axeron.manager.ui.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.edit
import frb.axeron.api.core.AxeronSettings
import frb.axeron.manager.R
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

object LocaleHelper {

    /**
     * Android 13+ keeps the app language itself (LocaleManager); below that it is our preference,
     * applied in attachBaseContext.
     */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    private val useSystemAppLocales: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * The languages the app is translated into, from res/xml/locales_config.xml: the same list the
     * system's per-app language page shows, so the two never disagree.
     */
    fun getSupportedLocales(context: Context): List<Locale> {
        val locales = mutableListOf<Locale>()
        try {
            context.resources.getXml(R.xml.locales_config).use { parser ->
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                        val tag = parser.getAttributeValue(ANDROID_NS, "name") ?: continue
                        locales.add(parseLocaleTag(tag))
                    }
                }
            }
        } catch (_: Exception) {
            // An unreadable list leaves only "System default".
        }
        return locales.sortedBy { it.getDisplayName(it).lowercase(it) }
    }

    /**
     * Switches the app to [locale], or back to the system language when null. Android 13+ recreates
     * the activity itself; below that the saved tag is applied on [Activity.recreate].
     */
    fun setAppLocale(activity: Activity, locale: Locale?) {
        if (useSystemAppLocales) {
            val localeManager = activity.getSystemService(LocaleManager::class.java)
            localeManager.applicationLocales =
                if (locale == null) LocaleList.getEmptyLocaleList() else LocaleList(locale)
        } else {
            preferences(activity).edit {
                putString(AxeronSettings.LANGUAGE, locale?.toLanguageTag() ?: "system")
            }
            activity.recreate()
        }
    }

    /**
     * Apply saved language setting to context (for Android < 13)
     */
    fun applyLanguage(context: Context): Context {
        // On Android 13+, language is handled by system
        if (useSystemAppLocales) {
            return context
        }

        val locale = savedLocale(context) ?: return context
        val configuration = Configuration()
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    /**
     * Get current app locale; null means the system language.
     */
    fun getCurrentAppLocale(context: Context): Locale? {
        return if (useSystemAppLocales) {
            try {
                val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
                if (locales.isEmpty) null else locales.get(0)
            } catch (_: Exception) {
                null
            }
        } else {
            savedLocale(context)
        }
    }

    private fun savedLocale(context: Context): Locale? {
        val tag = preferences(context).getString(AxeronSettings.LANGUAGE, "system") ?: "system"
        return if (tag.equals("system", ignoreCase = true)) null else parseLocaleTag(tag)
    }

    private fun preferences(context: Context) = try {
        AxeronSettings.getPreferences()
    } catch (_: Exception) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    /**
     * "in", "zh-CN", and the older "zh_CN" all parse; BCP 47 alone would turn "zh_CN" into an
     * empty locale.
     */
    private fun parseLocaleTag(tag: String): Locale = Locale.forLanguageTag(tag.replace('_', '-'))

    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
}
