package com.frb.engine.core;

public interface ConstantEngine {
    interface extra {
        String REQUEST_ID = "Apollo.REQUEST_ID";
        String SELECTED_APP = "Apollo.SELECTED_APP";
    }

    interface monitor {
        String CPU = "cpu";
        String GPU = "gpu";
        String RAM = "ram";
        String BATTERY = "battery";
        String TEMP = "temperature";
        String FPS = "fps";
        String TIME = "time";
    }

    interface data {
        String SYNC_DATA = "syncData";
        String BOT_DETECTION = "botDetection";
        String ERROR = "dataError";
    }

    interface cross {
        String SIZE = "size";
        String ROTATION = "rotation";
        String OPACITY = "opacity";
        String POSITION_X = "positionX";
        String POSITION_Y = "positionY";
        String CROSS_ID = "crossId";
        String COLOR_ID = "colorId";
    }

    interface toggle {
        String MODES = "toggleModes";
        String CROSS = "toggleCross";
        String TACTIX = "toggleTactix";
        String OPT_PING = "toggleOptPing";
        String MONITOR = "toggleMonitor";
    }

    interface parcel {
        String EXPORT_PLUGIN = "exportPlugin";
        String PLUGIN_ITEM = "pluginItem";
        String APP_ITEM = "appItem";
    }

    interface prefs {
        String ID = "idUser";
        String DEBUG_MODE = "debugMode";
        String SAVED_PLUGIN = "savedPlugin";
        String FIRST_INSTALL = "firstInstall";
        String META_DATA = "metaData";
        String META_USER = "metaVal";
        String MODES = "tweakModes";
        String ADDED_PLUGIN = "addedPlugin";
        String ENERGY = "energy";
        String USER_AGENT = "userAgent";
        String PLUGIN_DATA = "pluginData";
    }

    interface packages {
        String SHIZUKU = "moe.shizuku.privileged.api";
        String WHATSAPP = "com.whatsapp";
        String TIKTOK = "com.ss.android.ugc.trill";
    }

    interface folder {
        String ROOT = "/";
        String AX_EXTENSION = ".axplugin";
        String LOCAL_TEMP = "data/local/tmp/";
        String PARENT = "AxManager/";
        String PLUGIN = "plugins/";
        String CACHE = "cache/";
        String LAX2 = "LAX2/";
        String RELEASE = "release/";
        String DEBUG = "debug/";
        String INSTALLED = ".installed/";
        String BINARY = "bin/";
        String PARENT_PLUGIN = PARENT + PLUGIN;
        String PARENT_PLUGIN_RELEASE = PARENT_PLUGIN + RELEASE;
        String PARENT_PLUGIN_DEBUG = PARENT_PLUGIN + DEBUG;
        String PARENT_PLUGIN_INSTALLED = PARENT_PLUGIN + INSTALLED;
        String PARENT_CACHE = PARENT + CACHE;
        String PARENT_LAX2 = PARENT + LAX2;
        String PARENT_BINARY = PARENT + BINARY;
        String CONFIG = ".config";
        String PARENT_CONFIG = PARENT + CONFIG;
        String AX_PLUGIN_TOML = "axplugin.toml";
        String BANNER = "banner.png";
    }

    interface plugin {
        String ID = "id";
        String NAME = "name";
        String AUTHOR = "author";
        String VERSION_NAME = "version";
        String VERSION_CODE = "versionCode";
        String DESCRIPTION = "description";
        String WEB_SHELL = "config.webShell";
        String MIN_HEIGHT = "config.minHeight";
        String MIN_WIDTH = "config.minWidth";
        String BINARY = "config.binaryPath";
    }

    interface action {
        String UPDATE_PANEL_SIDE = "UPDATE_PANEL_SIDE";
        String START_MAGIC_TAP = "START_MAGIC_TAP";
        String INIT_MAGIC_TAP = "INIT_MAGIC_TAP";
        String FROM_PANEL_SIDE = "FROM_PANEL_SIDE";
        String PICK_FILE = "PICK_FILE";
        String STOP_FLOATING_FOREGROUND = "STOP_FLOATING_FOREGROUND";
    }

    interface panel {
        String POS_Y = "posY";
        String POS_X = "posX";
        String LOCATION = "location";
        int LOC_LEFT = -2;
        int LOC_TOP = -1;
        int LOC_RIGHT = 1;
        int LOC_BOTTOM = 2;
        int LOC_UNSPECIFIC = 0;
    }

    interface request {
        int CODE_PICK_ZIP = 1001;
    }

    interface permission {
        String SHIZUKU_SERVICES = "APOLLO_SHIZUKU_SERVICES";

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
