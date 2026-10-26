# <Icon name="link" /> Connect with an App

An app can run commands with AxManager's shell or root access through [axsh](./axsh.md), the same way a terminal does. This page is for app developers: the two ways to connect, what each costs, running your own code as shell or root with `ShellService`, and the demo app that tests it all.

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

Add the module from [JitPack](https://jitpack.io/#fahrez182/Axeron-API) (or as a Gradle project, see the Axeron API README), and keep native libraries extracted, since `libaxsh.so` is run as a program:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.fahrez182.Axeron-API:axsh:2.1.0")
}
android {
    packaging { jniLibs { useLegacyPackaging = true } }
}
```

Start the broker in your `Application` and run your own copy of `libaxsh.so`:

```kotlin
import frb.axeron.shell.Broker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Broker.start(this)
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
With AxManager's `libaxsh.so` in shell mode, the helper started by your app's first command leaves a small native process behind (about 3 MB), and the next commands connect to it like to a broker. Set it per app in AxManager (**Privilege → your app → Keep axsh ready**, under the Axeron permission):

- **Smart** (default): stays while your app uses axsh, and leaves after 5 minutes without a command.
- **No limit**: stays until the server stops or AxManager updates.
- **Off**: every command starts a new helper.

Changing it ends the helper running now; the next command starts one with the new setting.
:::

## Permission {#permission}

Your app needs AxManager's permission, set per app in **[Privilege](./app-tour.md#privilege)**. With **Ask** (the default), the first command shows AxManager's dialog and waits for your answer, up to 15 seconds. The command then runs, or stops with `AxManager permission required: allow this app in AxManager`.

## Run your own code: ShellService {#shell-service}

Commands are not always enough. `ShellService` runs a class of your app as shell or root, the way [libsu](https://github.com/topjohnwu/libsu)'s `RootService` does, but through axsh, so it works in shell mode too. It needs the `axsh` module (the broker is optional).

Write an AIDL interface, as for any bound service, and turn AIDL on:

```aidl
// src/main/aidl/com/example/IMyService.aidl
interface IMyService {
    int getUid();
    String exec(String command);
}
```

```kotlin
// build.gradle.kts
android { buildFeatures { aidl = true } }
```

The service. Everything inside it runs as shell (uid 2000) or root (uid 0):

```kotlin
class MyService : ShellService() {
    override fun onBind(intent: Intent): IBinder {
        return object : IMyService.Stub() {
            override fun getUid(): Int {
                return Os.getuid()
            }

            override fun exec(command: String): String {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                return process.inputStream.bufferedReader().readText()
            }
        }
    }
}
```

Bind it from your app, on the main thread:

```kotlin
val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        val service = IMyService.Stub.asInterface(binder)
        // call it off the main thread: service.exec("id")
    }

    override fun onServiceDisconnected(name: ComponentName) {}
}

ShellService.bind(Intent(context, MyService::class.java), connection)
// when done:
ShellService.unbind(connection)
```

Nothing goes in the manifest.

| | libsu `RootService` | `ShellService` |
|---|---|---|
| **Shell mode** | No | Yes |
| **Callbacks** | `onCreate`, `onBind`, `onUnbind`, `onRebind`, `onDestroy` | The same |
| **Calls** | `bind`, `unbind`, `stop` | The same |
| **Processes** | One for all your services, gone with the app | The same |
| **Daemon mode** | `CATEGORY_DAEMON_MODE` | `ShellService.CATEGORY_DAEMON_MODE` |
| **Start the process yourself** | `bindOrTask`, per bind | `ShellService.setLauncher`, once for all |
| **Users** | All | User 0 only |

### Daemon mode {#daemon-mode}

To keep a service running after your app closes, bind it with the daemon category and return `true` from `onUnbind`:

```kotlin
class MyDaemon : ShellService() {
    override fun onUnbind(intent: Intent): Boolean {
        return true
    }

    override fun onRebind(intent: Intent) {
        // a later bind reached this same instance
    }

    override fun onBind(intent: Intent): IBinder { /* … */ }
}

val intent = Intent(context, MyDaemon::class.java)
intent.addCategory(ShellService.CATEGORY_DAEMON_MODE)
ShellService.bind(intent, connection)
```

It runs in a second process that outlives your app. The next bind, even after your app restarts, reaches the same instance. `ShellService.stop(intent)` ends it.

::: tip Good to know
- The service's Context reports the uid's own package (`com.android.shell` in shell mode), so system services that check it, such as the clipboard, accept it.
- `getApplicationContext()` returns the service: no `Application` runs in its process.
- In a debuggable app, Android Studio's **Attach Debugger** lists the process as `<package>:shell_shared` or `<package>:shell_daemon`.
- When a bind fails, `ShellService.lastFailure(component)` says why.
:::

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
| **libsu RootService** | libsu's own `RootService` through axsh, to compare (root mode only) |
| **ShellService** | uid; `Runtime.exec` inside the service; two services in one process; a shared static; the clipboard from its Context; `getApplicationContext()` |
| **ShellService daemon** | The same pid after an unbind and after closing the app; a new pid after `stop` |

The last group needs the **Test Function** plugin (`plugins/test_function` in the AxManager repository) installed and re-ignited. It shows that a plugin's `system/bin` binaries can be called from any allowed app.

Compare **Latency** with `axsh_direct_connection` on and off, and with each runner, to see what each path costs on your device.
