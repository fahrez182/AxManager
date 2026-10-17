# <Icon name="link" /> Connect with an App

An app can run commands with AxManager's shell or root access through [axsh](./axsh.md), the same way a terminal does. This page is for app developers: the two ways to connect, what each costs, and the demo app that tests both.

[[toc]]

## Two ways to connect {#two-ways}

| | AxManager's `libaxsh.so` | The `axsh` module |
|---|---|---|
| **In your app** | Nothing to bundle | The `axsh` library from [Axeron API](https://github.com/fahrez182/Axeron-API) |
| **Version** | Always matches the installed AxManager | The one you built with |
| **Start of a command** | In shell mode: a short-lived helper asks AxManager for the server each time | A connect to your app's own broker |
| **Needs AxManager running** | Yes: it answers the helper (each time) | Only for the first command |

Both run the same `libaxsh.so` client, and both need AxManager's [permission](./axsh.md#permission). The difference is what waits on the other side: a helper started for that one command, or a broker already running in your app.

### AxManager's `libaxsh.so` {#manager-binary}

Declare AxManager in the manifest, so Android 11+ lets your app see it:

```xml
<queries>
    <package android:name="frb.axeron.manager" />
</queries>
```

Then run `libaxsh.so` from AxManager's library folder. The folder changes with every AxManager update, so look it up each time your app starts:

```kotlin
val dir = packageManager.getApplicationInfo("frb.axeron.manager", 0).nativeLibraryDir
val axsh = File(dir, "libaxsh.so")

val process = ProcessBuilder(axsh.path, "-c", "id").start()
```

### The `axsh` module {#axsh-module}

Add the module, as a Gradle project or from Maven local (see the Axeron API README), and keep native libraries extracted, since `libaxsh.so` is run as a program:

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":axsh"))   // or "dev.frb.axeron:axsh:<version>" from mavenLocal()
}
android {
    packaging { jniLibs { useLegacyPackaging = true } }
}
```

Start the broker in your `Application` and run your own copy of `libaxsh.so`:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AxshBroker.start(this)
    }
}

val axsh = File(applicationInfo.nativeLibraryDir, "libaxsh.so")
```

The broker listens inside your app's process. It asks AxManager for the server once, keeps it, and serves every later command from there.

## Running commands {#running-commands}

`libaxsh.so` takes the same arguments as `sh`. Run from an app, it has no terminal, so:

- what you write to it goes to the shell's input;
- stdout and stderr come back separately;
- the exit code is the shell's.

You can start one process per command, or keep one shell open with [libsu](https://github.com/topjohnwu/libsu):

```kotlin
Shell.setDefaultBuilder(Shell.Builder.create().setCommands(axsh.path))

Shell.cmd("pm list packages").exec()
```

| | One process per command | libsu |
|---|---|---|
| **Each command** | A new axsh session | A write to the open shell |
| **Several at once** | Each gets its own shell | They queue on the one shell |
| **State** (`cd`, variables) | Never carries over | Carries over unless you run each command in `( … )` |

## Speed {#speed}

What a command costs before it runs depends on the way you connect and, for AxManager's `libaxsh.so`, on how the server was started:

| Server | AxManager's `libaxsh.so` | The `axsh` module |
|---|---|---|
| **Root** | Straight to the server | Straight to the server |
| **Shell** | Helper started, AxManager asked, every command | Broker in your app: AxManager asked once |

::: tip Keep axsh ready
With AxManager's `libaxsh.so` in shell mode, turn on **Keep axsh ready** for your app in AxManager (**Privilege → your app**, under the Axeron permission). The helper then stays after the first command and the next commands connect to it like to a broker. It uses about 20–30 MB of memory while it waits, and leaves after the time you pick without use, or when AxManager updates. A change applies from the next helper started.
:::

## Permission {#permission}

Your app needs AxManager's permission, set per app in **[Privilege](./app-tour.md#privilege)**. With **Ask** (the default), the first command shows AxManager's dialog and waits for your answer, up to 15 seconds. The command then runs, or stops with `AxManager permission required: allow this app in AxManager`.

## Demo app {#demo-app}

`demo-axsh` in [Axeron API](https://github.com/fahrez182/Axeron-API) is a test bench for all of the above. Build it, install it with AxManager's server running, then tap **Run all**.

| Setting | Choices |
|---|---|
| **`axsh_direct_connection`** | On: its own `libaxsh.so` with the broker (the `axsh` module). Off: AxManager's `libaxsh.so` without a broker, as Termux runs it. Switching restarts the app. |
| **Runner** | `axsh -c` per command, or libsu's cached shell, which can be closed to time its start again. |

The tests:

| Group | What they check |
|---|---|
| **Connection** | The server answers; latency over five runs; four commands at once; nothing carried over between commands |
| **Shell** | Exit code; stdout and stderr apart; stdin up to its end; 100 000 lines of output; the server's environment |
| **Privileges** | uid and SELinux context; `pm list packages`; `settings`; `dumpsys` |
| **Test Function plugin** | Installed and enabled; `axtest` on `PATH` and callable from the app; not marked as a plugin process; its `system.prop`; plugin processes; every `[FAIL]` in its reports |

The last group needs the **Test Function** plugin (`plugins/test_function` in the AxManager repository) installed and re-ignited. It shows that a plugin's `system/bin` binaries can be called from any allowed app.

Compare **Latency** with `axsh_direct_connection` on and off, and with each runner, to see what each path costs on your device.
