# <Icon name="service" /> AxShellService

`AxShellService` runs a class of your app as shell or root, like [libsu](https://github.com/topjohnwu/libsu)'s `RootService`, but through axsh, so it works in shell mode too. It needs the `axsh` module ([option 2](./connect-app.md#axsh-module)) and starts its process through your app's broker, so call `AxBroker.start` first. An app without a broker gives `AxShellService.setLauncher` its own way to start the process, such as running AxManager's `libaxsh.so` with `-c`.

Your app has to be [allowed](./connect-app.md#access) in AxManager, as for any command.

[[toc]]

## Write the service {#service}

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

## Bind it {#bind}

From your app, on the main thread:

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

## Daemon mode {#daemon-mode}

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

## Start the process yourself {#launcher}

By default the service process starts through your app's broker. To start it your own way:

- **Once for all binds**: `AxShellService.setLauncher { argv -> … }` runs every service process's command line, for example through AxManager's `libaxsh.so` with `-c` in an app without a broker.
- **Per bind**: `AxShellService.bindOrTask(intent, connection)` binds like `bind`, but when a new process is needed it isn't started: you get a `Task` back, and run it with the launcher for this bind. `null` means the process is already running or starting, and the bind joins it.

::: code-group

```kotlin [Kotlin]
AxShellService.bindOrTask(Intent(context, MyService::class.java), connection)
    ?.run { argv -> Axsh.exec(argv) }   // or any other way to run argv
```

```java [Java]
AxShellService.Task task = AxShellService.bindOrTask(new Intent(context, MyService.class), connection);
if (task != null) task.run(argv -> Axsh.exec(argv));   // or any other way to run argv
```

:::

A task runs once; until it runs, the bind waits. If its process later goes while a bind still waits on it, the next process starts with the same launcher.

## Compared with libsu's RootService {#rootservice}

| | libsu `RootService` | `AxShellService` |
|---|---|---|
| **Shell mode** | No | Yes |
| **Callbacks** | `onCreate`, `onBind`, `onUnbind`, `onRebind`, `onDestroy` | The same |
| **Calls** | `bind`, `unbind`, `stop` | The same |
| **Processes** | One for all your services, gone with the app | The same |
| **Daemon mode** | `CATEGORY_DAEMON_MODE` | `AxShellService.CATEGORY_DAEMON_MODE` |
| **Start the process yourself** | `bindOrTask`, per bind | `AxShellService.bindOrTask`, per bind, or `AxShellService.setLauncher`, once for all |
| **Users** | All | User 0 only |

## Good to know {#tips}

- The service's Context reports the uid's own package (`com.android.shell` in shell mode), so system services that check it, such as the clipboard, accept it.
- `getApplicationContext()` returns the service: no `Application` runs in its process.
- In a debuggable app, Android Studio's **Attach Debugger** lists the process as `<package>:shell_shared` or `<package>:shell_daemon`.
- When a bind fails, `AxShellService.lastFailure(component)` says why.
- The service process hands its binder to your app through the provider the `axsh` library adds, which only AxManager's server may call.
