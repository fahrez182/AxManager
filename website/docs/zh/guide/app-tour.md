# <Icon name="tour" /> 应用导览

逐一介绍 AxManager 的每个页面及其选项的作用。如何启动 AxManager，请参阅[用户手册](./user-manual.md)。

::: info 关于界面文字
本文中的按钮和选项名称与应用界面保持一致。应用的简体中文翻译尚未覆盖所有文字，未翻译的部分在应用中显示为英文，本文也照样使用英文名称。
:::

[[toc]]

## 主页 {#home}

底部导航栏有四个页面：**主页**、**特权**、**插件**和**设置**。

### 状态卡片 {#status-card}

顶部卡片显示服务状态。点击它可打开[激活](./user-manual.md#start-axeron-manager)页面。

| 状态 | 含义 |
|---|---|
| **运行中** | 服务正在运行。徽章显示其模式（root 或 shell）。 |
| **未激活**（*Stopped*） | 服务没有运行。 |
| **需要额外的步骤**（*Action needed*） | 服务在运行，但系统阻止了 AxManager 需要的某些操作。点击**点击查看修复方法**。 |
| **更新中…** | 应用已更新，旧服务正在被替换为新版本。 |

运行时，卡片还会显示：

- **Wakelock**：点击该标签可开启或关闭。它在熄屏时保持 CPU 唤醒，避免服务被挂起。
- 运行时长、PID、UID 和服务版本。
- 电源按钮：重载插件、重启或关闭服务（参见[停止或重启](./user-manual.md#stop-or-restart)）。
- 您已允许的应用数量（**特权**）和已安装的**插件**数量。

停止时，卡片会显示上次的启动方式（**Last: ADB / Root / Root (shell)**）以及是否开启了[开机自启](./user-manual.md#activate-on-boot)（**Boot**）。

服务运行时，悬浮按钮可打开[快速终端](#quickshell)。如果服务已关闭，但 AxManager 仍持有设置相关的权限，会出现 **Quick Settings** 卡片，让您照常使用[快捷设置](#quick-settings)。

### 设备信息 {#device-information}

设备型号、Android 版本、安全补丁、内核、ABI 以及服务的 SELinux 上下文。点击某个值即可复制。

## 特权 {#privilege}

可以允许使用 AxManager 的应用列表。每个应用都会显示其 [Axeron permission](#axeron-permission)：**Allowed**、**Ask (default)** 或 **Denied**。可按名称或包名搜索；筛选按钮用于选择显示哪些应用：

- **Show**：**Installed**（用户应用，默认）、**System**（系统应用）或 **Shizuku**（基于 Shizuku-API 构建的应用，参见 [Shizuku 兼容](./shizuku-compatibility.md)）
- **Access**：**All**、**Allowed**、**Ask** 或 **Denied**

可按名称升序或降序排序。已停用的应用以灰度显示，并标记为 **Disabled**。

- **点击**应用打开其详情。
- **长按**应用打开快捷菜单：设置其 Axeron permission，下方一排按钮为 **Open app**、**Clear cache**、**Force stop** 和 **Disable** / **Enable**。强行停止和停用会先请求确认。

### 应用详情 {#app-details}

大多数操作需要服务正在运行。有些操作只能在 root 下进行，shell 模式不够时 AxManager 会提示。

应用名称旁边的播放按钮用于打开该应用。对于没有可打开界面的应用（服务、库）以及已停用的应用，该按钮为灰色。

#### Axeron permission {#axeron-permission}

应用请求使用 AxManager 时会发生什么。点击该行即可修改：

| 设置 | 含义 |
|---|---|
| **Allowed** | 无需询问即可使用 AxManager。 |
| **Ask**（默认） | 每次都询问您。对话框中的*仅本次允许*在应用关闭前有效。 |
| **Denied** | 立即拒绝，不显示对话框。 |

将应用从 **Allowed** 改为 **Ask** 或 **Denied** 会停止该应用，使其无法继续使用已不再拥有的权限。

**Actions（操作）**

- **Force stop**：停止该应用及其所有进程。
- **Clear cache**：shell 模式下只清除外部缓存（`Android/data/<包名>/cache`）；内部缓存需要 root。
- **Clear data**：删除该应用的全部数据。此操作无法撤销。
- **Uninstall**：用户应用会被卸载。系统应用只会**为当前用户**卸载，之后可以恢复。可以选择保留应用的数据和缓存。

破坏性操作会先请求确认；如果在限定时间内没有回应，请求会被取消。

**General（常规）**

- **Unrestricted battery**：让该应用不受电池优化（Doze）限制。
- **Background activity**：关闭后，Android 会阻止该应用在后台运行（`RUN_ANY_IN_BACKGROUND`）。
- **App enabled**：关闭后，该应用会对当前用户停用：从启动器中隐藏，在重新启用前无法运行。数据会保留。

**Special access（特殊访问权限）**

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

**Package 还是整个 uid**

Android 会把每个模式保存两份：一份针对应用的包，一份针对它的整个 uid（共享该 uid 的所有包）。系统会先检查 uid 模式：只要不是 *Default* 就以它为准，*Default* 则把决定权交还给包模式。对于与运行时权限相关的权限（例如位置），Android 往往会自行设置 uid 模式。

选择模式时，**Apply to** 决定写入的位置：

- **Package**（默认）：仅此包，与系统设置的做法相同。如果 uid 模式不是 *Default*，它会被重置，否则您的选择不会生效。
- **Whole uid**：uid 模式，作用于该 uid 的所有包。包模式保持不变。

面板会同时显示两种模式，并在 uid 模式覆盖包模式时加以标示。

**Permissions（权限）**

应用申请的运行时权限和开发权限可以通过开关授予或撤销。安装时授予或因签名授予的权限会单独列出，无法更改。

## 插件 {#plugin}

已安装的[插件](../plugin/what-is-plugin.md)。

- **+** 可安装一个或多个插件 ZIP。在 Android 11+ 上，AxManager 会先请求所有文件访问权限。
- 点击插件可展开它。**启用**（打开）按钮打开其 WebUI，**运行**按钮执行其 `action.sh`。
- 每个插件都可以启用、禁用、更新（插件提供了 `updateJson` 时）、卸载，或在卸载生效前恢复。
- 改动会在下一次重载时生效。有改动等待生效时，**+** 上方会出现一个**火焰**按钮；点击即可立即[重载](../plugin/re-ignite.md)。
- **WebUI 快捷方式**：插件的 **WebUI配置**（调节按钮）中有**添加快捷方式**，可将其 WebUI 固定到桌面，并设置名称和图标（插件的横幅、首字母或相册中的图片）。如果没有反应，请允许 AxManager 的"桌面快捷方式"权限。

**排序设置**按钮可按名称、大小、启用状态、有操作的插件或有 WebUI 的插件排序，并包含**开发者模式**开关。

开启开发者模式后，列表顶部的**开发者信息**卡片会显示插件脚本看到的内容：`$AXERONVER`、服务的 uid 和 SELinux 上下文、`$AXERONDIR`、插件文件夹、`$AXERONBIN`、`$AXERONXBIN`、`$TMPDIR` 和日志文件夹，以及插件数量、**随服务停止插件**设置和 `AXERON_PLUGIN=<id>` 进程标记。点击某个值即可复制。

## 快速终端 {#quickshell}

以服务的权限运行命令的终端：shell，或在以 root 启动服务时为 root。从主页的悬浮按钮打开。

- 输入命令并运行。命令运行时，输入框会把文字发送到它的标准输入。
- 开头的 `adb shell` 会被去掉，因此从教程中复制的命令可以直接使用。
- 输出工具栏中有**在输出中查找**、跳转到行、行号、自动换行和字号。
- 输出不会被截断，无论多长。保存的日志可以按输出类型筛选。

**Shell settings**（设置按钮）：

- **Snippets**：保存一条命令或整段脚本，之后运行或插入。在 AxManager 中任意位置（例如命令或输出）选中文字后，从文字选择菜单中选择 **Save snippet**，或使用 **Add snippet**。该菜单项只在 AxManager 内出现，不会出现在其他应用中。
- **Output**：针对每种输出类型（命令、stdout、stderr、退出码……），设置是否在屏幕上显示、是否保存在日志中。
- **按键拦截器**：在快速终端打开时拦截音量加和/或音量减键。
- **兼容模式**：通过内置的 BusyBox（`setsid` + `sh`）启动每个会话。您的命令仍在 `/system/bin/sh` 中运行，使用哪些工具取决于 `PATH`（参见 [Prefer System Binaries](#environment)）。如果会话无法启动，请关闭它。
- **Run in any terminal**：在 Termux 等其他终端应用中使用同一个 shell 的命令。参见[在任意终端中运行](./axsh.md)。

## 设置编辑器 {#settings-editor}

**设置 → 设置编辑器**（服务运行时）可浏览和编辑：

| 标签页 | 来源 | 可编辑 |
|---|---|---|
| **Global**、**Secure**、**System** | Android 设置（`settings` 命令） | 是 |
| **Props** | Android 系统属性（通过 `resetprop`） | 视服务的模式而定 |
| **Environment** | AxManager 服务的环境变量 | 是 |

**All** 同时显示所有来源，并概览每个来源的条目数量。可按键或值搜索。点击条目即可编辑，也可以添加新键或删除。

### 快捷设置 {#quick-settings}

在设置编辑器中长按某个设置并选择 **Add to Quick Settings**，即可一键访问。每个快捷设置可以是：

- **Custom value**：显示当前值；输入任意新值即可应用。
- **Switch**：两个值，分别对应开和关。
- **Options**：一组带标签的值。
- **Slider**：介于最小值和最大值之间、带步长的数字。

通过设置编辑器旁边的闪电按钮打开快捷设置。Global、Secure 和 System 的快捷设置在服务关闭时也能使用，依靠的是服务运行时授予 AxManager 的设置权限。

## 设置 {#settings}

### 环境 {#environment}

除最后一项外，仅在服务运行时显示：

- **AX-权限接管**：让基于 Shizuku-API 构建的应用使用 AxManager。参见 [Shizuku 兼容](./shizuku-compatibility.md)。
- **随服务停止插件**：服务停止或退出时，所有插件的进程也会被停止。关闭（默认）时，插件将独立继续运行。参见[插件进程](../plugin/environment.md#plugin-processes)。
- **Prefer System Binaries**：在 `PATH` 中把 Android 自带的程序放在 AxManager 的 BusyBox 之前，让命令使用系统版本。

### 常规 {#general}

- **开机自启**：参见[开机自启](./user-manual.md#activate-on-boot)。
- **自动重载**：每次 AxManager 连接到服务时都重载插件，而不仅是服务启动后的第一次。

### 维护 {#maintenance}

**重置 AxManager** 会禁用并移除您的所有插件：所有插件被标记为移除并运行点火器（因此会执行它们的 `uninstall.sh`），然后删除 AxManager 的文件夹并重启服务。Root 模式和 shell 模式使用[各自独立的文件夹](../plugin/environment.md#working-folder)：root 服务可以重置 root 文件夹、shell 文件夹或两者；shell 服务只能重置自己的文件夹。

### 主题设置 {#appearance}

- **语言设置**：English、Bahasa Indonesia、简体中文，或系统默认值。
- **主题风格**：跟随系统，或选择深色、浅色。
- **莫奈取色**：使用壁纸的颜色（Android 12+）。
- **调色板**：未开启莫奈取色时选择主色。

### 开发者 {#developer}

- **开发者模式**：显示面向开发者的隐藏设置和调试信息，例如插件页的[开发者信息](#plugin)卡片。
- **WebView 调试功能**：在开启开发者模式的情况下，允许对插件 WebUI 进行 WebView 远程调试，并添加页面内控制台（Eruda）。
