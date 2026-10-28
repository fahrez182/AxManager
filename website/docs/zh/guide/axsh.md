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

`axsh` 需要连接到 AxManager 的服务。它会在您的终端应用中，从 AxManager 的应用里启动一个小助手。助手向 AxManager 请求终端应用自己的服务连接：这个连接只对该应用有效，而且在您允许之前只能用来请求权限。之后：

| 服务 | 方式 |
|---|---|
| **Root** | 应用获准后，助手还会在终端的 `$TMPDIR` 中留下服务自己的 socket 的位置，之后的命令会直接连接服务。socket 的名称在服务每次启动时都会改变，且只告诉已获准的应用；服务重启后，第一条命令会再次经过助手。 |
| **Shell**（无线调试、电脑等） | Android 不允许应用直接与 shell 进程通信，因此每条命令都会经过助手。启动时会多一点延迟。 |

内置了 axsh 的应用（参见[与应用连接](./connect-app.md)）会跳过这个助手，也不会询问 AxManager：获准后，服务会自己把连接交给该应用。对于终端应用，**保持 axsh 就绪**（在 AxManager 的**特权 → 该应用**中，Axeron 权限下方）会在第一条命令后保留助手，之后的命令会立即启动。

只有已获准的应用才会被告知服务的 socket，或保留一个就绪的助手。已不再获准、但仍持有这些的应用在那里得不到任何回应；`axsh` 随后会像任何未获准的应用一样经过助手，权限对话框也从那里发起。请求只在这一个地方发生。

::: info MIUI / HyperOS
助手会向 AxManager 请求连接。MIUI 会杀死从最近任务中划掉的应用，并默认阻止其他应用再次启动它们，因此 AxManager 无法响应。为此，AxManager 每次连接到服务时都会为自己允许**自启动**（以及桌面快捷方式和后台弹出界面权限）。如果划掉 AxManager 后 `axsh` 仍提示服务未运行，请在系统设置中检查 AxManager 的**自启动**。内置了 axsh 的应用不会询问 AxManager，因此不受影响。
:::

::: info 它真的是服务吗？
应用无论得到什么，都不会立刻信任它。应用会先连接，只有当服务以自己的 uid（root 或 shell）回调，并说明连接者正是该应用本身时才接受；应用无法伪造这一点，其他应用也无法转交真实服务的回应。服务的 socket 同样只在由 root 或 shell 持有时才被接受。其他一律拒绝。
:::

## 权限 {#permission}

终端应用（例如 Termux）和其他应用一样，需要 AxManager 的权限。请在 **[特权](./privilege.md)** 中设置：

| 设置 | `axsh` 的行为 |
|---|---|
| **Allowed** | 立即启动。 |
| **Ask**（默认） | AxManager 显示授权对话框，`axsh` 等待您的回答（最长 15 秒）。如果允许，会话会直接开始，无需重新运行命令。如果拒绝、关闭对话框或超时，`axsh` 会立即停止并显示提示。 |
| **Denied** | 立即拒绝，不显示对话框。 |

将应用从 **Allowed** 改为 **Ask** 或 **Denied** 会停止该应用，因此对 Termux 这样修改会关闭 Termux。

开启 [AX-Stealth Mode](./app-tour.md#environment) 时，未获准的终端应用在任何地方都得不到对话框或回应：`axsh` 的提示与找不到可询问的 AxManager 时相同，即 `AxManager server is not running [no way to reach the server (…)]`。请先在**特权**中允许它。

## 提示信息 {#messages}

前两条提示的末尾可能带有方括号中的原因，例如 `AxManager server is not running [no way to reach the server (no answer)]`。它说明 `axsh` 停在了哪一步。

| 提示 | 含义 |
|---|---|
| `AxManager permission required: allow this app in AxManager` | 终端应用未获允许：被设为 **Denied**，或对话框被拒绝、未回答。 |
| `AxManager server is not running` | AxManager 没有交出连接：服务未运行。 |
| `AxManager server is not running [no way to reach the server (…)]` | 助手找不到可以询问的 AxManager，或没有得到回应：`libaxsh.so` 被复制到 AxManager 文件夹之外（请改用启动器）、AxManager 无法在后台启动（参见上方 MIUI 说明），或开启了 [AX-Stealth Mode](./app-tour.md#environment) 而该终端应用未获准。 |
| `AxManager server is not reachable […]` | 自己运行 `Axsh` 的应用找不到连接服务的方式。原因会说明是 broker 从未启动，还是没有收到连接：该应用未在 AxManager 中获准，或服务未运行。 |
| `… [the binder never called back …]` 或 `[… isn't the server's …]` | 找到了一个 binder，但无法确认它属于服务，因此被拒绝。 |
| `axsh: can't exec app_process (…)` | shell 模式的助手无法启动。 |

## 面向应用开发者 {#for-app-developers}

应用可以像终端一样使用 axsh：运行 AxManager 的 `libaxsh.so`，或内置 `axsh` 模块让命令跳过助手。两种方式、各自的速度以及用来测试它们的演示应用，请参见 **[与应用连接](./connect-app.md)**。
