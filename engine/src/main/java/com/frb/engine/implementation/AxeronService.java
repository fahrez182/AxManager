package com.frb.engine.implementation;

import static com.frb.engine.utils.ServerConstants.MANAGER_APPLICATION_ID;

import android.content.Context;
import android.content.IContentProvider;
import android.content.pm.ApplicationInfo;
import android.ddm.DdmHandleAppName;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.system.Os;

import com.frb.engine.IAxeronService;
import com.frb.engine.IFileService;
import com.frb.engine.IRuntimeService;
import com.frb.engine.utils.ApkChangedObservers;
import com.frb.engine.utils.BinderContainer;
import com.frb.engine.utils.BinderSender;
import com.frb.engine.utils.HandlerUtil;
import com.frb.engine.utils.IContentProviderCompat;
import com.frb.engine.utils.Logger;
import com.frb.engine.utils.ServerConstants;

import java.io.File;
import java.io.IOException;

import rikka.hidden.compat.ActivityManagerApis;
import rikka.hidden.compat.DeviceIdleControllerApis;
import rikka.hidden.compat.PackageManagerApis;
import rikka.hidden.compat.UserManagerApis;

public class AxeronService extends IAxeronService.Stub {

    public static final String VERSION_NAME = "V1.0.1";
    public static final long VERSION_CODE = 10100;
    private static final String TAG = "AxeronService";
    private static final Logger LOGGER = new Logger(TAG);
    private final Handler mainHandler = new Handler(Looper.myLooper());
    private final int managerAppId;

    public AxeronService() {
        HandlerUtil.setMainHandler(mainHandler);

        waitSystemService("package");
        waitSystemService(Context.ACTIVITY_SERVICE);
        waitSystemService(Context.USER_SERVICE);
        waitSystemService(Context.APP_OPS_SERVICE);

        ApplicationInfo ai = getManagerApplicationInfo();
        if (ai == null) {
            System.exit(ServerConstants.MANAGER_APP_NOT_FOUND);
        }

        assert ai != null;
        managerAppId = ai.uid;

        ApkChangedObservers.start(ai.sourceDir, () -> {
            if (getManagerApplicationInfo() == null) {
                System.exit(ServerConstants.MANAGER_APP_NOT_FOUND);
            }
        });

        BinderSender.register(this);

        mainHandler.post(() -> {
            for (int userId : UserManagerApis.getUserIdsNoThrow()) {
                sendBinderToManager(this, userId);
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

    public static void sendBinderToManager(IBinder binder, int userId) {
        sendBinderToUserApp(binder, MANAGER_APPLICATION_ID, userId, true);
    }

    private static void sendBinderToUserApp(IBinder binder, String packageName, int userId, boolean retry) {
        try {
            DeviceIdleControllerApis.addPowerSaveTempWhitelistApp(packageName, 30 * 1000, userId,
                    316/* PowerExemptionManager#REASON_SHELL */, "shell");
            LOGGER.v("Add %d:%s to power save temp whitelist for 30s", userId, packageName);
        } catch (Throwable tr) {
            LOGGER.e(tr, "Failed to add %d:%s to power save temp whitelist", userId, packageName);
        }

        String name = packageName + ".server";
        IContentProvider provider = null;

        /*
         When we pass IBinder through binder (and really crossed process), the receive side (here is system_server process)
         will always get a new instance of android.os.BinderProxy.

         In the implementation of getContentProviderExternal and removeContentProviderExternal, received
         IBinder is used as the key of a HashMap. But hashCode() is not implemented by BinderProxy, so
         removeContentProviderExternal will never work.

         Luckily, we can pass null. When token is token, count will be used.
         */
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
                    // For unknown reason, sometimes this could happens
                    // Kill Shizuku app and try again could work
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

    @Override
    public void destroy() throws RemoteException {
        System.exit(0);
    }

    @Override
    public IFileService getFileService() {
        return new FileService();
    }

    @Override
    public IRuntimeService getRuntimeService(String[] command, String[] env, String dir) throws RemoteException {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command, env, dir != null ? new File(dir) : null);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
        IBinder token = this;

        return new RuntimeService(process, token);
    }

    @Override
    public long getVersionCode() throws RemoteException {
        return VERSION_CODE;
    }

    @Override
    public String getVersionName() throws RemoteException {
        return VERSION_NAME;
    }

    @Override
    public int getUid() throws RemoteException {
        return Os.getuid();
    }

    @Override
    public int getPid() throws RemoteException {
        return Os.getpid();
    }

    @Override
    public String getSELinuxContext() throws RemoteException {
        return SELinux.getContext();
    }


}