package com.frb.engine.core;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frb.engine.client.Axeron;
import com.frb.engine.utils.BinderContainer;

public class AxeronProvider extends ContentProvider {
    // For receive Binder from Shizuku
    public static final String METHOD_SEND_BINDER = "sendBinder";
    // For share Binder between processes
    public static final String METHOD_GET_BINDER = "getBinder";
    public static final String ACTION_BINDER_RECEIVED = "AxServer.BINDER_RECEIVED";
    private static final String TAG = "AxProvider";
    public static final String EXTRA_BINDER = "AxServer.BINDER";

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);

        if (info.multiprocess)
            throw new IllegalStateException("android:multiprocess must be false");

        if (!info.exported)
            throw new IllegalStateException("android:exported must be true");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        Log.i(TAG, "Called");

        if (extras == null) {
            return null;
        }

        extras.setClassLoader(BinderContainer.class.getClassLoader());

        Bundle reply = new Bundle();
        switch (method) {
            case METHOD_SEND_BINDER: {
                handleSendBinder(extras);
                break;
            }
            case METHOD_GET_BINDER: {
                if (!handleGetBinder(reply)) {
                    return null;
                }
                break;
            }
        }
        return reply;
    }

    private void handleSendBinder(@NonNull Bundle extras) {
        if (Axeron.pingBinder()) {
            Log.d(TAG, "sendBinder is called when already a living binder");
            return;
        }

        BinderContainer container = extras.getParcelable(EXTRA_BINDER);
        if (container != null && container.binder != null) {
            Log.d(TAG, "binder received");

            Axeron.onBinderReceived(container.binder, getContext().getPackageName());

            Log.d(TAG, "broadcast binder");
            Intent intent = new Intent(ACTION_BINDER_RECEIVED)
                    .setPackage(getContext().getPackageName());
            getContext().sendBroadcast(intent);
        }
    }

    private boolean handleGetBinder(@NonNull Bundle reply) {
        // Other processes in the same app can read the provider without permission
        IBinder binder = Axeron.getBinder();
        if (binder == null || !binder.pingBinder())
            return false;

        reply.putParcelable(EXTRA_BINDER, new BinderContainer(binder));
        return true;
    }

    // no other provider methods
    @Nullable
    @Override
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public final String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public final Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public final int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public final int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
