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

使用无线调试启动适用于 Android 11 及以上版本。此启动方法无需连接到电脑。由于系统限制，每次重启后需要重新执行启动步骤，除非[开机自启](#activate-on-boot)为您完成。

#### 启用无线调试 {#enable-wireless-debugging}

1. 搜索如何为您的设备型号启用"**开发者选项**"
2. 启用"**开发者选项**"和"**USB 调试**"
3. 进入"**无线调试**"
4. 启用"**无线调试**"

::: tip
在激活页面的**通过无线调试激活**卡片中，**操作说明** → **开发者**会直接为您打开无线调试设置。
:::

#### 启动和配对（只需一次） {#start-pairing}

AxManager 通过通知提供配对码输入框，因此请先允许通知（在允许之前，按钮会显示**授予通知权限**）。

1. 在 AxManager 中点击**开始配对**
2. [启用"**无线调试**"](#enable-wireless-debugging)
3. 在"**无线调试**"中点击"**使用配对码配对设备**"
4. 在 AxManager 的通知中输入配对码，直到显示"**配对成功**"
5. 返回 AxManager 并等待连接成功（在某些设备上您需要再次点击**开始配对**）

配对完成后，**开始配对**会直接连接。如果连接失败（例如设备从未配对），AxManager 会重新启动配对服务，并提示您按照通知操作。

::: warning
AxManager 运行期间不要关闭"**开发者选项**"或"**USB 调试**"，否则它会停止。网络变化时，无线调试也会自动关闭。
:::

### 使用 TCP 调试启动 {#start-with-tcp-debugging}

当 ADB 已经在某个 TCP 端口上监听时（例如在电脑上执行过 `adb tcpip 5555`，或之前在开启该模式的情况下激活过），**TCP Debugging** 卡片会显示该端口和**连接 TCP 端口**按钮。它无需 Wi-Fi 或电脑即可启动 AxManager。

卡片标题旁的开关（TCP 模式）会改变无线调试激活的行为：

- **开启**（默认）：通过无线调试连接后，AxManager 会把 ADB 切换到 **TCP 端口**（1024–65535；除非设备已设置其他端口，否则为 5555）上的 TCP 模式。之后的激活可以复用该端口，即使没有 Wi-Fi，直到下次重启。
- **关闭**：如果 ADB 处于 TCP 模式，下一次无线调试激活会先把它切换回 USB。

修改端口后会在下一次激活时生效。**停止 TCP 端口**会把 ADB 切换回 USB。

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

此方法适用于任何设备，无论是否 root。它需要电脑，并且每次重启后都需要重新执行这些步骤。

#### 什么是 `adb`？ {#what-is-adb}

Android 调试桥（`adb`）是一个多功能命令行工具，让您能够与设备进行通信。`adb` 命令可用于各种设备操作，例如安装和调试应用，并提供对 Unix shell 的访问，您可以使用它在设备上运行各种命令。

更多信息请参阅 [Android Developer](https://developer.android.google.cn/tools/adb)。

#### 安装 `adb` {#install-adb}

1. 下载 Google 提供的"**SDK Platform Tools**"并将其解压到任意文件夹
2. 打开文件夹，右键选择
   - Windows 10/11：在此处打开 PowerShell 窗口（按住 Shift 以显示此选项）
   - Mac 或 Linux：打开终端
3. 输入 `adb`，如果成功，您会看到一长列内容，而不是提示找不到 adb。

::: tip
请不要关闭此窗口。后文提到的"**终端**"指的是此窗口（如果您关闭了窗口，请回到[步骤 2](#install-adb)）。
如果您使用 PowerShell 或 Linux/Mac，所有 `adb` 都应替换为 `./adb`
:::

#### 设置 `adb` {#setting-adb}

要使用 `adb`，您首先需要在设备上启用 USB 调试，通常按照以下步骤：

1. 搜索如何为您的设备型号启用"**开发者选项**"
2. 启用"**开发者选项**"和"**USB 调试**"
3. 将设备连接到电脑，并在终端中输入 `adb devices`。
4. 此时，设备上会出现"**允许调试**"对话框，勾选"**始终允许**"并确认。
5. 在终端中再次输入 `adb devices`。如果没有问题，您将看到类似以下内容。

   ```
   List of devices attached
   XXX     device
   ```

#### 启动 AxManager {#start-axmanager}

在激活页面中，打开**通过电脑命令行激活**下的**查看命令**。它会显示如下形式的命令：

```
adb shell /data/app/~~…/frb.axeron.manager-…/lib/arm64/libaxeron.so
```

具体路径取决于您的安装，因此请始终从应用中复制（**复制**，或通过**分享**发送到电脑）。将其粘贴到终端中。如果没有问题，AxManager 会显示已成功启动。

## 开机自启 {#activate-on-boot}

**设置 → 开机自启**（默认开启）会在重启后使用上次的启动方式再次启动 AxManager：

- **Root** 或**以 Shell 身份启动**：通过 root 再次启动。
- **ADB**（无线调试、TCP 或 OTG），仅限 Android 11+，**实验性**：AxManager 会打开无线调试并连接。这需要 `WRITE_SECURE_SETTINGS` 权限（服务运行时会授予 AxManager），并且开机时需要 Wi-Fi。
- 通过电脑启动、从未启动过，或 Android 11 以下的 ADB：开机时不会启动。

安全模式下不会启动任何东西。AxManager 停止时，主页状态卡片会显示上次使用的方式（**Last: …**）以及是否开启了开机启动（**Boot**）。

## 停止或重启 {#stop-or-restart}

AxManager 运行时，主页状态卡片上的电源按钮会打开电源菜单：

- **火焰**按钮会重载插件，参见 [Re-ignite](../plugin/re-ignite.md)。
- 将开关**向上**滑动可关闭服务。
- 将开关**向下**滑动可重启服务。

更新应用后，主页状态卡片会显示旧服务正在被替换为新版本。
