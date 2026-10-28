# <Icon name="key" /> 特权与权限

**特权**界面列出可以允许使用 AxManager 的应用，并打开每个应用的详情：权限、特殊访问权限、app-op 和操作。

[[toc]]

## 应用列表 {#list}

可以允许使用 AxManager 的应用列表。每个应用都会显示其 [Axeron permission](#axeron-permission)：**Allowed**、**Ask (default)** 或 **Denied**。可按名称或包名搜索；筛选按钮用于选择显示哪些应用：

- **Show**：**Installed**（用户应用，默认）、**System**（系统应用）或 **Shizuku**（基于 Shizuku-API 构建的应用，参见 [Shizuku 兼容](./shizuku-compatibility.md)）
- **Access**：**All**、**Allowed**、**Ask** 或 **Denied**

可按名称升序或降序排序。已停用的应用以灰度显示，并标记为 **Disabled**。

- **点击**应用打开其详情。
- **长按**应用打开快捷菜单：设置其 Axeron permission，下方一排按钮为 **Open app**、**Clear cache**、**Force stop** 和 **Disable** / **Enable**。强行停止和停用会先请求确认。

大多数操作需要服务正在运行。有些操作只能在 root 下进行，shell 模式不够时 AxManager 会提示。

应用名称旁边的播放按钮用于打开该应用。对于没有可打开界面的应用（服务、库）以及已停用的应用，该按钮为灰色。

## Axeron permission {#axeron-permission}

应用请求使用 AxManager 时会发生什么。点击该行即可修改：

| 设置 | 含义 |
|---|---|
| **Allowed** | 无需询问即可使用 AxManager。 |
| **Ask**（默认） | 每次都询问您。对话框中的*仅本次允许*在应用关闭前有效。 |
| **Denied** | 立即拒绝，不显示对话框。 |

将应用从 **Allowed** 改为 **Ask** 或 **Denied** 会停止该应用，它之前拥有的访问会在下一次调用时失效。

- **与应用的签名绑定。** 权限属于应用的签名证书。如果应用以其他密钥签名重新安装（例如用调试版替换正式版），或之后有其他应用获得了相同的 uid，都会被重新询问。
- **开启 [AX-Stealth Mode](./app-tour.md#environment) 时**，设为 **Ask** 的应用不会看到对话框：它无法得知 AxManager 已安装。请在这里允许它，它会立即连接。

## 操作 {#actions}

- **Force stop**：停止该应用及其所有进程。
- **Clear cache**：shell 模式下只清除外部缓存（`Android/data/<包名>/cache`）；内部缓存需要 root。
- **Clear data**：删除该应用的全部数据。此操作无法撤销。
- **Uninstall**：用户应用会被卸载。系统应用只会**为当前用户**卸载，之后可以恢复。可以选择保留应用的数据和缓存。

破坏性操作会先请求确认；如果在限定时间内没有回应，请求会被取消。

## 常规 {#general}

- **Unrestricted battery**：让该应用不受电池优化（Doze）限制。
- **Background activity**：关闭后，Android 会阻止该应用在后台运行（`RUN_ANY_IN_BACKGROUND`）。
- **App enabled**：关闭后，该应用会对当前用户停用：从启动器中隐藏，在重新启用前无法运行。数据会保留。

## 特殊访问权限 {#special-access}

对于应用申请的每项特殊访问权限（在其他应用上层显示、修改系统设置、使用情况访问权限、所有文件访问权限、安装未知应用、闹钟和提醒、媒体管理、全屏通知），可以选择一种模式：

| 模式 | 含义 |
|---|---|
| **Allow** | 始终允许 |
| **Foreground only** | 仅在应用使用期间允许 |
| **Ignore** | 静默拒绝：应用不会收到错误，只是没有效果 |
| **Deny** | 拒绝并返回错误，相当于在系统设置中关闭 |
| **Default** | 跟随权限授予情况，普通应用并不持有该权限 |

在 **MIUI / HyperOS** 上，每个应用还会在这里列出 ROM 自带的三项权限：**Autostart**（自启动：应用未运行时，其他应用可以启动它）、**Home screen shortcuts**（桌面快捷方式）和 **Open new windows while running in the background**（后台弹出界面）。

**Other app-ops（其他 app-op）**：特殊访问权限下方有一张折叠的卡片，列出 Android 为该应用记录的、上面各部分未涵盖的所有内容，包括 ROM 添加的权限。它们使用相同的模式。AxManager 不认识的项会以原始名称显示（例如 `MIUIOP(10050)`），并注明其含义未知。

::: warning ROM 自带权限的名称
ROM 会随意为自己的权限编号，并且不提供 AxManager 可以读取的名称。上面 MIUI 权限的名称来自 AxManager 内置的列表，且只在 MIUI / HyperOS 上使用：同一个编号在其他品牌的 ROM 上可能表示别的含义，因此在那里会以原始名称显示。
:::

## Package 还是整个 uid {#package-or-uid}

Android 会把每个模式保存两份：一份针对应用的包，一份针对它的整个 uid（共享该 uid 的所有包）。系统会先检查 uid 模式：只要不是 *Default* 就以它为准，*Default* 则把决定权交还给包模式。对于与运行时权限相关的权限（例如位置），Android 往往会自行设置 uid 模式。

选择模式时，**Apply to** 决定写入的位置：

- **Package**（默认）：仅此包，与系统设置的做法相同。如果 uid 模式不是 *Default*，它会被重置，否则您的选择不会生效。
- **Whole uid**：uid 模式，作用于该 uid 的所有包。包模式保持不变。

面板会同时显示两种模式，并在 uid 模式覆盖包模式时加以标示。

## 权限 {#permissions}

应用申请的运行时权限和开发权限可以通过开关授予或撤销。安装时授予或因签名授予的权限会单独列出，无法更改。
