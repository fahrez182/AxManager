# <Icon name="code" /> 在任意终端中运行（axsh）

`axsh` 让 Termux 等任意终端应用获得 AxManager 的 shell 或 root 权限，该应用中无需安装任何东西。它的参数与 `sh` 相同；在真正的终端中，您会得到完整的交互式会话：提示符、行编辑、颜色、`vi`、`top`。

::: info 关于界面文字
本文中的按钮和选项名称与应用界面保持一致。应用的简体中文翻译尚未覆盖所有文字，未翻译的部分在应用中显示为英文，本文也照样使用英文名称。
:::

[[toc]]

## 设置 {#set-it-up}

打开 **快速终端 → Shell settings → Run in any terminal**。卡片中有两条命令，点击即可复制。

| 命令 | 用途 |
|---|---|
| `sh /sdcard/axeron/axsh` | 在任何可以读取存储的终端中运行 |
| `install -m 700 /sdcard/axeron/axsh $PREFIX/bin/axsh` | 在 Termux 中安装一次，之后直接输入 `axsh` |

例如：

```sh
axsh                # 交互式 shell
axsh -c 'id'        # 一条命令
axsh script.sh      # 一个脚本文件
```

终端应用需要存储访问权限才能读取 `/sdcard`（在 Termux 中：`termux-setup-storage`），并且需要在 AxManager 中获得[权限](#permission)。

::: info 为什么要用 `sh /sdcard/axeron/axsh`
`/sdcard` 不允许直接运行其中的程序，因此那里的启动器要通过 `sh` 运行，或者像 Termux 命令那样复制一次到终端自己的文件夹中。
:::

### 启动器 {#the-launcher}

`/sdcard/axeron/axsh` 是一个小脚本，用于运行 AxManager 自带的 `libaxsh.so`。该文件位于 AxManager 的安装目录中，而这个路径会随着 AxManager 的每次更新而改变，因此：

- AxManager 通过服务写入启动器，并在它过期时重新写入：每次连接到服务时，以及您从卡片复制命令之前。
- 如果启动器中仍是旧路径（例如更新前安装到 Termux 的副本），它会自行找到当前路径。第一次会稍慢一些，但不会出错。

卡片还会显示 `libaxsh.so` 本身的路径，供想直接运行它的人使用。

## 终端模式 {#terminal-mode}

当 `axsh` 的输入和输出都是终端时，shell 会在 AxManager 一侧的终端（PTY）上运行：

- 您会得到提示符、行编辑、历史记录和颜色，`vi`、`nano`、`top`、`htop` 等全屏程序也能正常使用。
- 每个按键都会原样发送给 shell：`Ctrl+C` 停止正在运行的命令，`Ctrl+D` 结束会话。
- 调整窗口大小（旋转屏幕、显示或隐藏键盘）会同步过去，程序会以正确的尺寸重新绘制。
- 终端的 `TERM`、`COLORTERM`、`LANG` 和 `LC_*` 会被传递过去，因此颜色和字符能正确显示。终端的其他环境变量不会传递：shell 使用 AxManager 的[环境](../plugin/environment.md)（Termux 的 `PATH` 和 `LD_PRELOAD` 指向服务无法使用的位置）。

当 `axsh` 在脚本、管道或应用中使用（输入或输出不是终端）时，它不使用终端运行：您写入的内容进入 shell 的输入，它的输出和错误分开返回，退出码就是 `axsh` 的退出码。中间不经过 AxManager，因此大量输出也和在本地运行命令一样快。

## 连接方式 {#how-it-connects}

`axsh` 需要连接到 AxManager 的服务。具体方式取决于服务的启动方式：

| 服务 | 方式 |
|---|---|
| **Root** | 直接连接到服务。 |
| **Shell**（无线调试、电脑等） | Android 不允许应用直接与 shell 进程通信，因此 `axsh` 会在您的终端应用中，从 AxManager 的应用里启动一个小助手。助手从 AxManager 获取服务并交接会话。启动时会多一点延迟。 |

内置了 axsh 的应用（参见[面向应用开发者](#for-app-developers)）会跳过这个助手。

::: info MIUI / HyperOS
在 shell 模式下，助手会向 AxManager 请求服务。MIUI 会杀死从最近任务中划掉的应用，并默认阻止其他应用再次启动它们，因此 AxManager 无法响应。为此，AxManager 每次连接到服务时都会为自己允许**自启动**（以及桌面快捷方式和后台弹出界面权限）。如果划掉 AxManager 后 `axsh` 仍提示服务未运行，请在系统设置中检查 AxManager 的**自启动**。
:::

## 权限 {#permission}

终端应用（例如 Termux）和其他应用一样，需要 AxManager 的权限。请在 **[特权](./app-tour.md#privilege)** 中设置：

| 设置 | `axsh` 的行为 |
|---|---|
| **Allowed** | 立即启动。 |
| **Ask**（默认） | AxManager 显示授权对话框，`axsh` 等待您的回答（最长 15 秒）。如果允许，会话会直接开始，无需重新运行命令。如果拒绝、关闭对话框或超时，`axsh` 会立即停止并显示提示。 |
| **Denied** | 立即拒绝，不显示对话框。 |

将应用从 **Allowed** 改为 **Ask** 或 **Denied** 会停止该应用，因此对 Termux 这样修改会关闭 Termux。

## 提示信息 {#messages}

| 提示 | 含义 |
|---|---|
| `AxManager permission required: allow this app in AxManager` | 终端应用未获允许：被设为 **Denied**，或对话框被拒绝、未回答。 |
| `AxManager server is not running` | AxManager 没有交出服务：服务未运行，或 AxManager 无法在后台启动（参见上方 MIUI 说明）。 |
| `AxManager server is not reachable (…), and there is no broker to fall back on (…)` | `axsh` 找不到连接服务的方式。当 `libaxsh.so` 被复制到 AxManager 文件夹之外时会出现这种情况；请改用启动器。 |
| `axsh: can't exec app_process (…)` | shell 模式的助手无法启动。 |

## 面向应用开发者 {#for-app-developers}

应用可以像终端一样使用 axsh：

- **运行 AxManager 的 `libaxsh.so`。** 通过 `PackageManager.getApplicationInfo("frb.axeron.manager", 0).nativeLibraryDir` 获取其文件夹（Android 11+ 需要在 `<queries>` 中声明 `frb.axeron.manager`），然后从那里运行 `libaxsh.so`，例如作为 [libsu](https://github.com/topjohnwu/libsu) 的 shell。它始终与已安装的 AxManager 版本匹配。
- **内置 `axsh` 模块**，并在您的 `Application` 中启动它的 broker。这样 shell 模式的会话会跳过助手，启动更快。

无论哪种方式，应用都需要如上所述的 AxManager 权限。
