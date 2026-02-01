# Biarkan R8 tahu kita memang sengaja pakai hidden API
# Missing class android.app.IProcessObserver$Stub
# Missing class android.app.IUidObserver$Stub
# Jangan warning kalau class-class ini missing di runtime
-dontwarn android.app.**
-dontwarn android.content.**
-dontwarn android.os.**
-dontwarn android.view.**
-dontwarn android.hardware.**
-dontwarn android.permission.**
-dontwarn com.android.**
-dontwarn android.ddm.**

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepnames class * implements android.os.Parcelable

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-assumenosideeffects class java.util.Objects{
    ** requireNonNull(...);
}

# Missing class android.app.IProcessObserver$Stub
# Missing class android.app.IUidObserver$Stub
-keepclassmembers class rikka.hidden.compat.adapter.ProcessObserverAdapter {
    <methods>;
}

-keepclassmembers class rikka.hidden.compat.adapter.UidObserverAdapter {
    <methods>;
}

-keep class frb.axeron.server.AxeronService {
    public static void main(java.lang.String[]);
    public <init>(...);
}

# Entrance of Shizuku service
-keep class frb.axeron.server.ShizukuServiceIntercept

# Entrance of user service starter
-keep class frb.axeron.starter.ServiceStarter {
    public static void main(java.lang.String[]);
    public <init>(...);
}

-keep class frb.axeron.server.shell.Shell {
    public static void main(java.lang.String[], java.lang.String, android.os.IBinder, android.os.Handler);
}

-assumenosideeffects class frb.axeron.server.util.Logger {
    public *** d(...);
}

# Hindari penghapusan method native
-keepclassmembers class * {
    native <methods>;
}

# Hindari penghapusan constructor kosong yang mungkin diperlukan oleh reflection
-keepclassmembers class * {
    public <init>();
}

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}