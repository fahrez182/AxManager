# Android ARM64 build

This folder contains the Android-target source files. Build them with the Android NDK from the AxManager project, not with the host x86_64 libraries.

Required ABI: `arm64-v8a`; min API should match the app's configured minSdk.

The project's existing `CMakeLists.txt` references additional AxManager native sources (EmuTurbo.cpp, RamTurbo.cpp, CpuTurbo.cpp, IoTurbo.cpp, NetTurbo.cpp, GfxTurbo.cpp) which are not part of this source-only bundle. Merge these two AxElden translation units into the complete project CMake file, retaining the existing targets, then build with Gradle/NDK:

```bash
./gradlew :manager:externalNativeBuildDebug
./gradlew :manager:assembleDebug
```

No Android ARM64 `.so`, `.o`, or `.s` is included here because an Android NDK/sysroot is not installed in the build environment. The host x86_64 binaries are not installable on Android ARM64.
