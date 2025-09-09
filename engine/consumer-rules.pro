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

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-assumenosideeffects class java.util.Objects{
    ** requireNonNull(...);
}

-keepnames class com.frb.engine.utils.BinderContainer

# Missing class android.app.IProcessObserver$Stub
# Missing class android.app.IUidObserver$Stub
-keepclassmembers class rikka.hidden.compat.adapter.ProcessObserverAdapter {
    <methods>;
}

-keepclassmembers class rikka.hidden.compat.adapter.UidObserverAdapter {
    <methods>;
}

-keep class com.frb.engine.implementation.AxeronService {
    public static void main(java.lang.String[]);
    public <init>(...);
}

-assumenosideeffects class com.frb.engine.utils.Logger {
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