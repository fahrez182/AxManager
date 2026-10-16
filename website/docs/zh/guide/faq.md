# 常见问题

[[toc]]

许多制造商对 Android 系统进行了修改，导致 AxManager 无法正常工作。

## 通过无线调试启动：一直显示“正在搜索配对服务”

请允许 AxManager 在后台运行。

搜索配对服务需要访问本地网络，许多制造商在应用不可见时会立即禁用网络访问。您可以在网上搜索如何在您的设备上允许应用“**在后台运行**”。

## 通过无线调试启动：点击“输入配对码”后立即失败

::: info MIUI/HyperOS（小米、POCO）
在系统设置的“通知” - “通知中心”中将通知样式切换为“Android”。
:::

## 通过无线调试启动/通过连接电脑启动：adb 权限受限

::: info MIUI/HyperOS（小米、POCO）
在“开发者选项"中启用“USB 调试（安全设置）”。请注意，这是一个与“USB 调试”分开的选项。
:::

::: info ColorOS（OPPO、一加）
在“开发者选项”中禁用“权限监控”。
:::

::: info Flyme（魅族）
在“开发者选项”中禁用“Flyme 支付保护”。
:::

## 通过无线调试启动/通过连接电脑启动：AxManager 随机停止

::: info 所有设备

- 确保 AxManager 可以在后台运行。

- 不要禁用“USB 调试”和“开发者选项”。

- 在“开发者选项”中将 USB 使用模式更改为“仅充电”。

  - 在 Android 8 上，选项是“选择 USB 配置” - “仅充电”。
  - 在 Android 9+ 上，选项是“默认 USB 配置” - “不传输数据”。

- （Android 11+）启用“禁用 adb 授权超时”选项

:::

::: info EMUI/HarmonyOS 4.3及更低（华为）
在“开发者选项”中启用“允许在‘仅充电’模式下进行 ADB 调试选项”。
:::

::: info MIUI/HyperOS（小米、POCO）
不要使用 MIUI“安全”应用中的扫描功能，因为它会禁用“开发者选项”。
:::

::: info 索尼
不要点击连接 USB 后显示的对话框，因为它会更改 USB 使用模式。
:::

## 应用提示找不到 Shizuku {#shizuku-not-found}

1. 在 AxManager 运行时打开**设置 → AX-权限接管**。
2. 一些较旧的 Shizuku 客户端（例如旧版 ZArchiver）只有在**安装了** Shizuku 应用时才会连接。安装它即可，不需要启动。

参见 [Shizuku 兼容](./shizuku-compatibility.md)。

## Shizuku 应用提示需要更新 {#shizuku-update-note}

这是预期行为。由 AxManager 为 Shizuku 应用提供服务时，报告的服务版本为 `13.<AxManager 版本号>`，没有任何真实的 Shizuku 版本与之匹配，因此 Shizuku 应用会显示更新提示。无需更新任何东西。

## 服务停止后插件仍在运行 {#plugins-keep-running}

这是默认行为：插件启动后会独立运行。如需在服务停止或退出时停止所有插件的进程，请打开**设置 → 随服务停止插件**。参见[插件进程](../plugin/environment.md#plugin-processes)。

## 插件日志在哪里？ {#plugin-logs}

`post-fs-data.sh` 和 `uninstall.sh` 的输出写入 `$AXERONDIR/logs/<id>.post-fs-data.log` 和 `$AXERONDIR/logs/<id>.uninstall.log`：

- Root 模式：`/data/adb/axeron/logs`
- Shell 模式：`/data/user_de/0/com.android.shell/axeron/logs`

`service.sh` 的输出不会被保留；如有需要，请自行重定向到文件。安装插件和运行其操作时，输出会显示在屏幕上，并提供保存日志的按钮。