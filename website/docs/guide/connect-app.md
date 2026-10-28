# <Icon name="link" /> Connect with an App

Your app can run commands as AxManager's shell or root through [axsh](./axsh.md), like a terminal does. This page is for app developers.

::: warning 3.0 changed how apps connect
Apps built on the `axsh` library before 3.0 find no server on AxManager 3.0. Use `3.0.0` or later. See [Security in 3.0](./security.md).
:::

[[toc]]

## How to connect {#connect}

Pick one of two ways. Either way, run commands off the main thread, and your app has to be allowed in AxManager (see [Getting access](#access)). Neither needs AxManager's package name.

### Option 1: AxManager's `libaxsh.so` {#manager-binary}

Nothing to add to your app but a query, so it can find AxManager by the door AxManager opens to apps:

```xml
<queries>
    <intent>
        <action android:name="AxRuntime.REQUEST_ACCESS" />
    </intent>
</queries>
```

Then run AxManager's `libaxsh.so` like `sh`. Look its path up each time your app starts, since it changes with every AxManager update:

::: code-group

```kotlin [Kotlin]
val manager = packageManager.resolveActivity(Intent("AxRuntime.REQUEST_ACCESS"), 0)
    ?.activityInfo?.applicationInfo                     // null: not installed, or in stealth
val axsh = File(manager!!.nativeLibraryDir, "libaxsh.so")

val process = Runtime.getRuntime().exec(arrayOf(axsh.path, "-c", "pm list packages"))
val output = process.inputStream.bufferedReader().readText()
val code = process.waitFor()
```

```java [Java]
ResolveInfo info = getPackageManager().resolveActivity(new Intent("AxRuntime.REQUEST_ACCESS"), 0);
// null: not installed, or in stealth
File axsh = new File(info.activityInfo.applicationInfo.nativeLibraryDir, "libaxsh.so");

Process process = Runtime.getRuntime().exec(new String[]{axsh.getPath(), "-c", "pm list packages"});
String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
        .lines().collect(Collectors.joining("\n"));
int code = process.waitFor();
```

:::

With [AX-Stealth Mode](./app-tour.md#environment) on, AxManager doesn't show itself, so the lookup finds nothing until your app is allowed some other way.

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
    implementation("com.github.fahrez182.Axeron-API:axsh:3.0.0")
    // axsh brings aidl along at runtime, and its own API (AxBroker, Axsh, AxShellService) uses
    // none of its types. Add it, at the same version, only to call the server's binder
    // interfaces directly (IAxeronService, …).
    // implementation("com.github.fahrez182.Axeron-API:aidl:3.0.0")
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

Nothing is published for apps to pick up. Once your app is allowed, the server hands it its own connection, which works for your app alone; the broker takes it, checks that it really comes from the server, and keeps it. A command right after your app starts waits up to 3 seconds for it. AxManager only has to be installed, because the server runs from it, and the server has to be running.

The library also finds AxManager for you, with no package name and no manifest entry of your own: `AxBroker.findManager(context)` returns its `ApplicationInfo`, or `null` when it isn't installed or is in stealth.

Maven local and Gradle projects work too, see the [Axeron API README](https://github.com/fahrez182/Axeron-API#using-it).

## Getting access {#access}

Your app needs AxManager's permission, set per app in **[Privilege](./privilege.md)**: **Allowed**, **Ask** (the default) or **Denied**.

With **option 2**, an app that isn't allowed yet is given nothing, so it asks first. `AxBroker.requestAccess` opens AxManager's permission dialog; the answer comes back to your activity:

::: code-group

```kotlin [Kotlin]
if (!AxBroker.requestAccess(activity, REQUEST_AXMANAGER)) {
    // AxManager isn't installed, or is in stealth mode
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_AXMANAGER && resultCode == RESULT_OK) {
        // allowed: the server hands this app its connection by itself
    }
}
```

```java [Java]
if (!AxBroker.requestAccess(activity, REQUEST_AXMANAGER)) {
    // AxManager isn't installed, or is in stealth mode
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_AXMANAGER && resultCode == RESULT_OK) {
        // allowed: the server hands this app its connection by itself
    }
}
```

:::

An app already allowed gets `RESULT_OK` at once, without a dialog; a **Denied** one gets `RESULT_CANCELED`. With **option 1**, the first command asks instead, like a terminal: AxManager shows its dialog and the command waits up to 15 seconds. Denied, it stops with `AxManager permission required: allow this app in AxManager`.

The permission belongs to your app's signing certificate. A build signed with another key, such as a debug build after a release one, is asked again.

## Next {#next}

- **[AxShellService](./shell-service.md)**: run a class of your app as shell or root, like libsu's `RootService`.
- **[Tips & Demo App](./connect-tips.md)**: which option to pick, what each costs, more from `Axsh`, and the demo app that tests it all.
