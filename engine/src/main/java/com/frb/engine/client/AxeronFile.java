package com.frb.engine.client;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.frb.engine.FileStat;
import com.frb.engine.IFileService;
import com.frb.engine.IWriteCallback;
import com.frb.engine.core.Engine;
import com.frb.engine.utils.Excluder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AxeronFile extends Axeron {

    private static final String TAG = "AxeronFile";
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

    public static String getRelativePath(String rootPath, String fullPath) {
        return new File(rootPath).toURI().relativize(new File(fullPath).toURI()).getPath();
    }

    public FileInputStream setFileInputStream(String source) throws RemoteException {
        return new ParcelFileDescriptor.AutoCloseInputStream(getFS().read(source));
    }

    protected int getDynamicBufferSize(long fileSize) {
        return (int) Math.max(fileSize * 2 / 100, 4096);
    }

    protected void dynamicWriteFile(OutputStream os, InputStream in, long size) throws IOException {
        byte[] buffer = new byte[getDynamicBufferSize(size)];
        int length;
        while ((length = in.read(buffer)) > 0) {
            os.write(buffer, 0, length);
        }
    }

    public StreamSession getStreamSession(String destination, boolean ensureParents, boolean append) {
        try {
            return new StreamSession(getFS(), destination, ensureParents, append);
        } catch (IOException | RemoteException e) {
            return null;
        }
    }


//    private final Map<Integer, ParcelFileDescriptor> keepAlive = new HashMap<>();
//    private final AtomicInteger counter = new AtomicInteger();
//
//    public FileOutputStream getFileOutputStream(String destination, boolean ensureParents, boolean append) {
//        try {
//            FileStat file = getFS().stat(destination);
//
//            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
//            ParcelFileDescriptor readFd = pipe[0];
//            ParcelFileDescriptor writeFd = pipe[1];
//            if (ensureParents) {
//                FileStat p = getFS().stat(file.parent);
//                if (p != null && !p.exists) {
//                    getFS().mkdirs(p.path);
//                }
//            }
//
//            int id = counter.incrementAndGet();
//            keepAlive.put(id, readFd);
//
//            getFS().write(destination, readFd, append);
//            return new FileOutputStream(writeFd.getFileDescriptor()) {
//                @Override
//                public void close() throws IOException {
//                    super.close();
//                    // bersihin readFd setelah selesai
//                    ParcelFileDescriptor pfd = keepAlive.remove(id);
//                    if (pfd != null) {
//                        pfd.close();
//                    }
//                }
//            };
//        } catch (IOException | RemoteException e) {
//            return null;
//        }
//    }

    public boolean delete(String pathFrom) {
        return smartDelete(pathFrom, null) != null;
    }

    @Nullable
    public List<FileStat> smartDelete(String pathFrom, List<Excluder> excluders) {
        return smartDelete(pathFrom, excluders, true);
    }

    @Nullable
    public List<FileStat> smartDelete(String pathFrom, List<Excluder> excluders, boolean removeParent) {
        Log.d("smartDelete", "pathFrom: " + pathFrom);
        Set<String> checkedFolders = new HashSet<>();
        return smartDeleteRecursive(pathFrom, pathFrom, excluders, checkedFolders, removeParent);
    }

    @Nullable
    private List<FileStat> smartDeleteRecursive(String rootPath, String folderPath, List<Excluder> excluders, Set<String> checkedFolders, boolean removeParent) {
        String TAG = "smartDelete";
        List<FileStat> deleted = new ArrayList<>();

        try {
            FileStat pathStat = getFS().stat(folderPath);

            if (!pathStat.exists) return null;
            // Jika file/folder masuk daftar exclude, jangan dihapus
            checkedFolders.add(pathStat.parent);

            if (Excluder.shouldSmartExclude(pathStat.path, excluders)) {
                Log.d(TAG, "Skipping deleted: " + pathStat.path);
                return deleted;
            }

            if (pathStat.isDirectory) {
                List<FileStat> children = getDirectories(pathStat.path);

                if (children != null) {
                    for (FileStat childStat : children) {
                        File childFile = new File(childStat.path);

                        List<FileStat> result = smartDeleteRecursive(
                                rootPath,
                                childFile.getAbsolutePath(),
                                excluders,
                                checkedFolders,
                                removeParent
                        );
                        if (result == null) return null;
                        deleted.addAll(result);
                    }
                }

                if (!removeParent && Objects.equals(rootPath, folderPath)) return deleted;

//                Log.i(TAG, "FolderChecked: " + checkedFolders);
                if (checkedFolders.contains(folderPath)) {
                    getFS().delete(folderPath);
//                    if (!) {
//                        Log.e(TAG, "Gagal menghapus checked folder: " + folderPath);
//                    }
                    return deleted;
                }

                // Hapus folder setelah isinya dihapus
                if (!getFS().delete(folderPath)) {
                    Log.e(TAG, "Gagal menghapus folder: " + folderPath);
                    return null;
                }

                deleted.add(pathStat);
                Log.d(TAG, "Folder deleted: " + folderPath);
            } else {
                if (!getFS().delete(folderPath)) {
                    Log.e(TAG, "Gagal menghapus file: " + folderPath);
                    return null;
                }

                deleted.add(pathStat);
                Log.d(TAG, "File deleted: " + folderPath);
            }

        } catch (RemoteException e) {
            Log.e(TAG, "Error deleting: " + e.getMessage(), e);
            return null;
        }

        return deleted;
    }

    public List<FileStat> getDirectories(String folderPath) {
        try {
            return getFS().list(folderPath, IFileService.FILTER_ALL, IFileService.SORT_NAME, true, 0, -1);
        } catch (RemoteException e) {
            return new ArrayList<>();
        }
    }

    public boolean copy(String pathFrom, String destination) {
        return smartCopy(pathFrom, destination, null) != null;
    }

    @Nullable
    public List<FileStat> smartCopy(String pathFrom, String destination, List<Excluder> excluders) {
        return smartCopy(pathFrom, destination, excluders, false);
    }

    @Nullable
    public List<FileStat> smartCopy(String pathFrom, String destination, List<Excluder> excluders, boolean sanitize) {
        Set<String> sanitizedPath = sanitize ? new HashSet<>() : null;
        List<FileStat> result = smartCopyRecursive(pathFrom, pathFrom, destination, destination, excluders, sanitizedPath);
        if (sanitizedPath != null) {
            Log.d("smartDelete", "SanitizedCopy: " + sanitizedPath);
            smartDelete(destination, new ArrayList<>(List.of(
                    new Excluder(Excluder.Mode.START, sanitizedPath, false)
            )), false);
            sanitizedPath.clear();
        }
        return result;
    }

    @Nullable
    private List<FileStat> smartCopyRecursive(String rootPath, String folderPath, String rootDestination, String destination, List<Excluder> excluders, Set<String> sanitizedPath) {
        List<FileStat> copied = new ArrayList<>();

        try {
            FileStat pathStat = getFS().stat(folderPath);
            FileStat destStat = getFS().stat(destination);
            if (!pathStat.exists) return null;
            File dest = new File(destStat.path);
            String relativePath = getRelativePath(rootPath, pathStat.path);

            // Skip jika folder masuk daftar exclude
            if (Excluder.shouldSmartExclude(relativePath, excluders)) {
                // Jika file/folder sebelumnya sudah ada di destination dan sekarang excluded, hapus
                if (pathStat.isFile) {
                    getFS().delete(destStat.path);
                    Log.i(TAG, "File dihapus karena excluded");
                } else if (pathStat.isDirectory) {
                    if (delete(destStat.path)) {
                        Log.i(TAG, "Folder dihapus karena excluded");
                    } else {
                        Log.e(TAG, "Gagal menghapus folder karena excluded");
                    }
                } else {
                    Log.d(TAG, "Skipping excluded: " + getRelativePath(rootPath, pathStat.path));
                }

                return copied;
            }

            if (pathStat.isDirectory) {
                if (!getFS().mkdirs(destination)) {
                    Log.e(TAG, "Gagal membuat directory: " + destination);
                    return null;
                }

                List<FileStat> children = getDirectories(folderPath);
                if (children != null) {
                    for (FileStat childStat : children) {
                        File childFile = new File(childStat.path);
                        List<FileStat> result = smartCopyRecursive(
                                rootPath, // tetap selalusama
                                childFile.getAbsolutePath(),
                                rootDestination,
                                new File(dest, childFile.getName()).getAbsolutePath(),
                                excluders,
                                sanitizedPath
                        );
                        if (result == null) return null;
                        copied.addAll(result);
                    }
                }
            } else {
                if (sanitizedPath != null) {
                    if (!Objects.equals(rootDestination, destStat.path))
                        sanitizedPath.add(destStat.path);
                }

                if (destStat.isFile &&
                        pathStat.length == destStat.length &&
                        pathStat.lastModified == destStat.lastModified) {
                    Log.d(TAG, "Skipping identical file: " + pathStat.path);
                    return copied;
                }

                Log.i(TAG, "Copying from: " + pathStat.path + " to " + destStat.path);

                if (destStat.isFile && !getFS().delete(destStat.path)) {
                    Log.e(TAG, "Gagal menghapus file tujuan sebelum overwrite: " + destStat.path);
                    return null;
                }


                try (FileInputStream in = setFileInputStream(pathStat.path);
                     FileOutputStream out = getStreamSession(destStat.path, true, false).getOutputStream()) {

                    dynamicWriteFile(out, in, in.getChannel().size());

                    getFS().setLastModified(destination, pathStat.lastModified);
                    copied.add(destStat);
                    Log.d(TAG, "File copied successfully: " + folderPath);
                }
            }
        } catch (IOException | RemoteException e) {
            Log.e(TAG, "Error copying folder: " + e.getMessage(), e);
            return null;
        }

        return copied;
    }

    public void extractBusyBoxFromSo() {
        try {
            String abi = Build.SUPPORTED_ABIS[0];
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
                 FileOutputStream fos = getStreamSession(outPath, true, false).getOutputStream()) {

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

    public boolean createNewFile(String absolutePath) throws RemoteException {
        return getFS().createNewFile(absolutePath);
    }

    public static class StreamSession implements AutoCloseable {
        private final ParcelFileDescriptor readFd;
        private final ParcelFileDescriptor writeFd;

        public StreamSession(IFileService fs, String destination, boolean ensureParents, boolean append) throws IOException, RemoteException {
            FileStat file = fs.stat(destination);
            if (ensureParents) {
                FileStat p = fs.stat(file.parent);
                if (p != null && !p.exists) {
                    fs.mkdirs(p.path);
                }
            }

            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            readFd = pipe[0];
            writeFd = pipe[1];
            IWriteCallback callback = new IWriteCallback.Stub() {
                @Override
                public void onComplete() {
                    close();
                    Log.d("StreamSession", "Stream finished safely");
                }

                @Override
                public void onError(int code, String message) {
                    close();
                    Log.e("StreamSession", "Stream error: " + message);
                }
            };
            fs.write(destination, readFd, callback, append);
        }

        public FileOutputStream getOutputStream() {
            return new FileOutputStream(writeFd.getFileDescriptor());
        }

        @Override
        public void close() {
            try {
                if (writeFd != null) writeFd.close();
                if (readFd != null) readFd.close();
            } catch (IOException ignored) {
            }
        }
    }
}
