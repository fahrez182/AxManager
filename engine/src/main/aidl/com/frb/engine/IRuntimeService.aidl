// IRuntimeService.aidl
package com.frb.engine;

// Declare any non-default types here with import statements

import android.os.ParcelFileDescriptor;

interface IRuntimeService {
    ParcelFileDescriptor getOutputStream();

    ParcelFileDescriptor getInputStream();

    ParcelFileDescriptor getErrorStream();

    int waitFor();

    int exitValue();

    void destroy();

    boolean alive();

    boolean waitForTimeout(long timeout, String unit);
}