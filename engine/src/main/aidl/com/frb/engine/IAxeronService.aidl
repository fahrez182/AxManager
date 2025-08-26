// IAxeronService.aidl
package com.frb.engine;

import com.frb.engine.IFileService;
import com.frb.engine.IRuntimeService;
//import android.os.ParcelFileDescriptor;

interface IAxeronService {
    void destroy() = 16777114;
    IFileService getFileService() = 1;
    IRuntimeService getRuntimeService(in String[] command, in String[] env, in String dir) = 2;
    long getVersionCode() = 3;
    String getVersionName() = 4;
    int getUid() = 5;
    int getPid() = 6;
    String getSELinuxContext() = 7;
}