# <Icon name="service" /> AxShellService

`AxShellService` 以 shell 或 root 身份运行应用中的一个类，用法与 [libsu](https://github.com/topjohnwu/libsu) 的 `RootService` 相同，但它通过 axsh 启动，因此在 shell 模式下也能使用。需要 `axsh` 模块（[方式二](./connect-app.md#axsh-module)），并通过应用的 broker 启动进程，因此请先调用 `AxBroker.start`。没有 broker 的应用可以用 `AxShellService.setLauncher` 提供自己启动进程的方式，例如用 `-c` 运行 AxManager 的 `libaxsh.so`。

和任何命令一样，你的应用必须已在 AxManager 中[获准](./connect-app.md#access)。

[[toc]]

## 编写服务 {#service}

像普通绑定服务一样编写 AIDL 接口，并开启 AIDL：

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

服务本身。其中的所有代码都以 shell（uid 2000）或 root（uid 0）运行：

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

## 绑定 {#bind}

在应用的主线程中绑定它：

::: code-group

```kotlin [Kotlin]
val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        val service = IMyService.Stub.asInterface(binder)
        // 不要在主线程调用：service.exec("id")
    }

    override fun onServiceDisconnected(name: ComponentName) {}
}

AxShellService.bind(Intent(context, MyService::class.java), connection)
// 用完后：
AxShellService.unbind(connection)
```

```java [Java]
ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        IMyService service = IMyService.Stub.asInterface(binder);
        // 不要在主线程调用：service.exec("id")
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}
};

AxShellService.bind(new Intent(context, MyService.class), connection);
// 用完后：
AxShellService.unbind(connection);
```

:::

清单中无需添加任何内容。

## 守护模式 {#daemon-mode}

要让服务在应用关闭后继续运行，绑定时加上守护类别，并在 `onUnbind` 中返回 `true`：

::: code-group

```kotlin [Kotlin]
class MyDaemon : AxShellService() {
    override fun onUnbind(intent: Intent): Boolean {
        return true
    }

    override fun onRebind(intent: Intent) {
        // 之后的绑定到达了同一个实例
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
        // 之后的绑定到达了同一个实例
    }

    @Override
    public IBinder onBind(Intent intent) { /* … */ }
}

Intent intent = new Intent(context, MyDaemon.class);
intent.addCategory(AxShellService.CATEGORY_DAEMON_MODE);
AxShellService.bind(intent, connection);
```

:::

它运行在第二个进程中，该进程比应用活得更久。下一次绑定（即使应用已重启）会到达同一个实例。`AxShellService.stop(intent)` 会结束它。

## 自行启动进程 {#launcher}

默认情况下，服务进程通过应用的 broker 启动。要用自己的方式启动它：

- **对所有绑定一次设置**：`AxShellService.setLauncher { argv -> … }` 运行每个服务进程的命令行，例如在没有 broker 的应用中用 `-c` 运行 AxManager 的 `libaxsh.so`。
- **每次绑定**：`AxShellService.bindOrTask(intent, connection)` 与 `bind` 一样进行绑定，但需要新进程时不会自动启动：它返回一个 `Task`，由你用本次绑定的启动方式运行。返回 `null` 表示进程已在运行或正在启动，本次绑定会加入它。

::: code-group

```kotlin [Kotlin]
AxShellService.bindOrTask(Intent(context, MyService::class.java), connection)
    ?.run { argv -> Axsh.exec(argv) }   // 或任何其他运行 argv 的方式
```

```java [Java]
AxShellService.Task task = AxShellService.bindOrTask(new Intent(context, MyService.class), connection);
if (task != null) task.run(argv -> Axsh.exec(argv));   // 或任何其他运行 argv 的方式
```

:::

一个 Task 只能运行一次；在它运行之前，绑定会一直等待。如果它的进程之后退出而仍有绑定在等待，下一个进程会用相同的启动方式启动。

## 与 libsu 的 RootService 对比 {#rootservice}

| | libsu `RootService` | `AxShellService` |
|---|---|---|
| **shell 模式** | 否 | 是 |
| **回调** | `onCreate`、`onBind`、`onUnbind`、`onRebind`、`onDestroy` | 相同 |
| **调用** | `bind`、`unbind`、`stop` | 相同 |
| **进程** | 所有服务共用一个，随应用退出 | 相同 |
| **守护模式** | `CATEGORY_DAEMON_MODE` | `AxShellService.CATEGORY_DAEMON_MODE` |
| **自行启动进程** | `bindOrTask`，每次绑定 | `AxShellService.bindOrTask`，每次绑定；或 `AxShellService.setLauncher`，全局设置一次 |
| **用户** | 全部 | 仅用户 0 |

## 须知 {#tips}

- 服务的 Context 报告的是该 uid 自己的包名（shell 模式下为 `com.android.shell`），因此会检查它的系统服务（例如剪贴板）会接受它。
- `getApplicationContext()` 返回服务本身：其进程中没有运行 `Application`。
- 在可调试的应用中，Android Studio 的 **Attach Debugger** 会将该进程列为 `<package>:shell_shared` 或 `<package>:shell_daemon`。
- 绑定失败时，`AxShellService.lastFailure(component)` 会说明原因。
- 服务进程通过 `axsh` 库添加的 provider 把它的 binder 交给你的应用，而只有 AxManager 的服务能调用这个 provider。
