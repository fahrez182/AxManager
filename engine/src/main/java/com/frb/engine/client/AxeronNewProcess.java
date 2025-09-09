package com.frb.engine.client;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.NonNull;

import com.frb.engine.IRuntimeService;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

public class AxeronNewProcess extends Process implements Parcelable {

    public static final Creator<AxeronNewProcess> CREATOR = new Creator<>() {
        @Override
        public AxeronNewProcess createFromParcel(Parcel in) {
            return new AxeronNewProcess(in);
        }

        @Override
        public AxeronNewProcess[] newArray(int size) {
            return new AxeronNewProcess[size];
        }
    };
    private static final Set<AxeronNewProcess> CACHE = Collections.synchronizedSet(new ArraySet<>());
    private static final String TAG = "AxeronNewProcess";
    private IRuntimeService runtimeService;
    private OutputStream os;
    private InputStream is;


    public AxeronNewProcess(IRuntimeService remote) {
        this.runtimeService = remote;
        try {
            this.runtimeService.asBinder().linkToDeath(() -> {
                this.runtimeService = null;
                Log.v(TAG, "AxeronNewProcess is dead");

                CACHE.remove(AxeronNewProcess.this);
            }, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "linkToDeath", e);
        }

        // The reference to the binder object must be hold
        CACHE.add(this);
    }

    protected AxeronNewProcess(Parcel in) {
        runtimeService = IRuntimeService.Stub.asInterface(in.readStrongBinder());
    }

    @Override
    public OutputStream getOutputStream() {
        if (os == null) {
            try {
                os = new ParcelFileDescriptor.AutoCloseOutputStream(runtimeService.getOutputStream());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return os;
    }

    @Override
    public InputStream getInputStream() {
        if (is == null) {
            try {
                is = new ParcelFileDescriptor.AutoCloseInputStream(runtimeService.getInputStream());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return is;
    }

    @Override
    public InputStream getErrorStream() {
        try {
            return new ParcelFileDescriptor.AutoCloseInputStream(runtimeService.getErrorStream());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int waitFor() {
        try {
            return runtimeService.waitFor();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int exitValue() {
        try {
            return runtimeService.exitValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        try {
            runtimeService.destroy();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStrongBinder(runtimeService.asBinder());
    }
}
