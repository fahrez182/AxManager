package com.frb.engine.implementation;

import static android.Manifest.permission.WRITE_SECURE_SETTINGS;
import static com.frb.engine.utils.ServerConstants.MANAGER_APPLICATION_ID;

import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.frb.engine.Environment;
import com.frb.engine.IAxeronApplication;
import com.frb.engine.IAxeronService;
import com.frb.engine.IFileService;
import com.frb.engine.IRuntimeService;
import com.frb.engine.utils.Logger;
import com.frb.engine.utils.UserHandleCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rikka.hidden.compat.PackageManagerApis;
import rikka.hidden.compat.PermissionManagerApis;
import rikka.parcelablelist.ParcelableListSlice;

public class Service extends IAxeronService.Stub {
    protected static final String TAG = "AxeronService";
    protected static final Logger LOGGER = new Logger(TAG);
//    private final Context context;
    private boolean firstInitFlag = true;

//    public Service(Context context) {
//        this.context = context;
//    }

    @Override
    public IFileService getFileService() {
        return new FileService();
    }

    @Override
    public IRuntimeService getRuntimeService(String[] command, @Nullable Environment env, @Nullable String dir) throws RemoteException {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command, env != null ? env.getEnv() : null, dir != null ? new File(dir) : null);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
        IBinder token = this;

        return new RuntimeService(process, token);
    }

    @Override
    public AxeronInfo getInfo() throws RemoteException {
        return null;
    }

    @Override
    public void bindAxeronApplication(IAxeronApplication app) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        try {
            PermissionManagerApis.grantRuntimePermission(MANAGER_APPLICATION_ID,
                    WRITE_SECURE_SETTINGS, UserHandleCompat.getUserId(callingUid));
        } catch (RemoteException e) {
            LOGGER.w(e, "grant WRITE_SECURE_SETTINGS");
        }
        app.bindApplication(new Bundle());
    }

    @Override
    public ParcelableListSlice<PackageInfo> getPackages(int flags) throws RemoteException {
        List<PackageInfo> list = PackageManagerApis.getInstalledPackagesNoThrow(flags, 0);
        LOGGER.i(TAG, "getPackages: " + list.size());
        return new ParcelableListSlice<>(list);
    }

    @Override
    public List<String> getPlugins() throws RemoteException {
        String pluginsPath = "/data/local/tmp/AxManager/plugins";
        return readAllPluginAsString(pluginsPath);
    }

    @Override
    public String getPluginById(String id) throws RemoteException {
        File dir = new File("/data/local/tmp/AxManager/plugins", id);
        Map<String, String> pluginInfo = null;
        File propFile = new File(dir, "module.prop");
        if (propFile.exists() && propFile.isFile()) {
            pluginInfo = new HashMap<>(readFileProp(propFile));
        }

        if (pluginInfo == null) return "";

        pluginInfo.put("dir_id", pluginInfo.get("id"));
        pluginInfo.put("enabled", new File(dir, "disable").exists() ? "false" : "true");
        pluginInfo.put("update", new File(dir, "update").exists() ? "true" : "false");
        pluginInfo.put("remove", new File(dir, "remove").exists() ? "true" : "false");
        pluginInfo.put("action", new File(dir, "action.sh").exists() ? "true" : "false");
        pluginInfo.put("web", new File(dir, "webroot/index.html").exists() ? "true" : "false");
        pluginInfo.put("size", String.valueOf(getFolderSize(dir)));
        return new JSONObject(pluginInfo).toString();
    }

    @Override
    public boolean isFirstInit() {
        boolean firstFlag = firstInitFlag;
        if (firstInitFlag) {
            firstInitFlag = false;
        }
        return firstFlag;
    }

    private List<String> readAllPluginAsString(String pluginsDirPath) {
        List<String> result = new ArrayList<>();
        File pluginsDir = new File(pluginsDirPath);

        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            Log.e(TAG, "Plugins dir not found: " + pluginsDirPath);
            return result;
        }

        File[] subDirs = pluginsDir.listFiles(File::isDirectory);
        if (subDirs == null) return result;

        for (File dir : subDirs) {
            Map<String, String> pluginInfo = null;
            File propFile = new File(dir, "module.prop");
            if (propFile.exists() && propFile.isFile()) {
                pluginInfo = new HashMap<>(readFileProp(propFile));
            }

            if (pluginInfo == null) continue;
            String pluginId = pluginInfo.get("id");
            if (pluginId == null) continue;

            File updateDir = new File("/data/local/tmp/AxManager/plugins_update", pluginId);
            File[] folderUpdateChild = (updateDir.exists() && updateDir.isDirectory()) ? updateDir.listFiles() : null;
            boolean isUpdate = folderUpdateChild != null && folderUpdateChild.length > 0;

            pluginInfo.put("dir_id", pluginId);
            pluginInfo.put("enabled", new File(dir, "disable").exists() ? "false" : "true");
            pluginInfo.put("update", String.valueOf(isUpdate));
            pluginInfo.put("update_install", new File(updateDir, "update_install").exists() ? "true" : "false");
            pluginInfo.put("update_remove", new File(updateDir, "update_remove").exists() ? "true" : "false");
            pluginInfo.put("update_enable", new File(updateDir, "update_enable").exists() ? "true" : "false");
            pluginInfo.put("update_disable", new File(updateDir, "update_disable").exists() ? "true" : "false");
            pluginInfo.put("remove", new File(dir, "remove").exists() ? "true" : "false");
            pluginInfo.put("action", new File(dir, "action.sh").exists() ? "true" : "false");
            pluginInfo.put("web", new File(dir, "webroot/index.html").exists() ? "true" : "false");
            pluginInfo.put("size", String.valueOf(getFolderSize(dir)));

            result.add(new JSONObject(pluginInfo).toString());
        }

        return result;
    }

    private long getFolderSize(File folder) {
        long length = 0;

        if (folder != null && folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        length += file.length(); // ukuran file
                    } else {
                        length += getFolderSize(file); // rekursif ke subfolder
                    }
                }
            }
        }

        return length;
    }

    private Map<String, String> readFileProp(File file) {
        Map<String, String> map = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    map.put(key, value);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + file.getAbsolutePath(), e);
        }

        return map;
    }

    @Override
    public void destroy() throws RemoteException {
        System.exit(0);
    }

    public final void transactRemote(Parcel data, Parcel reply, int flags) throws RemoteException {
        IBinder targetBinder = data.readStrongBinder();
        int targetCode = data.readInt();
        int targetFlags = data.readInt();

        LOGGER.d("transact: uid=%d, descriptor=%s, code=%d", Binder.getCallingUid(), targetBinder.getInterfaceDescriptor(), targetCode);
        Parcel newData = Parcel.obtain();
        try {
            newData.appendFrom(data, data.dataPosition(), data.dataAvail());
        } catch (Throwable tr) {
            LOGGER.w(tr, "appendFrom");
            return;
        }
        try {
            long id = Binder.clearCallingIdentity();
            targetBinder.transact(targetCode, newData, reply, targetFlags);
            Binder.restoreCallingIdentity(id);
        } finally {
            newData.recycle();
        }
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == 1) {
            data.enforceInterface(IAxeronService.DESCRIPTOR);
            transactRemote(data, reply, flags);
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }
}
