# <Icon name="link" /> Connect with an App

Your app can run commands as AxManager's shell or root through [axsh](./axsh.md), like a terminal does. This page is for app developers.

[[toc]]

## How to connect {#connect}

Pick one of two ways. Either way, run commands off the main thread, and allow your app in AxManager first (see [Permission](#permission)).

### Option 1: AxManager's `libaxsh.so` {#manager-binary}

Nothing to add to your app. Let it see AxManager in the manifest:

```xml
<queries>
    <package android:name="frb.axeron.manager" />
</queries>
```

Then run AxManager's `libaxsh.so` like `sh`. Look its path up each time your app starts, since it changes with every AxManager update:

::: code-group

```kotlin [Kotlin]
val dir = packageManager.getApplicationInfo("frb.axeron.manager", 0).nativeLibraryDir
val axsh = File(dir, "libaxsh.so")

val process = Runtime.getRuntime().exec(arrayOf(axsh.path, "-c", "pm list packages"))
val output = process.inputStream.bufferedReader().readText()
val code = process.waitFor()
```

```java [Java]
String dir = getPackageManager().getApplicationInfo("frb.axeron.manager", 0).nativeLibraryDir;
File axsh = new File(dir, "libaxsh.so");

Process process = Runtime.getRuntime().exec(new String[]{axsh.getPath(), "-c", "pm list packages"});
String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
        .lines().collect(Collectors.joining("\n"));
int code = process.waitFor();
```

:::

::: warning Tied to AxManager's package name
This option finds AxManager by its package name, so your app knows it. If AxManager is later installed under a different, even randomized, package name, a lookup by `frb.axeron.manager` will not find it. Relying on that name is therefore not recommended in the long run; [option 2](#axsh-module) names no package.
:::

### Option 2: the `axsh` module {#axsh-module}

Add the library from [JitPack](https://jitpack.io/#fahrez182/Axeron-API):

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
    implementation("com.github.fahrez182.Axeron-API:axsh:2.1.1")
    // axsh brings aidl along at runtime, and its own API (AxBroker, Axsh, AxShellService) uses
    // none of its types. Add it, at the same version, only to call the server's binder
    // interfaces directly (IAxeronService, …).
    // implementation("com.github.fahrez182.Axeron-API:aidl:2.1.1")
}
```

Start the broker in your `Application`, then run commands with `Axsh`:

::: code-group

```kotlin [Kotlin]
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AxBroker.start(this)
    }
}

val process = Axsh.newProcess("-c", "pm list packages")
val output = process.inputStream.bufferedReader().readText()
val code = process.waitFor()
```

```java [Java]
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AxBroker.start(this);
    }
}

Process process = Axsh.newProcess("-c", "pm list packages");   // throws IOException
String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
        .lines().collect(Collectors.joining("\n"));
int code = process.waitFor();
```

:::

The broker reads the server's binder itself, from a broadcast the server leaves for apps to read, so AxManager is neither woken nor asked and your app needs no package name. AxManager only has to be installed, because the server runs from it, and the server has to be running. The first command still shows AxManager's [permission](#permission) dialog.

Maven local and Gradle projects work too, see the [Axeron API README](https://github.com/fahrez182/Axeron-API#using-it).

### Permission {#permission}

Your app needs AxManager's permission, set per app in **[Privilege](./app-tour.md#privilege)**. With **Ask** (the default), the first command shows AxManager's dialog and waits up to 15 seconds. Denied, the command stops with `AxManager permission required: allow this app in AxManager`.

## Run your own code: AxShellService {#shell-service}

`AxShellService` runs a class of your app as shell or root, like [libsu](https://github.com/topjohnwu/libsu)'s `RootService`, but through axsh, so it works in shell mode too. It needs the `axsh` module ([option 2](#axsh-module)) and starts its process through your app's broker, so call `AxBroker.start` first. An app without a broker gives `AxShellService.setLauncher` its own way to start the process, such as running AxManager's `libaxsh.so` with `-c`.

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

::: code-group

```kotlin [Kotlin]
class MyService : AxShellService() {
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

```java [Java]
public class MyService extends AxShellService {
    @Override
    public IBinder onBind(Intent intent) {
        return new IMyService.Stub() {
            @Override
            public int getUid() {
                return Os.getuid();
            }

            @Override
            public String exec(String command) throws RemoteException {
                try {
                    Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
                    return new BufferedReader(new InputStreamReader(process.getInputStream()))
                            .lines().collect(Collectors.joining("\n"));
                } catch (IOException e) {
                    throw new RemoteException(e.getMessage());
                }
            }
        };
    }
}
```

:::

Bind it from your app, on the main thread:

::: code-group

```kotlin [Kotlin]
val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        val service = IMyService.Stub.asInterface(binder)
        // call it off the main thread: service.exec("id")
    }

    override fun onServiceDisconnected(name: ComponentName) {}
}

AxShellService.bind(Intent(context, MyService::class.java), connection)
// when done:
AxShellService.unbind(connection)
```

```java [Java]
ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        IMyService service = IMyService.Stub.asInterface(binder);
        // call it off the main thread: service.exec("id")
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}
};

AxShellService.bind(new Intent(context, MyService.class), connection);
// when done:
AxShellService.unbind(connection);
```

:::

Nothing goes in the manifest.

### Daemon mode {#daemon-mode}

To keep a service running after your app closes, bind it with the daemon category and return `true` from `onUnbind`:

::: code-group

```kotlin [Kotlin]
class MyDaemon : AxShellService() {
    override fun onUnbind(intent: Intent): Boolean {
        return true
    }

    override fun onRebind(intent: Intent) {
        // a later bind reached this same instance
    }

    override fun onBind(intent: Intent): IBinder { /* … */ }
}

val intent = Intent(context, MyDaemon::class.java)
intent.addCategory(AxShellService.CATEGORY_DAEMON_MODE)
AxShellService.bind(intent, connection)
```

```java [Java]
public class MyDaemon extends AxShellService {
    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        // a later bind reached this same instance
    }

    @Override
    public IBinder onBind(Intent intent) { /* … */ }
}

Intent intent = new Intent(context, MyDaemon.class);
intent.addCategory(AxShellService.CATEGORY_DAEMON_MODE);
AxShellService.bind(intent, connection);
```

:::

It runs in a second process that outlives your app. The next bind, even after your app restarts, reaches the same instance. `AxShellService.stop(intent)` ends it.

## What's the difference {#difference}

### Option 1 or option 2 {#two-ways}

| | AxManager's `libaxsh.so` | The `axsh` module |
|---|---|---|
| **In your app** | Nothing | The `axsh` library |
| **Version** | Always matches the installed AxManager | The one you built with |
| **Each command, in shell mode** | A short-lived helper asks AxManager for the server | Your app's broker, which asked once |
| **Needs AxManager running** | For every command | No: installed is enough, with the server running |
| **Needs AxManager's package name** | Yes | No |
| **AxShellService** | No | Yes |

### Speed {#speed}

What a command costs before it runs:

| Server | AxManager's `libaxsh.so` | The `axsh` module |
|---|---|---|
| **Root** | Straight to the server | Straight to the server |
| **Shell** | Helper started, AxManager asked, every command | AxBroker in your app: reads the server's binder once, AxManager not asked |

### AxShellService and libsu's RootService {#rootservice}

| | libsu `RootService` | `AxShellService` |
|---|---|---|
| **Shell mode** | No | Yes |
| **Callbacks** | `onCreate`, `onBind`, `onUnbind`, `onRebind`, `onDestroy` | The same |
| **Calls** | `bind`, `unbind`, `stop` | The same |
| **Processes** | One for all your services, gone with the app | The same |
| **Daemon mode** | `CATEGORY_DAEMON_MODE` | `AxShellService.CATEGORY_DAEMON_MODE` |
| **Start the process yourself** | `bindOrTask`, per bind | `AxShellService.setLauncher`, once for all |
| **Users** | All | User 0 only |

## Tips {#tips}

### Keep axsh ready {#keep-axsh-ready}

With option 1 in shell mode, the helper started by your app's first command leaves a small native process behind (about 3 MB), and the next commands connect to it like to a broker. Set it per app in AxManager (**Privilege → your app → Keep axsh ready**, under the Axeron permission):

- **Smart** (default): stays while your app uses axsh, and leaves after 5 minutes without a command.
- **No limit**: stays until the server stops or AxManager updates.
- **Off**: every command starts a new helper.

Changing it ends the helper running now; the next command starts one with the new setting.

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

### AxShellService {#shell-service-tips}

- The service's Context reports the uid's own package (`com.android.shell` in shell mode), so system services that check it, such as the clipboard, accept it.
- `getApplicationContext()` returns the service: no `Application` runs in its process.
- In a debuggable app, Android Studio's **Attach Debugger** lists the process as `<package>:shell_shared` or `<package>:shell_daemon`.
- When a bind fails, `AxShellService.lastFailure(component)` says why.

## Demo app {#demo-app}

`demo-axsh` in [Axeron API](https://github.com/fahrez182/Axeron-API) is a test bench for all of the above. Build it, install it with AxManager's server running, then tap **Run all**.

| Setting | Choices |
|---|---|
| **`axsh_direct_connection`** | On: its own `libaxsh.so` with the broker (the `axsh` module). Off: AxManager's `libaxsh.so` without a broker, as Termux runs it (AxShellService then starts through that `libaxsh.so`, set with `setLauncher`). Switching restarts the app. Cold start tests that use `Axsh.exec` only run with it on. |
| **Runner** | `axsh -c` per command, or libsu's cached shell, which can be closed to time its start again. |

The tests:

| Group | What they check |
|---|---|
| **Cold start** | The first command against the next ones; `Axsh.exec` against a local `Runtime.exec`; reading the server's binder from its broadcast against AxManager's relay |
| **Connection** | The server answers; latency over five runs; four commands at once; nothing carried over between commands |
| **Shell** | Exit code; stdout and stderr apart; stdin up to its end; 100 000 lines of output; the server's environment |
| **Privileges** | uid and SELinux context; `pm list packages`; `settings`; `dumpsys` |
| **Test Function plugin** | Installed and enabled; `axtest` on `PATH` and callable from the app; not marked as a plugin process; its `system.prop`; plugin processes; every `[FAIL]` in its reports |
| **libsu RootService** | libsu's own `RootService` through axsh, to compare (root mode only) |
| **AxShellService** | uid; `Runtime.exec` inside the service; two services in one process; a shared static; the clipboard from its Context; `getApplicationContext()` |
| **AxShellService daemon** | The same pid after an unbind and after closing the app; a new pid after `stop` |

The last group needs the **Test Function** plugin (`plugins/test_function` in the AxManager repository) installed and re-ignited. It shows that a plugin's `system/bin` binaries can be called from any allowed app.

Compare **Latency** with `axsh_direct_connection` on and off, and with each runner, to see what each path costs on your device.
