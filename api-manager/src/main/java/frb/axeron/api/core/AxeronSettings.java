package frb.axeron.api.core;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.util.Locale;

import frb.axeron.api.utils.EmptySharedPreferencesImpl;


public class AxeronSettings {

    public static final String NAME = "settings";
    public static final String APP_THEME_ID = "app_theme_id";
    public static final String ENABLE_DYNAMIC_COLOR = "enable_dynamic_color";
    public static final String ENABLE_DEVELOPER_OPTIONS = "enable_developer_options";
    public static final String ENABLE_WEB_DEBUGGING = "enable_web_debugging";
    public static final String CUSTOM_PRIMARY_COLOR = "custom_primary_color";
    public static final String ENABLE_IGNITE_RELOG = "enable_ignite_relog";
    public static final String LANGUAGE = "language";
    public static final String KEEP_START_ON_BOOT = "start_on_boot";

    private static SharedPreferences sPreferences;

    public static SharedPreferences getPreferences() {
        return sPreferences;
    }

    @NonNull
    private static Context getSettingsStorageContext(@NonNull Context context) {
        Context storageContext;
        storageContext = context.createDeviceProtectedStorageContext();

        storageContext = new ContextWrapper(storageContext) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                try {
                    return super.getSharedPreferences(name, mode);
                } catch (IllegalStateException e) {
                    // SharedPreferences in credential encrypted storage are not available until after user is unlocked
                    return new EmptySharedPreferencesImpl();
                }
            }
        };

        return storageContext;
    }

    public static void initialize(Context context) {
        if (sPreferences == null) {
            sPreferences = getSettingsStorageContext(context)
                    .getSharedPreferences(NAME, Context.MODE_PRIVATE);
        }
    }

    @LaunchMethod
    public static int getLastLaunchMode() {
        return getPreferences().getInt("mode", LaunchMethod.UNKNOWN);
    }

    public static void setLastLaunchMode(@LaunchMethod int method) {
        getPreferences().edit().putInt("mode", method).apply();
    }

    // IGNITE RELOG

    public static boolean getEnableIgniteRelog() {
        return getPreferences().getBoolean(ENABLE_IGNITE_RELOG, false);
    }

    public static void setEnableIgniteRelog(boolean enable) {
        getPreferences().edit().putBoolean(ENABLE_IGNITE_RELOG, enable).apply();
    }


    // DEVELOPER MODE

    public static boolean getEnableDeveloperOptions() {
        return getPreferences().getBoolean(ENABLE_DEVELOPER_OPTIONS, false);
    }

    public static void setEnableDeveloperOptions(boolean enable) {
        getPreferences().edit().putBoolean(ENABLE_DEVELOPER_OPTIONS, enable).apply();
    }

    // WEB DEBUGGING

    public static boolean getEnableWebDebugging() {
        return getPreferences().getBoolean(ENABLE_WEB_DEBUGGING, false);
    }

    public static void setEnableWebDebugging(boolean enable) {
        getPreferences().edit().putBoolean(ENABLE_WEB_DEBUGGING, enable).apply();
    }

    // APP THEME

    public static int getAppThemeId() {
        return getPreferences().getInt(APP_THEME_ID, 0);
    }

    public static void setAppThemeId(int id) {
        getPreferences().edit().putInt(APP_THEME_ID, id).apply();
    }

    // DYNAMIC COLOR

    public static boolean getEnableDynamicColor() {
        return getPreferences().getBoolean(ENABLE_DYNAMIC_COLOR, false);
    }

    public static void setEnableDynamicColor(boolean enable) {
        getPreferences().edit().putBoolean(ENABLE_DYNAMIC_COLOR, enable).apply();
    }

    // ON BOOT

    public static boolean getStartOnBoot() {
        return getPreferences().getBoolean(KEEP_START_ON_BOOT, true);
    }

    public static void setStartOnBoot(boolean method) {
        getPreferences().edit().putBoolean(KEEP_START_ON_BOOT, method).apply();
    }

    //PRIMARY COLOR

    public static String getCustomPrimaryColor() {
        return getPreferences().getString(CUSTOM_PRIMARY_COLOR, null);
    }

    public static void setPrimaryColor(String hex) {
        getPreferences().edit().putString(CUSTOM_PRIMARY_COLOR, hex).apply();
    }

    public static void removePrimaryColor() {
        getPreferences().edit().remove(CUSTOM_PRIMARY_COLOR).apply();
    }

    public static Locale getLocale() {
        String tag = getPreferences().getString(LANGUAGE, null);
        if (TextUtils.isEmpty(tag) || "SYSTEM".equals(tag)) {
            return Locale.getDefault();
        }
        return Locale.forLanguageTag(tag);
    }

    @IntDef({
            LaunchMethod.UNKNOWN,
            LaunchMethod.ROOT,
            LaunchMethod.ADB,
    })
    @Retention(SOURCE)
    public @interface LaunchMethod {
        int UNKNOWN = -1;
        int ROOT = 0;
        int ADB = 1;
    }
}