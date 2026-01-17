package frb.axeron.server;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import frb.axeron.data.FileStat;
import frb.axeron.server.utils.Logger;

public class FileService extends IFileService.Stub {
    private static final String TAG = "FileService";
    private static final Logger LOGGER = new Logger(TAG);

    private static File f(String path) {
        return new File(path);
    }

    // ---------- Helpers ----------

    private static FileStat toStat(File file) {
        FileStat s = new FileStat();
        s.path = file.getAbsolutePath();
        s.name = file.getName();
        File parent = file.getParentFile();
        s.parent = (parent != null) ? parent.getAbsolutePath() : "";
        s.exists = file.exists();
        s.isFile = s.exists && file.isFile();
        s.isDirectory = s.exists && file.isDirectory();
        s.isHidden = file.isHidden();
        if (s.exists) {
            s.length = file.length();
            s.lastModified = file.lastModified();
        } else {
            s.length = 0L;
            s.lastModified = 0L;
        }
        try {
            s.canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            s.canonicalPath = "";
        }
        return s;
    }

    private static Comparator<File> comparator(int sortBy, boolean asc) {
        Comparator<File> c;
        switch (sortBy) {
            case IFileService.SORT_LAST_MOD:
                c = Comparator.comparingLong(File::lastModified);
                break;
            case IFileService.SORT_LENGTH:
                c = Comparator.comparingLong(f -> f.isFile() ? f.length() : -1L);
                break;
            case IFileService.SORT_NAME:
            default:
                c = Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER);
        }
        return asc ? c : c.reversed();
    }


    // ---------- Implementasi IAxFile ----------

    @Override
    public boolean mkdirs(String dirPath) {
        return f(dirPath).mkdirs();
    }

    @Override
    public boolean createNewFile(String path) {
        File newFile = new File(path);
        if (newFile.exists() && newFile.isFile()) return true;
        File parent = new File(newFile.getParent());
        if (!parent.exists()) {
            if (!parent.mkdirs()) return false;
        }

        try {
            FileOutputStream fos = new FileOutputStream(path, false);
            Os.fsync(fos.getFD());
            fos.close();
            return true;
        } catch (IOException | ErrnoException e) {
            return false;
        }
    }

    @Override
    public boolean delete(String path) {
        return f(path).delete();
    }

    @Override
    public boolean exists(String path) {
        return f(path).exists();
    }

    @Override
    public FileStat stat(String path) {
        return toStat(f(path));
    }

    @Override
    public String getCanonicalPath(String path) {
        try {
            return f(path).getCanonicalPath();
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public List<FileStat> list(String dirPath, int filter, int sortBy, boolean ascending, int offset, int limit) {
        List<FileStat> out = new ArrayList<>();
        File dir = f(dirPath);
        if (!dir.exists() || !dir.isDirectory()) return out;

        File[] children = dir.listFiles();
        if (children == null) return out;

        List<File> filtered = new ArrayList<>(children.length);
        for (File c : children) {
            switch (filter) {
                case IFileService.FILTER_FILES:
                    if (c.isFile()) filtered.add(c);
                    break;
                case IFileService.FILTER_DIRECTORIES:
                    if (c.isDirectory()) filtered.add(c);
                    break;
                case IFileService.FILTER_ALL:
                default:
                    filtered.add(c);
            }
        }

        filtered.sort(comparator(sortBy, ascending));

        int n = filtered.size();
        int start = Math.max(0, Math.min(offset, n));
        int end = (limit <= 0) ? n : Math.min(start + limit, n);

        for (int i = start; i < end; i++) out.add(toStat(filtered.get(i)));
        return out;
    }

    @Override
    public boolean setLastModified(String path, long newLastModified) {
        return f(path).setLastModified(newLastModified);
    }

    @Override
    public boolean chmod(String path, int mode) {
        try {
            Os.chmod(path, mode);
            return true;
        } catch (ErrnoException e) {
            return false;
        }
    }

    @Override
    public boolean chown(String path, int uid, int gid) {
        try {
            Os.chown(path, uid, gid);
            return true;
        } catch (ErrnoException e) {
            return false;
        }
    }

    @Override
    public boolean fsync(ParcelFileDescriptor pfd) {
        try (pfd) {
            try {
                Os.fsync(pfd.getFileDescriptor());
                return true;
            } catch (Exception e) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ParcelFileDescriptor inputStreamPfd(String path) {
        try {
            return ParcelFileDescriptor.open(f(path), ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public void outputStreamPfd(String path, ParcelFileDescriptor input, IOutputStreamCallback callback, boolean append) throws RemoteException {
        new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(input.getFileDescriptor());
                 FileOutputStream fos = new FileOutputStream(path, append)) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                Os.fsync(fos.getFD());
                fos.close();

                if (callback != null) callback.onComplete();
            } catch (IOException | RemoteException | ErrnoException e) {
                if (callback != null) {
                    try {
                        if (e instanceof IOException) {
                            callback.onError(IOutputStreamCallback.IO_EXCEPTION, e.getMessage());
                        } else if (e instanceof ErrnoException) {
                            callback.onError(IOutputStreamCallback.ERRNO_EXCEPTION, e.getMessage());
                        } else {
                            callback.onError(IOutputStreamCallback.FILE_NOT_FOUND, e.getMessage());
                        }
                    } catch (RemoteException ignored) {
                    }
                }
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ignored) {}
            }
        }).start();
    }

    private void fsyncDir(File dir) {
        if (dir == null) return;
        try (FileInputStream fis = new FileInputStream(dir)) {
            Os.fsync(fis.getFD());
        } catch (Throwable ignored) {}
    }

    private void copyFileDurable(File src, File dst) throws IOException, ErrnoException {
        final byte[] buf = new byte[64 * 1024];

        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {

            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            out.flush(); // flush userspace buffer
            Os.fsync(out.getFD()); // fsync FILE tujuan
        }

        // Optional: samakan timestamp (metadata)
        dst.setLastModified(src.lastModified());
    }

    @Override
    public boolean renameTo(String src, String dst, boolean overwrite) {
        File srcFile = f(src);
        File dstFile = f(dst);
        if (!srcFile.exists() || !srcFile.isFile()) return false;

        File dstParent = dstFile.getParentFile();
        if (dstParent != null && !dstParent.exists()) {
            if (!dstParent.mkdirs()) return false;
        }

        // Handle overwrite
        if (dstFile.exists()) {
            if (!overwrite) return false;
            if (!dstFile.delete()) return false;
            fsyncDir(dstParent);
        }

        // 1) TRY ATOMIC RENAME (same filesystem)
        if (srcFile.renameTo(dstFile)) {
            // rename is atomic; fsync directory to persist metadata
            fsyncDir(dstParent);
            return true;
        }

        // 2) FALLBACK: CROSS-FILESYSTEM MOVE
        // copy -> fsync(file) -> fsync(dst dir) -> delete src -> fsync(src dir)
        try {
            copyFileDurable(srcFile, dstFile);
        } catch (ErrnoException | IOException e) {
            return false;
        }
        fsyncDir(dstParent);

        if (!srcFile.delete()) return false;
        fsyncDir(srcFile.getParentFile());

        return true;
    }


//    @Override
//    public boolean move(String from, String to, boolean overwrite) {
//        File src = f(from);
//        File dst = f(to);
//        src.
//        try {
//            Files.move(
//                    src.toPath(),
//                    dst.toPath(),
//                    StandardCopyOption.ATOMIC_MOVE,
//                    StandardCopyOption.REPLACE_EXISTING
//            );
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
}
