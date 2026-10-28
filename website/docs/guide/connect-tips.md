# <Icon name="tips" /> Tips & Demo App

More on the two ways to [connect with an app](./connect-app.md): how they differ, what each costs, and the demo app that tests them.

[[toc]]

## Option 1 or option 2 {#two-ways}

| | AxManager's `libaxsh.so` | The `axsh` module |
|---|---|---|
| **In your app** | A `<queries>` entry | The `axsh` library |
| **Version** | Always matches the installed AxManager | The one you built with |
| **Each command, in shell mode** | A short-lived helper gets your app's connection through AxManager | Your app's broker, which got it once |
| **Needs AxManager running** | For every command, in shell mode | No: installed is enough, with the server running |
| **Asking for access** | The first command shows the dialog | `AxBroker.requestAccess` |
| **AxShellService** | No | Yes |

## Speed {#speed}

What a command costs before it runs:

| Server | AxManager's `libaxsh.so` | The `axsh` module |
|---|---|---|
| **Root** | Straight to the server, once the helper has passed on where it is (after the server's first command) | Straight to the server |
| **Shell** | Helper started, AxManager asked, every command (see below) | The broker in your app, which got its connection once |

### Keep axsh ready {#keep-axsh-ready}

With option 1 in shell mode, the helper started by your app's first command leaves a small native process behind (about 3 MB), and the next commands connect to it like to a broker. Set it per app in AxManager (**Privilege → your app → Keep axsh ready**, under the Axeron permission):

- **Smart** (default): stays while your app uses axsh, and leaves after 5 minutes without a command.
- **No limit**: stays until the server stops or AxManager updates.
- **Off**: every command starts a new helper.

Changing it ends the helper running now; the next command starts one with the new setting.

## Running commands {#commands}

### What `libaxsh.so` gives you {#running-commands}

`libaxsh.so` takes the same arguments as `sh`. Run from an app, it has no terminal, so what you write to it goes to the shell's input, stdout and stderr come back separately, and the exit code is the shell's. `Axsh.newProcess` behaves the same.

### More from `Axsh` {#run-axsh}

::: code-group

```kotlin [Kotlin]
val run = Axsh.exec(arrayOf("ls", "-l"), dir = "/")   // a program as it is, no shell around it
val term = Axsh.newTerminal(rows = 24, cols = 80)     // on a PTY: setWindowSize() when it resizes
```

```java [Java]
Process run = Axsh.exec(new String[]{"ls", "-l"}, null, "/");   // a program as it is, no shell around it
Axsh.Terminal term = Axsh.newTerminal(24, 80);                 // on a PTY: setWindowSize() when it resizes
```

:::

### Your own `libaxsh.so` {#run-libaxsh}

With option 2 you can also run the module's own `libaxsh.so` as in option 1, for something that needs a program rather than a `Process`, such as a terminal emulator. Its commands then go through your broker. It is run from your app's native library folder, so keep native libraries extracted:

```kotlin
// build.gradle.kts: only when you run your own libaxsh.so
android {
    packaging { jniLibs { useLegacyPackaging = true } }
}
```

::: code-group

```kotlin [Kotlin]
val axsh = File(applicationInfo.nativeLibraryDir, "libaxsh.so")
```

```java [Java]
File axsh = new File(getApplicationInfo().nativeLibraryDir, "libaxsh.so");
```

:::

`Axsh` and AxShellService don't need this: they run nothing from that folder.

### One open shell with libsu {#libsu}

If you already use [libsu](https://github.com/topjohnwu/libsu), it can keep one axsh shell open and send every command to it:

::: code-group

```kotlin [Kotlin]
Shell.setDefaultBuilder(Shell.Builder.create().setCommands(axsh.path))   // option 1
val shell = Shell.Builder.create().build(Axsh.newProcess())               // option 2
```

```java [Java]
Shell.setDefaultBuilder(Shell.Builder.create().setCommands(axsh.getPath()));   // option 1
Shell shell = Shell.Builder.create().build(Axsh.newProcess());                 // option 2
```

:::

Commands then queue on that one shell, and `cd` and variables carry over between them, unless you run each in `( … )`.

### From Java {#java}

Every example has a Java tab. `AxBroker`, `Axsh` and `AxShellService` are called statically, and `Axsh`'s calls throw a checked `IOException`. The names up to 2.1.0 (`Broker`, `ShellService`) are Kotlin-only aliases, so Java uses the `Ax` ones. Java isn't stopped from calling axsh's other public classes, but they are AxManager's own and change without notice.

## Demo app {#demo-app}

`demo-axsh` in [Axeron API](https://github.com/fahrez182/Axeron-API) is a test bench for all of the above. It names no AxManager package and no internal action anywhere: it finds AxManager with `AxBroker.findManager`, as your app can. Build it, install it with AxManager's server running, allow it in AxManager, then tap **Run all**.

| Setting | Choices |
|---|---|
| **`axsh_direct_connection`** | On: its own `libaxsh.so` with the broker (the `axsh` module). Off: AxManager's `libaxsh.so` without a broker, as Termux runs it (AxShellService then starts through that `libaxsh.so`, set with `setLauncher`). Switching restarts the app. Cold start tests that use `Axsh.exec` only run with it on. |
| **Runner** | `axsh -c` per command, or libsu's cached shell, which can be closed to time its start again. |

The tests:

| Group | What they check |
|---|---|
| **Cold start** | The first command against the next ones; `Axsh.exec` against a local `Runtime.exec`; how long `AxBroker.findManager` takes |
| **Security** | What another app could try, done from the demo: knocking on the pre-3.0 server socket `@axeron` (nothing listens), planting a binder through its own axsh provider (refused: only root or shell are heard), and what `AxBroker.findManager` sees (nothing in stealth mode) |
| **Connection** | The server answers; latency over five runs; four commands at once; nothing carried over between commands |
| **Shell** | Exit code; stdout and stderr apart; stdin up to its end; 100 000 lines of output; the server's environment |
| **Privileges** | uid and SELinux context; `pm list packages`; `settings`; `dumpsys` |
| **Test Function plugin** | Installed and enabled; `axtest` on `PATH` and callable from the app; not marked as a plugin process; its `system.prop`; plugin processes; every `[FAIL]` in its reports |
| **libsu RootService** | libsu's own `RootService` through axsh, to compare (root mode only) |
| **AxShellService** | uid; `Runtime.exec` inside the service; two services in one process; a shared static; the clipboard from its Context; `getApplicationContext()` |
| **AxShellService daemon** | The same pid after an unbind and after closing the app; a new pid after `stop` |

The **Test Function plugin** group needs the plugin (`plugins/test_function` in the AxManager repository) installed and re-ignited. It shows that a plugin's `system/bin` binaries can be called from any allowed app.

Compare **Latency** with `axsh_direct_connection` on and off, and with each runner, to see what each path costs on your device.
