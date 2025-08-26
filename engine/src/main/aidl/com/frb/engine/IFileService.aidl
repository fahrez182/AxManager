// IFileService.aidl
package com.frb.engine;

parcelable FileStat;
// Declare any non-default types here with import statements

interface IFileService {
    // Filter
    const int FILTER_ALL         = 0;
    const int FILTER_FILES       = 1;
    const int FILTER_DIRECTORIES = 2;

    // Sort
    const int SORT_NAME      = 0;
    const int SORT_LAST_MOD  = 1;
    const int SORT_LENGTH    = 2;

    // ---- Operasi dasar
    boolean mkdirs(String dirPath);
    boolean createNewFile(String path);
    boolean delete(String path);               // non-recursive
    boolean deleteRecursive(String path);      // hati-hati: rekursif
    boolean rename(String from, String to);
    boolean exists(String path);

    // ---- Metadata
    FileStat stat(String path);                // metadata lengkap
    String getCanonicalPath(String path);      // bisa kosong jika gagal

    // ---- Listing + filter/sort/paging (server-side)
    List<FileStat> list(String dirPath, int filter, int sortBy, boolean ascending, int offset, int limit);

    // ---- Time & permission
    boolean setLastModified(String path, long newLastModified);
    boolean chmod(String path, int mode);
    boolean chown(String path, int uid, int gid);

    // ---- Open file dgn flags PFD; ensureParents = auto-mkdir parent jika true
    ParcelFileDescriptor open(String path, int flags, boolean ensureParents);

    // ---- Copy/Move (overwrite optional)
    boolean copy(String from, String to, boolean overwrite);
    boolean move(String from, String to, boolean overwrite);
}