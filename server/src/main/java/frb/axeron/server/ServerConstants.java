package frb.axeron.server;

public class ServerConstants {

    public static final int MANAGER_APP_NOT_FOUND = 50;

    public static final String PERMISSION = "moe.shizuku.manager.permission.API_V23";
    public static final String MANAGER_APPLICATION_ID = "frb.axeron.manager";
    public static final String SHIZUKU_MANAGER_APPLICATION_ID = "moe.shizuku.privileged.api";
    public static final String REQUEST_PERMISSION_ACTION = MANAGER_APPLICATION_ID + ".intent.action.REQUEST_PERMISSION";
    public static final String REQUEST_BINDER_ACTION = MANAGER_APPLICATION_ID + ".intent.action.REQUEST_BINDER";

    public static final int BINDER_TRANSACTION_getApplications = 10001;
}
