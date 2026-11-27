package com.axdubai.manager.core;
public class AxOmniBridge {
static { try { System.loadLibrary("axelden"); } catch (UnsatisfiedLinkError e) {} }
public static native long scanNative(String path, int threads);
public static String hwInfo() {
try { return android.os.Build.MODEL + " SDK:" + android.os.Build.VERSION.SDK_INT; }
catch (Exception e) { return "unknown"; }
}
}
