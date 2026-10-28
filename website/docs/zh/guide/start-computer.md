# <Icon name="usb" /> 使用电脑启动（adb）

此方法适用于任何设备，无论是否 root。它需要电脑，并且每次重启后都需要重新执行这些步骤。

## 什么是 `adb`？ {#what-is-adb}

Android 调试桥（`adb`）是一个多功能命令行工具，让您能够与设备进行通信。`adb` 命令可用于各种设备操作，例如安装和调试应用，并提供对 Unix shell 的访问，您可以使用它在设备上运行各种命令。

更多信息请参阅 [Android Developer](https://developer.android.google.cn/tools/adb)。

## 安装 `adb` {#install-adb}

1. 下载 Google 提供的"**SDK Platform Tools**"并将其解压到任意文件夹
2. 打开文件夹，右键选择
   - Windows 10/11：在此处打开 PowerShell 窗口（按住 Shift 以显示此选项）
   - Mac 或 Linux：打开终端
3. 输入 `adb`，如果成功，您会看到一长列内容，而不是提示找不到 adb。

::: tip
请不要关闭此窗口。后文提到的"**终端**"指的是此窗口（如果您关闭了窗口，请回到[步骤 2](#install-adb)）。
如果您使用 PowerShell 或 Linux/Mac，所有 `adb` 都应替换为 `./adb`
:::

## 设置 `adb` {#setting-adb}

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

## 启动 AxManager {#start-axmanager}

在激活页面中，打开**通过电脑命令行激活**下的**查看命令**。它会显示如下形式的命令：

```
adb shell /data/app/~~…/frb.axeron.manager-…/lib/arm64/libaxeron.so
```

具体路径取决于您的安装，因此请始终从应用中复制（**复制**，或通过**分享**发送到电脑）。将其粘贴到终端中。如果没有问题，AxManager 会显示已成功启动。
