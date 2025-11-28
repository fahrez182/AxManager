// IAxeronService.aidl
package frb.axeron.server;

import frb.axeron.server.IFileService;
import frb.axeron.server.IRuntimeService;
import frb.axeron.server.IAxeronApplication;
import moe.shizuku.server.IShizukuService;
import rikka.parcelablelist.ParcelableListSlice;
import frb.axeron.data.ServerInfo;
import frb.axeron.data.PluginInfo;
import frb.axeron.data.Environment;

interface IAxeronService {
    IFileService getFileService() = 2;
    IRuntimeService getRuntimeService(in String[] command, in Environment env, in String dir) = 3;
    ServerInfo getServerInfo() = 4;
    void bindAxeronApplication(in IAxeronApplication app) = 5;
    ParcelableListSlice<PackageInfo> getPackages(int flags) = 6;
    ParcelableListSlice<PluginInfo> getPlugins() = 7;
    PluginInfo getPluginById(in String id) = 8;
    boolean isFirstInit(boolean markAsFirstInit) = 9;
    IShizukuService getShizukuService() = 10;
    void enableShizukuService(boolean enable) = 11;
    Environment getEnvironment(int envType) = 12;
    void setNewEnvironment(in Environment env) = 13;
    void destroy() = 16777114;
}