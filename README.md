# Axeron Manager (AxManager)

**AxManager** is an Android application designed to provide deeper control over apps and the system.  
Unlike tools such as *KernelSU* or other root-based “Managers,” **AxManager** is dedicated to **ADB/Non-Root mode** — while still allowing execution with **Root access** if available.

## ✨ Features
- ⚡ **App Optimization**  
  Manage apps to improve performance, efficiency, and stability.

- 🖥️ **Shell Executor**  
  Run shell commands directly from the app.  
  - Supports **ADB / Non-Root execution**.  
  - Optional **Root execution** if the device has root access.  

- 🌐 **WebShell (UI Shell)**  
  Execute shell commands with a web-based interactive interface.

## 📱 Key Difference from Root Managers
- 🚫 Does **not** depend on Root access.  
- ✅ Focused on **ADB/Non-Root first**, making it usable on a wider range of devices.  
- 🔑 Root support is **optional**, not a requirement.  
- 🌐 Provides **WebShell UI** as a unique feature.  

## 📖 Roadmap
- [x] Wireless Debugging Activator.
- [x] Command-line / Root Activator.
- [x] Shell Executor basic support (ADB/Non-Root).
- [ ] App optimization based on profiles.  
- [ ] WebShell with multi-session support.  
- [ ] Plugin system for third-party extensions.  
- [ ] Developer Mode & Advanced Debugging tools.  

## 🔧 Build & Install
Clone the repository and build using Android Studio or Gradle:

```bash
git clone https://github.com/username/AxManager.git
cd AxManager
./gradlew assembleDebug
```

Install to your device via ADB:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🤝 Contribution
Contributions are welcome!  
Feel free to open **issues**, submit **pull requests**, or start a discussion for new ideas and improvements.


## 🙏 Credits
- **[Shizuku](https://github.com/RikkaApps/Shizuku) / [API](https://github.com/RikkaApps/Shizuku-API)** "The starting point and transition to learning IPC, also the reference for ADB handling."
- **[KernelSU](https://github.com/tiann/KernelSU) / [Next](https://github.com/KernelSU-Next/KernelSU-Next)** "Inspiration for the UI and WebUI features."

## 📜 License
Licensed under the [Apache License 2.0](LICENSE).  
