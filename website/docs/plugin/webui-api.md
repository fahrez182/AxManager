# <Icon name="web" /> WebUI API

A plugin with a `webroot/index.html` gets a WebUI: a web page AxManager opens in its own window, with JavaScript objects that run shell commands through AxManager's server. The `ksu` object follows [KernelSU's WebUI API](https://kernelsu.org/guide/module-webui.html), so most KernelSU WebUIs work unchanged; AxManager adds an `Axeron` object of its own.

[[toc]]

## The page

- `webroot/` is served as the site root at `ax://plugin.local/`; the WebUI opens `ax://plugin.local/index.html`. The same files are also served at `https://mui.kernelsu.org/`, as in KernelSU.
- `/internal/insets.css` defines the window insets as CSS variables (`--safe-area-inset-top`, `--window-inset-bottom`, …), and `/internal/colors.css` the app's current color scheme.
- App icons: `ax://package.icon/<package>` or `ksu://icon/<package>` returns the icon of an installed app as a PNG.
- Local files outside `webroot/` cannot be loaded.

A WebUI can be pinned to the home screen from the plugin's **WebUI Configuration** (see [App Tour](../guide/app-tour.md#plugin)).

::: tip Debugging
Turn on **Enable Developer Mode** and **Enable Debugging WebView** in Settings → Developer: plugin WebUIs then allow Chrome remote debugging (`chrome://inspect`) and get an in-page console (Eruda).
:::

## Commands run as the plugin

Every command a WebUI runs is started by AxManager's server, with the server's privileges (shell, or root in root mode) and its [environment](./environment.md#environment-variables), plus `AXERON_PLUGIN=<id>`. So whatever your page starts, daemons included, is stopped with the plugin when it is disabled or removed (see [Plugin processes](./environment.md#plugin-processes)).

::: warning Quoting
The command string is handed to `sh -c "…"` by a shell, inside double quotes. Escape `"`, `$` and backticks that should reach your command as they are, or put longer logic in a script inside your plugin and run that (`sh ./script.sh` with the `cwd` option set to the plugin's folder from `moduleInfo()`).
:::

::: tip Keep the page responsive
A call into `ksu` or `Axeron` blocks the page's JavaScript until it returns: nothing redraws, timers stop, a spinner freezes. The callback variants (`ksu.exec(command, options, callback)`, `ksu.spawn`, `Axeron.exec(command, options, callback)`) return at once and run the command in the background, so prefer them, or the `kernelsu.js` / `axeron.js` helpers built on them, for anything that can take time. The synchronous variants are fine for quick calls.
:::

## The `ksu` object

### `ksu.exec(command)`

Runs `command` and waits for it, blocking the page meanwhile. Returns stdout as a string, followed by a newline and stderr when stderr is not empty.

### `ksu.exec(command, options, callbackName)`

Returns at once and runs `command` in the background, then calls `window[callbackName](errno, stdout, stderr)`. `options` is a JSON string:

| Option | Meaning |
|---|---|
| `cwd` | Working directory (`cd` before the command) |
| `env` | An object of extra environment variables |

The plugin's `system/bin` is put first in `PATH`. `ksu.exec(command, callbackName)` is the same with no options.

### `ksu.spawn(command, args, options, callbackName)`

Starts `command` with `args` (a JSON array string, appended to the command) and streams its output. `window[callbackName]` must be an object with `stdout` and `stderr` emitters and an `emit` method; AxManager calls:

- `stdout.emit('data', chunk)` and `stderr.emit('data', chunk)` as output arrives,
- `emit('error', err)` when the exit code is not 0 (`err.exitCode`, `err.message` holds stderr),
- `emit('exit', code)` last, when it ends. It comes after `error`, so you can drop the object on `exit`.

`options` and `PATH` work as for `exec`. The [`kernelsu` helper](#helper-modules) builds this object for you.

### Other methods

| Method | Returns |
|---|---|
| `ksu.toast(message)` | Shows a toast. |
| `ksu.fullScreen(enable)` | Hides or shows the system bars. |
| `ksu.moduleInfo()` | JSON string: the plugin's `module.prop` fields and state, plus `moduleDir` (its folder). |
| `ksu.listPackages(type)` | JSON array of package names: `"user"`, `"system"`, or anything else for all. |
| `ksu.listUserPackages()`, `ksu.listSystemPackages()`, `ksu.listAllPackages()` | The same, as separate methods. |
| `ksu.getPackagesInfo(packages)` | `packages` is a JSON array string. Returns a JSON array of `{packageName, versionName, versionCode, appLabel, isSystem, uid}`, or `{packageName, error}` for a package that isn't found. |
| `ksu.getPackagesIcons(packages, size)` | JSON array of `{packageName, icon}`, `icon` being a `data:image/png;base64,…` URL (empty when unavailable). |
| `ksu.cacheAllPackageIcons(size)` | Loads every app icon ahead of time for `getPackagesIcons`. |

Package lists cover the device's main user (user 0).

## The `Axeron` object

| Method | Returns |
|---|---|
| `Axeron.exec(command, options)` | Runs `command` and waits for it, blocking the page. `options` is a JSON string as for `ksu.exec`. Returns a JSON string `{"errno": …, "stdout": "…", "stderr": "…"}`. |
| `Axeron.exec(command, options, callbackName)` | Returns at once and runs `command` in the background, then calls `window[callbackName](errno, stdout, stderr)`. |
| `Axeron.toast(message)` | Shows a toast. |

## Helper modules

AxManager serves two small ES modules that wrap these objects in promises:

**`kernelsu.js`** (served at `ax://kernelsu.js/`), the same functions as KernelSU's `kernelsu` npm package:

```js
import { exec, spawn, toast, fullScreen } from "kernelsu";

const { errno, stdout, stderr } = await exec("ls -l", { cwd: "/sdcard" });

const child = spawn("logcat -d");
child.stdout.on("data", (line) => console.log(line));
child.on("exit", (code) => toast(`exit ${code}`));
```

**`axeron.js`** (served at `ax://axeron.js/`):

```js
import { exec } from "ax://axeron.js/";

const { errno, stdout, stderr } = await exec("id", { cwd: "/" }); // non-blocking
```

`spawn(command, args)`, `spawn(command, options)` and `spawn(command, args, options)` all work: an array is always `args`, an object is `options`.
