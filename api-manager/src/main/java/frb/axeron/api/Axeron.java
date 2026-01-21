package frb.axeron.api;


import static frb.axeron.shared.AxeronConstant.server.TYPE_ENV;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import frb.axeron.api.core.AxeronSettings;
import frb.axeron.api.core.Engine;
import frb.axeron.shared.AxeronConstant;
import frb.axeron.shared.Environment;
import frb.axeron.shared.PluginInfo;
import frb.axeron.server.IAxeronApplication;
import frb.axeron.server.IAxeronService;
import moe.shizuku.server.IShizukuService;
import rikka.shizuku.Shizuku;

public class Axeron {
    private static final List<ListenerHolder<OnBinderReceivedListener>> RECEIVED_LISTENERS = new ArrayList<>();
    private static final List<ListenerHolder<OnBinderDeadListener>> DEAD_LISTENERS = new ArrayList<>();

    protected static String TAG = "AxeronApplication";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static IBinder binder;
    private static IAxeronService service;
    private static AxeronInfo axeronInfo = null;
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

    public static void onBinderReceived(IBinder newBinder) {
        if (binder == newBinder) return;

        if (newBinder == null) {
            binder = null;
            service = null;
            axeronInfo = null;

            scheduleBinderDeadListeners();
        } else {
//            SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

            if (pingBinder()) {
                binder.unlinkToDeath(DEATH_RECIPIENT, 0);
            }
            binder = newBinder;
            service = IAxeronService.Stub.asInterface(newBinder);
            notifyShizuku();

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
                                || AxeronSettings.getEnableIgniteRelog()) {
                            Log.d(TAG, "igniteService");
                            AxeronPluginService.igniteService();
                        }
                    }
                });
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static IShizukuService getShizukuService() {
        try {
            if (service == null) return null;
            return service.getShizukuService();
        } catch (RemoteException e) {
            Log.e(TAG, "getShizukuService", e);
            return null;
        }
    }

    public synchronized static void notifyShizuku() {
        IShizukuService shizukuService = getShizukuService();
        if (shizukuService != null) {
            Shizuku.onBinderReceived(shizukuService.asBinder(), Engine.getApplication().getPackageName());
        } else {
            Shizuku.onBinderReceived(null, Engine.getApplication().getPackageName());
        }
    }

    public static void enableShizukuService(boolean enable) {
        try {
            requireService().enableShizukuService(enable);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
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
        onBinderReceived(null);
    };

    public static AxeronNewProcess newProcess(@NonNull String cmd) {
        return newProcess(new String[]{"sh", "-c", cmd});
    }

    public static AxeronNewProcess newProcess(@NonNull String[] cmd) {
        return newProcess(cmd, Axeron.getEnvironment(TYPE_ENV), null);
    }

    public static AxeronNewProcess newProcess(@NonNull String[] cmd, @Nullable Environment env, @Nullable String dir) {
        try {
            return new AxeronNewProcess(requireService().getRuntimeService(cmd, env, dir));
        } catch (RemoteException | NullPointerException e) {
//            Log.d(TAG, "Failed to execute command", e);
            throw new RuntimeException("Failed to execute command", e);
        }
    }

    public static AxeronInfo getAxeronInfo() {
        if (axeronInfo != null) return axeronInfo;
        try {
            axeronInfo = new AxeronInfo(requireService().getServerInfo());
        } catch (RemoteException e) {
            Log.e(TAG, "getInfo", e);
        }
        return axeronInfo;

    }

    public static List<PackageInfo> getPackages(int flags) {
        try {
            return requireService().getPackages(flags).getList();
        } catch (RemoteException ignored) {
        }
        return new ArrayList<>();
    }

    public static List<PluginInfo> getPlugins() {
        try {
            return requireService().getPlugins().getList();
        } catch (RemoteException ignored) {
        }
        return new ArrayList<>();
    }

    public static PluginInfo getPluginById(String id) {
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
            } catch (RemoteException ignored) {
            }
            binder = null;
            service = null;
            axeronInfo = null;
            scheduleBinderDeadListeners();
            notifyShizuku();
        }
    }

    public static Environment getEnvironment() {
        return getEnvironment(TYPE_ENV);
    }

    public static Environment getEnvironment(int envType) {
        try {
            return requireService().getEnvironment(envType);
        } catch (Exception e) {
            return null;
        }
    }

    public static void setNewEnvironment(Environment env) {
        try {
            requireService().setNewEnvironment(env);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private static RuntimeException rethrowAsRuntimeException(String message, RemoteException e) {
        return new RuntimeException(message, e);
    }

    private static RuntimeException rethrowAsRuntimeException(RemoteException e) {
        return new RuntimeException(e);
    }

    public static void transactRemote(@NonNull Parcel data, @Nullable Parcel reply, int flags) {
        try {
            requireService().asBinder().transact(1, data, reply, flags);
        } catch (RemoteException e) {
            throw rethrowAsRuntimeException("Axeron", e);
        }
    }

    public static boolean isUpdated() {
        return AxeronConstant.server.getActualVersion() <= Axeron.getAxeronInfo().getActualVersion();
    }

    public interface OnBinderReceivedListener {
        void onBinderReceived();
    }

    public interface OnBinderDeadListener {
        void onBinderDead();
    }

    private record ListenerHolder<T>(T listener, Handler handler) {

        private ListenerHolder(@NonNull T listener, @Nullable Handler handler) {
            this.listener = listener;
            this.handler = handler;
        }

    }


}
