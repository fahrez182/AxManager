# Run in Any Terminal (axsh)

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

`axsh` needs to reach AxManager's server. How depends on how the server was started:

| Server | Path |
|---|---|
| **Root** | Directly to the server. |
| **Shell** (wireless debugging, computer, …) | Android doesn't let apps talk to a shell process directly, so `axsh` starts a small helper from AxManager's app inside your terminal app. The helper gets the server from AxManager and hands the session over. This adds a short delay at start. |

Apps that build axsh in (see [For app developers](#for-app-developers)) skip the helper.

::: info MIUI / HyperOS
In shell mode the helper asks AxManager for the server. MIUI kills apps swiped away from recents and by default blocks other apps from starting them again, so AxManager could not answer. AxManager therefore allows its own **Autostart** (and its home screen shortcut and background window permissions) whenever it connects to the server. If `axsh` still says the server is not running after you swipe AxManager away, check **Autostart** for AxManager in the system settings.
:::

## Permission {#permission}

The terminal app (for example Termux) needs AxManager's permission, like any other app. Set it in **[Privilege](./app-tour.md#privilege)**:

| Setting | What `axsh` does |
|---|---|
| **Allowed** | Starts right away. |
| **Ask** (default) | AxManager shows its permission dialog and `axsh` waits for your answer (up to 15 seconds). If you allow it, the session starts without running the command again. If you deny it, close the dialog or let it time out, `axsh` stops with a message right away. |
| **Denied** | Refused right away, without a dialog. |

Changing an app from **Allowed** to **Ask** or **Denied** stops that app, so changing it for Termux closes Termux.

## Messages {#messages}

| Message | Meaning |
|---|---|
| `AxManager permission required: allow this app in AxManager` | The terminal app is not allowed: it is set to **Denied**, or the dialog was denied or not answered. |
| `AxManager server is not running` | AxManager didn't hand over the server: it isn't running, or AxManager couldn't be started in the background (see the MIUI note above). |
| `AxManager server is not reachable (…), and there is no broker to fall back on (…)` | `axsh` found no way to the server. This happens when `libaxsh.so` was copied out of AxManager's folder; use the launcher instead. |
| `axsh: can't exec app_process (…)` | The shell-mode helper could not start. |

## For app developers {#for-app-developers}

An app can use axsh the same way a terminal does:

- **Run AxManager's `libaxsh.so`.** Get its folder from `PackageManager.getApplicationInfo("frb.axeron.manager", 0).nativeLibraryDir` (declare `frb.axeron.manager` in `<queries>` on Android 11+) and run `libaxsh.so` from there, for example as a [libsu](https://github.com/topjohnwu/libsu) shell. It is always the version that matches the installed AxManager.
- **Build the `axsh` module in** and start its broker from your `Application`. Then shell-mode sessions skip the helper and start faster.

Either way the app needs AxManager's permission, as above.
