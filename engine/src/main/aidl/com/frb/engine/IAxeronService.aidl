// IAxeronService.aidl
package com.frb.engine;

import com.frb.engine.IFileService;
import com.frb.engine.IRuntimeService;
import com.frb.engine.IAxeronApplication;

parcelable Environment;
//import android.os.ParcelFileDescriptor;

interface IAxeronService {
    IFileService getFileService() = 2;
    IRuntimeService getRuntimeService(in String[] command, in Environment env, in String dir) = 3;
    long getVersionCode() = 4;
    String getVersionName() = 5;
    int getUid() = 6;
    int getPid() = 7;
    String getSELinuxContext() = 8;
    void bindAxeronApplication(in IAxeronApplication app) = 9;
    void destroy() = 16777114;
}