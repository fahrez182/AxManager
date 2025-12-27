package rikka.shizuku.server;

import static android.Manifest.permission.WRITE_SECURE_SETTINGS;
import static rikka.shizuku.ShizukuApiConstants.ATTACH_APPLICATION_API_VERSION;
import static rikka.shizuku.ShizukuApiConstants.ATTACH_APPLICATION_PACKAGE_NAME;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_PERMISSION_GRANTED;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_PATCH_VERSION;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_SECONTEXT;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_UID;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_VERSION;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE;
import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;
import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME;
import static rikka.shizuku.manager.ServerConstants.MANAGER_APPLICATION_ID;
import static rikka.shizuku.manager.ServerConstants.PERMISSION;
import static rikka.shizuku.manager.ServerConstants.SHIZUKU_MANAGER_APPLICATION_ID;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import frb.axeron.server.IAxeronService;
import kotlin.collections.ArraysKt;
import moe.shizuku.server.IShizukuApplication;
import rikka.hidden.compat.ActivityManagerApis;
import rikka.hidden.compat.PackageManagerApis;
import rikka.hidden.compat.PermissionManagerApis;
import rikka.hidden.compat.UserManagerApis;
import rikka.parcelablelist.ParcelableListSlice;
import rikka.shizuku.ShizukuApiConstants;
import rikka.shizuku.manager.ServerConstants;
import rikka.shizuku.manager.ShizukuClientManager;
import rikka.shizuku.manager.ShizukuConfig;
import rikka.shizuku.manager.ShizukuConfigManager;
import rikka.shizuku.manager.ShizukuUserServiceManager;
import rikka.shizuku.server.util.BuildUtils;
import rikka.shizuku.server.util.HandlerUtil;
import rikka.shizuku.server.util.OsUtils;
import rikka.shizuku.server.util.UserHandleCompat;

public class ShizukuService extends Service<ShizukuUserServiceManager, ShizukuClientManager, ShizukuConfigManager> {

    private final ShizukuClientManager clientManager;
    private final ShizukuConfigManager configManager;
    private final int managerAppId;
    private final int shizukuManagerAppId;

    public ShizukuService(
            Handler mainHandler,
            ShizukuUserServiceManager shizukuUserServiceManager,
            IAxeronService axeronService
    ) {
        super(shizukuUserServiceManager);
        HandlerUtil.setMainHandler(mainHandler);

        waitSystemService("package");
        waitSystemService(Context.ACTIVITY_SERVICE);
        waitSystemService(Context.USER_SERVICE);
        waitSystemService(Context.APP_OPS_SERVICE);

        ApplicationInfo ai = getManagerApplicationInfo();
        if (ai == null) {
            System.exit(ServerConstants.MANAGER_APP_NOT_FOUND);
        }

        assert ai != null;
        managerAppId = ai.uid;

        ApplicationInfo sai = getShizukuManagerApplicationInfo();
        shizukuManagerAppId = Objects.requireNonNullElse(sai, ai).uid;

        configManager = getConfigManager();
        clientManager = getClientManager();
        this.axeronService = axeronService;
    }

    public static ApplicationInfo getManagerApplicationInfo() {
        return PackageManagerApis.getApplicationInfoNoThrow(MANAGER_APPLICATION_ID, 0, 0);
    }

    private static ApplicationInfo getShizukuManagerApplicationInfo() {
        return PackageManagerApis.getApplicationInfoNoThrow(SHIZUKU_MANAGER_APPLICATION_ID, 0, 0);
    }

    private static void waitSystemService(String name) {
        while (ServiceManager.getService(name) == null) {
            try {
                LOGGER.i("service " + name + " is not started, wait 1s.");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.w(e.getMessage(), e);
            }
        }
    }

    private synchronized ParcelableListSlice<PackageInfo> getApplications(int userId) {
        List<PackageInfo> list = new ArrayList<>();
        List<Integer> users = new ArrayList<>();
        if (userId == -1) {
            users.addAll(UserManagerApis.getUserIdsNoThrow());
        } else {
            users.add(userId);
        }

        for (int user : users) {
            for (PackageInfo pi : PackageManagerApis.getInstalledPackagesNoThrow(PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS, user)) {
                if (Objects.equals(MANAGER_APPLICATION_ID, pi.packageName)) continue;
                if (pi.applicationInfo == null) continue;

                int uid = pi.applicationInfo.uid;
                int flags = 0;
                ShizukuConfig.PackageEntry entry = configManager.find(uid);
                if (entry != null) {
                    if (entry.packages != null && !entry.packages.contains(pi.packageName))
                        continue;
                    flags = entry.flags & ConfigManager.MASK_PERMISSION;
                }

                if (flags != 0) {
                    list.add(pi);
                } else if (pi.applicationInfo.metaData != null
                        && pi.applicationInfo.metaData.getBoolean("moe.shizuku.client.V3_SUPPORT", false)
                        && pi.requestedPermissions != null
                        && ArraysKt.contains(pi.requestedPermissions, PERMISSION)) {
                    list.add(pi);
                }
            }

        }
        return new ParcelableListSlice<>(list);
    }

    @Override
    public ShizukuClientManager onCreateClientManager() {
        return new ShizukuClientManager(getConfigManager());
    }

    @Override
    public ShizukuConfigManager onCreateConfigManager() {
        return new ShizukuConfigManager();
    }

    public synchronized boolean checkCaller(int callingUid) {
        return UserHandleCompat.getAppId(callingUid) == managerAppId || UserHandleCompat.getAppId(callingUid) == shizukuManagerAppId;
    }

    @Override
    public boolean checkCallerManagerPermission(String func, int callingUid, int callingPid) {
        return checkCaller(callingUid);
    }

    @Override
    public boolean checkCallerPermission(String func, int callingUid, int callingPid, @Nullable ClientRecord clientRecord) {
        if (checkCaller(callingUid)) {
            return true;
        }
        return clientRecord == null;
    }

    @Override
    public void exit() throws RemoteException {
        enforceManagerPermission("exit");
        axeronService.enableShizukuService(false);
        LOGGER.i("exit");
    }

    @Override
    public void attachUserService(IBinder binder, Bundle options) {
        enforceManagerPermission("func");

        super.attachUserService(binder, options);
    }

    @Override
    public void attachApplication(IShizukuApplication application, Bundle args) {
        if (application == null || args == null) {
            return;
        }

        String requestPackageName = args.getString(ATTACH_APPLICATION_PACKAGE_NAME);
        if (requestPackageName == null) {
            return;
        }
        int apiVersion = args.getInt(ATTACH_APPLICATION_API_VERSION, -1);

        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        boolean isManager;
        ClientRecord clientRecord = null;

        List<String> packages = PackageManagerApis.getPackagesForUidNoThrow(callingUid);
        if (!packages.contains(requestPackageName)) {
            LOGGER.w("Request package " + requestPackageName + "does not belong to uid " + callingUid);
            throw new SecurityException("Request package " + requestPackageName + "does not belong to uid " + callingUid);
        }

        isManager = MANAGER_APPLICATION_ID.equals(requestPackageName);

        if (clientManager.findClient(callingUid, callingPid) == null) {
            synchronized (this) {
                clientRecord = clientManager.addClient(callingUid, callingPid, application, requestPackageName, apiVersion);
            }
            if (clientRecord == null) {
                LOGGER.w("Add client failed");
                return;
            }
        }

        LOGGER.d("attachApplication: %s %d %d", requestPackageName, callingUid, callingPid);

        int replyServerVersion = ShizukuApiConstants.SERVER_VERSION;
        if (apiVersion == -1) {
            // ShizukuBinderWrapper has adapted API v13 in dev.rikka.shizuku:api 12.2.0, however
            // attachApplication in 12.2.0 is still old, so that server treat the client as pre 13.
            // This finally cause transactRemote fails.
            // So we can pass 12 here to pretend we are v12 server.
            replyServerVersion = 12;
        }

        Bundle reply = new Bundle();
        reply.putInt(BIND_APPLICATION_SERVER_UID, OsUtils.getUid());
        reply.putInt(BIND_APPLICATION_SERVER_VERSION, replyServerVersion);
        reply.putString(BIND_APPLICATION_SERVER_SECONTEXT, OsUtils.getSELinuxContext());
        reply.putInt(BIND_APPLICATION_SERVER_PATCH_VERSION, ShizukuApiConstants.SERVER_PATCH_VERSION);
        if (!isManager) {
            reply.putBoolean(BIND_APPLICATION_PERMISSION_GRANTED, Objects.requireNonNull(clientRecord).allowed);
            reply.putBoolean(BIND_APPLICATION_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE, false);
        } else {
            try {
                PermissionManagerApis.grantRuntimePermission(MANAGER_APPLICATION_ID,
                        WRITE_SECURE_SETTINGS, UserHandleCompat.getUserId(callingUid));
            } catch (RemoteException e) {
                LOGGER.w(e, "grant WRITE_SECURE_SETTINGS");
            }
        }
        try {
            application.bindApplication(reply);
        } catch (Throwable e) {
            LOGGER.w(e, "attachApplication");
        }
    }

    @Override
    public void showPermissionConfirmation(int requestCode, @NonNull ClientRecord clientRecord, int callingUid, int callingPid, int userId) {
        ApplicationInfo ai = PackageManagerApis.getApplicationInfoNoThrow(clientRecord.packageName, 0, userId);
        if (ai == null) {
            return;
        }

        PackageInfo pi = PackageManagerApis.getPackageInfoNoThrow(MANAGER_APPLICATION_ID, 0, userId);
        UserInfo userInfo = UserManagerApis.getUserInfo(userId);
        boolean isWorkProfileUser = BuildUtils.atLeast30() ?
                "android.os.usertype.profile.MANAGED".equals(userInfo.userType) :
                (userInfo.flags & UserInfo.FLAG_MANAGED_PROFILE) != 0;
        if (pi == null && !isWorkProfileUser) {
            LOGGER.w("Manager not found in non work profile user %d. Revoke permission", userId);
            clientRecord.dispatchRequestPermissionResult(requestCode, false);
            return;
        }

        LOGGER.i("Requesting Permission");
        Intent intent = new Intent(ServerConstants.REQUEST_PERMISSION_ACTION)
                .setPackage(MANAGER_APPLICATION_ID)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                .putExtra("uid", callingUid)
                .putExtra("pid", callingPid)
                .putExtra("requestCode", requestCode)
                .putExtra("applicationInfo", ai);
        ActivityManagerApis.startActivityNoThrow(intent, null, isWorkProfileUser ? 0 : userId);
    }

    @Override
    public void dispatchPermissionConfirmationResult(int requestUid, int requestPid, int requestCode, Bundle data) throws RemoteException {
        if (!checkCaller(Binder.getCallingUid())) {
            LOGGER.w("dispatchPermissionConfirmationResult called not from the manager package");
            return;
        }

        if (data == null) {
            return;
        }

        boolean allowed = data.getBoolean(REQUEST_PERMISSION_REPLY_ALLOWED);
        boolean onetime = data.getBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME);

        LOGGER.i("dispatchPermissionConfirmationResult: uid=%d, pid=%d, requestCode=%d, allowed=%s, onetime=%s",
                requestUid, requestPid, requestCode, Boolean.toString(allowed), Boolean.toString(onetime));

        List<ClientRecord> records = clientManager.findClients(requestUid);
        List<String> packages = new ArrayList<>();
        if (records.isEmpty()) {
            LOGGER.w("dispatchPermissionConfirmationResult: no client for uid %d was found", requestUid);
        } else {
            for (ClientRecord record : records) {
                packages.add(record.packageName);
                record.allowed = allowed;
                if (record.pid == requestPid) {
                    record.dispatchRequestPermissionResult(requestCode, allowed);
                    if (!allowed) {
                        onPermissionRevoked(record.packageName);
                    }
                }
            }
        }


        if (!onetime) {
            configManager.update(requestUid, packages, ConfigManager.MASK_PERMISSION, allowed ? ConfigManager.FLAG_ALLOWED : ConfigManager.FLAG_DENIED);
        }
    }

    private synchronized int getFlagsForUidInternal(int uid, int mask) {
        ShizukuConfig.PackageEntry entry = configManager.find(uid);
        if (entry != null) {
            return entry.flags & mask;
        }
        return 0;
    }

    @Override
    public int getFlagsForUid(int uid, int mask) {
        if (!checkCaller(Binder.getCallingUid())) {
            LOGGER.w("updateFlagsForUid is allowed to be called only from the manager");
            return 0;
        }
        return getFlagsForUidInternal(uid, mask);
    }

    @Override
    public void updateFlagsForUid(int uid, int mask, int value) throws RemoteException {
        if (!checkCaller(Binder.getCallingUid())) {
            LOGGER.w("updateFlagsForUid is allowed to be called only from the manager");
            return;
        }

        if ((mask & ConfigManager.MASK_PERMISSION) != 0) {
            boolean allowed = (value & ConfigManager.FLAG_ALLOWED) != 0;
            boolean denied = (value & ConfigManager.FLAG_DENIED) != 0;

            List<ClientRecord> records = clientManager.findClients(uid);
            for (ClientRecord record : records) {
                if (allowed) {
                    record.allowed = true;
                } else {
                    record.allowed = false;
                    ActivityManagerApis.forceStopPackageNoThrow(record.packageName, UserHandleCompat.getUserId(record.uid));
                    onPermissionRevoked(record.packageName);
                }
            }
        }

        configManager.update(uid, null, mask, value);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        //LOGGER.d("transact: code=%d, calling uid=%d", code, Binder.getCallingUid());
        if (code == ServerConstants.BINDER_TRANSACTION_getApplications) {
            data.enforceInterface(ShizukuApiConstants.BINDER_DESCRIPTOR);
            int userId = data.readInt();
            ParcelableListSlice<PackageInfo> result = getApplications(userId);
            reply.writeNoException();
            result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    private synchronized void onPermissionRevoked(String packageName) {
        getUserServiceManager().removeUserServicesForPackage(packageName);
    }

    @Override
    public void dispatchPackageChanged(Intent intent) throws RemoteException {

    }

    @Override
    public boolean isHidden(int uid) throws RemoteException {
        return false;
    }
}
