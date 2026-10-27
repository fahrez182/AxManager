# <Icon name="link" /> 与应用连接

你的应用可以通过 [axsh](./axsh.md)，像终端一样以 AxManager 的 shell 或 root 权限运行命令。本页面向应用开发者。

[[toc]]

## 如何连接 {#connect}

两种方式任选其一。无论哪种，都请在主线程之外运行命令，并先在 AxManager 中允许你的应用（参见[权限](#permission)）。

### 方式一：AxManager 的 `libaxsh.so` {#manager-binary}

应用中无需添加任何东西。在清单中声明 AxManager，让应用能看到它：

```xml
<queries>
    <package android:name="frb.axeron.manager" />
</queries>
```

然后像 `sh` 一样运行 AxManager 的 `libaxsh.so`。它的路径会随每次 AxManager 更新而变化，因此请在应用每次启动时重新获取：

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

### 方式二：`axsh` 模块 {#axsh-module}

从 [JitPack](https://jitpack.io/#fahrez182/Axeron-API) 添加该库：

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
    // axsh 会在运行时自动带上 aidl，而且它自己的 API（AxBroker、Axsh、AxShellService）
    // 不使用其中的任何类型。只有在需要直接调用服务的 binder 接口（IAxeronService 等）时，
    // 才以相同版本添加：
    // implementation("com.github.fahrez182.Axeron-API:aidl:2.1.1")
}
```

在 `Application` 中启动 broker，然后用 `Axsh` 运行命令：

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

Process process = Axsh.newProcess("-c", "pm list packages");   // 会抛出 IOException
String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
        .lines().collect(Collectors.joining("\n"));
int code = process.waitFor();
```

:::

也可以从 Maven local 引入或作为 Gradle 项目使用，参见 [Axeron API 的 README](https://github.com/fahrez182/Axeron-API#using-it)。

### 权限 {#permission}

应用需要 AxManager 的权限，在 **[特权](./app-tour.md#privilege)** 中按应用设置。设为 **Ask**（默认）时，第一条命令会显示 AxManager 的对话框并最多等待 15 秒。被拒绝时，命令会以 `AxManager permission required: allow this app in AxManager` 停止。

## 运行自己的代码：AxShellService {#shell-service}

`AxShellService` 以 shell 或 root 身份运行应用中的一个类，用法与 [libsu](https://github.com/topjohnwu/libsu) 的 `RootService` 相同，但它通过 axsh 启动，因此在 shell 模式下也能使用。需要 `axsh` 模块（[方式二](#axsh-module)；broker 可选）。

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

### 守护模式 {#daemon-mode}

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

## 有什么区别 {#difference}

### 方式一还是方式二 {#two-ways}

| | AxManager 的 `libaxsh.so` | `axsh` 模块 |
|---|---|---|
| **应用中需要** | 无 | `axsh` 库 |
| **版本** | 始终与已安装的 AxManager 匹配 | 构建时使用的版本 |
| **shell 模式下的每条命令** | 启动一个短暂的助手向 AxManager 请求服务 | 应用的 broker，只请求过一次 |
| **需要 AxManager 运行** | 每条命令都需要 | 仅第一条命令需要 |
| **AxShellService** | 否 | 是 |

### 速度 {#speed}

命令运行前的开销：

| 服务 | AxManager 的 `libaxsh.so` | `axsh` 模块 |
|---|---|---|
| **Root** | 直接连接服务 | 直接连接服务 |
| **Shell** | 每条命令都启动助手并请求 AxManager | 应用内的 broker：只请求 AxManager 一次 |

### AxShellService 与 libsu 的 RootService {#rootservice}

| | libsu `RootService` | `AxShellService` |
|---|---|---|
| **shell 模式** | 否 | 是 |
| **回调** | `onCreate`、`onBind`、`onUnbind`、`onRebind`、`onDestroy` | 相同 |
| **调用** | `bind`、`unbind`、`stop` | 相同 |
| **进程** | 所有服务共用一个，随应用退出 | 相同 |
| **守护模式** | `CATEGORY_DAEMON_MODE` | `AxShellService.CATEGORY_DAEMON_MODE` |
| **自行启动进程** | `bindOrTask`，每次绑定 | `AxShellService.setLauncher`，全局设置一次 |
| **用户** | 全部 | 仅用户 0 |

## 技巧 {#tips}

### 保持 axsh 就绪 {#keep-axsh-ready}

在 shell 模式下使用方式一时，应用第一条命令启动的助手会留下一个很小的原生进程（约 3 MB），之后的命令像连接 broker 一样连接它。可在 AxManager 中按应用设置（**特权 → 你的应用 → 保持 axsh 就绪**，在 Axeron 权限下方）：

- **智能**（默认）：应用使用 axsh 期间保留，5 分钟没有命令后退出。
- **无限制**：保留到服务停止或 AxManager 更新。
- **关闭**：每条命令都启动新的助手。

更改设置会结束当前运行的助手，下一条命令会以新设置启动助手。

### `libaxsh.so` 的行为 {#running-commands}

`libaxsh.so` 接受与 `sh` 相同的参数。从应用中运行时没有终端，因此写入它的内容会成为 shell 的输入，stdout 和 stderr 分别返回，退出码就是 shell 的退出码。`Axsh.newProcess` 的行为相同。

### `Axsh` 的更多用法 {#run-axsh}

::: code-group

```kotlin [Kotlin]
val run = Axsh.exec(arrayOf("ls", "-l"), dir = "/")   // 直接运行程序，外面不套 shell
val term = Axsh.newTerminal(rows = 24, cols = 80)     // 在 PTY 上运行：尺寸变化时调用 setWindowSize()
```

```java [Java]
Process run = Axsh.exec(new String[]{"ls", "-l"}, null, "/");   // 直接运行程序，外面不套 shell
Axsh.Terminal term = Axsh.newTerminal(24, 80);                 // 在 PTY 上运行：尺寸变化时调用 setWindowSize()
```

:::

### 应用自己的 `libaxsh.so` {#run-libaxsh}

使用方式二时，也可以像方式一那样运行模块自带的 `libaxsh.so`，用于需要一个程序而不是 `Process` 的场景，例如终端模拟器。这时命令会经过你的 broker。它从应用的原生库文件夹中运行，因此需要保持原生库解压：

```kotlin
// build.gradle.kts：仅在运行应用自己的 libaxsh.so 时需要
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

`Axsh` 和 AxShellService 不需要这个：它们不会从该文件夹运行任何东西。

### 用 libsu 保持一个 shell 打开 {#libsu}

如果你已经在使用 [libsu](https://github.com/topjohnwu/libsu)，它可以保持一个 axsh shell 打开，并把每条命令都发送给它：

::: code-group

```kotlin [Kotlin]
Shell.setDefaultBuilder(Shell.Builder.create().setCommands(axsh.path))   // 方式一
val shell = Shell.Builder.create().build(Axsh.newProcess())               // 方式二
```

```java [Java]
Shell.setDefaultBuilder(Shell.Builder.create().setCommands(axsh.getPath()));   // 方式一
Shell shell = Shell.Builder.create().build(Axsh.newProcess());                 // 方式二
```

:::

这样命令会在这一个 shell 上排队，`cd` 和变量会在命令之间延续，除非每条命令都在 `( … )` 中运行。

### 在 Java 中使用 {#java}

每个示例都有 Java 标签页。`AxBroker`、`Axsh` 和 `AxShellService` 都以静态方式调用，`Axsh` 的方法会抛出受检异常 `IOException`。2.1.0 及之前的名称（`Broker`、`ShellService`）只是 Kotlin 的别名，因此 Java 中请使用 `Ax` 开头的名称。Java 不会阻止调用 axsh 的其他公开类，但它们属于 AxManager 自身，可能随时变化。

### AxShellService {#shell-service-tips}

- 服务的 Context 报告的是该 uid 自己的包名（shell 模式下为 `com.android.shell`），因此会检查它的系统服务（例如剪贴板）会接受它。
- `getApplicationContext()` 返回服务本身：其进程中没有运行 `Application`。
- 在可调试的应用中，Android Studio 的 **Attach Debugger** 会将该进程列为 `<package>:shell_shared` 或 `<package>:shell_daemon`。
- 绑定失败时，`AxShellService.lastFailure(component)` 会说明原因。

## 演示应用 {#demo-app}

[Axeron API](https://github.com/fahrez182/Axeron-API) 中的 `demo-axsh` 是以上所有内容的测试台。构建并安装它，在 AxManager 服务运行时点击 **Run all**。

| 设置 | 选项 |
|---|---|
| **`axsh_direct_connection`** | 开：使用自己的 `libaxsh.so` 和 broker（`axsh` 模块）。关：使用 AxManager 的 `libaxsh.so`，不使用 broker，与 Termux 的运行方式相同。切换会重启应用。 |
| **Runner** | 每条命令一个 `axsh -c`，或 libsu 缓存的 shell（可以关闭它以重新计时启动）。 |

测试：

| 分组 | 检查内容 |
|---|---|
| **Connection** | 服务有响应；五次运行的延迟；同时运行四条命令；命令之间不延续任何状态 |
| **Shell** | 退出码；stdout 与 stderr 分开；stdin 直到结束；100 000 行输出；服务的环境变量 |
| **Privileges** | uid 和 SELinux 上下文；`pm list packages`；`settings`；`dumpsys` |
| **Test Function plugin** | 已安装并启用；`axtest` 在 `PATH` 中并可从应用调用；未被标记为插件进程；它的 `system.prop`；插件进程；其报告中的每个 `[FAIL]` |
| **libsu RootService** | 通过 axsh 启动的 libsu `RootService`，用于对比（仅 root 模式） |
| **AxShellService** | uid；服务内的 `Runtime.exec`；两个服务在同一进程中；共享的静态变量；从其 Context 访问剪贴板；`getApplicationContext()` |
| **AxShellService daemon** | 解绑后、关闭应用后 pid 不变；`stop` 后 pid 改变 |

最后一组需要安装 **Test Function** 插件（AxManager 仓库中的 `plugins/test_function`）并重新点燃。它说明插件 `system/bin` 中的程序可以被任何获准的应用调用。

将 **Latency** 在 `axsh_direct_connection` 开和关、以及两种 Runner 下进行比较，就能看到每条路径在你的设备上的开销。
