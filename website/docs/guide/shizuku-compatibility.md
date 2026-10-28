# <Icon name="swap" /> Shizuku Compatibility

AxManager can stand in for [Shizuku](https://github.com/RikkaApps/Shizuku): apps built on [Shizuku-API](https://github.com/RikkaApps/Shizuku-API) connect to AxManager's server and get the same shell or root access, without Shizuku running.

[[toc]]

## Turn it on

1. [Start AxManager](./user-manual.md#start-axeron-manager).
2. Open **Settings → AX-Permission Intercept** and turn it on (it is off by default).

AxManager then hands its Shizuku-compatible service to apps that use Shizuku-API, including apps that are already running. The switch is stored per mode (root and shell keep [separate folders](../plugin/environment.md#working-folder)) and survives restarts of the server.

## Granting apps

When an app asks for Shizuku permission, AxManager shows its own permission request: **Allow all the time**, **Allow once this time**, or **Don't Allow**. You can change the decision later in **Privilege** (the **Shizuku** filter lists apps built on Shizuku-API), where each app is **Allowed**, **Ask** or **Denied** (see [Axeron permission](./privilege.md#axeron-permission)).

The Shizuku app has no "ask" setting of its own, so what it does maps to **Ask**, never to **Denied**:

- Turning an app off in the Shizuku app sets it to **Ask**: the app is no longer allowed, but it can still request again.
- **Deny** in the Shizuku app's permission dialog refuses that one request and is not remembered.

Only AxManager's **Privilege** screen sets an app to **Denied**, where its requests are refused without a dialog.

::: info With AX-Stealth Mode on
A Shizuku client you haven't allowed isn't handed the service at all: to it, Shizuku isn't running, and a request gets no dialog. Allow it in **Privilege** (or in the Shizuku app's own list); it gets the service right away, without restarting. See [AX-Stealth Mode](./app-tour.md#environment).
:::

## Apps that need the Shizuku app installed

Some older Shizuku clients (for example older versions of ZArchiver) only try to connect when the Shizuku app (`moe.shizuku.privileged.api`) is **installed**. If such an app says Shizuku is not found:

- Install the Shizuku app. It does **not** need to be started; AxManager serves the app instead.
- While the intercept is on and the Shizuku app is missing, **Settings** shows this hint under AX-Permission Intercept.

## What the Shizuku app shows

If the Shizuku app is installed, it connects to AxManager too:

- It shows the server as **running**, with version `13.<AxManager version code>` (for example `13.30000`). The part after the dot is AxManager's version code, so you can tell that AxManager is serving it.
- Because that number does not match a real Shizuku release, the Shizuku app shows an **update** note. This is expected; nothing needs updating.
- Its app list shows the apps that already have a permission entry, plus apps built on Shizuku-API. AxManager itself is not listed; manage all other apps in AxManager's **Privilege** screen.

## Stopping

**Stop** in the Shizuku app does not stop AxManager. It turns **AX-Permission Intercept** off (and closes the Shizuku app), the same as switching it off in AxManager's Settings:

- Apps that already hold the service are refused from then on; services they started through it are removed.
- Only AxManager and the Shizuku app may switch the intercept. Any other Shizuku client that tries is refused.
- The Shizuku app gets its rights only through the intercept, and only as the app reaching it through Shizuku-API: an app that merely takes its package name gets nothing more than any other client.

Turn it on again from AxManager's Settings.
