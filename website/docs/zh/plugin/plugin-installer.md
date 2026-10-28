# <Icon name="plugin" /> 插件安装程序

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

## 自定义 {#customize}

如果您需要自定义模块安装过程，可以选择在安装程序中创建一个名为 `customize.sh` 的脚本。此脚本将在所有文件被提取并应用默认权限和安全上下文后，由模块安装程序脚本**source**（而不是执行）。如果您的模块需要根据设备 ABI 进行额外设置，或者您需要为某些模块文件设置特殊权限/安全上下文，这非常有用。

如果您想完全控制和自定义安装过程，请在 `customize.sh` 中声明 `SKIPUNZIP=1` 以跳过所有默认安装步骤。这样，您的 `customize.sh` 将负责自行安装所有内容。

customize.sh 脚本在启用独立模式的 AxManager BusyBox ash shell 中运行。以下变量和函数可用：

## 变量

- `AXERON` (bool)：一个标记脚本在 AxManager 环境中运行的变量，此变量的值将始终为 true。您可以使用它来区分 AxManager、KernelSU 和 Magisk。
- `AXERONVER` (int)：当前运行的 AxManager 服务器的版本整数（例如 30000）。
- `BOOTMODE` (bool)：在 AxManager 中始终为 true。
- `MODPATH` (path)：应该安装模块文件的路径。
- `TMPDIR` (path)：您可以临时存储文件的地方。
- `ZIPFILE` (path)：模块的安装 ZIP。
- `ARCH` (string)：设备的 CPU 架构。值为 arm、arm64、x86 或 x64。
- `IS64BIT` (bool)：如果 $ARCH 是 arm64 或 x64，则为 true。
- `API` (int)：设备的 API 级别（Android 版本）（例如，Android 6.0 为 23）。

## 函数

```
ui_print <msg>
    将 <msg> 打印到控制台
    避免使用 'echo'，因为它不会在自定义恢复的控制台中显示

abort <msg>
    将错误消息 <msg> 打印到控制台并终止安装
    避免使用 'exit'，因为它会跳过终止清理步骤

set_perm <target> <owner> <group> <permission> [context]
    此函数是以下命令的简写：
       chown owner:group target    （尽力而为，失败会被忽略）
       chmod permission target     （始终执行；失败时 set_perm 返回失败）
       chcon context target        （仅在设置了 [context] 时执行）
    如果未设置 [context]，SELinux 上下文保持不变

set_perm_recursive <directory> <owner> <group> <dirpermission> <filepermission> [context]
    对于 <directory> 中的所有文件和符号链接，它将调用：
       set_perm file owner group filepermission context
    对于 <directory> 中的所有目录（包括其自身），它将调用：
       set_perm dir owner group dirpermission context
```
