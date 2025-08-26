package com.frb.engine.implementation;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;

import com.frb.engine.FileStat;
import com.frb.engine.IFileService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileService extends IFileService.Stub {
    private static File f(String path) { return new File(path); }

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

    @Override public boolean mkdirs(String dirPath) { return f(dirPath).mkdirs(); }

    @Override public boolean createNewFile(String path) {
        try {
            File file = f(path);
            File p = file.getParentFile();
            if (p != null && !p.exists()) p.mkdirs();
            return file.createNewFile();
        } catch (IOException e) {
            return false;
        }
    }

    @Override public boolean delete(String path) { return f(path).delete(); }

    @Override public boolean deleteRecursive(String path) { return deleteRecursiveInternal(f(path)); }

    @Override public boolean rename(String from, String to) { return f(from).renameTo(f(to)); }

    @Override public boolean exists(String path) { return f(path).exists(); }

    @Override public FileStat stat(String path) { return toStat(f(path)); }

    @Override
    public String getCanonicalPath(String path) {
        try { return f(path).getCanonicalPath(); } catch (IOException e) { return ""; }
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

    @Override public boolean setLastModified(String path, long newLastModified) {
        return f(path).setLastModified(newLastModified);
    }

//    @Override public boolean setReadable(String path, boolean readable, boolean ownerOnly) {
//        return f(path).setReadable(readable, ownerOnly);
//    }
//
//    @Override public boolean setWritable(String path, boolean writable, boolean ownerOnly) {
//        return f(path).setWritable(writable, ownerOnly);
//    }
//
//    @Override public boolean setExecutable(String path, boolean executable, boolean ownerOnly) {
//        return f(path).setExecutable(executable, ownerOnly);
//    }

    @Override public boolean chmod(String path, int mode) {
        try {
            Os.chmod(path, mode);
            return true;
        } catch (ErrnoException e) {
            return false;
        }
    }

    @Override public boolean chown(String path, int uid, int gid) {
        try {
            Os.chown(path, uid, gid);
            return true;
        } catch (ErrnoException e) {
            return false;
        }
    }



    @Override
    public ParcelFileDescriptor open(String path, int flags, boolean ensureParents) throws RemoteException {
        File file = f(path);
        try {
            if (ensureParents) {
                File p = file.getParentFile();
                if (p != null && !p.exists()) p.mkdirs();
            }
            return ParcelFileDescriptor.open(file, flags);
        } catch (IOException e) {
            throw new RemoteException("open failed: " + e.getMessage());
        }
    }

    @Override
    public boolean copy(String from, String to, boolean overwrite) {
        try { return copyFile(f(from), f(to), overwrite); }
        catch (IOException e) { return false; }
    }

    @Override
    public boolean move(String from, String to, boolean overwrite) {
        File src = f(from);
        File dst = f(to);
        if (!src.exists()) return false;

        // coba rename langsung
        if (!dst.exists() || (overwrite && dst.delete())) {
            if (src.renameTo(dst)) return true;
        }

        // beda mount / gagal rename → copy + delete
        try {
            if (!copyFile(src, dst, overwrite)) return false;
        } catch (IOException e) {
            return false;
        }
        // hapus sumber setelah copy sukses
        return deleteRecursiveInternal(src);
    }
}
