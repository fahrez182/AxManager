# <Icon name="folder" /> Environment & Paths

What a plugin's scripts see when AxManager runs them, where things live, and how AxManager starts and stops a plugin's processes.

[[toc]]

## Environment variables

Every process the server starts (plugin scripts, `action.sh`, WebUI commands, QuickShell) gets these variables:

| Variable | Value |
|---|---|
| `AXERON` | `true`. Use it to tell AxManager apart from KernelSU and Magisk. |
| `AXERONVER` | The server's version code, e.g. `20000`. This is what `axeronPlugin` in `module.prop` is compared against. |
| `AXERONDIR` | AxManager's [working folder](#working-folder) for the current mode. |
| `AXERONBIN` | `$AXERONDIR/bin`: BusyBox and AxManager's tools. |
| `AXERONXBIN` | `$AXERONDIR/xbin`: links to every enabled plugin's `system/bin`. |
| `TMPDIR` | `$AXERONDIR/cache`: a place for temporary files. |
| `AXERONLIB` | The native library folder of the AxManager app. |
| `PATH` | `$AXERONXBIN:$AXERONBIN` before the system `PATH`, or after it when **Prefer System Binaries** is on. |
| `LD_LIBRARY_PATH` | `$AXERONLIB`, before any existing `LD_LIBRARY_PATH`. |
| `HOSTNAME` | `axeron` |
| `AXERON_PLUGIN` | Only in a plugin's own processes: the plugin's id. See [Plugin processes](#plugin-processes). |

`customize.sh` gets more variables during installation (`MODPATH`, `ZIPFILE`, `ARCH`, `API`, …); see [Customize](./what-is-plugin.md#customize).

These are locked: the user's own env, from Settings Editor, can add variables and entries to `PATH` / `LD_LIBRARY_PATH`, but can't replace them. See [Environment](../guide/app-tour.md#environment-editor).

::: tip
In developer mode, the **Developer info** card on the Plugin page shows these values as your scripts see them. Tap one to copy it.
:::

## Working folder

Root mode and shell mode each keep their own folder, exposed to every script as `$AXERONDIR`:

| Mode | `$AXERONDIR` |
|---|---|
| Root | `/data/adb/axeron` |
| Shell | `/data/user_de/0/com.android.shell/axeron` |

"Root" is a server started with **Start** on a rooted device; **Start as Shell** and every ADB method use the shell folder. Since files made as root never land in the shell folder, switching modes never leaves shell mode with files it cannot touch. Each mode has its own plugins.

Inside it:

| Path | Contents |
|---|---|
| `plugins/` | Installed plugins, one folder per plugin id |
| `plugins_update/` | Plugins installed or updated since the last re-ignite |
| `bin/` | BusyBox and AxManager's tools (`$AXERONBIN`), rebuilt on every start |
| `xbin/` | Plugins' `system/bin` files, linked here (`$AXERONXBIN`) |
| `cache/` | Temporary files (`$TMPDIR`) |
| `logs/` | Output of `post-fs-data.sh` and `uninstall.sh` |

::: tip
Root mode used to live in `/data/user_de/0/android/axeron`. It is moved to `/data/adb/axeron` automatically the first time AxManager starts as root. Always use `$AXERONDIR` instead of hardcoding either path.
:::

## Plugin processes

Every process a plugin starts carries the environment variable `AXERON_PLUGIN=<id>`: processes from `post-fs-data.sh`, `service.sh`, `uninstall.sh`, `customize.sh`, `action.sh` and the plugin's [WebUI](./webui-api.md), and everything they start in turn (children inherit it, even after `setsid`). AxManager uses this marker to find a plugin's processes.

- When the plugin is **disabled, updated or removed**, every process with its marker is stopped, daemons included, together with the members of any session or process group they lead. They get `SIGTERM` first and about 3 seconds to exit; whatever is left is `SIGKILL`ed.
- A plugin counts as running while any of its marked processes is alive, so a `service.sh` whose processes have all died is started again on the next re-ignite.
- Do not clear or overwrite `AXERON_PLUGIN` in your scripts, or AxManager loses track of those processes.

### Stop plugins with the server

By default plugins keep running on their own when the AxManager server stops. Turn on **Settings → Stop plugins with the server** to stop every plugin's processes whenever the server stops or dies, by the same rules as above. Processes started by a new server that is already coming up are left alone.

## post-fs-data.sh and uninstall.sh

These two scripts block the plugin igniter, so:

- Each may run for at most **40 seconds**. A script still running after that is left running and the igniter moves on to the next plugin.
- Their stdin is `/dev/null`, and their output (stdout and stderr) goes to `$AXERONDIR/logs/<id>.post-fs-data.log` and `$AXERONDIR/logs/<id>.uninstall.log`.
- Anything `uninstall.sh` leaves running is stopped right after it, before the plugin's folder is deleted.

Start long-running work from `service.sh` instead. `post-fs-data.sh` runs once per boot, and again when the plugin starts after being disabled or updated.
