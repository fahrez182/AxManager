# Axeron Manager (AxManager) (非最终版)


> 嗨！本项目仍处于早期开发阶段。我在构建它的同时也在学习和尝试，所以功能可能不完整或以后会发生变化。
感谢关注！

**AxManager** 是一款 Android 应用程序，旨在对应用和系统提供更深层次的控制。
与 *KernelSU* 或其他基于 Root 的“管理器”不同，**AxManager** 致力于 **ADB/免 Root 模式** —— 同时在设备拥有 Root 权限时，也支持使用 **Root 权限**执行。[了解更多](https://fahrez182.github.io/AxManager/)

## ✨ 特性
- 🖥️ **Shell 执行器**
  直接从应用内运行 Shell 命令。
  - 支持 **ADB / 免 Root 执行**。
  - 如果设备拥有 Root 权限，可选 **Root 执行**。

- ⚡ **插件 (免 Root 模块)**
  管理免 Root 访问的第三方模块。[了解更多](https://fahrez182.github.io/AxManager/plugin/what-is-plugin.html)

- 🌐 **WebUI (免 Root 版)**
  通过基于 Web 的交互式界面执行 Shell 命令。[了解更多](https://fahrez182.github.io/AxManager/plugin/what-is-plugin.html#webui)

## 📱 与 Root 管理器的主要区别
- 🚫 **不**依赖 Root 权限。
- ✅ 专注于 **ADB/免 Root 优先**，使其适用于更广泛的设备。
- 🔑 Root 支持是**可选的**，而非必须。
- 🌐 提供 **WebShell UI** 作为一项独特功能。

## 📖 路线图
- [x] 无线调试激活器。
- [x] 命令行 / Root 激活器。
- [x] Shell 执行器基础支持 (ADB/免 Root)。
- [x] 使用无线调试时自动激活 (测试中)
- [x] ~~支持多会话的 WebUI。~~
- [x] 用于第三方扩展的 [插件](https://fahrez182.github.io/AxManager/plugin/what-is-plugin.html) 系统。
- [x] 开发者模式与高级调试工具。
- [ ] 基于配置文件的应用优化。

## 🔧 构建与安装
克隆仓库并使用 Android Studio 或 Gradle 进行构建：

```bash
git clone [https://github.com/username/AxManager.git](https://github.com/username/AxManager.git)
cd AxManager
./gradlew assembleDebug
