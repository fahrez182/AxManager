# <Icon name="shield" /> Security in 3.0

AxManager runs with ADB's privileges, or root's. 3.0 changes how that power is reached, so that an app you never allowed can't get to it, or even see that AxManager is here. Update both AxManager and any app built on its API.

::: warning Apps built on the Axeron API before 3.0 can't connect
3.0 changes how apps reach the server. An app using the `axsh` library from 2.x finds no server; it needs `3.0.0` or later. Apps built on Shizuku-API are not affected.
:::

## What was fixed

All of these let an app or a website reach AxManager's shell without you allowing it. None of them needed root or a computer.

| Before 3.0 | Now |
|---|---|
| An app could take the Shizuku app's package name and be treated as AxManager's manager. | Only the Shizuku app reaching the server through [Shizuku compatibility](./shizuku-compatibility.md) gets the Shizuku app's rights, and only there. |
| An app that got the uid of an uninstalled app could inherit its permission. | A permission belongs to the app's signing certificate, not its uid. Another app, or the same app signed by someone else, is asked again. |
| A website opened from a plugin's WebUI could run commands. | WebUI pages are the plugin's own; other sites open in the browser and can't be framed or posted to. See [WebUI API](../plugin/webui-api.md#what-a-page-can-reach). |
| An app could listen on axsh's socket before the server and receive the terminal's input. | axsh connects only to a socket held by root or shell, under a name only allowed apps are told. |
| An app could pass itself off as the server to other apps. | Apps accept a server only once it proves itself from root or shell, and only for their own connection. |

## What changes for you

- **Every app gets its own access.** The server never hands its own connection to another app. Each allowed app gets one that works for that app alone, and stops working the moment you revoke it.
- **Reinstalled apps may ask again.** If an app comes back signed with another key, for example a debug build replacing a release one, AxManager treats it as a new app.
- **Apps ask through AxManager's dialog.** An app that isn't allowed yet can open AxManager's permission dialog; until you answer, it gets nothing.
- **You can hide AxManager.** With [AX-Stealth Mode](./app-tour.md#environment) on, an app you haven't allowed gets no dialog and no answer, as if AxManager weren't installed. Shizuku clients included. You allow new apps from the app list instead.

AxManager itself still shows in the launcher and to apps that can list every installed app: those come from Android, not from AxManager.

## What changes for app developers

- Use the `axsh` library at `3.0.0`.
- Your app no longer reads the server from a broadcast. Once you allow it, the server hands your app its own connection; `AxBroker` waits for it on the first command.
- Ask for access with `AxBroker.requestAccess(activity, requestCode)`, and find AxManager with `AxBroker.findManager(context)`: no package name is needed anywhere.

See **[Connect with an App](./connect-app.md)** for the details.
