# <Icon name="key" /> Privilege & Permissions

The **Privilege** screen lists the apps you can allow to use AxManager, and opens each app's details: its permissions, special access, app-ops and actions.

[[toc]]

## The app list {#list}

Each app shows its [Axeron permission](#axeron-permission): **Allowed**, **Ask (default)** or **Denied**. Search by name or package name; the filter button chooses which apps show:

- **Show**: **Installed** (user apps, default), **System** (system apps) or **Shizuku** (apps built on Shizuku-API, see [Shizuku compatibility](./shizuku-compatibility.md))
- **Access**: **All**, **Allowed**, **Ask** or **Denied**

Sort by name, ascending or descending. A disabled app is shown in grayscale, marked **Disabled**.

- **Tap** an app to open its details.
- **Long-press** an app for a quick menu: set its Axeron permission, then **Open app**, **Clear cache**, **Force stop** and **Disable** / **Enable** in one row of buttons. Force stop and disable ask for confirmation first.

Most actions in an app's details need the server running. Some are only possible with root, and AxManager says so when shell mode is not enough. The play button next to the app's name opens the app. It is greyed out for apps that have nothing to open (services, libraries) and for disabled apps.

## Axeron permission {#axeron-permission}

What happens when the app asks to use AxManager. Tap the row to change it:

| Setting | Meaning |
|---|---|
| **Allowed** | Uses AxManager without asking. |
| **Ask** (default) | AxManager asks you each time. *Allow once* in that dialog lasts until the app is closed. |
| **Denied** | Refused at once, with no dialog. |

Changing an app from **Allowed** to **Ask** or **Denied** stops it, and the access it had stops working at its next call.

- **Tied to the app's signature.** A permission belongs to the app's signing certificate. If the app is reinstalled signed with another key (a debug build replacing a release one, say), or another app later gets the same uid, it is asked again.
- **With [AX-Stealth Mode](./app-tour.md#environment) on**, an app set to **Ask** gets no dialog: it can't tell AxManager is installed. Allow it here instead; it connects right away.

## Actions {#actions}

- **Force stop**: stop the app and all of its processes.
- **Clear cache**: in shell mode only the external cache (`Android/data/<package>/cache`) is cleared; the internal cache needs root.
- **Clear data**: delete all data of the app. This cannot be undone.
- **Uninstall**: a user app is removed. A system app is removed **for this user only** and can be restored later. You can keep the app's data and cache.

Destructive actions ask for confirmation first; if you don't answer in time, the request is cancelled.

## General {#general}

- **Unrestricted battery**: exempt the app from battery optimization (Doze).
- **Background activity**: when off, Android stops the app from running in the background (`RUN_ANY_IN_BACKGROUND`).
- **App enabled**: when off, the app is disabled for this user: hidden from the launcher and unable to run until enabled again. Its data is kept.

## Special access {#special-access}

For each special access the app requests (display over other apps, modify system settings, usage access, all files access, install unknown apps, alarms & reminders, media management, full screen notifications), pick a mode:

| Mode | Meaning |
|---|---|
| **Allow** | Always allowed |
| **Foreground only** | Allowed only while the app is in use |
| **Ignore** | Refused quietly: the app gets no error, just nothing |
| **Deny** | Refused with an error, like turning it off in system Settings |
| **Default** | Follows the permission grant, which ordinary apps don't hold |

On **MIUI / HyperOS**, every app also lists three of the ROM's own permissions here: **Autostart** (others may start the app when it isn't running), **Home screen shortcuts** and **Open new windows while running in the background**.

**Other app-ops**: below special access, a folded card lists everything else Android keeps for the app that the sections above don't cover, including permissions your ROM added. They use the same modes. Ones AxManager doesn't know are shown by their raw name (for example `MIUIOP(10050)`), with a note that their meaning isn't known.

::: warning Names of a ROM's own permissions
A ROM numbers its own permissions however it likes, and gives them no names AxManager can read. The names above for MIUI's come from a list in AxManager and are only used on MIUI / HyperOS: the same number can mean something else on another brand's ROM, so there it shows by its raw name.
:::

## Package or whole uid {#package-or-uid}

Android stores each of these modes twice: for the app's package, and for its whole uid (every package that shares it). The uid mode is checked first: anything but *Default* there wins, and *Default* hands the decision back to the package mode. Android often sets uid modes itself for permissions tied to runtime permissions, such as location.

When you pick a mode, **Apply to** chooses where it goes:

- **Package** (default): this package only, as system Settings does it. If the uid mode isn't *Default*, it is reset, or your choice wouldn't take effect.
- **Whole uid**: the uid mode, for every package of the uid. The package mode is left as it is.

The sheet shows both modes, and marks the uid mode when it is overriding the package mode.

## Permissions {#permissions}

Runtime and development permissions the app requests can be granted or revoked with a switch. Permissions granted at install or by signature are listed separately and cannot be changed.
