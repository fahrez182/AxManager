package com.frb.engine.core;

public interface ConstantEngine {

    interface folder {
        String ROOT = "/";
        String SHELL = "data/data/com.android.shell/";
        String PARENT = "AxManager/";
        String PLUGIN = "plugins/";
        String PLUGIN_UPDATE = "plugins_update/";
        String CACHE = "cache/";
        String LOG = "logs/";
        String BINARY = "bin/";
        String ZIP = "zip/";

        String SHELL_ROOT = ROOT + SHELL;
        String PARENT_PLUGIN = PARENT + PLUGIN;
        String PARENT_LOG = PARENT + LOG;
        String PARENT_BINARY = PARENT + BINARY;
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
