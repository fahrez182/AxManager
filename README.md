# Axeron Manager (AxManager)

A privileged environment for Android that runs on **ADB (shell) permissions**, or on **root** when you have it. AxManager keeps a background server alive and builds on it: Magisk-style plugins with a WebUI, per-app privilege control, a Shizuku-compatible API, a shell, and a system settings editor.

**Website: [axmanager.app](https://axmanager.app)**, the official site and documentation. Download, activate from the browser, guides and the plugin docs.

[Switch to Chinese translation 切换到中文翻译](README_cn.md)

## What it is

AxManager starts its own server process, either as the `shell` user (through Wireless Debugging, a computer, or another phone over OTG) or as `root`. The app, plugins, their WebUIs and the other apps you allow all work through that server. Most features need no root; with root they run with root's reach.

## Features

### Activation
- **Root**: start directly from the app.
- **Wireless Debugging** (Android 11+): pair and start on the device itself, without a computer.
- **TCP debugging**, with a configurable port.
- **Computer**: start it with one `adb shell` command.
- **Browser**: start it over USB from [axmanager.app/activate](https://axmanager.app/activate) in Chrome or Edge, without installing `adb`.
- **OTG**: use this phone as the computer and start AxManager on another phone over a USB cable.
- **Start as Shell**: run in shell mode even on a rooted device.
- **Start on boot**: root, or ADB on Android 11+ (experimental).
- An activation log streams each step, so a failure says where it stopped.

### Home
- Server status, how it was started (root, root as shell, or ADB) and whether it starts on boot.
- A wakelock switch that keeps the server from being suspended.
- Device details: model, Android version, security patch, kernel, supported ABIs, SELinux context.
- An update notice when a newer release is out.

### Privilege
- Grant or deny AxManager access per app, with filters for installed, system and Shizuku-API apps.
- **Shizuku compatibility**: apps built on [Shizuku-API](https://github.com/RikkaApps/Shizuku-API) connect to AxManager without Shizuku running. Some older apps still require the Shizuku app to be installed (not running), and Settings warns you when it isn't.
- **Per-app detail**:
  - Force stop, clear cache, clear data, and uninstall (system apps for the current user).
  - Battery optimization, background activity, and enabling or disabling the app.
  - Special access as app-op modes (allow, foreground, ignore, deny, default): display over other apps, usage access, all files access, install unknown apps, alarms, and more.
  - Grant or revoke runtime and development permissions; every other permission is listed read-only.
  - The page refreshes when you come back, so changes made elsewhere show up.

### Plugins (unrooted modules)
Modules in the familiar Magisk/KernelSU layout, run by AxManager's server instead of a root daemon. See the [plugin docs](https://axmanager.app/plugin/what-is-plugin.html).
- `module.prop`, `customize.sh`, `post-fs-data.sh`, `service.sh`, `system.prop`, `action.sh`, `uninstall.sh`, and `system/bin` (linked into `$AXERONXBIN`, which is on `PATH`).
- A full BusyBox (with Standalone Mode), `resetprop`, and the `AXERON`, `AXERONVER`, `AXERONDIR`, `AXERONBIN`, `AXERONXBIN` and `TMPDIR` environment variables.
- **WebUI** with a KernelSU-compatible `ksu` JavaScript API and AxManager's own `Axeron` API. Any plugin's WebUI can be pinned to the home screen with its own name and icon.
- **Process lifecycle**: every process a plugin starts (from its scripts, its action or its WebUI) carries an `AXERON_PLUGIN=<id>` marker. Disabling or removing the plugin stops all of them, daemons included: SIGTERM first, SIGKILL after 3 seconds.
- `post-fs-data.sh` and `uninstall.sh` can block for 40 seconds at most; their output is kept in `$AXERONDIR/logs`.
- Plugins keep running when the server stops, unless you turn on **Stop plugins with the server**.
- Install several zips at once, filter and sort the list, and re-ignite from the page.
- **Developer mode** adds a card with the server's paths, version and plugin counts, as plugin scripts see them.

### QuickShell
- Run commands with the server's privileges (shell or root, depending on how it was started).
- Save commands and whole scripts as snippets, also from selected text anywhere in the app ("Save snippet" in the selection menu).
- Find in output, unlimited output, a key event blocker, and a compatibility mode that starts sessions through BusyBox.
- `axsh` brings the same privileges to a terminal app: run the manager's `libaxsh.so` with plain `sh` arguments (`axsh -c 'id'`), nothing to install in that app.

### Settings Editor
- Browse, search and edit the `system`, `secure` and `global` settings tables, and add or remove entries.
- **Quick Settings**: keep the values you change often in one list and switch them in one tap.

### Settings and appearance
- Environment: the Shizuku intercept, stopping plugins with the server, and preferring the system's binaries over BusyBox on `PATH`.
- Start on boot, and re-ignite plugins every time the app reconnects to the server (by default only on the first connection after the server starts).
- Reset per mode (root, shell, or both).
- A theme that follows the system, dynamic color or a custom palette, and the app language (English, Indonesian, Chinese).
- Developer mode and WebView debugging for plugin WebUIs.

## Where things live

Root and shell mode each keep their own folder, so files made as root never block shell mode.

| Mode | Folder |
|---|---|
| Root | `/data/adb/axeron` |
| Shell | `/data/user_de/0/com.android.shell/axeron` |

Each one holds:

| Folder | Contents |
|---|---|
| `plugins/` | Installed plugins |
| `bin/` | BusyBox and the bundled tools, rebuilt on every start |
| `xbin/` | Links to the plugins' `system/bin` |
| `cache/` | `$TMPDIR` |
| `logs/` | Output of `post-fs-data.sh` and `uninstall.sh` |

Root mode used to live in `/data/user_de/0/android/axeron`; it moves to `/data/adb/axeron` automatically the first time the server starts as root.

## Requirements
- Android 8.0 (API 26) or newer.
- Wireless Debugging activation needs Android 11 or newer.

## Install
Download the latest APK from [Releases](https://github.com/fahrez182/AxManager/releases), then follow the [user manual](https://axmanager.app/guide/user-manual.html) to start it.

## Build
The API lives in a git submodule, so clone with it:

```bash
git clone --recurse-submodules https://github.com/fahrez182/AxManager.git
cd AxManager
./gradlew :manager:assembleDebug
```

Already cloned without it? Run `git submodule update --init`. To build against a local copy of the API, set `api.useLocal=true` and `api.dir=<path>` in `local.properties`.

Install the debug build:

```bash
adb install manager/build/outputs/apk/debug/manager-debug.apk
```

| Module | What it is |
|---|---|
| `manager` | The app |
| `server` | The privileged server and its native starter |
| `reignite` | The plugin igniter (`libaxreignite.so`, a dex run in app_process), which starts and stops plugins |
| `adb` | Wireless Debugging pairing and the ADB client |
| `api` (submodule) | AIDL, client API, shared server code, Shizuku compatibility, `axsh` |

## Documentation
Everything is on [axmanager.app](https://axmanager.app):
- [What is Axeron Manager?](https://axmanager.app/guide/what-is-axeron-manager.html)
- [User manual](https://axmanager.app/guide/user-manual.html)
- [Plugins](https://axmanager.app/plugin/what-is-plugin.html)
- [Re-ignite](https://axmanager.app/plugin/re-ignite.html)
- [FAQ](https://axmanager.app/guide/faq.html)

## Contributing
Contributions are welcome. Open an issue, send a pull request, or start a discussion.

## Credits
- **[Magisk](https://github.com/topjohnwu/Magisk)**: BusyBox, `resetprop`, and the module system that plugins are modelled on.
- **[Shizuku](https://github.com/RikkaApps/Shizuku) / [Shizuku-API](https://github.com/RikkaApps/Shizuku-API)**: the starting point and reference for Android IPC and ADB-based permissions.
- **[KernelSU](https://github.com/tiann/KernelSU) / [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next)**: inspiration for the UI and the WebUI.

## License
Licensed under the [Apache License 2.0](LICENSE). Third-party code, bundled binaries and libraries are listed with their licenses in [NOTICE](NOTICE).

AxManager is not affiliated with Shizuku and does not claim to be an official replacement.
