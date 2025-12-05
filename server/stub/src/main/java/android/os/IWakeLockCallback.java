package android.os;

public interface IWakeLockCallback {
    void onStateChanged(boolean enabled);
}
