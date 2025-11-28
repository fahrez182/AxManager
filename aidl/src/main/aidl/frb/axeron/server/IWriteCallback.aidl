// IWriteCallback.aidl
package frb.axeron.server;

interface IWriteCallback {
    void onComplete();
    void onError(int code, String message);
}
