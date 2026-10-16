# <Icon name="web" /> WebUI API

带有 `webroot/index.html` 的插件会获得一个 WebUI：AxManager 在独立窗口中打开的网页，页面中的 JavaScript 对象可以通过 AxManager 的服务运行 shell 命令。`ksu` 对象遵循 [KernelSU 的 WebUI API](https://kernelsu.org/guide/module-webui.html)，因此大多数 KernelSU 的 WebUI 无需修改即可使用；AxManager 还额外提供了自己的 `Axeron` 对象。

[[toc]]

## 页面 {#the-page}

- `webroot/` 作为站点根目录，地址为 `ax://plugin.local/`；WebUI 打开的是 `ax://plugin.local/index.html`。与 KernelSU 一样，同样的文件也在 `https://mui.kernelsu.org/` 下提供。
- `/internal/insets.css` 以 CSS 变量定义窗口边距（`--safe-area-inset-top`、`--window-inset-bottom` 等），`/internal/colors.css` 定义应用当前的配色。
- 应用图标：`ax://package.icon/<包名>` 或 `ksu://icon/<包名>` 以 PNG 返回已安装应用的图标。
- 无法加载 `webroot/` 之外的本地文件。

可以在插件的 **WebUI配置**中把 WebUI 固定到桌面（参见[应用导览](../guide/app-tour.md#plugin)）。

::: tip 调试
在设置 → 开发者中打开**开发者模式**和 **WebView 调试功能**：插件 WebUI 即可使用 Chrome 远程调试（`chrome://inspect`），并带有页面内控制台（Eruda）。
:::

## 命令以插件身份运行 {#commands-run-as-the-plugin}

WebUI 运行的每条命令都由 AxManager 的服务启动，拥有服务的权限（shell，或 root 模式下的 root）及其[环境变量](./environment.md#environment-variables)，并带有 `AXERON_PLUGIN=<id>`。因此页面启动的任何进程（包括守护进程）都会在插件被禁用或移除时一并停止（参见[插件进程](./environment.md#plugin-processes)）。

::: warning 引号
命令字符串会由一个 shell 以 `sh -c "…"` 的形式传递，即放在双引号内。需要原样传给命令的 `"`、`$` 和反引号请转义；较长的逻辑可以放进插件内的脚本中再运行（`sh ./script.sh`，并把 `cwd` 选项设为 `moduleInfo()` 中的插件文件夹）。
:::

::: tip 保持页面响应
调用 `ksu` 或 `Axeron` 时，页面的 JavaScript 会一直阻塞到调用返回：页面不会重绘，定时器停止，加载动画也会卡住。回调形式（`ksu.exec(command, options, callback)`、`ksu.spawn`、`Axeron.exec(command, options, callback)`）会立即返回并在后台运行命令，因此耗时的操作请优先使用它们，或基于它们的 `kernelsu.js` / `axeron.js` 辅助模块。同步形式适合很快就能完成的调用。
:::

## `ksu` 对象 {#the-ksu-object}

### `ksu.exec(command)` {#ksu-exec-command}

运行 `command` 并等待其结束，期间页面会被阻塞。返回 stdout 字符串；如果 stderr 不为空，后面会附加换行和 stderr。

### `ksu.exec(command, options, callbackName)` {#ksu-exec-callback}

立即返回并在后台运行 `command`，结束后调用 `window[callbackName](errno, stdout, stderr)`。`options` 是一个 JSON 字符串：

| 选项 | 含义 |
|---|---|
| `cwd` | 工作目录（在命令前执行 `cd`） |
| `env` | 额外环境变量组成的对象 |

插件的 `system/bin` 会被放在 `PATH` 的最前面。`ksu.exec(command, callbackName)` 与之相同，只是没有选项。

### `ksu.spawn(command, args, options, callbackName)` {#ksu-spawn}

以 `args`（JSON 数组字符串，追加在命令之后）启动 `command`，并以流的方式输出。`window[callbackName]` 必须是一个带有 `stdout`、`stderr` 事件发射器和 `emit` 方法的对象；AxManager 会调用：

- 输出到达时调用 `stdout.emit('data', chunk)` 和 `stderr.emit('data', chunk)`，
- 退出码不为 0 时调用 `emit('error', err)`（`err.exitCode`，`err.message` 为 stderr），
- 最后在结束时调用 `emit('exit', code)`。它在 `error` 之后，因此可以在 `exit` 时丢弃该对象。

`options` 和 `PATH` 的处理与 `exec` 相同。[`kernelsu` 辅助模块](#helper-modules)会为您构建这个对象。

### 其他方法 {#other-methods}

| 方法 | 返回 |
|---|---|
| `ksu.toast(message)` | 显示一条 Toast。 |
| `ksu.fullScreen(enable)` | 隐藏或显示系统栏。 |
| `ksu.moduleInfo()` | JSON 字符串：插件的 `module.prop` 字段和状态，以及 `moduleDir`（插件文件夹）。 |
| `ksu.listPackages(type)` | 包名组成的 JSON 数组：`"user"`、`"system"`，其他值则为全部。 |
| `ksu.listUserPackages()`、`ksu.listSystemPackages()`、`ksu.listAllPackages()` | 同上，作为单独的方法。 |
| `ksu.getPackagesInfo(packages)` | `packages` 为 JSON 数组字符串。返回 `{packageName, versionName, versionCode, appLabel, isSystem, uid}` 组成的 JSON 数组；找不到的包返回 `{packageName, error}`。 |
| `ksu.getPackagesIcons(packages, size)` | `{packageName, icon}` 组成的 JSON 数组，`icon` 为 `data:image/png;base64,…` URL（无法获取时为空）。 |
| `ksu.cacheAllPackageIcons(size)` | 预先加载所有应用图标，供 `getPackagesIcons` 使用。 |

包列表只包含设备的主用户（用户 0）。

## `Axeron` 对象 {#the-axeron-object}

| 方法 | 返回 |
|---|---|
| `Axeron.exec(command, options)` | 运行 `command` 并等待其结束，期间页面会被阻塞。`options` 是与 `ksu.exec` 相同的 JSON 字符串。返回 JSON 字符串 `{"errno": …, "stdout": "…", "stderr": "…"}`。 |
| `Axeron.exec(command, options, callbackName)` | 立即返回并在后台运行 `command`，结束后调用 `window[callbackName](errno, stdout, stderr)`。 |
| `Axeron.toast(message)` | 显示一条 Toast。 |

## 辅助模块 {#helper-modules}

AxManager 提供两个小型 ES 模块，用 Promise 封装上述对象。

**`kernelsu.js`**（地址 `ax://kernelsu.js/`），与 KernelSU 的 `kernelsu` npm 包提供相同的函数：

```js
import { exec, spawn, toast, fullScreen } from "kernelsu";

const { errno, stdout, stderr } = await exec("ls -l", { cwd: "/sdcard" });

const child = spawn("logcat -d");
child.stdout.on("data", (line) => console.log(line));
child.on("exit", (code) => toast(`exit ${code}`));
```

**`axeron.js`**（地址 `ax://axeron.js/`）：

```js
import { exec } from "ax://axeron.js/";

const { errno, stdout, stderr } = await exec("id", { cwd: "/" }); // 不会阻塞页面
```

`spawn(command, args)`、`spawn(command, options)` 和 `spawn(command, args, options)` 都可以使用：数组总是 `args`，对象则是 `options`。
