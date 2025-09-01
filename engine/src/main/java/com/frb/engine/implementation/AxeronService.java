package com.frb.engine.implementation;

import static android.Manifest.permission.WRITE_SECURE_SETTINGS;
import static com.frb.engine.utils.ServerConstants.MANAGER_APPLICATION_ID;

import android.app.ActivityThread;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextHidden;
import android.content.IContentProvider;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.ddm.DdmHandleAppName;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserHandleHidden;
import android.system.Os;

import androidx.annotation.Nullable;

import com.frb.engine.Environment;
import com.frb.engine.IAxeronApplication;
import com.frb.engine.IAxeronService;
import com.frb.engine.IFileService;
import com.frb.engine.IRuntimeService;
import com.frb.engine.core.Engine;
import com.frb.engine.utils.ApkChangedObservers;
import com.frb.engine.utils.BinderContainer;
import com.frb.engine.utils.BinderSender;
import com.frb.engine.utils.HandlerUtil;
import com.frb.engine.utils.IContentProviderCompat;
import com.frb.engine.utils.Logger;
import com.frb.engine.utils.ServerConstants;
import com.frb.engine.utils.UserHandleCompat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dev.rikka.tools.refine.Refine;
import rikka.hidden.compat.ActivityManagerApis;
import rikka.hidden.compat.DeviceIdleControllerApis;
import rikka.hidden.compat.PackageManagerApis;
import rikka.hidden.compat.PermissionManagerApis;
import rikka.hidden.compat.UserManagerApis;

public class AxeronService extends IAxeronService.Stub {

    public static final String VERSION_NAME = "V1.0.3";
    public static final int VERSION_CODE = 10400;
    private static final String TAG = "AxeronService";
    private static final Logger LOGGER = new Logger(TAG);
    private final Handler mainHandler = new Handler(Looper.myLooper());
//    private final int managerAppId;

    public AxeronService() {
        HandlerUtil.setMainHandler(mainHandler);

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
            for (int userId : UserManagerApis.getUserIdsNoThrow()) {
                sendBinderToManager(this, userId);
            }
        });
    }

    public static void main(String[] args) {
        DdmHandleAppName.setAppName("axeron_server", 0);

        Looper.prepareMainLooper();

        ActivityThread activityThread = ActivityThread.systemMain();
        Context systemContext = activityThread.getSystemContext();

        waitSystemService("package");
        waitSystemService(Context.ACTIVITY_SERVICE);
        waitSystemService(Context.USER_SERVICE);
        waitSystemService(Context.APP_OPS_SERVICE);

        int userIdManager = getManagerApplicationInfo().uid / 100000;

        UserHandle userHandle = Refine.unsafeCast(
                UserHandleHidden.of(userIdManager));
        try {
            Context context = Refine.<ContextHidden>unsafeCast(systemContext).createPackageContextAsUser(MANAGER_APPLICATION_ID, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY, userHandle);

            Field mPackageInfo = context.getClass().getDeclaredField("mPackageInfo");
            mPackageInfo.setAccessible(true);
            Object loadedApk = mPackageInfo.get(context);
            Method makeApplication = loadedApk.getClass().getDeclaredMethod("makeApplication", boolean.class, Instrumentation.class);
            Application application = (Application) makeApplication.invoke(loadedApk, true, null);
            ClassLoader classLoader = application.getClassLoader();
            Class<?> serviceClass = classLoader.loadClass(AxeronService.class.getName());

            serviceClass.newInstance();
            LOGGER.i("createPackageContextAsUser: %d: %s",  userIdManager, context);
        } catch (PackageManager.NameNotFoundException | NoSuchFieldException |
                 IllegalAccessException | NoSuchMethodException | InvocationTargetException |
                 ClassNotFoundException | InstantiationException e) {
            LOGGER.e(e, "Failed to create package context: " + userIdManager);
        }

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

    public static Environment getEnvironment() {
        return new Environment.Builder(true)
                .put("AXERON", "true")
                .put("AXERONBIN", "/data/local/tmp/AxManager/bin")
                .put("AXERONLIB", Engine.getApplication().getApplicationInfo().nativeLibraryDir)
                .put("TMPDIR", "/data/local/tmp/AxManager/cache")
                .put("PATH", "$PATH:$AXERONBIN:/data/local/tmp/AxManager/bin/busybox")
                .build();
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
    public IRuntimeService getRuntimeService(String[] command, @Nullable Environment env, @Nullable String dir) throws RemoteException {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command, env != null ? env.getEnv() : null, dir != null ? new File(dir) : null);
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

    @Override
    public void bindAxeronApplication(IAxeronApplication app) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        try {
            PermissionManagerApis.grantRuntimePermission(MANAGER_APPLICATION_ID,
                    WRITE_SECURE_SETTINGS, UserHandleCompat.getUserId(callingUid));
        } catch (RemoteException e) {
            LOGGER.w(e, "grant WRITE_SECURE_SETTINGS");
        }
        app.bindApplication(new Bundle());
    }


    public final void transactRemote(Parcel data, Parcel reply, int flags) throws RemoteException {
        IBinder targetBinder = data.readStrongBinder();
        int targetCode = data.readInt();
        int targetFlags = data.readInt();

        LOGGER.d("transact: uid=%d, descriptor=%s, code=%d", Binder.getCallingUid(), targetBinder.getInterfaceDescriptor(), targetCode);
        Parcel newData = Parcel.obtain();
        try {
            newData.appendFrom(data, data.dataPosition(), data.dataAvail());
        } catch (Throwable tr) {
            LOGGER.w(tr, "appendFrom");
            return;
        }
        try {
            long id = Binder.clearCallingIdentity();
            targetBinder.transact(targetCode, newData, reply, targetFlags);
            Binder.restoreCallingIdentity(id);
        } finally {
            newData.recycle();
        }
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == 1) {
            data.enforceInterface(IAxeronService.DESCRIPTOR);
            transactRemote(data, reply, flags);
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }
}