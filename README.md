# Axeron Manager (Proof of Concept)

> **Axeron Manager (AxManager)** is a Proof of Concept (POC) for a self-created environment on Android that leverages ADB permissions to provide system-level control. This project explores the idea of creating a persistent, independent ADB-based execution layer within the system.

[Switch to Chinese translation 切换到中文翻译](README_cn.md)

## 💡 The Concept
This project is a personal exploration into creating a dedicated **ADB Environment** on Android. Instead of just being a simple command runner, AxManager aims to establish a background infrastructure that can host plugins, manage system optimizations, and provide a unified interface for privileged operations—all without requiring full root access (though it can utilize root if available).

## ✨ Features
- 🏗️ **Internal ADB Environment**  
  A self-contained environment designed to maintain and utilize ADB-level privileges.
- 🖥️ **Shell Executor**  
  Run shell commands with persistent sessions.  
  - Supports **ADB / Non-Root execution**.  
  - Optional **Root execution** for enhanced capabilities.  

- ⚡ **Plugin (Unrooted Module)**  
  A system to manage third-party modules within the unrooted environment. [Learn more](https://fahrez182.github.io/AxManager/plugin/what-is-plugin.html)  

- 🌐 **WebUI Interface**  
  Manage and interact with the system environment through a web-based interface.

## 📱 Why this POC?
- **Independence**: Aims to minimize reliance on external PCs for ADB tasks once set up.
- **Environment-centric**: Focuses on creating a resident privileged layer rather than just one-off command execution.
- **Accessibility**: Bringing "Root-like" capabilities to non-rooted devices through native system mechanisms.

## 📖 Roadmap
- [x] Wireless Debugging Activator.
- [x] Command-line / Root Activator.
- [x] Shell Executor basic support (ADB/Non-Root).
- [x] Auto active when use Wireless Debugging (Test)
- [x] [Plugin](https://fahrez182.github.io/AxManager/plugin/what-is-plugin.html) system for third-party extensions.  
- [x] Developer Mode & Advanced Debugging tools.  
- [ ] App optimization based on profiles.

## 🔧 Build & Install
Clone the repository and build using Android Studio or Gradle:

```bash
git clone https://github.com/fahrez182/AxManager.git
cd AxManager
./gradlew :manager:assembleDebug
```

Install the manager app to your device via ADB:

```bash
adb install manager/build/outputs/apk/debug/manager-debug.apk
```

## 🤝 Contribution
Contributions are welcome!  
Feel free to open **issues**, submit **pull requests**, or start a discussion for new ideas and improvements.


## 🙏 Credits
- **[Magisk]()** "**BusyBox** and Plugin (Unrooted module) ideas"
- **[Shizuku](https://github.com/RikkaApps/Shizuku) / [API](https://github.com/RikkaApps/Shizuku-API)** "Starting point and reference for learning Android IPC and ADB-based permission handling"
- **[KernelSU](https://github.com/tiann/KernelSU) / [Next](https://github.com/KernelSU-Next/KernelSU-Next)** "Inspiration for the UI and WebUI features."

## ⚠️ Notices & Legal Disclaimer
This project includes adapted portions of code from:
- Shizuku Manager (© Rikka Apps)
  Licensed under the Apache License, Version 2.0
  Repository: https://github.com/RikkaApps/Shizuku
- Other open-source projects as credited above.

AxManager does not include or distribute any original Shizuku Manager visual assets or claim to be an official replacement.
All adapted code is used strictly for educational and experimental purposes, with clear attribution and compliance with the Apache License 2.0.

## 📜 License
Licensed under the [Apache License 2.0](LICENSE).
