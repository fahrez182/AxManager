package com.frb.engine;

import android.content.IContentProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.system.Os;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Keep;

import com.frb.engine.utils.BinderContainer;
import com.frb.engine.utils.IContentProviderCompat;

import rikka.hidden.compat.ActivityManagerApis;

@Keep
public class Starter {
    private static final String TAG = "AxStarter";
    private static final String EXTRA_BINDER = "AxServer.BINDER";
    private static final String TOKEN = "AxServer.TOKEN";
    @SuppressWarnings("FieldCanBeLocal")
    private static IBinder shizukuBinder;

    public static void main(String[] args) {
        if (Looper.getMainLooper() == null) {
            System.out.println("AxManager : Creating main Looper");
            Looper.prepareMainLooper();
        }

        int uid = Os.getuid();
        int pid = Os.getpid();
        System.out.println("AxManager : UID=" + uid + " PID=" + pid);

        IBinder service;
        String token;

        Pair<IBinder, String> result = UserService.create(uid, args);

        if (result == null) {
            System.out.println("AxManager : Failed to create service");
            System.exit(1);
            return;
        }

        service = result.first;
        token = result.second;

        System.out.println("AxManager : Success to create service : " + service + " " + token);

        if (!sendBinder(service, token)) {
            System.out.println("AxManager : Failed to send service");
            System.exit(1);
        }

        System.out.println("AxManager : Success to send service");

        Looper.loop();
        System.exit(0);
    }

    private static boolean sendBinder(IBinder binder, String token) {
        return sendBinder(binder, token, true);
    }

    private static boolean sendBinder(IBinder binder, String token, boolean retry) {
        String packageName = "com.frb.axmanager";
        String name = packageName + ".server";
        int userId = 0;
        IContentProvider provider = null;

        try {
            provider = ActivityManagerApis.getContentProviderExternal(name, userId, null, name);
            if (provider == null) {
                System.err.printf("provider is null %s %d%n", name, userId);
                return false;
            }
            if (!provider.asBinder().pingBinder()) {
                System.err.printf("provider is dead %s %d", name, userId);

                if (retry) {
                    // For unknown reason, sometimes this could happens
                    // Kill Shizuku app and try again could work
                    ActivityManagerApis.forceStopPackageNoThrow(packageName, userId);
                    System.err.printf("kill %s in user %d and try again", packageName, userId);
                    Thread.sleep(1000);
                    return sendBinder(binder, token, false);
                }
                return false;
            }

            if (!retry) {
                Log.e(TAG, "retry works");
            }

            Bundle extra = new Bundle();
            extra.putParcelable(EXTRA_BINDER, new BinderContainer(binder));
            extra.putString(TOKEN, token);

            Bundle reply = IContentProviderCompat.call(provider, null, null, name, "sendUserService", null, extra);

            if (reply != null) {
                reply.setClassLoader(BinderContainer.class.getClassLoader());

                Log.i(TAG, String.format("send binder to %s in user %d", packageName, userId));
                BinderContainer container = reply.getParcelable(EXTRA_BINDER);

                if (container != null && container.binder != null && container.binder.pingBinder()) {
                    shizukuBinder = container.binder;
                    shizukuBinder.linkToDeath(() -> {
                        Log.i(TAG, "exiting...");
                        System.exit(0);
                    }, 0);
                    return true;
                } else {
                    Log.w(TAG, "server binder not received");
                }
            }

            return false;
        } catch (Throwable tr) {
            Log.e(TAG, String.format("failed send binder to %s in user %d", packageName, userId), tr);
            return false;
        } finally {
            if (provider != null) {
                try {
                    ActivityManagerApis.removeContentProviderExternal(name, null);
                } catch (Throwable tr) {
                    Log.w(TAG, "removeContentProviderExternal", tr);
                }
            }
        }
    }

}
