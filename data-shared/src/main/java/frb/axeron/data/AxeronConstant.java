package frb.axeron.shared;

public class AxeronConstant {
    public static class server {
        public static final String VERSION_NAME = BuildConfig.SERVER_VERSION_NAME;
        public static final long VERSION_CODE = BuildConfig.SERVER_VERSION_CODE;
        public static final long PATCH_CODE = BuildConfig.SERVER_PATCH_CODE;
        public static final int TYPE_DEFAULT_ENV = -1;
        public static final int TYPE_ENV = 0;
        public static final int TYPE_NEW_ENV = 1;

        public static long getActualVersion() {
            return VERSION_CODE + PATCH_CODE;
        }
    }

    public interface folder {
        String ROOT = "/";
        String TMP = "data/local/tmp/";
        String SHELL = "data/data/com.android.shell/";
        String SHELL_DE = "data/user_de/0/com.android.shell/";
        String PARENT = "axeron/";
        String PLUGIN = "plugins/";
        String PLUGIN_UPDATE = "plugins_update/";
        String CACHE = "cache/";
        String LOG = "logs/";
        String BINARY = "bin/";
        String EXTERNAL_BINARY = "xbin/";
        String ZIP = "zip/";

        String PARENT_PLUGIN = PARENT + PLUGIN;
        String PARENT_CACHE = PARENT + CACHE;
        String PARENT_LOG = PARENT + LOG;
        String PARENT_BINARY = PARENT + BINARY;
        String PARENT_EXTERNAL_BINARY = PARENT + EXTERNAL_BINARY;
        String PARENT_ZIP = PARENT + ZIP;
        String PARENT_PLUGIN_UPDATE = PARENT + PLUGIN_UPDATE;
    }


    interface permission {

        interface ops {
            int OP_COARSE_LOCATION = 0;
            int OP_FINE_LOCATION = 1;
            int OP_GPS = 2;
            int OP_VIBRATE = 3;
            int OP_CAMERA = 26;
            int OP_RECORD_AUDIO = 27;
            int OP_SYSTEM_ALERT_WINDOW = 24;
            int OP_ACCESS_NOTIFICATION_POLICY = 25;
            int OP_WAKE_LOCK = 40;
            int OP_GET_USAGE_STATS = 43;
            int OP_ACTIVATE_VPN = 47;
            int OP_REQUEST_INSTALL_PACKAGES = 63;
            int OP_MANAGE_EXTERNAL_STORAGE = 92;
            int OP_ACCESS_MEDIA_LOCATION = 87;
            int OP_ACCESS_NOTIFICATIONS = 88;
            int OP_BODY_SENSORS = 56;
            int OP_READ_CONTACTS = 4;
            int OP_WRITE_CONTACTS = 5;
            int OP_READ_CALL_LOG = 6;
            int OP_WRITE_CALL_LOG = 7;
            int OP_READ_SMS = 14;
            int OP_RECEIVE_SMS = 16;
            int OP_SEND_SMS = 20;
            int OP_RECEIVE_MMS = 18;
            int OP_READ_EXTERNAL_STORAGE = 59;
            int OP_WRITE_EXTERNAL_STORAGE = 60;
            int OP_WRITE_SETTINGS = 23;
            int OP_RUN_ANY_IN_BACKGROUND = 70;

        }
    }
}
