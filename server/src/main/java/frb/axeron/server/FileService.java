package frb.axeron.server;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

    private static boolean copyFile(File src, File dst, boolean overwrite) throws IOException {
        if (!src.exists() || !src.isFile()) return false;
        File parent = dst.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            // gagal buat parent
        }
        if (dst.exists()) {
            if (!overwrite) return false;
            if (!dst.delete()) return false;
        }
        final byte[] buf = new byte[64 * 1024];
        try (InputStream in = new BufferedInputStream(new FileInputStream(src));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dst))) {
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            out.flush();
        }
        // sync metadata minimal
        dst.setLastModified(src.lastModified());
        return true;
    }

    private static boolean deleteRecursiveInternal(File file) {
        if (!file.exists()) return true;
        if (file.isFile()) return file.delete();
        File[] children = file.listFiles();
        if (children != null) {
            for (File c : children) {
                if (!deleteRecursiveInternal(c)) return false;
            }
        }
        return file.delete();
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
            fos.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean delete(String path) {
        return f(path).delete();
    }

    @Override
    public boolean deleteRecursive(String path) {
        return deleteRecursiveInternal(f(path));
    }

    @Override
    public boolean rename(String from, String to) {
        return f(from).renameTo(f(to));
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
    public ParcelFileDescriptor read(String path) throws RemoteException {
        try {
            return ParcelFileDescriptor.open(f(path), ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public void write(String path, ParcelFileDescriptor stream, IWriteCallback callback, boolean append) throws RemoteException {
        new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(stream.getFileDescriptor());
                 FileOutputStream fos = new FileOutputStream(path, append)) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                fos.close();
                fos.getFD().sync();

                if (callback != null) callback.onComplete();
            } catch (IOException | RemoteException e) {
                if (callback != null) {
                    try {
                        callback.onError(-1, e.getMessage());
                    } catch (RemoteException ignored) {
                    }
                }
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                        stream.getFileDescriptor().sync();
                    }
                } catch (IOException ignored) {}
            }
        }).start();
    }

    @Override
    public boolean copy(String from, String to, boolean overwrite) {
        try {
            return copyFile(f(from), f(to), overwrite);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean move(String from, String to, boolean overwrite) {
        File src = f(from);
        File dst = f(to);
        try {
            Files.move(
                    src.toPath(),
                    dst.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
