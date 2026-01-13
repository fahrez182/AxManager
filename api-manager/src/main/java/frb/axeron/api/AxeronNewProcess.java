package frb.axeron.api;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

import frb.axeron.server.IRuntimeService;

public class AxeronNewProcess extends Process implements Parcelable {

    public static final Creator<AxeronNewProcess> CREATOR = new Creator<AxeronNewProcess>() {
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
    private IRuntimeService remote;
    private OutputStream os;
    private InputStream is;


    public AxeronNewProcess(IRuntimeService remote) {
        this.remote = remote;
        try {
            this.remote.asBinder().linkToDeath(() -> {
                this.remote = null;
                Log.v(TAG, "remote process is dead");

                CACHE.remove(AxeronNewProcess.this);
            }, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "linkToDeath", e);
        }

        // The reference to the binder object must be hold
        CACHE.add(this);
    }

    protected AxeronNewProcess(Parcel in) {
        remote = IRuntimeService.Stub.asInterface(in.readStrongBinder());
    }

    @Override
    public OutputStream getOutputStream() {
        if (os == null) {
            try {
                os = new ParcelFileDescriptor.AutoCloseOutputStream(remote.getOutputStream());
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
                is = new ParcelFileDescriptor.AutoCloseInputStream(remote.getInputStream());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return is;
    }

    @Override
    public InputStream getErrorStream() {
        try {
            return new ParcelFileDescriptor.AutoCloseInputStream(remote.getErrorStream());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int waitFor() throws InterruptedException {
        try {
            return remote.waitFor();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int exitValue() {
        try {
            return remote.exitValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        try {
            remote.destroy();
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
        dest.writeStrongBinder(remote.asBinder());
    }
}
