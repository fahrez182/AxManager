package com.frb.engine.implementation;

import static com.frb.engine.utils.ServerConstants.MANAGER_APPLICATION_ID;
import static rikka.shizuku.manager.ServerConstants.PERMISSION;

import android.content.Context;
import android.content.IContentProvider;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.ddm.DdmHandleAppName;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.system.Os;

import com.frb.engine.utils.ApkChangedObservers;
import com.frb.engine.utils.BinderSender;
import com.frb.engine.utils.IContentProviderCompat;
import com.frb.engine.utils.ServerConstants;

import kotlin.collections.ArraysKt;
import moe.shizuku.api.BinderContainer;
import moe.shizuku.server.IShizukuService;
import rikka.hidden.compat.ActivityManagerApis;
import rikka.hidden.compat.DeviceIdleControllerApis;
import rikka.hidden.compat.PackageManagerApis;
import rikka.hidden.compat.UserManagerApis;

public class AxeronService extends Service {
    public static final String VERSION_NAME = "V1.2.9";
    public static final int VERSION_CODE = 12927;

    private final Long starting = SystemClock.elapsedRealtime();

    public AxeronService() {
        super();
        waitSystemService("package");
        waitSystemService(Context.ACTIVITY_SERVICE);
        waitSystemService(Context.USER_SERVICE);
        waitSystemService(Context.APP_OPS_SERVICE);

        ApplicationInfo ai = getManagerApplicationInfo();
        if (ai == null) {
            System.exit(ServerConstants.MANAGER_APP_NOT_FOUND);
        }

        assert ai != null;
        ApkChangedObservers.start(ai.sourceDir, () -> {
            if (getManagerApplicationInfo() == null) {
                System.exit(ServerConstants.MANAGER_APP_NOT_FOUND);
            }
        });

        BinderSender.register(this);

        mainHandler.post(() -> {
            try {
                sendBinderToClient();
            } catch (RemoteException e) {
                LOGGER.e("exception when call sendBinderToClient", e);
            }
            sendBinderToManager();
        });
    }

    private static void sendBinderToClient(IBinder binder, int userId) {
        try {
            for (PackageInfo pi : PackageManagerApis.getInstalledPackagesNoThrow(PackageManager.GET_PERMISSIONS, userId)) {
                if (pi == null || pi.requestedPermissions == null)
                    continue;

                if (ArraysKt.contains(pi.requestedPermissions, PERMISSION)) {
                    sendBinderToUserApp(binder, pi.packageName, userId);
                }
            }
        } catch (Throwable tr) {
            LOGGER.e("exception when call getInstalledPackages", tr);
        }
    }

    private static void sendBinderToManger(Binder binder) {
        for (int userId : UserManagerApis.getUserIdsNoThrow()) {
            sendBinderToManager(binder, userId);
        }
    }

    public static void sendBinderToManager(IBinder binder, int userId) {
        sendBinderToUserApp(binder, MANAGER_APPLICATION_ID, userId);
    }

    public static void sendBinderToUserApp(IBinder binder, String packageName, int userId) {
        sendBinderToUserApp(binder, packageName, userId, true);
    }

    public static void main(String[] args) {
        DdmHandleAppName.setAppName("axeron_server", 0);

        Looper.prepareMainLooper();
        new AxeronService();
        Looper.loop();
    }

    private static void waitSystemService(String name) {
        while (ServiceManager.getService(name) == null) {
            try {
                LOGGER.i("service " + name + " is not started, wait 1s.");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.w(e.getMessage(), e);
            }
        }
    }

    public static ApplicationInfo getManagerApplicationInfo() {
        return PackageManagerApis.getApplicationInfoNoThrow(MANAGER_APPLICATION_ID, 0, 0);
    }

    public static void sendBinderToUserApp(IBinder binder, String packageName, int userId, boolean retry) {
        try {
            DeviceIdleControllerApis.addPowerSaveTempWhitelistApp(packageName, 30 * 1000, userId,
                    316/* PowerExemptionManager#REASON_SHELL */, "shell");
            LOGGER.v("Add %d:%s to power save temp whitelist for 30s", userId, packageName);
        } catch (Throwable tr) {
            LOGGER.e(tr, "Failed to add %d:%s to power save temp whitelist", userId, packageName);
        }

        String name;
        String extraBinder;
        if (packageName.equals(MANAGER_APPLICATION_ID)) {
            name = packageName + ".server";
            extraBinder = "AxServer.BINDER";
        } else {
            name = packageName + ".shizuku";
            extraBinder = "moe.shizuku.privileged.api.intent.extra.BINDER";
        }
        IContentProvider provider = null;
        IBinder token = null;

        try {
            provider = ActivityManagerApis.getContentProviderExternal(name, userId, token, name);
            if (provider == null) {
                LOGGER.e("provider is null %s %d", name, userId);
                return;
            }
            if (!provider.asBinder().pingBinder()) {
                LOGGER.e("provider is dead %s %d", name, userId);

                if (retry) {
                    ActivityManagerApis.forceStopPackageNoThrow(packageName, userId);
                    LOGGER.e("kill %s in user %d and try again", packageName, userId);
                    Thread.sleep(1000);
                    sendBinderToUserApp(binder, packageName, userId, false);
                }
                return;
            }

            if (!retry) {
                LOGGER.e("retry works");
            }

            Bundle extra = new Bundle();
            extra.putParcelable(extraBinder, new BinderContainer(binder));
            Bundle reply = IContentProviderCompat.callCompat(provider, null, name, "sendBinder", null, extra);
            if (reply != null) {
                LOGGER.i("send binder to user app %s in user %d", packageName, userId);
            } else {
                LOGGER.w("failed to send binder to user app %s in user %d", packageName, userId);
            }
        } catch (Throwable tr) {
            LOGGER.e(tr, "failed send binder to user app %s in user %d", packageName, userId);
        } finally {
            if (provider != null) {
                try {
                    ActivityManagerApis.removeContentProviderExternal(name, token);
                } catch (Throwable tr) {
                    LOGGER.w(tr, "removeContentProviderExternal");
                }
            }
        }
    }

    void sendBinderToClient() throws RemoteException {
        for (int userId : UserManagerApis.getUserIdsNoThrow()) {
            IShizukuService shizukuService = getShizukuService();
            if (shizukuService != null) {
                sendBinderToClient(shizukuService.asBinder(), userId);
            }
        }
    }

    void sendBinderToManager() {
        sendBinderToManger(this);
    }

    public boolean checkRuntime() {
        try {
            if (checkPermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Override
    public AxeronInfo getInfo() {
        return new AxeronInfo(
                VERSION_NAME,
                VERSION_CODE,
                Os.getuid(),
                Os.getpid(),
                SELinux.getContext(),
                starting,
                checkRuntime()
        );
    }
}