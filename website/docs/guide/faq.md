# <Icon name="help" /> FAQ

[[toc]]

Many manufacturers have made modifications to the Android system that prevent AxManager from working properly.

## Start via wireless debugging: keeps showing "Searching for pairing service"

Please allow AxManager to run in the background.

Searching for pairing service requires access to the local network, and many manufacturers disable network access for apps as soon as they become invisible. You can search the web for how to allow apps to `run in the background` on your device.

## Start via wireless debugging: immediately fail after tapping "Enter pairing code"

::: info MIUI (Xiaomi, POCO)
Switch notification style to "Android" from "Notification" - "Notification shade" in system settings.
:::

## Start via wireless debugging/Start by connecting to a computer: the permission of adb is limited

::: info MIUI (Xiaomi, POCO)
Enable "USB debugging (Security options)" in "Developer options". Note that this is a separate option from "USB debugging".
:::

::: info ColorOS (OPPO & OnePlus)
Disable "Permission monitoring" in "Developer options".
:::

::: info Flyme (Meizu)
Disable "Flyme payment protection" in "Developer options".
:::

## Start via wireless debugging/Start by connecting to a computer: AxManager randomly stops

::: info All devices

- Make sure AxManager can run in the background.

- Do not disable "USB debugging" and "Developer options".

- Change the USB usage mode to "Charge only" in the "Developer options".

  - On Android 8, the option is "Select USB configuration" - "Charge only".
  - On Android 9+, the option is "Default USB configuration" - "No data transfer".

- (Android 11+) Enable "Disable adb authorization timeout" option

:::

::: info EMUI (Huawei)
Enable "Allow ADB debugging options in 'Charge only' mode" in "Developer options".
:::

::: info MIUI (Xiaomi, POCO)
Do not use the scan feature in MIUI's "Security" app, since it will disable "Developer options".
:::

::: info Sony
Don't click the dialog shows after connecting the USB, because it will change USB usage mode.
:::

## An app says Shizuku is not found

1. Turn on **Settings → AX-Permission Intercept** while AxManager is running.
3. Some older Shizuku clients (for example older versions of ZArchiver) only connect when the Shizuku app is **installed**. Install it; it doesn't need to be started.

See [Shizuku compatibility](./shizuku-compatibility.md).

## The Shizuku app says an update is needed

This is expected. When AxManager serves the Shizuku app, the server version it reports is `13.<AxManager version code>`, which no real Shizuku release matches, so the Shizuku app shows its update note. Nothing needs updating.

## axsh says "AxManager permission required"

The terminal app isn't allowed. Set it to **Allowed** in **Privilege**, or leave it on **Ask** and allow it in the dialog that appears; `axsh` waits for your answer for up to 15 seconds. If it is set to **Denied**, no dialog appears. See [Run in any terminal](./axsh.md#permission).

## axsh says "AxManager server is not running" after swiping AxManager away

In shell mode, `axsh` asks AxManager for the server, so AxManager has to be started in the background. Some systems, MIUI / HyperOS in particular, kill apps swiped from recents and block them from being started again. AxManager allows its own **Autostart** on MIUI / HyperOS when it connects to the server; if the message persists, allow **Autostart** (or "run in background") for AxManager in the system settings. See [How it connects](./axsh.md#how-it-connects).

## An app I allowed asks for permission again

A permission belongs to the app's signing certificate. If the app was reinstalled signed with another key, for example a debug build after a release one or a build from another source, AxManager treats it as a new app and asks again. This also keeps an app that gets an uninstalled app's uid from inheriting its permission. See [Security in 3.0](./security.md).

## An app says AxManager isn't installed

With **AX-Stealth Mode** on, AxManager doesn't show itself to apps you haven't allowed: no dialog, no answer. Allow the app in **Privilege**, or turn stealth off while it asks. See [AX-Stealth Mode](./app-tour.md#environment).

## An app built on the Axeron API stopped connecting after updating AxManager

AxManager 3.0 changed how apps connect, and apps built on the `axsh` library before 3.0 can't reach a 3.0 server. The app needs an update built with `3.0.0` or later. See [Security in 3.0](./security.md#what-changes-for-app-developers).

## An app-op shows a raw name like MIUIOP(10050)

It is a permission your ROM added, and the ROM gives it no name AxManager can read. AxManager shows it by its number so you can still change it. Names exist only for the few it knows, and only on the ROM they belong to, because another brand can use the same number for something else. See [Special access](./privilege.md#special-access).

## Plugins keep running after the server stops

That is the default: plugins run on their own once started. To stop every plugin's processes whenever the server stops or dies, turn on **Settings → Stop plugins with the server**. See [Plugin processes](../plugin/environment.md#plugin-processes).

## Where are the plugin logs?

`post-fs-data.sh` and `uninstall.sh` write their output to `$AXERONDIR/logs/<id>.post-fs-data.log` and `$AXERONDIR/logs/<id>.uninstall.log`:

- Root mode: `/data/adb/axeron/logs`
- Shell mode: `/data/user_de/0/com.android.shell/axeron/logs`

Output of `service.sh` is not kept; redirect it to a file yourself if you need it. Installing a plugin and running its action show their output on screen, with a button to save the log.
