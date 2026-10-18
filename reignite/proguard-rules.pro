# The igniter's own dex, libaxreignite.so (see igniterDex in build.gradle.kts): app_process
# starts Igniter.main, and R8 keeps only what that reaches.
-keep class frb.axeron.reignite.Igniter {
    public static void main(java.lang.String[]);
}

-allowaccessmodification
-repackageclasses

# Hidden framework classes (DdmHandleAppName) are there at runtime.
-dontwarn **
