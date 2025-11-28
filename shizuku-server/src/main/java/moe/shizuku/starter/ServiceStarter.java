package moe.shizuku.starter;

import android.content.IContentProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;

import java.util.Locale;

import kotlin.Triple;
import moe.shizuku.api.BinderContainer;
import moe.shizuku.starter.util.IContentProviderCompat;
import rikka.hidden.compat.ActivityManagerApis;
import rikka.shizuku.ShizukuApiConstants;
import rikka.shizuku.manager.ServerConstants;
import rikka.shizuku.server.UserService;
import rikka.shizuku.server.util.Logger;

public class ServiceStarter {

    public static final String DEBUG_ARGS;
    private static final String TAG = "ShizukuServiceStarter";
    private static final String EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER";
    private static final Logger LOGGER = new Logger(TAG);
    private static final String USER_SERVICE_CMD_FORMAT = "(CLASSPATH='%s' %s%s /system/bin " +
            "--nice-name='%s' moe.shizuku.starter.ServiceStarter " +
            "--token='%s' --package='%s' --class='%s' --uid=%d%s)&";
    // DeathRecipient will automatically be unlinked when all references to the
    // binder is dropped, so we hold the reference here.
    @SuppressWarnings("FieldCanBeLocal")
    private static IBinder shizukuBinder;

    static {
        int sdk = Build.VERSION.SDK_INT;
        if (sdk >= 30) {
            DEBUG_ARGS = "-Xcompiler-option" + " --debuggable" +
                    " -XjdwpProvider:adbconnection" +
                    " -XjdwpOptions:suspend=n,server=y";
        } else if (sdk >= 28) {
            DEBUG_ARGS = "-Xcompiler-option" + " --debuggable" +
                    " -XjdwpProvider:internal" +
                    " -XjdwpOptions:transport=dt_android_adb,suspend=n,server=y";
        } else {
            DEBUG_ARGS = "-Xcompiler-option" + " --debuggable" +
                    " -agentlib:jdwp=transport=dt_android_adb,suspend=n,server=y";
        }
    }

    public static String commandForUserService(String appProcess, String managerApkPath, String token, String packageName, String classname, String processNameSuffix, int callingUid, boolean debug) {
        String processName = String.format("%s:%s", packageName, processNameSuffix);
        return String.format(Locale.ENGLISH, USER_SERVICE_CMD_FORMAT,
                managerApkPath, appProcess, debug ? (" " + DEBUG_ARGS) : "",
                processName,
                token, packageName, classname, callingUid, debug ? (" " + "--debug-name=" + processName) : "");
    }

    public static void main(String[] args) {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        IBinder service;
        String token;
        int pid;

        UserService.setTag(TAG);
        Triple<IBinder, String, Integer> result = UserService.create(args);

        if (result == null) {
            System.exit(1);
            return;
        }

        service = result.getFirst();
        token = result.getSecond();
        pid = result.getThird();

        if (!sendBinder(service, token, pid)) {
            System.exit(1);
        }

        Looper.loop();
        System.exit(0);

        LOGGER.i("service exited");
    }

    private static boolean sendBinder(IBinder binder, String token, int pid) {
        return sendBinder(binder, token, pid, true);
    }

    private static boolean sendBinder(IBinder binder, String token, int pid, boolean retry) {
        String packageName = ServerConstants.MANAGER_APPLICATION_ID;
        String name = packageName + ".server";
        int userId = 0;
        IContentProvider provider = null;

        try {
            provider = ActivityManagerApis.getContentProviderExternal(name, userId, null, name);
            if (provider == null) {
                LOGGER.e("provider is null %s %d", name, userId);
                return false;
            }
            if (!provider.asBinder().pingBinder()) {
                LOGGER.e("provider is dead %s %d", name, userId);

                if (retry) {
                    // For unknown reason, sometimes this could happens
                    // Kill Shizuku app and try again could work
                    ActivityManagerApis.forceStopPackageNoThrow(packageName, userId);
                    LOGGER.e("kill %s in user %d and try again", packageName, userId);
                    Thread.sleep(1000);
                    return sendBinder(binder, token, pid, false);
                }
                return false;
            }

            if (!retry) {
                LOGGER.e("retry works");
            }

            Bundle extra = new Bundle();
            extra.putParcelable(EXTRA_BINDER, new BinderContainer(binder));
            extra.putString(ShizukuApiConstants.USER_SERVICE_ARG_TOKEN, token);
            extra.putInt(ShizukuApiConstants.USER_SERVICE_ARG_PID, pid);

            Bundle reply = IContentProviderCompat.call(provider, null, null, name, "sendUserService", null, extra);

            if (reply != null) {
                reply.setClassLoader(BinderContainer.class.getClassLoader());

                LOGGER.i("send binder to %s in user %d", packageName, userId);
                BinderContainer container = reply.getParcelable(EXTRA_BINDER);

                if (container != null && container.binder != null && container.binder.pingBinder()) {
                    shizukuBinder = container.binder;
                    shizukuBinder.linkToDeath(() -> {
                        LOGGER.i( "exiting...");
                        System.exit(0);
                    }, 0);
                    return true;
                } else {
                    LOGGER.w("server binder not received");
                }
            }

            return false;
        } catch (Throwable tr) {
            LOGGER.e("failed send binder to %s in user %d, %s", packageName, userId, tr);
            return false;
        } finally {
            if (provider != null) {
                try {
                    ActivityManagerApis.removeContentProviderExternal(name, null);
                } catch (Throwable tr) {
                    LOGGER.w("removeContentProviderExternal", tr);
                }
            }
        }
    }
}
