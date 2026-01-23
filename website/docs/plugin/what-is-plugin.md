# Plugin (Unrooted Module)

::: warning Conclusion
**Know your limits**
:::

[[toc]]

## Background

The Plugin concept is built on the same architecture as root modules. It is not intended as a replacement, but rather as an **additional feature** that introduces an unrooted version within **AxManager**.

At this stage, the idea serves as an initial exploration: extending the root-module foundation into a form that can operate without root access, while still remaining part of the same modular framework.

In the future, this approach can be further developed to provide broader functionality and new use cases, balancing between rooted and unrooted environments.

## WebUI

AxManager's plugins support displaying interfaces and interacting with users same as **KernelSU** but without root access. For more details, refer to the [KernelSU WebUI documentation](https://kernelsu.org/guide/module-webui.html).

::: info DIFFERENCE WITH KERNELSU
AxManager is support **KernelSU** API and only differs in permissions and how to treat local directories from `webroot/index.html`
:::

## BusyBox

AxManager ships with a feature-complete BusyBox binary (including full SELinux support). The executable is located at `/data/user_de/0/com.android.shell/axeron/bin/busybox`,
AxManager's BusyBox supports runtime toggle-able "ASH Standalone Shell Mode". What this Standalone Mode means is that when running in the ash shell of BusyBox, every single command will directly use the applet within BusyBox, regardless of what is set as `PATH`. For example, commands like `ls`, `rm`, `chmod` will **NOT** use what is in PATH (in the case of Android by default it will be `/system/bin/ls`, `/system/bin/rm`, and `/system/bin/chmod` respectively), but will instead directly call internal BusyBox applets. This makes sure that scripts always run in a predictable environment and always have the full suite of commands no matter which Android version it is running on. To force a command not to use BusyBox, you have to call the executable with full paths.

Every single shell script running in the context of AxManager will be executed in BusyBox's `ash` shell with Standalone Mode enabled. For what is relevant to 3rd party developers, this includes all boot scripts and module installation scripts.

::: info DIFFERENCE WITH MAGISK / KERNELSU
AxManager's BusyBox is now using the binary file compiled directly from the Magisk project. Thanks to Magisk! Therefore, you don't need to worry about compatibility issues between BusyBox scripts in Magisk, KernelSU and AxManager, as they're exactly the same!
:::

## AxManager plugins

A AxManager plugins is a folder placed in `/data/user_de/0/com.android.shell/axeron/plugins` with the structure below:

```
/data/user_de/0/com.android.shell/axeron/plugins
├── .
├── .
│
├── $MODID                  <--- The folder is named with the ID of the module
│   │
│   │      *** Module Identity ***
│   │
│   ├── module.prop         <--- This file stores the metadata of the module
│   │
│   │      *** Main Contents ***
│   │
│   ├── system
│   │   └── bin             <--- This folder will be added on PATH
│   │
│   │      *** Status Flags ***
│   │
│   ├── disable             <--- If exists, the module will be disabled
│   ├── remove              <--- If exists, the module will be removed next reboot
│   │
│   │      *** Optional Files ***
│   │
│   ├── post-fs-data.sh     <--- This script will be executed in BOOT_COMPLETED first sync
│   ├── service.sh          <--- This script will be executed in BOOT_COMPLETED late_start service
│   ├── uninstall.sh        <--- This script will be executed when AxManager removes your module
│   ├── action.sh           <--- This script will be executed when user click the Action button in AxManager app
│   ├── system.prop         <--- Properties in this file will be loaded as system properties by setprop (debug only)
│   │
│   │      *** Any additional files / folders are allowed ***
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

::: warning DIFFERENCE WITH ROOT METHOD

AxManager uses the **BOOT_COMPLETED** receiver and **Hot Restart System** approach to start `post-fs-data.sh`, `system.prop`, `late_start service.sh`, and all other files are treated the same in root mode. except the root init method.
:::

### module.prop

module.prop is a configuration file for a module. In AxManager, if a module doesn't contain this file, it won't be recognized as a module. The format of this file is as follows:

```prop{7}
id=<string>
name=<string>
version=<string>
versionCode=<int>
author=<string>
description=<string>
axeronPlugin=<int> <--- A field for targeting AxManager server version supported
```

- `id` has to match this regular expression: `^[a-zA-Z][a-zA-Z0-9._-]+$`
  Example: ✓ `a_module`, ✓ `a.module`, ✓ `module-101`, ✗ `a module`, ✗ `1_module`, ✗ `-a-module`
  This is the **unique identifier** of your module. You should not change it once published.
- `versionCode` has to be an **integer**. This is used to compare versions.
- Others that were not mentioned above can be any **single line string.**
- Make sure to use the `UNIX (LF)` line break type and not the `Windows (CR+LF)` or `Macintosh (CR)`.

::: warning
Make sure the `axeronPlugin=version` you are using is **equal to or lower** than your AxManager **server version**. If the **server version** is lower than `axeronPlugin=version`, you **won’t** be able to flash it on AxManager.
:::

### Shell scripts

In all scripts of your module, please use `MODDIR=${0%/*}` to get your module's base directory path **EXCEPT** `customize.sh`; do **NOT** hardcode your module path in scripts.

::: warning DIFFERENCE WITH ROOT METHOD
You can use the environment variable `AXERON` to determine if a script is running in AxManager, KernelSU or Magisk. If running in AxManager, this value will be set to true.
:::

### `system` directory

::: warning DIFFERENCE WITH ROOT METHOD

AxManager **for now** only affects `/system/bin` by adding it to the `PATH`, so binaries there can be called directly in any script.
:::

## Plugin installer

A AxManager plugin installer is a `root` module packaged in a ZIP file that can be flashed in the AxManager. The simplest AxManager plugin installer is just a `root` module packed as a ZIP file.

```
module.zip
│
├── customize.sh                       <--- (Optional, more details later)
│
├── ...
├── ...  /* The rest of module's files */
│
```

::: warning

KernelSU modules are NOT compatible for installation through custom Recovery.

AxManager plugins strictly follow the behavior of the underlying `root` module being used.
:::

### Customize

If you need to customize the module installation process, optionally you can create a script in the installer named `customize.sh`. This script will be **sourced** (not executed) by the module installer script after all files are extracted and default permissions and secontext are applied. This is very useful if your module requires additional setup based on the device ABI, or you need to set special permissions/secontext for some of your module files.

If you would like to fully control and customize the installation process, declare `SKIPUNZIP=1` in `customize.sh` to skip all default installation steps. By doing so, your `customize.sh` will be responsible to install everything by itself.

The customize.sh script runs in AxManager's BusyBox ash shell with Standalone Mode enabled. The following variables and functions are available:

#### Variables

- `AXERON` (bool): a variable to mark that the script is running in the AxManager environment, and the value of this variable will always be true. You can use it to distinguish between AxManager, KernelSU and Magisk.
- `AXERONVER` (int): the version int of currently running AxManager Server (e.g. 10400).
- `BOOTMODE` (bool): always be true in AxManager.
- `MODPATH` (path): the path where your module files should be installed.
- `TMPDIR` (path): a place where you can temporarily store files.
- `ZIPFILE` (path): your module's installation ZIP.
- `ARCH` (string): the CPU architecture of the device. Value is either arm, arm64, x86, or x64.
- `IS64BIT` (bool): true if $ARCH is either arm64 or x64.
- `API` (int): the API level (Android version) of the device (e.g., 23 for Android 6.0).

#### Functions

```
ui_print <msg>
    print <msg> to console
    Avoid using 'echo' as it will not display in custom recovery's console

abort <msg>
    print error message <msg> to console and terminate the installation
    Avoid using 'exit' as it will skip the termination cleanup steps

set_perm <target> <owner> <group> <permission> [context]
    if [context] is not set, the default is "u:object_r:system_file:s0"
    this function is a shorthand for the following commands:
       chown owner.group target
       chmod permission target
       chcon context target

set_perm_recursive <directory> <owner> <group> <dirpermission> <filepermission> [context]
    if [context] is not set, the default is "u:object_r:system_file:s0"
    for all files in <directory>, it will call:
       set_perm file owner group filepermission context
    for all directories in <directory> (including itself), it will call:
       set_perm dir owner group dirpermission context
```
