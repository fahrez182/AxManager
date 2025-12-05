package android.os;

import androidx.annotation.RequiresApi;

public interface IPowerManager extends IInterface {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName,
                                int uidtoblame, int displayId, IWakeLockCallback callback);

    @RequiresApi(Build.VERSION_CODES.S)
    void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName,
                                int uidtoblame, int displayId);

    void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName,
                                int uidtoblame);

    void releaseWakeLock(IBinder lock, int flags);

    abstract class Stub extends Binder implements IPowerManager {

        public static IPowerManager asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
    }
}
