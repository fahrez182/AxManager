// IWriteCallback.aidl
package com.frb.engine;

interface IWriteCallback {
    void onComplete();
    void onError(int code, String message);
}
