package com.frb.engine.implementation;

import static com.frb.engine.utils.ServerConstants.MANAGER_APPLICATION_ID;

import android.content.Context;
import android.content.IContentProvider;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.ddm.DdmHandleAppName;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.system.Os;

import com.frb.engine.utils.ApkChangedObservers;
import com.frb.engine.utils.BinderContainer;
import com.frb.engine.utils.BinderSender;
import com.frb.engine.utils.IContentProviderCompat;
import com.frb.engine.utils.ServerConstants;

import rikka.hidden.compat.ActivityManagerApis;
import rikka.hidden.compat.DeviceIdleControllerApis;
import rikka.hidden.compat.PackageManagerApis;
import rikka.hidden.compat.PermissionManagerApis;
import rikka.hidden.compat.UserManagerApis;

public class AxeronService extends Service {
    public static final String VERSION_NAME = "V1.2.1";
    public static final int VERSION_CODE = 12100;

    private final Long starting = SystemClock.elapsedRealtime();

    private final Handler mainHandler = new Handler(Looper.myLooper());

    public AxeronService() {
//        super(context);
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

        BinderSender.register(this.asBinder());

        mainHandler.post(() -> {
            for (int userId : UserManagerApis.getUserIdsNoThrow()) {
                sendBinderToManager(this, userId, true);
            }
        });
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


    public static void sendBinderToManager(IBinder binder, int userId, boolean retry) {
        String packageName = MANAGER_APPLICATION_ID;
        try {
            DeviceIdleControllerApis.addPowerSaveTempWhitelistApp(packageName, 30 * 1000, userId,
                    316/* PowerExemptionManager#REASON_SHELL */, "shell");
            LOGGER.v("Add %d:%s to power save temp whitelist for 30s", userId, packageName);
        } catch (Throwable tr) {
            LOGGER.e(tr, "Failed to add %d:%s to power save temp whitelist", userId, packageName);
        }

        String name = packageName + ".server";
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
                    sendBinderToManager(binder, userId, false);
                }
                return;
            }

            if (!retry) {
                LOGGER.e("retry works");
            }

            Bundle extra = new Bundle();
            extra.putParcelable("AxServer.BINDER", new BinderContainer(binder));

            Bundle reply = IContentProviderCompat.call(provider, null, null, name, "sendBinder", null, extra);
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

    public int checkPermission(String permission) {
        try {
            int uid = Os.getuid();
            if (uid == 0) return PackageManager.PERMISSION_GRANTED;
            return PermissionManagerApis.checkPermission(permission, uid);
        } catch (RemoteException e) {
            return PackageManager.PERMISSION_DENIED;
        }
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
                checkPermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED
        );
    }
}