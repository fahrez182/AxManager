package com.frb.engine;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frb.engine.client.AxeronNewProcess;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Axeron {

    private static final List<ListenerHolder<OnBinderReceivedListener>> RECEIVED_LISTENERS = new ArrayList<>();
    private static final List<ListenerHolder<OnBinderDeadListener>> DEAD_LISTENERS = new ArrayList<>();
//    private static final List<ListenerHolder<OnRequestPermissionResultListener>> PERMISSION_LISTENERS = new ArrayList<>();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static IBinder binder;
    private static IAxeronService service;
    private static int serverUid = -1;
    private static int serverPid = -1;
    private static String serverVersionName = null;
    private static long serverVersionCode = -1;
    private static String serverContext = null;
    private static boolean binderReady = false;

    private static void scheduleBinderDeadListeners() {
        synchronized (RECEIVED_LISTENERS) {
            for (ListenerHolder<OnBinderDeadListener> holder : DEAD_LISTENERS) {
                if (holder.handler != null) {
                    holder.handler.post(holder.listener::onBinderDead);
                } else {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        holder.listener.onBinderDead();
                    } else {
                        MAIN_HANDLER.post(holder.listener::onBinderDead);
                    }
                }

            }
        }
    }

    public static void addBinderDeadListener(@NonNull OnBinderDeadListener listener) {
        addBinderDeadListener(listener, null);
    }

    public static void addBinderDeadListener(@NonNull OnBinderDeadListener listener, @Nullable Handler handler) {
        synchronized (RECEIVED_LISTENERS) {
            DEAD_LISTENERS.add(new ListenerHolder<>(listener, handler));
        }
    }

    public static boolean removeBinderDeadListener(@NonNull OnBinderDeadListener listener) {
        synchronized (RECEIVED_LISTENERS) {
            return DEAD_LISTENERS.removeIf(holder -> holder.listener == listener);
        }
    }

    private static void scheduleBinderReceivedListeners() {
        synchronized (RECEIVED_LISTENERS) {
            for (ListenerHolder<OnBinderReceivedListener> holder : RECEIVED_LISTENERS) {
                if (holder.handler != null) {
                    holder.handler.post(holder.listener::onBinderReceived);
                } else {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        holder.listener.onBinderReceived();
                    } else {
                        MAIN_HANDLER.post(holder.listener::onBinderReceived);
                    }
                }
            }
        }
        binderReady = true;
    }

    public static void addBinderReceivedListener(@NonNull OnBinderReceivedListener listener) {
        addBinderReceivedListener(listener, null);
    }

    public static void addBinderReceivedListener(@NonNull OnBinderReceivedListener listener, @Nullable Handler handler) {
        addBinderReceivedListener(Objects.requireNonNull(listener), false, handler);
    }

    public static void addBinderReceivedListenerSticky(@NonNull OnBinderReceivedListener listener) {
        addBinderReceivedListenerSticky(Objects.requireNonNull(listener), null);
    }

    public static void addBinderReceivedListenerSticky(@NonNull OnBinderReceivedListener listener, @Nullable Handler handler) {
        addBinderReceivedListener(Objects.requireNonNull(listener), true, handler);
    }

    private static void addBinderReceivedListener(@NonNull OnBinderReceivedListener listener, boolean sticky, @Nullable Handler handler) {
        if (sticky && binderReady) {
            if (handler != null) {
                handler.post(listener::onBinderReceived);
            } else if (Looper.myLooper() == Looper.getMainLooper()) {
                listener.onBinderReceived();
            } else {
                MAIN_HANDLER.post(listener::onBinderReceived);
            }
        }
        synchronized (RECEIVED_LISTENERS) {
            RECEIVED_LISTENERS.add(new ListenerHolder<>(listener, handler));
        }
    }

    public static boolean removeBinderReceivedListener(@NonNull OnBinderReceivedListener listener) {
        synchronized (RECEIVED_LISTENERS) {
            return RECEIVED_LISTENERS.removeIf(holder -> holder.listener == listener);
        }
    }

    public static void onBinderReceived(IBinder newBinder, String packageName) {
        if (binder == newBinder) return;

        if (newBinder == null) {
            binder = null;
            service = null;
            serverUid = -1;
            serverPid = -1;
            serverVersionName = null;
            serverVersionCode = -1;
            serverContext = null;

            scheduleBinderDeadListeners();
        } else {
            if (binder != null) {
                binder.unlinkToDeath(DEATH_RECIPIENT, 0);
            }
            binder = newBinder;
            service = IAxeronService.Stub.asInterface(newBinder);

            try {
                binder.linkToDeath(DEATH_RECIPIENT, 0);
            } catch (Throwable e) {
                Log.i("AxeronApplication", "attachApplication");
            }

            binderReady = true;
            scheduleBinderReceivedListeners();

        }
    }

    @NonNull
    protected static IAxeronService requireService() {
        if (service == null) {
            throw new IllegalStateException("binder haven't been received");
        }
        return service;
    }

    public static boolean pingBinder() {
        return binder != null && binder.pingBinder();
    }

    @Nullable
    public static IBinder getBinder() {
        return binder;
    }    private static final IBinder.DeathRecipient DEATH_RECIPIENT = () -> {
        binderReady = false;
        onBinderReceived(null, null);
    };

    public static AxeronNewProcess newProcess(@NonNull String cmd) {
        return newProcess(new String[]{"sh", "-c", cmd}, null, null);
    }

    public static AxeronNewProcess newProcess(@NonNull String[] cmd, @Nullable String[] env, @Nullable String dir) {
        try {
            return new AxeronNewProcess(requireService().getRuntimeService(cmd, env, dir));
        } catch (RemoteException | NullPointerException e) {
//            Log.d(TAG, "Failed to execute command", e);
            throw new RuntimeException("Failed to execute command", e);
        }
    }

    public static int getUid() {
        if (serverUid == -1) {
            try {
                serverUid = requireService().getUid();
            } catch (RemoteException e) {
                Log.e("AxeronApplication", "getServerUid", e);
            }
        }
        return serverUid;
    }

    public static int getPid() {
        if (serverPid == -1) {
            try {
                serverPid = requireService().getPid();
            } catch (RemoteException e) {
                Log.e("AxeronApplication", "getServerPid", e);
            }
        }
        return serverPid;
    }

    public static String getSELinuxContext() {
        if (serverContext != null) return serverContext;
        try {
            serverContext = requireService().getSELinuxContext();
        } catch (RemoteException e) {
            Log.e("AxeronApplication", "getSELinuxContext", e);
        }
        return serverContext;
    }

    public static String getVersionName() {
        if (serverVersionName != null) return serverVersionName;
        try {
            serverVersionName = requireService().getVersionName();
        } catch (RemoteException e) {
            Log.e("AxeronApplication", "getServerVersionName", e);
        }
        return serverVersionName;
    }

    public static long getVersionCode() {
        if (serverVersionCode == -1) {
            try {
                serverVersionCode = requireService().getVersionCode();
            } catch (RemoteException e) {
                Log.e("AxeronApplication", "getServerVersionCode", e);
            }
        }
        return serverVersionCode;
    }

    public static void destroy() {
        if (binder != null) {
            try {
                requireService().destroy();
            } catch (RemoteException e) {
                Log.e("AxeronApplication", "destroy", e);
            }
            binder = null;
            service = null;
            serverUid = -1;
            serverPid = -1;
            serverVersionName = null;
            serverVersionCode = -1;
            serverContext = null;
        }
    }

    public interface OnBinderReceivedListener {
        void onBinderReceived();
    }

    public interface OnBinderDeadListener {
        void onBinderDead();
    }

    private static class ListenerHolder<T> {

        private final T listener;
        private final Handler handler;

        private ListenerHolder(@NonNull T listener, @Nullable Handler handler) {
            this.listener = listener;
            this.handler = handler;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListenerHolder<?> that = (ListenerHolder<?>) o;
            return Objects.equals(listener, that.listener) && Objects.equals(handler, that.handler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(listener, handler);
        }
    }


}
