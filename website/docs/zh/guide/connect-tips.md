# <Icon name="tips" /> 技巧与演示应用

关于[与应用连接](./connect-app.md)的两种方式的更多内容：它们的区别、各自的开销，以及测试它们的演示应用。

[[toc]]

## 方式一还是方式二 {#two-ways}

| | AxManager 的 `libaxsh.so` | `axsh` 模块 |
|---|---|---|
| **应用中需要** | 一条 `<queries>` 声明 | `axsh` 库 |
| **版本** | 始终与已安装的 AxManager 匹配 | 构建时使用的版本 |
| **shell 模式下的每条命令** | 启动一个短暂的助手，通过 AxManager 取得应用自己的连接 | 应用的 broker，只取得过一次 |
| **需要 AxManager 运行** | shell 模式下每条命令都需要 | 不需要：已安装即可，且服务在运行 |
| **请求权限** | 第一条命令显示对话框 | `AxBroker.requestAccess` |
| **AxShellService** | 否 | 是 |

## 速度 {#speed}

命令运行前的开销：

| 服务 | AxManager 的 `libaxsh.so` | `axsh` 模块 |
|---|---|---|
| **Root** | 助手告知服务的位置后（服务启动后的第一条命令之后）直接连接服务 | 直接连接服务 |
| **Shell** | 每条命令都启动助手并请求 AxManager（见下文） | 应用内的 broker，只取得过一次连接 |

### 保持 axsh 就绪 {#keep-axsh-ready}

在 shell 模式下使用方式一时，应用第一条命令启动的助手会留下一个很小的原生进程（约 3 MB），之后的命令像连接 broker 一样连接它。可在 AxManager 中按应用设置（**特权 → 你的应用 → 保持 axsh 就绪**，在 Axeron 权限下方）：

- **智能**（默认）：应用使用 axsh 期间保留，5 分钟没有命令后退出。
- **无限制**：保留到服务停止或 AxManager 更新。
- **关闭**：每条命令都启动新的助手。

更改设置会结束当前运行的助手，下一条命令会以新设置启动助手。

## 运行命令 {#commands}

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

## 演示应用 {#demo-app}

[Axeron API](https://github.com/fahrez182/Axeron-API) 中的 `demo-axsh` 是以上所有内容的测试台。它在任何地方都不写 AxManager 的包名或内部 action：它像你的应用一样，用 `AxBroker.findManager` 找到 AxManager。构建并安装它，在 AxManager 服务运行时在 AxManager 中允许它，然后点击 **Run all**。

| 设置 | 选项 |
|---|---|
| **`axsh_direct_connection`** | 开：使用自己的 `libaxsh.so` 和 broker（`axsh` 模块）。关：使用 AxManager 的 `libaxsh.so`，不使用 broker，与 Termux 的运行方式相同（此时 AxShellService 通过用 `setLauncher` 设置的那个 `libaxsh.so` 启动）。切换会重启应用。使用 `Axsh.exec` 的 Cold start 测试只在开启时运行。 |
| **Runner** | 每条命令一个 `axsh -c`，或 libsu 缓存的 shell（可以关闭它以重新计时启动）。 |

测试：

| 分组 | 检查内容 |
|---|---|
| **Cold start** | 第一条命令与之后命令的耗时；`Axsh.exec` 与本地 `Runtime.exec` 的对比；`AxBroker.findManager` 的耗时 |
| **Security** | 其他应用可能做的尝试，由演示应用自己来做：连接 3.0 之前的服务 socket `@axeron`（没有人在监听）、通过自己的 axsh provider 植入一个 binder（被拒绝：只接受 root 或 shell）、以及 `AxBroker.findManager` 能看到什么（隐身模式下什么也看不到） |
| **Connection** | 服务有响应；五次运行的延迟；同时运行四条命令；命令之间不延续任何状态 |
| **Shell** | 退出码；stdout 与 stderr 分开；stdin 直到结束；100 000 行输出；服务的环境变量 |
| **Privileges** | uid 和 SELinux 上下文；`pm list packages`；`settings`；`dumpsys` |
| **Test Function plugin** | 已安装并启用；`axtest` 在 `PATH` 中并可从应用调用；未被标记为插件进程；它的 `system.prop`；插件进程；其报告中的每个 `[FAIL]` |
| **libsu RootService** | 通过 axsh 启动的 libsu `RootService`，用于对比（仅 root 模式） |
| **AxShellService** | uid；服务内的 `Runtime.exec`；两个服务在同一进程中；共享的静态变量；从其 Context 访问剪贴板；`getApplicationContext()` |
| **AxShellService daemon** | 解绑后、关闭应用后 pid 不变；`stop` 后 pid 改变 |

**Test Function plugin** 一组需要安装该插件（AxManager 仓库中的 `plugins/test_function`）并重新点燃。它说明插件 `system/bin` 中的程序可以被任何获准的应用调用。

将 **Latency** 在 `axsh_direct_connection` 开和关、以及两种 Runner 下进行比较，就能看到每条路径在你的设备上的开销。
