# <Icon name="code" /> Run in Any Terminal (axsh)

`axsh` brings AxManager's shell or root access to any terminal app, such as Termux, with nothing to install in that app. It takes the same arguments as `sh`, and in a real terminal you get a full interactive session: prompt, line editing, colors, `vi`, `top`.

[[toc]]

## Set it up

Open **QuickShell → Shell settings → Run in any terminal**. The card has two commands; tap one to copy it.

| Command | Use |
|---|---|
| `sh /sdcard/axeron/axsh` | Run it from any terminal that can read your storage |
| `install -m 700 /sdcard/axeron/axsh $PREFIX/bin/axsh` | Install it into Termux once; after that just type `axsh` |

Then, for example:

```sh
axsh                # interactive shell
axsh -c 'id'        # one command
axsh script.sh      # a script file
```

The terminal app needs storage access to read `/sdcard` (in Termux: `termux-setup-storage`). It also needs [permission](#permission) in AxManager.

::: info Why `sh /sdcard/axeron/axsh`
`/sdcard` doesn't allow running programs from it, so the launcher there is run through `sh`, or copied once into the terminal's own folder as the Termux command does.
:::

### The launcher

`/sdcard/axeron/axsh` is a small script that runs AxManager's own `libaxsh.so`. That file lives in AxManager's install folder, whose path changes with every update of AxManager, so:

- AxManager writes the launcher through the server and rewrites it whenever it is out of date: each time it connects to the server, and before you copy a command from the card.
- If the launcher still has an old path (for example a copy you installed into Termux before an update), it finds the current one by itself. That takes a moment the first time; nothing breaks.

The card also shows the path of `libaxsh.so` itself, for anyone who wants to run it directly.

## Terminal mode {#terminal-mode}

When both the input and the output of `axsh` are a terminal, the shell runs on a terminal (a PTY) on AxManager's side:

- You get a prompt, line editing, history and colors, and full-screen programs like `vi`, `nano`, `top` or `htop` work.
- Every key goes to the shell as it is: `Ctrl+C` stops the running command, `Ctrl+D` ends the session.
- Resizing the window (rotating the screen, the keyboard showing or hiding) is passed on, so programs redraw at the right size.
- Your terminal's `TERM`, `COLORTERM`, `LANG` and `LC_*` are passed on, so colors and characters display correctly. The rest of the terminal's environment is not: the shell gets AxManager's [environment](../plugin/environment.md) (Termux's `PATH` and `LD_PRELOAD` point to places the server can't use).

When `axsh` is used from a script, a pipe or an app (input or output is not a terminal), it runs without a terminal: what you write goes to the shell's input, its output and errors come back separately, and the exit code is `axsh`'s exit code. Nothing passes through AxManager in between, so large outputs are as fast as running the command locally.

## How it connects {#how-it-connects}

`axsh` needs to reach AxManager's server. It starts a small helper from AxManager's app inside your terminal app. The helper asks AxManager for the terminal app's own connection to the server: one that works for that app alone, and only lets it ask for permission until you allow it. Then:

| Server | Path |
|---|---|
| **Root** | Once the app is allowed, the helper also leaves where the server's own socket is, in the terminal's `$TMPDIR`, so the next commands go straight to the server. The socket's name is new every time the server starts and only allowed apps are told it; after a restart the first command goes through the helper again. |
| **Shell** (wireless debugging, computer, …) | Android doesn't let apps talk to a shell process directly, so every command goes through the helper. This adds a short delay at start. |

Apps that build axsh in (see [Connect with an App](./connect-app.md)) skip the helper, and they don't ask AxManager either: once allowed, the server hands the app its connection itself. For a terminal app, **Keep axsh ready** (in AxManager, **Privilege → the app**, under the Axeron permission) keeps the helper after the first command, so the next ones start at once.

Only an allowed app is told the server's socket or keeps a ready helper. An app that is no longer allowed but still has them gets no answer there at all; `axsh` then goes through the helper, like any app not allowed, and the permission dialog comes from there. Asking happens in that one place.

::: info MIUI / HyperOS
The helper asks AxManager for the connection. MIUI kills apps swiped away from recents and by default blocks other apps from starting them again, so AxManager could not answer. AxManager therefore allows its own **Autostart** (and its home screen shortcut and background window permissions) whenever it connects to the server. If `axsh` still says the server is not running after you swipe AxManager away, check **Autostart** for AxManager in the system settings. Apps that build axsh in don't ask AxManager, so this doesn't affect them.
:::

::: info Is that really the server?
Whatever an app is handed, it does not trust yet. It attaches and accepts the connection only when the server calls back from its own uid (root, or shell), which an app cannot fake, and names the app itself as the one that attached, so another app can't pass a real server's answer on. The server's socket, too, is only accepted when root or shell holds it. Anything else is refused.
:::

## Permission {#permission}

The terminal app (for example Termux) needs AxManager's permission, like any other app. Set it in **[Privilege](./privilege.md)**:

| Setting | What `axsh` does |
|---|---|
| **Allowed** | Starts right away. |
| **Ask** (default) | AxManager shows its permission dialog and `axsh` waits for your answer (up to 15 seconds). If you allow it, the session starts without running the command again. If you deny it, close the dialog or let it time out, `axsh` stops with a message right away. |
| **Denied** | Refused right away, without a dialog. |

Changing an app from **Allowed** to **Ask** or **Denied** stops that app, so changing it for Termux closes Termux.

With [AX-Stealth Mode](./app-tour.md#environment) on, a terminal app that isn't allowed gets no dialog and no answer from anywhere: `axsh` says what it says when there is no AxManager to ask, `AxManager server is not running [no way to reach the server (…)]`. Allow it in **Privilege** first.

## Messages {#messages}

The first two messages can end with a reason in square brackets, such as `AxManager server is not running [no way to reach the server (no answer)]`. It says where `axsh` stopped.

| Message | Meaning |
|---|---|
| `AxManager permission required: allow this app in AxManager` | The terminal app is not allowed: it is set to **Denied**, or the dialog was denied or not answered. |
| `AxManager server is not running` | AxManager handed over no connection: the server isn't running. |
| `AxManager server is not running [no way to reach the server (…)]` | The helper found no AxManager to ask, or none answered: `libaxsh.so` was copied out of AxManager's folder (use the launcher instead), AxManager couldn't be started in the background (see the MIUI note above), or [AX-Stealth Mode](./app-tour.md#environment) is on and the terminal app isn't allowed. |
| `AxManager server is not reachable [… ]` | An app that runs `Axsh` itself found no way to the server. The reason says whether its broker was never started, or no connection arrived: the app isn't allowed in AxManager, or the server isn't running. |
| `… [the binder never called back …]` or `[… isn't the server's …]` | A binder was found but not confirmed as the server's, so it was refused. |
| `axsh: can't exec app_process (…)` | The shell-mode helper could not start. |

## For app developers {#for-app-developers}

An app can use axsh the same way a terminal does: run AxManager's `libaxsh.so`, or build the `axsh` module in so its commands skip the helper. See **[Connect with an App](./connect-app.md)** for both, how fast each is, and the demo app that tests them.
