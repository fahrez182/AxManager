// IAxeronService.aidl
package com.frb.engine;

import com.frb.engine.IFileService;
import com.frb.engine.IRuntimeService;
import com.frb.engine.IAxeronApplication;
import rikka.parcelablelist.ParcelableListSlice;
import com.frb.engine.implementation.AxeronInfo;

parcelable Environment;

interface IAxeronService {
    IFileService getFileService() = 2;
    IRuntimeService getRuntimeService(in String[] command, in Environment env, in String dir) = 3;
    AxeronInfo getInfo() = 4;
    void bindAxeronApplication(in IAxeronApplication app) = 5;
    ParcelableListSlice<PackageInfo> getPackages(int flags) = 6;
    List<String> getPlugins() = 7;
    String getPluginById(in String id) = 8;
    boolean isFirstInit(boolean markAsFirstInit) = 9;
    void destroy() = 16777114;
}