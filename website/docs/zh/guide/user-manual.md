# <Icon name="start" /> 用户手册

[[toc]]

::: info 关于界面文字
本文中的按钮和选项名称与应用界面保持一致。应用的简体中文翻译尚未覆盖所有文字，未翻译的部分在应用中显示为英文，本文也照样使用英文名称。
:::

## 启动 Axeron Manager {#start-axeron-manager}

AxManager 通过一个后台服务完成所有工作。在做其他事情之前，请先用下面任一方法启动该服务。在**主页**点击状态卡片即可打开**激活**页面。

激活页面分为两部分：

- **This device**（本设备）：TCP 调试、无线调试（Android 11+）和 Root。
- **Another device**（其他设备）：OTG（用本机在另一台手机上启动 AxManager），以及在电脑上使用的命令。

| 方法 | 需要 | 重启后自动恢复 |
|---|---|---|
| [Root](#start-with-root) | 已 root 的设备 | 是，需开启[开机自启](#activate-on-boot) |
| [无线调试](#start-with-wireless-debugging) | Android 11+、Wi-Fi | 实验性，需开启[开机自启](#activate-on-boot) |
| [TCP 调试](#start-with-tcp-debugging) | ADB 已在 TCP 端口上监听 | 否 |
| [OTG](#start-from-another-phone-otg) | 另一台手机和 OTG 线 | 否 |
| [电脑](#start-by-connecting-to-a-computer) | 装有 `adb` 的电脑 | 否 |
| [浏览器](#start-from-the-browser) | 电脑上的 Chrome 或 Edge，或另一台手机上的 Chrome；一根 USB 线 | 否 |

### 激活日志 {#the-activation-log}

激活页面上的每种方法都会打开一个面板，实时显示每一步的日志：查找 ADB 端口、连接、运行启动器及其输出，以及等待服务回连。只有服务真正连接上才算成功；如果启动器已运行但服务一直没有连接，面板会提示并指向日志。

- 成功时，面板会在 3 秒后自动关闭。
- 失败时，面板保持打开。可使用 **Copy log** 复制日志并分享出错信息。
- 关闭面板不会取消仍在进行中的尝试。

### 使用 Root 启动 {#start-with-root}

在已 root 的设备上，找到**直接激活（仅限拥有 Root 权限的设备）**卡片并点击**启动**。AxManager 会请求 root 权限，然后以 `root` 身份启动服务。

**以 Shell 身份启动**则以 `shell` 用户身份启动服务（通过 `su 2000`），即使设备已 root。用它可以让 AxManager 只拥有 ADB 级别的权限。Root 模式和 shell 模式使用[各自独立的文件夹](../plugin/environment.md#working-folder)，因此它们的插件和设置互不混用。

AxManager 可以在重启后自动启动，参见[开机自启](#activate-on-boot)。如果没有自动启动，请检查您的系统或第三方工具是否限制了 AxManager 在后台启动（参考 [Don't kill my app!](https://dontkillmyapp.com/)）。

### 使用无线调试启动 {#start-with-wireless-debugging}

Android 11 及以上，需要 Wi-Fi，不需要电脑：在无线调试设置中配对一次，然后从 AxManager 启动。步骤见 **[使用无线调试启动](./start-wireless.md)**。

### 使用 TCP 调试启动 {#start-with-tcp-debugging}

当 ADB 已经在某个 TCP 端口上监听时（例如在电脑上执行过 `adb tcpip 5555`，或之前在开启该模式的情况下激活过），**TCP Debugging** 卡片会显示该端口和**连接 TCP 端口**按钮。它无需 Wi-Fi 或电脑即可启动 AxManager。

卡片标题旁的开关（TCP 模式）会改变无线调试激活的行为：

- **开启**（默认）：通过无线调试连接后，AxManager 会把 ADB 切换到 **TCP 端口**（1024–65535；除非设备已设置其他端口，否则为 5555）上的 TCP 模式。之后可以用 **Connect to TCP** 在该端口上启动 AxManager，即使没有 Wi-Fi，直到下次重启。只有当 ADB 尚未处于该端口时，AxManager 才会切换它。
- **关闭**：如果 ADB 处于 TCP 模式，下一次无线调试激活会先把它切换回 USB。

修改端口会立即生效：当 ADB 正在其他 TCP 端口上监听时，保存新端口或开启 TCP 模式，会把 ADB 切换到新端口，并在该端口上重启服务。如果切换失败，可点击端口输入框中的重启图标重试。**停止 TCP 端口**会把 ADB 切换回 USB。

### 从另一台手机启动（OTG） {#start-from-another-phone-otg}

AxManager 可以充当另一台手机的"电脑"：用 OTG 线连接两台手机，在另一台手机上启动 AxManager。在两台手机的 **Activate via OTG** 卡片中都点击 **Start OTG**，并分别选择角色：

- **Computer**（电脑）：本机激活另一台手机。需要支持 USB 主机（OTG）。
- **Target**（目标）：本机被激活。不支持 USB 主机的手机只能作为目标。

在目标手机上：

1. 安装 AxManager
2. 打开 **USB 调试**
3. 打开 **Activate via OTG** 并选择 **Target**，它会显示进度
4. 用 OTG 线连接两台手机
5. 询问 USB 调试时点击**允许**

随后，作为电脑的手机会找到设备、建立连接并在其上启动 AxManager。如果目标手机上的 AxManager 开启了 TCP 模式，之后其 ADB 会切换到目标手机自己设置的 TCP 端口。

::: tip USB-C 对 USB-C 数据线
另一台手机可能会反过来成为 USB 主机。在任一手机上打开 USB 通知，将"**USB 控制方**"设置为由作为电脑的手机控制。
:::

### 通过连接电脑启动 {#start-by-connecting-to-a-computer}

任何设备，无论是否 root，都需要一台装有 `adb` 的电脑：每次重启后运行 AxManager 显示的命令。安装和设置 `adb` 见 **[使用电脑启动（adb）](./start-computer.md)**。

### 通过浏览器启动 {#start-from-the-browser}

[通过浏览器激活](/activate)与电脑方法的作用相同，只是通过网页经 USB 完成，无需安装 `adb`。可在电脑上的 Chrome 或 Edge 中使用，也可在用 OTG 线连接的另一台手机上的 Chrome 中使用。

1. 在设备上开启 **USB 调试**（[方法](./start-computer.md#setting-adb)），并用 USB 线连接。
2. 打开 [axmanager.app/activate](https://axmanager.app/activate)，点击 **Connect** 并选择该设备。
3. 在设备上允许 USB 调试，然后点击 **Activate**。

页面会显示与应用激活相同的日志，服务运行后状态变为绿色。如果开启了 **TCP 模式**（**TCP Debugging** 卡片上的开关），页面会先把 ADB 切换到该卡片设置的 TCP 端口（与无线调试激活相同），这样在下次重启前，都可以通过 [TCP 调试](#start-with-tcp-debugging)再次启动 AxManager。

请先关闭其他正在使用 ADB 的程序（`adb` 服务、Android Studio、scrcpy）：同一时间只能有一个程序占用设备。Windows USB 驱动等[要求](/activate#requirements)请参见该页面。

## 开机自启 {#activate-on-boot}

**设置 → 开机自启**（默认开启）会在重启后使用上次的启动方式再次启动 AxManager：

- **Root** 或**以 Shell 身份启动**：通过 root 再次启动。
- **ADB**（无线调试、TCP 或 OTG），仅限 Android 11+，**实验性**：AxManager 会打开无线调试并连接。这需要 `WRITE_SECURE_SETTINGS` 权限（服务运行时会授予 AxManager），并且开机时需要 Wi-Fi。
- 通过电脑或浏览器启动、从未启动过，或 Android 11 以下的 ADB：开机时不会启动。

安全模式下不会启动任何东西。AxManager 停止时，主页状态卡片会显示上次使用的方式（**Last: …**）以及是否开启了开机启动（**Boot**）。

重启后通过 ADB 启动时，会显示一条 **Starting AxManager** 通知，带步骤计数并随每一步更新：等待 Wi-Fi、打开无线调试、查找其端口、连接 adbd、启动服务、等待服务连接。等待 Wi-Fi 时，通知上有 **Open Wi-Fi** 按钮，因为应用无法自己打开 Wi-Fi。如果某一步失败或超时，通知会说明并提供 **Retry**。通知会保留到设备解锁后消失。如果无法启动这条通知，AxManager 会不带通知直接激活。

## 停止或重启 {#stop-or-restart}

AxManager 运行时，主页状态卡片上的电源按钮会打开电源菜单：

- **火焰**按钮会重载插件，参见 [Re-ignite](../plugin/re-ignite.md)。
- 将开关**向上**滑动可关闭服务。
- 将开关**向下**滑动可重启服务。

重载、关闭和重启都会在一个实时显示日志的面板中进行。**Copy log** 会连同每行的时间一起复制日志，操作仍在进行时也可以复制，便于报告卡住的情况。**Cancel** 可以中止操作，但服务正在被替换时除外。

开启**随服务停止插件**后，关闭和重启会先停止所有插件并在日志中显示；重启之后会在同一面板中重新重载插件。

更新应用后，主页状态卡片会显示旧服务正在被替换为新版本。
