# Axeron 管理器 (AxManager)

> 一个运行在 **ADB（shell）权限**上的 Android 特权环境，如果设备已 root，也可以使用 **root** 运行。AxManager 会在后台保持一个服务进程运行，并在此基础上提供：带 WebUI 的 Magisk 风格插件、按应用的权限控制、兼容 Shizuku 的 API、Shell 以及系统设置编辑器。

[Switch to English 切换到英文](README.md)

## 💡 这是什么

AxManager 以 `shell` 用户（通过无线调试、电脑或另一台手机经 OTG）或以 `root` 身份启动自己的服务进程。其余一切都与该服务通信：应用本身、插件、插件的 WebUI，以及您允许的其他应用。大部分功能都不需要 root；有 root 时，同样的功能将以 root 的权限范围运行。

## ✨ 功能

### 🚀 激活
- **Root**：直接启动，并在开机时自动启动。
- **无线调试**（Android 11+）：在设备本机上配对并启动，无需电脑。
- **TCP 调试**和**电脑（adb）**：经典的启动方式。
- **OTG**：把这台手机当作"电脑"，通过 USB 线在另一台手机上启动 AxManager。
- **以 Shell 启动**：即使设备已 root，也以 shell 模式运行。
- 实时输出的激活日志会显示每一步发生了什么。

### 🧩 插件（无需 root 的模块）
采用熟悉的 Magisk/KernelSU 结构的模块，由 AxManager 的服务运行，而不是 root 守护进程。[插件文档](https://fahrez182.github.io/AxManager/zh/plugin/what-is-plugin.html)
- `module.prop`、`customize.sh`、`post-fs-data.sh`、`service.sh`、`system.prop`、`action.sh`、`uninstall.sh`，以及 `system/bin`（链接到位于 `PATH` 中的 `$AXERONXBIN`）。
- 完整的 **BusyBox**（支持独立模式），以及 `AXERON`、`AXERONVER`、`AXERONDIR`、`AXERONBIN`、`TMPDIR` 环境变量。
- **WebUI**：兼容 KernelSU 的 `ksu` JavaScript API，加上 AxManager 自己的 `Axeron` API，并可为插件的 WebUI 创建桌面快捷方式。
- **干净的进程生命周期**：插件启动的每个进程（无论来自其任何脚本、操作按钮还是 WebUI）都带有 `AXERON_PLUGIN=<id>` 标记，因此禁用或移除插件会停止它的全部进程，包括守护进程。
- 可选：**服务停止时停止所有插件**，或让插件独立继续运行。
- **开发者模式**会增加一张信息卡片，显示插件脚本看到的路径和变量值。

### 🛡️ 权限
- 按应用授予或拒绝对 AxManager 的访问。
- **兼容 Shizuku API**：基于 [Shizuku-API](https://github.com/RikkaApps/Shizuku-API) 构建的应用无需运行 Shizuku 即可连接 AxManager。部分旧应用仍要求*安装*了 Shizuku 应用；遇到这种情况 AxManager 会提示您。
- **应用详情**：强行停止、清除缓存或数据、卸载（也支持系统应用，按用户）；授予或撤销运行时权限和开发权限；设置特殊访问的 app-op 模式（显示在其他应用上层、使用情况访问、所有文件访问权限……）；电池优化、后台活动、启用或停用应用。

### 💻 Shell
- **QuickShell**：以服务的权限运行命令（shell 或 root，取决于启动方式），支持代码片段、在输出中查找以及无限输出。
- `axrun` / `axerish`：在终端应用中使用 AxManager 的权限。

### ⚙️ 系统
- **设置编辑器**：浏览、搜索和编辑 `system`、`secure` 和 `global` 设置，常用项可放入**快捷设置**。
- 快速设置磁贴、可选的唤醒锁以保持服务存活，以及一个让 `PATH` 优先使用系统二进制文件而非 BusyBox 的开关。

## 📂 文件位置

Root 模式和 shell 模式各自使用独立的文件夹，因此以 root 创建的文件不会妨碍 shell 模式。

| 模式 | 文件夹 |
|---|---|
| Root | `/data/adb/axeron` |
| Shell | `/data/user_de/0/com.android.shell/axeron` |

其中包含：`plugins/`、`bin/`（BusyBox 和工具，每次启动时重建）、`xbin/`（插件的 `system/bin`）、`cache/`（`$TMPDIR`）以及 `logs/`（`post-fs-data.sh` 和 `uninstall.sh` 的输出）。Root 模式以前位于 `/data/user_de/0/android/axeron`；首次以 root 启动时会自动迁移到 `/data/adb/axeron`。

## 📋 要求
- Android 8.0（API 26）或更高版本。
- 无线调试激活需要 Android 11 或更高版本。

## 📦 安装
从 [Releases](https://github.com/fahrez182/AxManager/releases) 下载最新的 APK，然后按照[用户手册](https://fahrez182.github.io/AxManager/zh/guide/user-manual.html)启动它。

## 🔧 构建
API 位于一个 git 子模块中，因此克隆时请一并获取：

```bash
git clone --recurse-submodules https://github.com/fahrez182/AxManager.git
cd AxManager
./gradlew :manager:assembleDebug
```

已经在没有子模块的情况下克隆了？运行 `git submodule update --init`。如果要改用本地的 API 副本构建，请在 `local.properties` 中设置 `api.useLocal=true` 和 `api.dir=<路径>`。

安装调试版本：

```bash
adb install manager/build/outputs/apk/debug/manager-debug.apk
```

| 模块 | 说明 |
|---|---|
| `manager` | 应用本身 |
| `server` | 特权服务及其原生启动器 |
| `reignite` | 插件点火器（`ax_reignite.dex`），负责启动和停止插件 |
| `adb` | 无线调试配对和 ADB 客户端 |
| `api`（子模块） | AIDL、客户端 API、共享的服务端代码、Shizuku 兼容层、`rish`、`axerish` |

## 📖 文档
- [什么是 Axeron 管理器？](https://fahrez182.github.io/AxManager/zh/guide/what-is-axeron-manager.html)
- [用户手册](https://fahrez182.github.io/AxManager/zh/guide/user-manual.html)
- [插件](https://fahrez182.github.io/AxManager/zh/plugin/what-is-plugin.html)
- [Re-ignite](https://fahrez182.github.io/AxManager/zh/plugin/re-ignite.html)
- [常见问题](https://fahrez182.github.io/AxManager/zh/guide/faq.html)

## 🤝 贡献
欢迎贡献！您可以提交 **issue**、发送 **pull request** 或发起讨论。

## 🙏 致谢
- **[Magisk](https://github.com/topjohnwu/Magisk)**：BusyBox、`resetprop`，以及插件所参照的模块系统。
- **[Shizuku](https://github.com/RikkaApps/Shizuku) / [Shizuku-API](https://github.com/RikkaApps/Shizuku-API)**：学习 Android 进程间通信（IPC）和基于 ADB 的权限处理的起点和参考。
- **[KernelSU](https://github.com/tiann/KernelSU) / [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next)**：UI 和 WebUI 的灵感来源。

## ⚠️ 声明与法律免责声明
本项目包含改编自以下项目的代码：
- Shizuku Manager（© Rikka Apps），遵循 Apache License 2.0 许可协议。代码仓库：https://github.com/RikkaApps/Shizuku
- 上述提及的其他开源项目。

AxManager 不包含或分发任何 Shizuku Manager 的原始视觉素材，也不声称是其官方替代品。改编代码均已明确注明出处，且符合 Apache License 2.0 许可协议。

## 📜 许可协议
遵循 [Apache License 2.0](LICENSE) 许可协议。
