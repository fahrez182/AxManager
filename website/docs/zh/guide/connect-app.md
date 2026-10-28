# <Icon name="link" /> 与应用连接

你的应用可以通过 [axsh](./axsh.md)，像终端一样以 AxManager 的 shell 或 root 权限运行命令。本页面向应用开发者。

::: warning 3.0 改变了应用的连接方式
3.0 之前使用 `axsh` 库构建的应用在 AxManager 3.0 上找不到服务。请使用 `3.0.0` 或更高版本。参见 [3.0 的安全更新](./security.md)。
:::

[[toc]]

## 如何连接 {#connect}

两种方式任选其一。无论哪种，都请在主线程之外运行命令，并且你的应用必须已在 AxManager 中获准（参见[获取权限](#access)）。两种方式都不需要 AxManager 的包名。

### 方式一：AxManager 的 `libaxsh.so` {#manager-binary}

应用中只需添加一条查询声明，通过 AxManager 向应用开放的入口找到它：

```xml
<queries>
    <intent>
        <action android:name="AxRuntime.REQUEST_ACCESS" />
    </intent>
</queries>
```

然后像 `sh` 一样运行 AxManager 的 `libaxsh.so`。它的路径会随每次 AxManager 更新而变化，因此请在应用每次启动时重新获取：

::: code-group

```kotlin [Kotlin]
val manager = packageManager.resolveActivity(Intent("AxRuntime.REQUEST_ACCESS"), 0)
    ?.activityInfo?.applicationInfo                     // null：未安装，或处于隐身模式
val axsh = File(manager!!.nativeLibraryDir, "libaxsh.so")

val process = Runtime.getRuntime().exec(arrayOf(axsh.path, "-c", "pm list packages"))
val output = process.inputStream.bufferedReader().readText()
val code = process.waitFor()
```

```java [Java]
ResolveInfo info = getPackageManager().resolveActivity(new Intent("AxRuntime.REQUEST_ACCESS"), 0);
// null：未安装，或处于隐身模式
File axsh = new File(info.activityInfo.applicationInfo.nativeLibraryDir, "libaxsh.so");

Process process = Runtime.getRuntime().exec(new String[]{axsh.getPath(), "-c", "pm list packages"});
String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
        .lines().collect(Collectors.joining("\n"));
int code = process.waitFor();
```

:::

开启 [AX-Stealth Mode](./app-tour.md#environment) 时，AxManager 不会向应用显示自己，因此在你的应用通过其他方式获准之前，这次查找什么也找不到。

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
    implementation("com.github.fahrez182.Axeron-API:axsh:3.0.0")
    // axsh 会在运行时自动带上 aidl，而且它自己的 API（AxBroker、Axsh、AxShellService）
    // 不使用其中的任何类型。只有在需要直接调用服务的 binder 接口（IAxeronService 等）时，
    // 才以相同版本添加：
    // implementation("com.github.fahrez182.Axeron-API:aidl:3.0.0")
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

服务不会发布任何东西供应用读取。你的应用获准后，服务会把只属于你的应用的连接交给它；broker 接收它，确认它确实来自服务，然后保留下来。应用刚启动时的第一条命令最多会等待 3 秒以接收它。AxManager 只需已安装（服务从它运行），且服务必须在运行。

该库还能帮你找到 AxManager，无需包名，也无需你自己声明清单：`AxBroker.findManager(context)` 返回它的 `ApplicationInfo`；未安装或处于隐身模式时返回 `null`。

也可以从 Maven local 引入或作为 Gradle 项目使用，参见 [Axeron API 的 README](https://github.com/fahrez182/Axeron-API#using-it)。

## 获取权限 {#access}

应用需要 AxManager 的权限，在 **[特权](./privilege.md)** 中按应用设置：**Allowed**、**Ask**（默认）或 **Denied**。

使用**方式二**时，尚未获准的应用什么也拿不到，因此要先请求。`AxBroker.requestAccess` 会打开 AxManager 的权限对话框，结果返回给你的 Activity：

::: code-group

```kotlin [Kotlin]
if (!AxBroker.requestAccess(activity, REQUEST_AXMANAGER)) {
    // 未安装 AxManager，或处于隐身模式
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_AXMANAGER && resultCode == RESULT_OK) {
        // 已获准：服务会自行把连接交给此应用
    }
}
```

```java [Java]
if (!AxBroker.requestAccess(activity, REQUEST_AXMANAGER)) {
    // 未安装 AxManager，或处于隐身模式
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_AXMANAGER && resultCode == RESULT_OK) {
        // 已获准：服务会自行把连接交给此应用
    }
}
```

:::

已获准的应用会立即得到 `RESULT_OK`，不显示对话框；**Denied** 的应用会得到 `RESULT_CANCELED`。使用**方式一**时，则像终端一样由第一条命令请求：AxManager 显示对话框，命令最多等待 15 秒。被拒绝时，命令会以 `AxManager permission required: allow this app in AxManager` 停止。

权限属于应用的签名证书。用其他密钥签名的构建（例如在正式版之后安装的调试版）会被重新询问。

## 下一步 {#next}

- **[AxShellService](./shell-service.md)**：以 shell 或 root 身份运行应用中的一个类，类似 libsu 的 `RootService`。
- **[技巧与演示应用](./connect-tips.md)**：该选哪种方式、各自的开销、`Axsh` 的更多用法，以及测试这一切的演示应用。
