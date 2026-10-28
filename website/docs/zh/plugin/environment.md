# <Icon name="folder" /> 环境与路径

AxManager 运行插件脚本时脚本能看到什么、文件放在哪里，以及 AxManager 如何启动和停止插件的进程。

[[toc]]

## 环境变量 {#environment-variables}

服务启动的每个进程（插件脚本、`action.sh`、WebUI 命令、快速终端）都会获得以下变量：

| 变量 | 值 |
|---|---|
| `AXERON` | `true`。可用于区分 AxManager 与 KernelSU、Magisk。 |
| `AXERONVER` | 服务的版本号，例如 `30000`。`module.prop` 中的 `axeronPlugin` 就是与它比较。 |
| `AXERONDIR` | 当前模式下 AxManager 的[工作目录](#working-folder)。 |
| `AXERONBIN` | `$AXERONDIR/bin`：BusyBox 和 AxManager 的工具。 |
| `AXERONXBIN` | `$AXERONDIR/xbin`：指向每个已启用插件 `system/bin` 的链接。 |
| `TMPDIR` | `$AXERONDIR/cache`：存放临时文件的位置。 |
| `AXERONLIB` | AxManager 应用的原生库目录。 |
| `PATH` | `$AXERONXBIN:$AXERONBIN` 位于系统 `PATH` 之前；开启 **Prefer System Binaries** 时位于其后。 |
| `LD_LIBRARY_PATH` | `$AXERONLIB`，位于原有 `LD_LIBRARY_PATH` 之前。 |
| `HOSTNAME` | `axeron` |
| `AXERON_PLUGIN` | 仅存在于插件自己的进程中：插件的 id。参见[插件进程](#plugin-processes)。 |

`customize.sh` 在安装期间还会获得更多变量（`MODPATH`、`ZIPFILE`、`ARCH`、`API` 等），参见[自定义](./plugin-installer.md#customize)。

这些变量是锁定的：用户在设置编辑器中的环境变量可以添加变量，以及向 `PATH` / `LD_LIBRARY_PATH` 添加条目，但不能替换它们。参见[环境变量](../guide/app-tour.md#environment-editor)。

::: tip
开启开发者模式后，插件页的**开发者信息**卡片会显示脚本看到的这些值。点击某个值即可复制。
:::

## 工作目录 {#working-folder}

Root 模式和 shell 模式各自使用独立的文件夹，并以 `$AXERONDIR` 提供给所有脚本：

| 模式 | `$AXERONDIR` |
|---|---|
| Root | `/data/adb/axeron` |
| Shell | `/data/user_de/0/com.android.shell/axeron` |

"Root" 指在已 root 的设备上通过**启动**启动的服务；**以 Shell 身份启动**以及所有 ADB 方式都使用 shell 文件夹。由于以 root 创建的文件不会出现在 shell 文件夹中，切换模式后 shell 模式不会遇到无法操作的文件。每个模式都有自己的插件。

其中包含：

| 路径 | 内容 |
|---|---|
| `plugins/` | 已安装的插件，每个插件 id 一个文件夹 |
| `plugins_update/` | 上次重载以来安装或更新的插件 |
| `bin/` | BusyBox 和 AxManager 的工具（`$AXERONBIN`），每次启动时重建 |
| `xbin/` | 链接到此处的插件 `system/bin` 文件（`$AXERONXBIN`） |
| `cache/` | 临时文件（`$TMPDIR`） |
| `logs/` | `post-fs-data.sh` 和 `uninstall.sh` 的输出 |

::: tip
Root 模式以前位于 `/data/user_de/0/android/axeron`。AxManager 首次以 root 启动时会自动将其迁移到 `/data/adb/axeron`。请始终使用 `$AXERONDIR`，不要硬编码任何一个路径。
:::

## 插件进程 {#plugin-processes}

插件启动的每个进程都带有环境变量 `AXERON_PLUGIN=<id>`：包括来自 `post-fs-data.sh`、`service.sh`、`uninstall.sh`、`customize.sh`、`action.sh` 和插件 [WebUI](./webui-api.md) 的进程，以及它们再启动的所有进程（子进程会继承该变量，即使调用了 `setsid`）。AxManager 通过这个标记找到插件的进程。

- 当插件被**禁用、更新或移除**时，所有带有其标记的进程都会被停止（包括守护进程），以及由它们领导的会话或进程组中的所有成员。它们先收到 `SIGTERM`，有大约 3 秒时间退出；之后仍存在的进程会被 `SIGKILL`。
- 只要插件的任一带标记进程仍然存活，该插件就被视为正在运行；因此，如果 `service.sh` 的进程全部退出，它会在下次重载时重新启动。
- 请不要在脚本中清除或覆盖 `AXERON_PLUGIN`，否则 AxManager 将无法追踪这些进程。

### 随服务停止插件 {#stop-plugins-with-the-server}

默认情况下，AxManager 服务停止后，插件会独立继续运行。打开**设置 → 随服务停止插件**，即可在服务停止或退出时按上述规则停止所有插件的进程。已经在启动中的新服务所启动的进程不受影响。

## post-fs-data.sh 和 uninstall.sh {#post-fs-data-sh-and-uninstall-sh}

这两个脚本会阻塞插件点火器（igniter），因此：

- 每个脚本最多运行 **40 秒**。超时后仍在运行的脚本会被保留运行，点火器继续处理下一个插件。
- 它们的 stdin 为 `/dev/null`，输出（stdout 和 stderr）写入 `$AXERONDIR/logs/<id>.post-fs-data.log` 和 `$AXERONDIR/logs/<id>.uninstall.log`。
- `uninstall.sh` 留下的任何进程都会在它结束后、插件文件夹被删除之前被停止。

长时间运行的任务请改为从 `service.sh` 启动。`post-fs-data.sh` 每次开机运行一次，插件在被禁用或更新后再次启动时也会重新运行。
