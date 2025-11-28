package rikka.shizuku.server;

import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_TRANSACTION_destroy;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;

import java.io.IOException;
import java.util.UUID;

import moe.shizuku.server.IShizukuServiceConnection;
import rikka.shizuku.ShizukuApiConstants;
import rikka.shizuku.server.util.HandlerUtil;
import rikka.shizuku.server.util.Logger;

public abstract class UserServiceRecord {

    protected static final Logger LOGGER = new Logger("UserServiceRecord");
    public final int versionCode;
    public final RemoteCallbackList<IShizukuServiceConnection> callbacks = new ConnectionList();
    private final IBinder.DeathRecipient deathRecipient;
    public String token;
    public int pid;
    public IBinder service;
    public boolean daemon;
    public boolean starting;
    public String[] environment;
    private Runnable startTimeoutCallback;
    public UserServiceRecord(int versionCode, boolean daemon, String[] environment) {
        this.versionCode = versionCode;
        this.token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
        this.deathRecipient = () -> {
            LOGGER.v("Binder for service record %s is dead", token);
            removeSelf();
        };
        this.daemon = daemon;
        this.environment = environment;
    }

    public void setStartingTimeout(long timeoutMillis) {
        if (starting) {
            LOGGER.w("Service record %s is already starting", token);
            return;
        }

        LOGGER.v("Set starting timeout for service record %s: %d", token, timeoutMillis);

        starting = true;
        startTimeoutCallback = () -> {
            if (starting) {
                LOGGER.w("Service record %s is not started in %d ms", token, timeoutMillis);
                removeSelf();
            }
        };
        HandlerUtil.getMainHandler().postDelayed(startTimeoutCallback, timeoutMillis);
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public void setBinder(IBinder binder) {
        LOGGER.v("Binder received for service record %s", token);

        HandlerUtil.getMainHandler().removeCallbacks(startTimeoutCallback);

        service = binder;

        try {
            binder.linkToDeath(deathRecipient, 0);
        } catch (Throwable tr) {
            LOGGER.w("linkToDeath %s", token);
        }

        broadcastBinderReceived();
    }

    public void broadcastBinderReceived() {
        LOGGER.v("Broadcast binder received for service record %s", token);

        int count = callbacks.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                callbacks.getBroadcastItem(i).connected(service);
            } catch (Throwable e) {
                LOGGER.w("Failed to call connected %s", token);
            }
        }
        callbacks.finishBroadcast();
    }

    public void broadcastBinderDied() {
        LOGGER.v("Broadcast binder died for service record %s", token);

        int count = callbacks.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                callbacks.getBroadcastItem(i).died();
            } catch (Throwable e) {
                LOGGER.w("Failed to call died %s", token);
            }
        }
        callbacks.finishBroadcast();
    }

    public abstract void removeSelf();

    public void destroy() {
        LOGGER.i("Destroy service record %s", token);
        if (service != null) {
            service.unlinkToDeath(deathRecipient, 0);
        }

        if (service != null && service.pingBinder()) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(ShizukuApiConstants.BINDER_DESCRIPTOR);
                service.transact(USER_SERVICE_TRANSACTION_destroy, data, reply, Binder.FLAG_ONEWAY);
            } catch (Throwable e) {
                LOGGER.w("Failed to call destroy %s", token);
            } finally {
                data.recycle();
                reply.recycle();
            }
        }

        callbacks.kill();
        try {
            int result = Runtime.getRuntime().exec("kill " + pid).waitFor();
            if (result != 0) {
                LOGGER.w("Failed to kill service pid record %s", pid);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.w("Failed to kill service pid record %s", pid);
        }
    }

    private class ConnectionList extends RemoteCallbackList<IShizukuServiceConnection> {

        @Override
        public void onCallbackDied(IShizukuServiceConnection callback) {
            if (daemon || getRegisteredCallbackCount() != 0) {
                return;
            }

            LOGGER.v("Remove service record %s since it does not run as a daemon and all connections are gone", token);
            removeSelf();
        }
    }
}
