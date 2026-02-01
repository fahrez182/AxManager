package frb.axeron.server;

import static frb.axeron.server.util.HandlerUtil.getMainHandler;

import android.content.pm.PackageInfo;
import android.util.ArrayMap;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import frb.axeron.starter.ServiceStarter;
import rikka.hidden.compat.PackageManagerApis;
import rikka.hidden.compat.UserManagerApis;

public class AxeronUserServiceManager extends UserServiceManager {

    private final Map<UserServiceRecord, ApkChangedListener> apkChangedListeners = new ArrayMap<>();
    private final Map<String, List<UserServiceRecord>> userServiceRecords = Collections.synchronizedMap(new ArrayMap<>());


    public AxeronUserServiceManager(String[] env) {
        super(env);
    }

    @Override
    public String[] getUserServiceCmd() {
        String busybox = Objects.requireNonNull(AxeronService.getManagerApplicationInfo()).nativeLibraryDir + "/libbusybox.so";
        return new String[]{busybox, "setsid", "sh"};
    }

    @Override
    public String getUserServiceStartCmd(
            UserServiceRecord record, String key, String token, String packageName,
            String classname, String processNameSuffix, int callingUid, boolean use32Bits, boolean debug) {


        String appProcess = "/system/bin/app_process";
        if (use32Bits && new File("/system/bin/app_process32").exists()) {
            appProcess = "/system/bin/app_process32";
        }

        LOGGER.i("ShizukuUserServiceManager.getUserServiceStartCmd: appProcess=%s", appProcess);
        return ServiceStarter.commandForUserService(
                appProcess,
                Objects.requireNonNull(AxeronService.getManagerApplicationInfo()).sourceDir,
                token, packageName, classname, processNameSuffix, callingUid, debug);
    }

    @Override
    public void onUserServiceRecordCreated(UserServiceRecord record, PackageInfo packageInfo) {
        LOGGER.i("onUserServiceRecordCreated1: %s", record);
        String packageName = packageInfo.packageName;
        ApkChangedListener listener = new ApkChangedListener() {
            @Override
            public void onApkChanged() {
                String newSourceDir = null;

                for (int userId : UserManagerApis.getUserIdsNoThrow()) {
                    PackageInfo pi = PackageManagerApis.getPackageInfoNoThrow(packageName, 0, userId);
                    if (pi != null && pi.applicationInfo != null && pi.applicationInfo.sourceDir != null) {
                        newSourceDir = pi.applicationInfo.sourceDir;
                        break;
                    }
                }

                if (newSourceDir == null) {
                    LOGGER.v("remove record %s because package %s has been removed", record.token, packageName);
                    record.removeSelf();
                } else {
                    LOGGER.v("update apk listener for record %s since package %s is upgrading", record.token, packageName);
                    ApkChangedObservers.stop(this);
                    ApkChangedObservers.start(newSourceDir, getMainHandler(), this);
                }
            }
        };

        LOGGER.i("onUserServiceRecordCreated2: %s", record);
        ApkChangedObservers.start(packageInfo.applicationInfo.sourceDir, getMainHandler(), listener);
        LOGGER.i("onUserServiceRecordCreated3: %s", record);
        apkChangedListeners.put(record, listener);
        LOGGER.i("onUserServiceRecordCreated4: %s", record);
    }

    @Override
    public void onUserServiceRecordRemoved(UserServiceRecord record) {
        ApkChangedListener listener = apkChangedListeners.get(record);
        if (listener != null) {
            ApkChangedObservers.stop(listener);
            apkChangedListeners.remove(record);
        }
    }
}
