package com.frb.engine.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frb.engine.Environment;
import com.frb.engine.IAxeronApplication;
import com.frb.engine.IAxeronService;
import com.frb.engine.core.ConstantEngine;
import com.frb.engine.core.Engine;
import com.frb.engine.implementation.AxeronInfo;
import com.frb.engine.utils.PathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import rikka.parcelablelist.ParcelableListSlice;

public class Axeron {
    private static final List<ListenerHolder<OnBinderReceivedListener>> RECEIVED_LISTENERS = new ArrayList<>();
    private static final List<ListenerHolder<OnBinderDeadListener>> DEAD_LISTENERS = new ArrayList<>();

    protected static String TAG = "AxeronApplication";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static IBinder binder;
    private static IAxeronService service;
    private static AxeronInfo serverInfo = null;
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

    public static void onBinderReceived(IBinder newBinder, Context context) {
        if (binder == newBinder) return;

        if (newBinder == null) {
            binder = null;
            service = null;
            serverInfo = null;

            scheduleBinderDeadListeners();
        } else {
            SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

            if (binder != null) {
                binder.unlinkToDeath(DEATH_RECIPIENT, 0);
            }
            binder = newBinder;
            service = IAxeronService.Stub.asInterface(newBinder);

            try {
                binder.linkToDeath(DEATH_RECIPIENT, 0);
            } catch (Throwable e) {
                Log.i(TAG, "attachApplication");
            }

            scheduleBinderReceivedListeners();

            try {
                service.bindAxeronApplication(new IAxeronApplication.Stub() {
                    @Override
                    public void bindApplication(Bundle data) {
                        if (isFirstInit(false)
                                || prefs.getBoolean("ignite_when_relog", false)) {
                            Log.d(TAG, "igniteService");
                            PluginService.igniteService();
                        }
                    }
                });
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

        }
    }

    protected static boolean isFirstInit(boolean markAsFirstInit) {
        try {
            return requireService().isFirstInit(markAsFirstInit);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
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
    }

    public static AxeronFileService newFileService() {
        try {
            return new AxeronFileService(requireService().getFileService());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private static final IBinder.DeathRecipient DEATH_RECIPIENT = () -> {
        binderReady = false;
        onBinderReceived(null, null);
    };

    public static AxeronNewProcess newProcess(@NonNull String cmd) {
        return newProcess(new String[]{"sh", "-c", cmd});
    }

    public static AxeronNewProcess newProcess(@NonNull String[] cmd) {
        return newProcess(cmd, Axeron.getEnvironment(), null);
    }

    public static AxeronNewProcess newProcess(@NonNull String[] cmd, @Nullable Environment env, @Nullable String dir) {
        try {
            return new AxeronNewProcess(requireService().getRuntimeService(cmd, env, dir));
        } catch (RemoteException | NullPointerException e) {
//            Log.d(TAG, "Failed to execute command", e);
            throw new RuntimeException("Failed to execute command", e);
        }
    }

    public static AxeronInfo getInfo() {
        if (serverInfo != null) return serverInfo;
        try {
            serverInfo = requireService().getInfo();
        } catch (RemoteException e) {
            Log.e(TAG, "getInfo", e);
        }
        return serverInfo;

    }

    public static ParcelableListSlice<PackageInfo> getPackages(int flags) {
        try {
            return requireService().getPackages(flags);
        } catch (RemoteException ignored) {
        }
        return null;
    }

    public static List<String> getPlugins() {
        try {
            return requireService().getPlugins();
        } catch (RemoteException e) {
            return null;
        }
    }

    public static String getPluginById(String id) {
        try {
            return requireService().getPluginById(id);
        } catch (RemoteException e) {
            return null;
        }
    }

    public static void destroy() {
        if (binder != null) {
            try {
                requireService().destroy();
            } catch (RemoteException ignored) {}
            binder = null;
            service = null;
            serverInfo = null;
        }
    }

    public static Environment getEnvironment() {
        return new Environment.Builder(false)
                .put("AXERON", "true")
                .put("AXERONDIR", PathHelper.getShellPath(ConstantEngine.folder.PARENT).getAbsolutePath())
                .put("AXERONBIN", PathHelper.getShellPath(ConstantEngine.folder.PARENT_BINARY).getAbsolutePath())
                .put("AXERONLIB", Engine.getApplication().getApplicationInfo().nativeLibraryDir)
                .put("AXERONVER", String.valueOf(getInfo().getVersionCode()))
                .put("TMPDIR", PathHelper.getShellPath(ConstantEngine.folder.CACHE).getAbsolutePath())
                .put("PATH", "$PATH:$AXERONBIN")
                .build();
    }



    private static RuntimeException rethrowAsRuntimeException(RemoteException e) {
        return new RuntimeException(e);
    }

    public static void transactRemote(@NonNull Parcel data, @Nullable Parcel reply, int flags) {
        try {
            requireService().asBinder().transact(1, data, reply, flags);
            Log.d(TAG, "transactRemote");
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException(e);
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
