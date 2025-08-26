package com.frb.engine.client;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.frb.engine.Engine;
import com.frb.engine.IFileService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AxeronFile extends Axeron {

    private IFileService fileService;

    private IFileService getFS() {
        if (fileService == null) {
            try {
                fileService = requireService().getFileService();
            } catch (RemoteException e) {
                Log.e(TAG, "getFileService", e);
            }
        }
        return fileService;
    }

    public FileInputStream setFileInputStream(String source) throws RemoteException {
        return new ParcelFileDescriptor.AutoCloseInputStream(
                getFS().open(
                        source,
                        ParcelFileDescriptor.MODE_READ_ONLY,
                        false // ensureParents gak perlu kalau cuma read
                )
        );
    }

    public FileOutputStream setFileOutputStream(String destination) throws RemoteException {
        return new ParcelFileDescriptor.AutoCloseOutputStream(
                getFS().open(
                        destination,
                        ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_WRITE_ONLY,
                        true
                        )
        );
    }

    public void extractBusyBoxFromSo() {
        try {
            String abi = android.os.Build.SUPPORTED_ABIS[0];
            File outFile = new File("/data/local/tmp/AxManager/bin", "busybox");
            String libPath = Engine.getApplication().getApplicationInfo().nativeLibraryDir + "/libbusybox.so";
            String outPath = outFile.getAbsolutePath();

            if (getFS().exists(outFile.getAbsolutePath())) {
                return; // sudah diekstrak
            }

            if (!getFS().exists(libPath)) {
                Log.e(TAG, "BusyBox lib not found: " + libPath);
                return;
            }

            // copy ke internal storage
            try (InputStream is = setFileInputStream(libPath);
                 FileOutputStream fos = setFileOutputStream(outPath)) {

                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
            }

            // ubah permission biar bisa dieksekusi
            if (!getFS().chown(outPath, 2000, 2000)) {
                Log.e(TAG, "Failed to chown " + outPath);
            }
            if (!getFS().chmod(outPath, 0755)) {
                Log.e(TAG, "Failed to chmod " + outPath);
            }

            Log.i(TAG, "BusyBox extracted to: " + outPath);
        } catch (RemoteException | IOException e) {
            throw new RuntimeException(e);
        }
    }



}
