// IOutputStreamCallback.aidl
package frb.axeron.server;

interface IOutputStreamCallback {

    // Error codes
    const int IO_EXCEPTION = -1;
    const int ERRNO_EXCEPTION = -2;
    const int FILE_NOT_FOUND = -3;

    void onComplete();
    void onError(int errCode, String message);
}
