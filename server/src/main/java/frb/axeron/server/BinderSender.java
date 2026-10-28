package frb.axeron.server;

import static android.app.ActivityManagerHidden.UID_OBSERVER_ACTIVE;
import static android.app.ActivityManagerHidden.UID_OBSERVER_CACHED;
import static android.app.ActivityManagerHidden.UID_OBSERVER_GONE;
import static android.app.ActivityManagerHidden.UID_OBSERVER_IDLE;

import android.app.ActivityManagerHidden;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import frb.axeron.server.util.Logger;
import kotlin.collections.ArraysKt;
import moe.shizuku.server.IShizukuService;
import rikka.hidden.compat.ActivityManagerApis;
import rikka.hidden.compat.PackageManagerApis;
import rikka.hidden.compat.PermissionManagerApis;
import rikka.hidden.compat.adapter.ProcessObserverAdapter;
import rikka.hidden.compat.adapter.UidObserverAdapter;

public class BinderSender {

    private static final Logger LOGGER = new Logger("BinderSender");

    private static final String SHIZUKU_PERMISSION_MANAGER = "moe.shizuku.manager.permission.MANAGER";

    private static final String PERMISSION = "moe.shizuku.manager.permission.API_V23";

    private static AxeronService axeronService;

    private static final Map<Integer, List<String>> UID_PACKAGE_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> SENT_BINDERS = ConcurrentHashMap.newKeySet();

    // Runtime-permission state the manager last received the binder with.
    private static volatile boolean managerRuntime;

    private static List<String> getPackagesCached(int uid) {
        return UID_PACKAGE_CACHE.computeIfAbsent(uid, PackageManagerApis::getPackagesForUidNoThrow);
    }

    private static boolean currentRuntime() {
        try {
            return axeronService.checkRuntime();
        } catch (Throwable e) {
            return managerRuntime;
        }
    }

    private static void sendToManager(int userId) {
        // The uid cache is not reset here: the manager seeds it in attachApplication from the
        // uid it was validated with, so a reinstalled manager still refreshes it, and a push of
        // the same binder doesn't leave the gate without a uid while the manager's first gated
        // calls are on their way.
        managerRuntime = currentRuntime();
        AxeronService.sendBinderToManager(axeronService.asBinder(), userId);
    }

    /**
     * "Need extra step" flips in system settings without any binder event, so the manager can't
     * see it. When the manager comes back to the foreground and the runtime-permission check has
     * changed since its last binder, push the same binder again so it re-reads the server info.
     */
    private static void pushRuntimeChange(int uid) {
        if (!axeronService.isManagerUid(uid))
            return;

        boolean runtime = currentRuntime();
        if (runtime == managerRuntime)
            return;

        LOGGER.i("Runtime permission changed to %s, re-push binder to manager", Boolean.toString(runtime));
        sendToManager(uid / 100000);
    }

    private static void sendBinder(int uid, int pid) throws RemoteException {
        int userId = uid / 100000;

        List<String> packages = getPackagesCached(uid);
        if (packages.isEmpty())
            return;

        // By the manager this server started from (its package and signing certificate), not by
        // a package name or a permission that names one.
        boolean isOurManager = axeronService.isManagerUid(uid);

        IShizukuService shizukuService = axeronService.getShizukuService();

        // Non-manager uids can only be served while the Shizuku intercept is enabled. Do not mark
        // them as sent, so they still receive the binder once the intercept is enabled later.
        if (shizukuService == null && !isOurManager)
            return;

        String key = uid + ":" + pid;
        if (!SENT_BINDERS.add(key)) {
            LOGGER.v("Skip duplicate binder send for %s", key);
            return;
        }

        // 🔒 secure fast-path
        if (isOurManager) {
            LOGGER.d("uid %d is the manager, send binder directly", uid);
            sendToManager(userId);
            return;
        }

        LOGGER.d(
                "sendBinder to uid %d: packages=%s",
                uid,
                TextUtils.join(", ", packages)
        );

        for (String packageName : packages) {
            try {
                PackageInfo pi = PackageManagerApis.getPackageInfoNoThrow(
                        packageName,
                        PackageManager.GET_PERMISSIONS,
                        userId
                );
                if (pi == null || pi.requestedPermissions == null)
                    continue;

                if (ArraysKt.contains(pi.requestedPermissions, SHIZUKU_PERMISSION_MANAGER)) {
                    boolean granted = pid == -1
                            ? PermissionManagerApis.checkPermission(SHIZUKU_PERMISSION_MANAGER, uid)
                            == PackageManager.PERMISSION_GRANTED
                            : ActivityManagerApis.checkPermission(SHIZUKU_PERMISSION_MANAGER, pid, uid)
                            == PackageManager.PERMISSION_GRANTED;

                    if (granted) {
                        // The server caches the Shizuku app's uid on its first permission check;
                        // if Shizuku was installed (or reinstalled) after that, the cache is stale
                        // and its calls get denied. Its process always passes here before it can
                        // call the server, so refresh the cache now.
                        axeronService.dispatchPackageChanged(null);
                        AxeronService.sendBinderToShizukuManager(
                                shizukuService.asBinder(),
                                userId
                        );
                        return;
                    }

                } else if (ArraysKt.contains(pi.requestedPermissions, PERMISSION)) {
                    AxeronService.sendBinderToUserApp(
                            shizukuService.asBinder(),
                            packageName,
                            userId
                    );
                    return;
                }

            } catch (Throwable e) {
                LOGGER.w(e, "sendBinder failed for package %s", packageName);
            }
        }
    }

    public static void register(AxeronService service) {
        axeronService = service;
        managerRuntime = currentRuntime();

        try {
            ActivityManagerApis.registerProcessObserver(new ProcessObserver());
        } catch (Throwable tr) {
            LOGGER.e(tr, "registerProcessObserver");
        }

        int flags = UID_OBSERVER_GONE | UID_OBSERVER_IDLE | UID_OBSERVER_ACTIVE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            flags |= UID_OBSERVER_CACHED;
        }
        try {
            ActivityManagerApis.registerUidObserver(new UidObserver(), flags,
                    ActivityManagerHidden.PROCESS_STATE_UNKNOWN,
                    null);
        } catch (Throwable tr) {
            LOGGER.e(tr, "registerUidObserver");
        }
    }

    private static class ProcessObserver extends ProcessObserverAdapter {

        private static final Set<Integer> PID_LIST = new HashSet<>();

        @Override
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) throws RemoteException {
            LOGGER.d("onForegroundActivitiesChanged: pid=%d, uid=%d, foregroundActivities=%s", pid, uid, foregroundActivities ? "true" : "false");

            if (!foregroundActivities) {
                return;
            }

            boolean alreadySent;
            synchronized (PID_LIST) {
                alreadySent = !PID_LIST.add(pid);
            }

            if (alreadySent) {
                pushRuntimeChange(uid);
                return;
            }

            sendBinder(uid, pid);
        }

        @Override
        public void onProcessDied(int pid, int uid) {
            LOGGER.d("onProcessDied: pid=%d, uid=%d", pid, uid);

            synchronized (PID_LIST) {
                PID_LIST.remove(pid);
            }
            SENT_BINDERS.remove(uid + ":" + pid);
        }

        @Override
        public void onProcessStateChanged(int pid, int uid, int procState) throws RemoteException {
            LOGGER.d("onProcessStateChanged: pid=%d, uid=%d, procState=%d", pid, uid, procState);

            synchronized (PID_LIST) {
                if (PID_LIST.contains(pid)) {
                    return;
                }
                PID_LIST.add(pid);
            }

            sendBinder(uid, pid);
        }
    }

    private static class UidObserver extends UidObserverAdapter {

        private static final Set<Integer> UID_LIST = new HashSet<>();

        @Override
        public void onUidActive(int uid) throws RemoteException {
            LOGGER.d("onUidCachedChanged: uid=%d", uid);

            uidStarts(uid);
        }

        @Override
        public void onUidCachedChanged(int uid, boolean cached) throws RemoteException {
            LOGGER.d("onUidCachedChanged: uid=%d, cached=%s", uid, Boolean.toString(cached));

            if (!cached) {
                uidStarts(uid);
            }
        }

        @Override
        public void onUidIdle(int uid, boolean disabled) throws RemoteException {
            LOGGER.d("onUidIdle: uid=%d, disabled=%s", uid, Boolean.toString(disabled));

            uidStarts(uid);
        }

        @Override
        public void onUidGone(int uid, boolean disabled) {
            LOGGER.d("onUidGone: uid=%d, disabled=%s", uid, Boolean.toString(disabled));

            uidGone(uid);
        }

        private void uidStarts(int uid) throws RemoteException {
            synchronized (UID_LIST) {
                if (UID_LIST.contains(uid)) {
                    LOGGER.v("Uid %d already starts", uid);
                    return;
                }
                UID_LIST.add(uid);
                LOGGER.v("Uid %d starts", uid);
            }

            sendBinder(uid, -1);
        }

        private void uidGone(int uid) {
            synchronized (UID_LIST) {
                UID_LIST.remove(uid);
                LOGGER.v("Uid %d dead", uid);
            }
            // Drop cached packages and sent-binder marks so a restarted uid is served again.
            UID_PACKAGE_CACHE.remove(uid);
            SENT_BINDERS.removeIf(key -> key.startsWith(uid + ":"));
            // An uninstall ends here too, and the uid may go to another app: check its grant again.
            axeronService.getConfigManager().invalidate(uid);
        }
    }
}