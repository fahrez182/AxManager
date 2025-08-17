package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IPackageManager extends IInterface {

    PackageInfo getPackageInfoVersioned(VersionedPackage versionedPackage,
                                        int flags, int userId) throws RemoteException;
    ApplicationInfo getApplicationInfo(String packageName, long flags, int userId) throws RemoteException;

    int checkPermission(String permName, String pkgName, int userId) throws RemoteException;

    void noMethod(String packageName);

    void deleteExistingPackageAsUser(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId) throws RemoteException;

    void grantRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException;

    abstract class Stub extends Binder implements IPackageManager {

        public static IPackageManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
