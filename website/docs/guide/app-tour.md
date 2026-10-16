# App Tour

A walk through every screen of AxManager and what each option does. To start AxManager in the first place, see the [User Manual](./user-manual.md).

[[toc]]

## Home

The bottom bar has four pages: **Home**, **Privilege**, **Plugin** and **Settings**.

### Status card

The top card shows the server state. Tap it to open the [Activate](./user-manual.md#start-axeron-manager) screen.

| State | Meaning |
|---|---|
| **Running** | The server is up. A badge shows its mode (root or shell). |
| **Need to Activate** (*Stopped*) | The server is not running. |
| **Need extra step** (*Action needed*) | The server runs, but your system blocks something AxManager needs. Tap **Click to see how to fix**. |
| **Updating…** | The app was updated and the old server is being replaced with the new version. |

While running, the card also shows:

- **Wakelock**: tap the chip to turn it on or off. It keeps the CPU awake with the screen off so the server is not suspended.
- Uptime, PID, UID and the server version.
- The power button: re-ignite plugins, restart or shut down the server (see [Stop or restart](./user-manual.md#stop-or-restart)).
- The number of apps you allowed (**Privileges**) and installed **Plugins**.

While stopped, the card shows how AxManager was last started (**Last: ADB / Root / Root (shell)**) and whether [Activate on Boot](./user-manual.md#activate-on-boot) is on.

A floating button opens [QuickShell](#quickshell) while the server runs. If the server is off but AxManager still holds its settings permissions, a **Quick Settings** card lets you use your [Quick Settings](#quick-settings) anyway.

### Device Information

Device model, Android version, security patch, kernel, ABI and the server's SELinux context. Tap a value to copy it.

## Privilege

The list of apps you can allow to use AxManager. Search by name or package name; the filter button chooses which apps show:

- **Installed**: user apps (default)
- **System**: system apps
- **Shizuku**: apps built on Shizuku-API (see [Shizuku compatibility](./shizuku-compatibility.md))

Sort by name, ascending or descending. Tap an app to open its details.

### App details

Most actions need the server running. Some are only possible with root, and AxManager says so when shell mode is not enough.

**Axeron permission**: allow or deny this app access to AxManager.

**Actions**

- **Force stop**: stop the app and all of its processes.
- **Clear cache**: in shell mode only the external cache (`Android/data/<package>/cache`) is cleared; the internal cache needs root.
- **Clear data**: delete all data of the app. This cannot be undone.
- **Uninstall**: a user app is removed. A system app is removed **for this user only** and can be restored later. You can keep the app's data and cache.

Destructive actions ask for confirmation first; if you don't answer in time, the request is cancelled.

**General**

- **Unrestricted battery**: exempt the app from battery optimization (Doze).
- **Background activity**: when off, Android stops the app from running in the background (`RUN_ANY_IN_BACKGROUND`).
- **App enabled**: when off, the app is disabled for this user: hidden from the launcher and unable to run until enabled again. Its data is kept.

**Special access**

For each special access the app requests (display over other apps, modify system settings, usage access, all files access, install unknown apps, alarms & reminders, media management, full screen notifications), pick a mode:

| Mode | Meaning |
|---|---|
| **Allow** | Always allowed |
| **Foreground only** | Allowed only while the app is in use |
| **Ignore** | Refused quietly: the app gets no error, just nothing |
| **Deny** | Refused with an error, like turning it off in system Settings |
| **Default** | Follows the permission grant, which ordinary apps don't hold |

::: info Uid-wide mode
Android stores these modes per app and also per uid. A uid-wide mode other than *Default* overrides the app mode, so changing only the app mode would do nothing. AxManager shows when this happens, and picking a mode resets the uid-wide mode to *Default*.
:::

**Permissions**

Runtime and development permissions the app requests can be granted or revoked with a switch. Permissions granted at install or by signature are listed separately and cannot be changed.

## Plugin

Your installed [plugins](../plugin/what-is-plugin.md).

- **+** installs one or more plugin ZIPs. On Android 11+ AxManager first asks for all files access.
- Tap a plugin to expand it. **Open** opens its WebUI, **Action** runs its `action.sh`.
- Each plugin can be enabled, disabled, updated (when it provides an `updateJson`), uninstalled, or restored before the uninstall is applied.
- Changes take effect on the next re-ignite. When one is waiting, a **flame** button appears above **+**; tap it to [re-ignite](../plugin/re-ignite.md) now.
- **WebUI shortcuts**: the plugin's **WebUI Configuration** (tune button) has **Add WebUI Shortcut**, which pins its WebUI to your home screen with a name and an icon (the plugin's banner, initials, or an image from your gallery). If nothing happens, allow AxManager's "Home screen shortcuts" permission.

The filter button sorts the list by name, size, enabled state, plugins with an action, or plugins with a WebUI, and holds the **Enable Developer Mode** switch.

In developer mode, a **Developer info** card at the top of the list shows what plugin scripts see: `$AXERONVER`, the server's uid and SELinux context, `$AXERONDIR`, the plugins folder, `$AXERONBIN`, `$AXERONXBIN`, `$TMPDIR` and the logs folder, plus plugin counts, the **Stop plugins with the server** setting and the `AXERON_PLUGIN=<id>` process marker. Tap a value to copy it.

## QuickShell

A terminal that runs commands with the server's privileges: shell, or root when the server was started as root. Open it from the floating button on Home.

- Type a command and run it. While a command runs, the input field sends text to its stdin instead.
- A leading `adb shell` is dropped, so commands copied from guides work as they are.
- **Find in output**, go to a line, line numbers, word wrap and text size are in the output toolbar.
- The output is not cut off, however long it gets. Saved logs can be filtered by output kind.

**Shell settings** (the settings button):

- **Snippets**: save a command or a whole script and run or insert it later. Select text anywhere in AxManager (for example in the command or the output) and pick **Save snippet** from the text-selection menu, or use **Add snippet**. The menu entry only appears inside AxManager, not in other apps.
- **Output**: per output kind (command, stdout, stderr, exit code, …), whether it is shown on screen and whether it is kept in saved logs.
- **Key Event Blocker**: block Volume Up and/or Volume Down while QuickShell is open.
- **Compatibility Mode**: starts each session through the bundled BusyBox (`setsid` + `sh`). Your commands still run in `/system/bin/sh`, and which tools they use follows `PATH` (see [Prefer System Binaries](#environment)). Turn it off if sessions fail to start.

## Settings Editor

**Settings → Settings Editor** (while the server runs) browses and edits:

| Tab | Source | Editable |
|---|---|---|
| **Global**, **Secure**, **System** | Android settings (`settings` command) | Yes |
| **Props** | Android system properties (through `resetprop`) | As far as the server's mode allows |
| **Environment** | The environment of AxManager's server | Yes |

**All** shows every source together, with an overview of how many entries each has. Search by key or value. Tap an entry to edit it, add new keys, or remove them.

### Quick Settings

Long-press a setting in Settings Editor and choose **Add to Quick Settings** to keep it one tap away. Each quick setting can be:

- **Custom value**: shows the current value; type any new value to apply.
- **Switch**: two values, for on and off.
- **Options**: a list of labelled values.
- **Slider**: a number between a minimum and maximum, with a step.

Open Quick Settings from the bolt button next to Settings Editor. Global, Secure and System quick settings also work while the server is off, through the settings permissions AxManager was granted while it ran.

## Settings

### Environment

Shown while the server runs, except the last one:

- **AX-Permission Intercept**: let apps built on Shizuku-API use AxManager. See [Shizuku compatibility](./shizuku-compatibility.md).
- **Stop plugins with the server**: when the server stops or dies, every plugin's processes are stopped too. Off (default): plugins keep running on their own. See [Plugin processes](../plugin/environment.md#plugin-processes).
- **Prefer System Binaries**: put Android's own binaries before AxManager's BusyBox in `PATH`, so commands use the system versions.

### General

- **Activate on Boot**: see [Activate on Boot](./user-manual.md#activate-on-boot).
- **Relog to Ignite**: re-ignite plugins every time AxManager connects to the server, not only the first time after the server starts.

### Maintenance

**Reset AxManager** disables and removes all of your plugins: every plugin is marked for removal and the igniter runs (so their `uninstall.sh` runs), then AxManager's folder is deleted and the server restarts. Root and shell mode keep [separate folders](../plugin/environment.md#working-folder): a root server can reset the root folder, the shell folder, or both; a shell server resets its own.

### Appearance

- **Language**: English, Bahasa Indonesia or Simplified Chinese, or the system default.
- **Auto Theme**: follow the system, or pick dark or light.
- **Dynamic Color**: use the wallpaper's colors (Android 12+).
- **Color Palette**: pick the primary color when Dynamic Color is off.

### Developer

- **Enable Developer Mode**: show hidden settings and debug info for developers, like the plugin [Developer info](#plugin) card.
- **Enable Debugging WebView**: with developer mode on, allows WebView remote debugging of plugin WebUIs and adds an in-page console (Eruda).
