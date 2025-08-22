package com.frb.engine.implementation;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.frb.engine.IAxeronFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AxeronFile extends IAxeronFile.Stub {

    @Override
    public boolean mkdirs(String folderName) throws RemoteException {
        File folder = new File(folderName);
        return folder.mkdirs(); // folder berhasil dibuat]
    }

    @Override
    public boolean exists(String path) throws RemoteException {
        File file = new File(path);
        return file.exists();
    }

    @Override
    public boolean delete(String path) throws RemoteException {
        File file = new File(path);
        return file.delete();
    }

    @Override
    public boolean isFile(String filePath) throws RemoteException {
        File file = new File(filePath);
        return file.isFile();
    }

    @Override
    public boolean isDirectory(String path) throws RemoteException {
        File file = new File(path);
        return file.isDirectory();
    }

    @Override
    public boolean renameTo(String from, String to) throws RemoteException {
        File fileFrom = new File(from);
        File fileTo = new File(to);
        return fileFrom.renameTo(fileTo);
    }

    @Override
    public long length(String filePath) throws RemoteException {
        File file = new File(filePath);
        return file.length();
    }

    @Override
    public long lastModified(String filePath) throws RemoteException {
        File file = new File(filePath);
        return file.lastModified();
    }

    @Override
    public boolean setLastModified(String filePath, long newLastModified) throws RemoteException {
        File file = new File(filePath);
        return file.setLastModified(newLastModified);
    }

    @Override
    public List<String> getDirectories(String directoryPath) throws RemoteException {
        List<String> filePaths = new ArrayList<>();
        File directory = new File(directoryPath);

        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    filePaths.add(file.getAbsolutePath());
                }
            }
        }
        return filePaths;
    }


    @Override
    public ParcelFileDescriptor readFile(String filePath) throws RemoteException {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            return null;
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            return ParcelFileDescriptor.dup(fis.getFD()); // Convert ke PFD
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public ParcelFileDescriptor readFileUri(ParcelFileDescriptor pfd) throws RemoteException {
        return pfd;
    }

//    @Override
//    public ParcelFileDescriptor readFileUri(Uri fileUri) throws RemoteException {
//        if (fileUri == null) {
//            return null;
//        }
//
//        try {
//            ParcelFileDescriptor pfd = Axeron.getInstance().getApplicationContext().getContentResolver().openFileDescriptor(fileUri, "r");
//            return pfd;
//        } catch (FileNotFoundException e) {
//            return null;
//        }
//    }

    @Override
    public ParcelFileDescriptor readFileTouch(String filePath) throws RemoteException {
        File file = new File(filePath);

        try {
            if (!file.exists() || !file.isFile()) {
                file.createNewFile();
            }
            FileInputStream fis = new FileInputStream(file);
            return ParcelFileDescriptor.dup(fis.getFD()); // Convert ke PFD
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public ParcelFileDescriptor writeFile(String path) throws RemoteException {
        File file = new File(path);

        //file.setExecutable(true, false);

        try {
            // Buka file dalam mode tulis
            return ParcelFileDescriptor.open(file,
                    ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_WRITE_ONLY);
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        }
    }
}
