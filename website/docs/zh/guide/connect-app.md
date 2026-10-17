# <Icon name="link" /> 与应用连接

应用可以通过 [axsh](./axsh.md)，像终端一样以 AxManager 的 shell 或 root 权限运行命令。本页面向应用开发者：两种连接方式、各自的开销，以及用来测试两者的演示应用。

[[toc]]

## 两种连接方式 {#two-ways}

| | AxManager 的 `libaxsh.so` | `axsh` 模块 |
|---|---|---|
| **应用中需要** | 无需打包任何东西 | [Axeron API](https://github.com/fahrez182/Axeron-API) 中的 `axsh` 库 |
| **版本** | 始终与已安装的 AxManager 匹配 | 构建时使用的版本 |
| **每条命令开始时** | shell 模式下：每次都会启动一个短暂的助手向 AxManager 请求服务 | 连接到应用自己的 broker |
| **需要 AxManager 运行** | 是：每次都由它响应助手 | 仅第一条命令需要 |

两者运行的是同一个 `libaxsh.so` 客户端，也都需要 AxManager 的[权限](./axsh.md#permission)。区别在于另一端等待的是什么：为这一条命令启动的助手，还是已经在应用中运行的 broker。

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

添加该模块（作为 Gradle 项目，或从 Maven local，参见 Axeron API 的 README），并保持原生库解压，因为 `libaxsh.so` 是作为程序运行的：

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":axsh"))   // 或从 mavenLocal() 使用 "dev.frb.axeron:axsh:<version>"
}
android {
    packaging { jniLibs { useLegacyPackaging = true } }
}
```

在 `Application` 中启动 broker，并运行应用自己的 `libaxsh.so`：

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

broker 在应用的进程内监听。它只向 AxManager 请求一次服务并保留下来，之后的每条命令都由它处理。

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

最后一组需要安装 **Test Function** 插件（AxManager 仓库中的 `plugins/test_function`）并重新点燃。它说明插件 `system/bin` 中的程序可以被任何获准的应用调用。

将 **Latency** 在 `axsh_direct_connection` 开和关、以及两种 Runner 下进行比较，就能看到每条路径在你的设备上的开销。
