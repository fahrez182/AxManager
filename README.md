# Axeron 管理器 (AxManager)（非最终版）

> 大家好！本项目仍处于早期开发阶段。我在开发过程中不断学习和尝试，因此某些功能可能尚未完善，并且未来可能会有所更改。

感谢您的关注！

**AxManager** 是一款 Android 应用，旨在提供对应用和系统的深度控制。

与 *KernelSU* 或其他基于 root 权限的“管理器”等工具不同，**AxManager** 专用于 **ADB/非 root 模式**，同时在设备拥有 **root 权限** 时，也允许执行命令。[了解更多](https://fahrez182.github.io/AxManager/)

## ✨ 功能

- 🖥️ **Shell 执行器**

直接从应用运行 shell 命令。

- 支持 **ADB/非 root 执行**。

- 如果设备拥有 root 权限，则可选择 **root 执行**。

- ⚡ **插件（无需root权限的模块）**

无需root权限即可管理第三方模块。[了解更多](https://fahrez182.github.io/AxManager/plugin/what-is-plugin.html)

- 🌐 **WebUI（无需root权限的版本）**

通过基于Web的交互式界面执行shell命令。[了解更多](https://fahrez182.github.io/AxManager/plugin/what-is-plugin.html#webui)

## 📱 与root管理器的主要区别

- 🚫 **不**依赖root权限。

- ✅ 优先考虑**ADB/非root权限**，使其可在更广泛的设备上使用。

- 🔑 Root支持是**可选的**，并非必需。

- 🌐 提供**WebShell UI**作为一项独特功能。

## 📖 路线图

- [x] 无线调试激活器。

- [x] 命令行/Root 激活器。

- [x] Shell 执行器基本支持（ADB/非 Root）。

- [x] 使用无线调试时自动激活（测试）

- [x] 支持多会话的 WebUI。

- [x] 第三方扩展的插件系统。

- [x] 开发者模式和高级调试工具。

- [ ] 基于配置文件的应用优化。

## 🔧 构建与安装

克隆仓库并使用 Android Studio 或 Gradle 进行构建：

```bash

git clone https://github.com/username/AxManager.git

cd AxManager

./gradlew assembleDebug

```

通过 ADB 安装到您的设备：

```bash

adb install app/build/outputs/apk/debug/app-debug.apk

```

## 🤝 贡献

欢迎贡献！

您可以提交**issues**、**pull request**或发起讨论，提出新的想法和改进建议。

## 🙏 致谢

- **[Magisk]()** “BusyBox 和插件（无需root权限的模块）的灵感来源”

- **[Shizuku](https://github.com/RikkaApps/Shizuku) / [API](https://github.com/RikkaApps/Shizuku-API)** “学习 Android 进程间通信 (IPC) 和基于 ADB 的权限处理的起点和参考”

- **[KernelSU](https://github.com/tiann/KernelSU) / [Next](https://github.com/KernelSU-Next/KernelSU-Next)** “UI 和 WebUI 功能的灵感来源”。

## ⚠️ 声明与法律免责声明

本项目包含以下项目的改编代码：

- Shizuku Manager (© Rikka Apps)

遵循 Apache License 2.0 许可协议

代码仓库：https://github.com/RikkaApps/Shizuku

- 上述提及的其他开源项目。

AxManager 不包含或分发任何 Shizuku Manager 的原始视觉素材，也不声称是其官方替代品。

所有改编代码均严格用于教育和实验目的，并已明确注明出处，且符合 Apache License 2.0 许可协议。

## 📜 许可协议

遵循 [Apache License 2.0](LICENSE) 许可协议。
