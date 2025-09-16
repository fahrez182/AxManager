# Axeron Manager (AxManager) (Not-Final)


> Hi! This project is still in early development.  I’m learning and experimenting while building it, so things may not be complete or could change later.  
Thanks for checking it out!

**AxManager** is an Android application designed to provide deeper control over apps and the system.  
Unlike tools such as *KernelSU* or other root-based “Managers,” **AxManager** is dedicated to **ADB/Non-Root mode** — while still allowing execution with **Root access** if available. [Learn More](https://fahrez182.github.io/AxManager/)

## ✨ Features
- 🖥️ **Shell Executor**  
  Run shell commands directly from the app.  
  - Supports **ADB / Non-Root execution**.  
  - Optional **Root execution** if the device has root access.  

- ⚡ **Plugin (Unrooted Module)**  
  Manage third-party modules with unrooted access. [Learn more](https://fahrez182.github.io/AxManager/plugin/what-is-plugin.html)  

- 🌐 **WebUI (Unrooted Version)**  
  Execute shell commands with a web-based interactive interface. [Learn More](https://fahrez182.github.io/AxManager/plugin/what-is-plugin.html#webui)

## 📱 Key Difference from Root Managers
- 🚫 Does **not** depend on Root access.  
- ✅ Focused on **ADB/Non-Root first**, making it usable on a wider range of devices.  
- 🔑 Root support is **optional**, not a requirement.  
- 🌐 Provides **WebShell UI** as a unique feature.  

## 📖 Roadmap
- [x] Wireless Debugging Activator.
- [x] Command-line / Root Activator.
- [x] Shell Executor basic support (ADB/Non-Root).
- [x] Auto active when use Wireless Debugging (Test)
- [x] ~~WebUI with multi-session support.~~
- [x] [Plugin](https://fahrez182.github.io/AxManager/plugin/what-is-plugin.html) system for third-party extensions.  
- [x] Developer Mode & Advanced Debugging tools.  
- [ ] App optimization based on profiles.

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
- **[Magisk]()** "**BusyBox** and Plugin (Unrooted module) ideas"
- **[Shizuku](https://github.com/RikkaApps/Shizuku) / [API](https://github.com/RikkaApps/Shizuku-API)** "The starting point and transition to learning IPC, also the reference for ADB handling."
- **[KernelSU](https://github.com/tiann/KernelSU) / [Next](https://github.com/KernelSU-Next/KernelSU-Next)** "Inspiration for the UI and WebUI features."

## 📜 License
Licensed under the [Apache License 2.0](LICENSE).
