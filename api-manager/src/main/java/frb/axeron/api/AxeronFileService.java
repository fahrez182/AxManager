package frb.axeron.api;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import frb.axeron.api.utils.Excluder;
import frb.axeron.shared.FileStat;
import frb.axeron.server.IFileService;
import frb.axeron.server.IOutputStreamCallback;

public class AxeronFileService implements Parcelable {

    public static final Creator<AxeronFileService> CREATOR = new Creator<>() {
        @Override
        public AxeronFileService createFromParcel(Parcel in) {
            return new AxeronFileService(in);
        }

        @Override
        public AxeronFileService[] newArray(int size) {
            return new AxeronFileService[size];
        }
    };
    private static final Set<AxeronFileService> CACHE = Collections.synchronizedSet(new ArraySet<>());
    private IFileService fileService;
    private static final String TAG = "AxeronFileService";

    public AxeronFileService(IFileService fileService) {
        this.fileService = fileService;
        try {
            this.fileService.asBinder().linkToDeath(() -> {
                this.fileService = null;
                Log.v(TAG, "AxeronFileService is dead");

                CACHE.remove(AxeronFileService.this);
            }, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "linkToDeath", e);
        }

        // The reference to the binder object must be hold
        CACHE.add(this);
    }

    private IFileService getFS() {
        if (fileService != null) return fileService;
        try {
            fileService = Axeron.requireService().getFileService();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return fileService;
    }

    public static String getRelativePath(String rootPath, String fullPath) {
        return new File(rootPath).toURI().relativize(new File(fullPath).toURI()).getPath();
    }

    protected AxeronFileService(Parcel in) {
        fileService = IFileService.Stub.asInterface(in.readStrongBinder());
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

    public FileInputStream setFileInputStream(String source) throws RemoteException, FileNotFoundException {
        ParcelFileDescriptor pfd = getFS().inputStreamPfd(source);
        if (pfd == null) throw new FileNotFoundException();
        return new ParcelFileDescriptor.AutoCloseInputStream(pfd);
    }

    public boolean exists(String path) {
        try {
            return getFS().exists(path);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean mkdirs(String path) {
        try {
            return getFS().mkdirs(path);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean fsync(ParcelFileDescriptor pfd) {
        try {
            return getFS().fsync(pfd);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean delete(String pathFrom) {
        return delete(pathFrom, true);
    }

    public boolean delete(String pathFrom, boolean removeParent) {
        return smartDelete(pathFrom, null, removeParent) != null;
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

                if (checkedFolders.contains(folderPath)) {
                    getFS().delete(folderPath);
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

    public boolean createNewFile(String absolutePath) throws RemoteException {
        return getFS().createNewFile(absolutePath);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(getFS().asBinder());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static class StreamSession implements AutoCloseable {
        private final ParcelFileDescriptor inputFd;
        private final ParcelFileDescriptor outputFd;

        private final IFileService fileService;

        public StreamSession(IFileService fileService, String destination, boolean ensureParents, boolean append) throws IOException, RemoteException {
            this.fileService = fileService;
            FileStat file = fileService.stat(destination);
            if (ensureParents) {
                FileStat p = fileService.stat(file.parent);
                if (p != null && !p.exists) {
                    fileService.mkdirs(p.path);
                }
            }

            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            inputFd = pipe[0];
            outputFd = pipe[1];
            IOutputStreamCallback callback = new IOutputStreamCallback.Stub() {
                @Override
                public void onComplete() {
                    close();
                    Log.d("StreamSession", "Stream finished safely");
                }

                @Override
                public void onError(int errCode, String message) {
                    close();
                    Log.e("StreamSession", "Stream error: " + message);
                }
            };
            fileService.outputStreamPfd(destination, inputFd, callback, append);
        }

        public FileOutputStream getOutputStream() {
            return new FileOutputStream(outputFd.getFileDescriptor());
        }

        @Override
        public void close() {
            try {
                if (inputFd != null) {
                    inputFd.close();
                }
                if (outputFd != null) {
                    fileService.fsync(outputFd);
                    outputFd.close();
                }
            } catch (IOException | RemoteException ignored) {
            }
        }
    }
}
