# <Icon name="link" /> 与应用连接

应用可以通过 [axsh](./axsh.md)，像终端一样以 AxManager 的 shell 或 root 权限运行命令。本页面向应用开发者：两种连接方式、各自的开销、用 `ShellService` 以 shell 或 root 身份运行自己的代码，以及用来测试这一切的演示应用。

[[toc]]

## 两种连接方式 {#two-ways}

| | AxManager 的 `libaxsh.so` | `axsh` 模块 |
|---|---|---|
| **应用中需要** | 无需打包任何东西 | [Axeron API](https://github.com/fahrez182/Axeron-API) 中的 `axsh` 库 |
| **版本** | 始终与已安装的 AxManager 匹配 | 构建时使用的版本 |
| **每条命令开始时** | shell 模式下：每次都会启动一个短暂的助手向 AxManager 请求服务 | 连接到应用自己的 broker |
| **需要 AxManager 运行** | 是：每次都由它响应助手 | 仅第一条命令需要 |

两者都需要 AxManager 的[权限](./axsh.md#permission)。AxManager 的 `libaxsh.so` 与应用自己的副本是同一个客户端；区别在于另一端等待的是什么：为这一条命令启动的助手，还是已经在应用中运行的 broker。使用模块时，`Axsh` 还可以跳过该客户端，直接向 broker 持有的服务发起请求。

### AxManager 的 `libaxsh.so` {#manager-binary}

在清单中声明 AxManager，Android 11+ 才会让应用看到它：

```xml
<queries>
    <package android:name="frb.axeron.manager" />
</queries>
```

然后从 AxManager 的库文件夹运行 `libaxsh.so`。该文件夹会随每次 AxManager 更新而变化，因此请在应用每次启动时重新获取：

```kotlin
val dir = packageManager.getApplicationInfo("frb.axeron.manager", 0).nativeLibraryDir
val axsh = File(dir, "libaxsh.so")

val process = ProcessBuilder(axsh.path, "-c", "id").start()
```

### `axsh` 模块 {#axsh-module}

从 [JitPack](https://jitpack.io/#fahrez182/Axeron-API) 添加该模块（也可以从 Maven local 引入或作为 Gradle 项目，参见 [Axeron API 的 README](https://github.com/fahrez182/Axeron-API#using-it)）：

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
    // axsh 会在运行时自动带上这两个模块，而且它自己的 API（Broker、Axsh、ShellService）
    // 不使用它们的任何类型。只有在需要直接使用其中的类（IAxeronService、AxeronApiConstant 等）
    // 时，才以相同版本添加：
    // implementation("com.github.fahrez182.Axeron-API:aidl:2.1.0")
    // implementation("com.github.fahrez182.Axeron-API:shared:2.1.0")
}
```

在 `Application` 中启动 broker：

```kotlin
import frb.axeron.shell.Broker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Broker.start(this)
    }
}
```

broker 在应用的进程内监听。它只向 AxManager 请求一次服务并保留下来，之后的每条命令都由它处理。之后可以用以下两种方式运行命令。

**`Axsh`，在应用自己的进程中。** 通过一次 binder 调用，在服务上得到一个作为 `java.lang.Process` 的 shell。不会执行任何程序，因此无需解压原生库。请在主线程之外调用：

```kotlin
import frb.axeron.shell.Axsh

val process = Axsh.newProcess("-c", "id")             // sh 的参数；不带参数则为读取 stdin 的 shell
val run = Axsh.exec(arrayOf("ls", "-l"), dir = "/")   // 直接运行程序，外面不套 shell
val term = Axsh.newTerminal(rows = 24, cols = 80)     // 在 PTY 上运行：尺寸变化时调用 setWindowSize()
```

用于 libsu：`Shell.Builder.create().build(Axsh.newProcess())`。

**应用自己的 `libaxsh.so`，作为程序运行。** 适用于需要一个可执行文件而不是 `Process` 的场景：终端模拟器、libsu 的 `setCommands`、其他进程。它从应用的原生库文件夹中运行，因此需要保持原生库解压：

```kotlin
// build.gradle.kts：仅在自己运行 libaxsh.so 时需要
android {
    packaging { jniLibs { useLegacyPackaging = true } }
}
```

```kotlin
val axsh = File(applicationInfo.nativeLibraryDir, "libaxsh.so")
```

[ShellService](#shell-service) 同样不需要 `useLegacyPackaging`：它通过 `Axsh` 启动进程；broker 未运行时则通过 AxManager 的 `libaxsh.so`，从不使用应用自己的副本。

## 运行命令 {#running-commands}

`libaxsh.so` 接受与 `sh` 相同的参数。从应用中运行时没有终端，因此：

- 写入它的内容会成为 shell 的输入；
- stdout 和 stderr 分别返回；
- 退出码就是 shell 的退出码。

可以每条命令启动一个进程，也可以用 [libsu](https://github.com/topjohnwu/libsu) 保持一个 shell 打开：

```kotlin
Shell.setDefaultBuilder(Shell.Builder.create().setCommands(axsh.path))

Shell.cmd("pm list packages").exec()
```

| | 每条命令一个进程 | libsu |
|---|---|---|
| **每条命令** | 一个新的 axsh 会话 | 写入已打开的 shell |
| **同时多条** | 各自拥有自己的 shell | 在同一个 shell 上排队 |
| **状态**（`cd`、变量） | 不会延续 | 会延续，除非每条命令都在 `( … )` 中运行 |

## 速度 {#speed}

命令运行前的开销取决于连接方式，对于 AxManager 的 `libaxsh.so` 还取决于服务的启动方式：

| 服务 | AxManager 的 `libaxsh.so` | `axsh` 模块 |
|---|---|---|
| **Root** | 直接连接服务 | 直接连接服务 |
| **Shell** | 每条命令都启动助手并请求 AxManager | 应用内的 broker：只请求 AxManager 一次 |

::: tip 保持 axsh 就绪
在 shell 模式下使用 AxManager 的 `libaxsh.so` 时，应用第一条命令启动的助手会留下一个很小的原生进程（约 3 MB），之后的命令像连接 broker 一样连接它。可在 AxManager 中按应用设置（**特权 → 你的应用 → 保持 axsh 就绪**，在 Axeron 权限下方）：

- **智能**（默认）：应用使用 axsh 期间保留，5 分钟没有命令后退出。
- **无限制**：保留到服务停止或 AxManager 更新。
- **关闭**：每条命令都启动新的助手。

更改设置会结束当前运行的助手，下一条命令会以新设置启动助手。
:::

## 权限 {#permission}

应用需要 AxManager 的权限，在 **[特权](./app-tour.md#privilege)** 中按应用设置。设为 **Ask**（默认）时，第一条命令会显示 AxManager 的对话框并等待你的回答，最多 15 秒。之后命令会运行，或以 `AxManager permission required: allow this app in AxManager` 停止。

## 运行自己的代码：ShellService {#shell-service}

只运行命令有时不够。`ShellService` 以 shell 或 root 身份运行应用中的一个类，用法与 [libsu](https://github.com/topjohnwu/libsu) 的 `RootService` 相同，但它通过 axsh 启动，因此在 shell 模式下也能使用。需要 `axsh` 模块（broker 可选）。

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

在应用的主线程中绑定它：

```kotlin
val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        val service = IMyService.Stub.asInterface(binder)
        // 不要在主线程调用：service.exec("id")
    }

    override fun onServiceDisconnected(name: ComponentName) {}
}

ShellService.bind(Intent(context, MyService::class.java), connection)
// 用完后：
ShellService.unbind(connection)
```

清单中无需添加任何内容。

| | libsu `RootService` | `ShellService` |
|---|---|---|
| **shell 模式** | 否 | 是 |
| **回调** | `onCreate`、`onBind`、`onUnbind`、`onRebind`、`onDestroy` | 相同 |
| **调用** | `bind`、`unbind`、`stop` | 相同 |
| **进程** | 所有服务共用一个，随应用退出 | 相同 |
| **守护模式** | `CATEGORY_DAEMON_MODE` | `ShellService.CATEGORY_DAEMON_MODE` |
| **自行启动进程** | `bindOrTask`，每次绑定 | `ShellService.setLauncher`，全局设置一次 |
| **用户** | 全部 | 仅用户 0 |

### 守护模式 {#daemon-mode}

要让服务在应用关闭后继续运行，绑定时加上守护类别，并在 `onUnbind` 中返回 `true`：

```kotlin
class MyDaemon : ShellService() {
    override fun onUnbind(intent: Intent): Boolean {
        return true
    }

    override fun onRebind(intent: Intent) {
        // 之后的绑定到达了同一个实例
    }

    override fun onBind(intent: Intent): IBinder { /* … */ }
}

val intent = Intent(context, MyDaemon::class.java)
intent.addCategory(ShellService.CATEGORY_DAEMON_MODE)
ShellService.bind(intent, connection)
```

它运行在第二个进程中，该进程比应用活得更久。下一次绑定（即使应用已重启）会到达同一个实例。`ShellService.stop(intent)` 会结束它。

::: tip 须知
- 服务的 Context 报告的是该 uid 自己的包名（shell 模式下为 `com.android.shell`），因此会检查它的系统服务（例如剪贴板）会接受它。
- `getApplicationContext()` 返回服务本身：其进程中没有运行 `Application`。
- 在可调试的应用中，Android Studio 的 **Attach Debugger** 会将该进程列为 `<package>:shell_shared` 或 `<package>:shell_daemon`。
- 绑定失败时，`ShellService.lastFailure(component)` 会说明原因。
:::

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
| **ShellService** | uid；服务内的 `Runtime.exec`；两个服务在同一进程中；共享的静态变量；从其 Context 访问剪贴板；`getApplicationContext()` |
| **ShellService daemon** | 解绑后、关闭应用后 pid 不变；`stop` 后 pid 改变 |

最后一组需要安装 **Test Function** 插件（AxManager 仓库中的 `plugins/test_function`）并重新点燃。它说明插件 `system/bin` 中的程序可以被任何获准的应用调用。

将 **Latency** 在 `axsh_direct_connection` 开和关、以及两种 Runner 下进行比较，就能看到每条路径在你的设备上的开销。
