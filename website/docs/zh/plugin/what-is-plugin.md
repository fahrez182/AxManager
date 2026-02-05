# 插件（非 Root 模块）

::: warning 结论
**了解你的限制**
:::

[[toc]]

## 背景

插件概念建立在与 root 模块相同的架构上。它不是作为替代品，而是作为 **AxManager** 中的一个**附加功能**，引入了非 Root 版本。

在这个阶段，这个想法作为一个初步的探索：将 root 模块的基础扩展为一种可以在没有 root 访问权限的情况下运行的形式，同时仍然是同一模块化框架的一部分。

未来，这种方法可以进一步发展，以提供更广泛的功能和新的用例，在 Root 和非 Root 环境之间取得平衡。

## WebUI

AxManager 的插件支持显示界面和与用户交互，与 **KernelSU** 相同，但不需要 root 访问权限。更多详情请参阅 [KernelSU WebUI 文档](https://kernelsu.org/guide/module-webui.html)。

::: info 与 KernelSU 的区别
AxManager 支持 **KernelSU** API，仅在权限和如何处理 `webroot/index.html` 的本地目录方面有所不同
:::

## BusyBox

AxManager 附带一个功能完整的 BusyBox 二进制文件（包括完整的 SELinux 支持）。可执行文件位于 `/data/user_de/0/com.android.shell/axeron/bin/busybox`。

AxManager 的 BusyBox 支持运行时可切换的"ASH 独立 Shell 模式"。这种独立模式意味着当在 BusyBox 的 ash shell 中运行时，每个命令都将直接使用 BusyBox 内部的小程序，而不管 `PATH` 设置为什么。例如，`ls`、`rm`、`chmod` 等命令将**不会**使用 PATH 中的内容（在 Android 的情况下，默认将是 `/system/bin/ls`、`/system/bin/rm` 和 `/system/bin/chmod`），而是直接调用内部 BusyBox 小程序。这确保脚本始终在可预测的环境中运行，并且无论运行在哪个 Android 版本上，都具有完整的命令套件。要强制命令不使用 BusyBox，您必须使用完整路径调用可执行文件。

在 AxManager 上下文中运行的每个 shell 脚本都将在启用独立模式的 BusyBox `ash` shell 中执行。对于第三方开发者相关的内容，这包括所有启动脚本和模块安装脚本。

::: info 与 Magisk / KernelSU 的区别
AxManager 的 BusyBox 现在使用直接从 Magisk 项目编译的二进制文件。感谢 Magisk！因此，您不必担心 Magisk、KernelSU 和 AxManager 之间 BusyBox 脚本的兼容性问题，因为它们完全相同！
:::

## AxManager 插件

AxManager 插件是放置在 `/data/user_de/0/com.android.shell/axeron/plugins` 中的文件夹，结构如下：

```
/data/user_de/0/com.android.shell/axeron/plugins
├── .
├── .
│
├── $MODID                  <--- 文件夹以模块的 ID 命名
│   │
│   │      *** 模块标识 ***
│   │
│   ├── module.prop         <--- 此文件存储模块的元数据
│   │
│   │      *** 主要内容 ***
│   │
│   ├── system
│   │   └── bin             <--- 此文件夹将添加到 PATH
│   │
│   │      *** 状态标志 ***
│   │
│   ├── disable             <--- 如果存在，模块将被禁用
│   ├── remove              <--- 如果存在，模块将在下次重启时被移除
│   │
│   │      *** 可选文件 ***
│   │
│   ├── post-fs-data.sh     <--- 此脚本将在 BOOT_COMPLETED 首次同步时执行
│   ├── service.sh          <--- 此脚本将在 BOOT_COMPLETED late_start service 中执行
│   ├── uninstall.sh        <--- 当 AxManager 移除您的模块时将执行此脚本
│   ├── action.sh           <--- 当用户在 AxManager 应用中点击操作按钮时将执行此脚本
│   ├── system.prop         <--- 此文件中的属性将通过 setprop 加载为系统属性（仅调试）
│   │
│   │      *** 允许任何其他文件/文件夹 ***
│   │
│   ├── ...
│   └── ...
│
├── another_module
│   ├── .
│   └── .
├── .
├── .
```

::: warning 与 Root 方法的区别

AxManager 使用 **BOOT_COMPLETED** 接收器和 **热重启系统** 方法来启动 `post-fs-data.sh`、`system.prop`、`late_start service.sh`，所有其他文件在 root 模式下的处理方式相同。除了 root init 方法。
:::

### module.prop

module.prop 是模块的配置文件。在 AxManager 中，如果模块不包含此文件，它将不会被识别为模块。此文件的格式如下：

```prop{7}
id=<string>
name=<string>
version=<string>
versionCode=<int>
author=<string>
description=<string>
axeronPlugin=<int> <--- 用于定位支持的 AxManager 服务器版本的字段
```

- `id` 必须匹配此正则表达式：`^[a-zA-Z][a-zA-Z0-9._-]+$`
  示例：✓ `a_module`，✓ `a.module`，✓ `module-101`，✗ `a module`，✗ `1_module`，✗ `-a-module`
  这是您模块的**唯一标识符**。一旦发布，您不应更改它。
- `versionCode` 必须是**整数**。这用于比较版本。
- 上面未提到的其他内容可以是任何**单行字符串**。
- 确保使用 `UNIX (LF)` 换行类型，而不是 `Windows (CR+LF)` 或 `Macintosh (CR)`。

::: warning
确保您使用的 `axeronPlugin=version` **等于或低于**您的 AxManager **服务器版本**。如果**服务器版本**低于 `axeronPlugin=version`，您将**无法**在 AxManager 上刷入它。
:::

### Shell 脚本

在模块的所有脚本中，请使用 `MODDIR=${0%/*}` 来获取模块的基本目录路径，**除了** `customize.sh`；请**不要**在脚本中硬编码模块路径。

::: warning 与 Root 方法的区别
您可以使用环境变量 `AXERON` 来确定脚本是在 AxManager、KernelSU 还是 Magisk 中运行。如果在 AxManager 中运行，此值将设置为 true。
:::

### `system` 目录

::: warning 与 Root 方法的区别

AxManager **目前**仅通过将 `/system/bin` 添加到 `PATH` 来影响它，因此那里的二进制文件可以在任何脚本中直接调用。
:::

## 插件安装程序

AxManager 插件安装程序是打包在 ZIP 文件中的 `root` 模块，可以在 AxManager 中刷入。最简单的 AxManager 插件安装程序只是打包为 ZIP 文件的 `root` 模块。

```
module.zip
│
├── customize.sh                       <--- （可选，更多详情见后文）
│
├── ...
├── ...  /* 模块的其余文件 */
│
```

::: warning

KernelSU 模块不兼容通过自定义 Recovery 安装。

AxManager 插件严格遵循所使用的底层 `root` 模块的行为。
:::

### 自定义

如果您需要自定义模块安装过程，可以选择在安装程序中创建一个名为 `customize.sh` 的脚本。此脚本将在所有文件被提取并应用默认权限和安全上下文后，由模块安装程序脚本**source**（而不是执行）。如果您的模块需要根据设备 ABI 进行额外设置，或者您需要为某些模块文件设置特殊权限/安全上下文，这非常有用。

如果您想完全控制和自定义安装过程，请在 `customize.sh` 中声明 `SKIPUNZIP=1` 以跳过所有默认安装步骤。这样，您的 `customize.sh` 将负责自行安装所有内容。

customize.sh 脚本在启用独立模式的 AxManager BusyBox ash shell 中运行。以下变量和函数可用：

#### 变量

- `AXERON` (bool)：一个标记脚本在 AxManager 环境中运行的变量，此变量的值将始终为 true。您可以使用它来区分 AxManager、KernelSU 和 Magisk。
- `AXERONVER` (int)：当前运行的 AxManager 服务器的版本整数（例如 10400）。
- `BOOTMODE` (bool)：在 AxManager 中始终为 true。
- `MODPATH` (path)：应该安装模块文件的路径。
- `TMPDIR` (path)：您可以临时存储文件的地方。
- `ZIPFILE` (path)：模块的安装 ZIP。
- `ARCH` (string)：设备的 CPU 架构。值为 arm、arm64、x86 或 x64。
- `IS64BIT` (bool)：如果 $ARCH 是 arm64 或 x64，则为 true。
- `API` (int)：设备的 API 级别（Android 版本）（例如，Android 6.0 为 23）。

#### 函数

```
ui_print <msg>
    将 <msg> 打印到控制台
    避免使用 'echo'，因为它不会在自定义恢复的控制台中显示

abort <msg>
    将错误消息 <msg> 打印到控制台并终止安装
    避免使用 'exit'，因为它会跳过终止清理步骤

set_perm <target> <owner> <group> <permission> [context]
    如果未设置 [context]，默认为 "u:object_r:system_file:s0"
    此函数是以下命令的简写：
       chown owner.group target
       chmod permission target
       chcon context target

set_perm_recursive <directory> <owner> <group> <dirpermission> <filepermission> [context]
    如果未设置 [context]，默认为 "u:object_r:system_file:s0"
    对于 <directory> 中的所有文件，它将调用：
       set_perm file owner group filepermission context
    对于 <directory> 中的所有目录（包括其自身），它将调用：
       set_perm dir owner group dirpermission context
```