# <Icon name="plugin" /> Plugin Installer

An AxManager plugin installer is a `root` module packaged in a ZIP file that can be flashed in the AxManager. The simplest AxManager plugin installer is just a `root` module packed as a ZIP file.

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

## Customize

If you need to customize the module installation process, optionally you can create a script in the installer named `customize.sh`. This script will be **sourced** (not executed) by the module installer script after all files are extracted and default permissions and secontext are applied. This is very useful if your module requires additional setup based on the device ABI, or you need to set special permissions/secontext for some of your module files.

If you would like to fully control and customize the installation process, declare `SKIPUNZIP=1` in `customize.sh` to skip all default installation steps. By doing so, your `customize.sh` will be responsible to install everything by itself.

The customize.sh script runs in AxManager's BusyBox ash shell with Standalone Mode enabled. The following variables and functions are available:

## Variables

- `AXERON` (bool): a variable to mark that the script is running in the AxManager environment, and the value of this variable will always be true. You can use it to distinguish between AxManager, KernelSU and Magisk.
- `AXERONVER` (int): the version int of currently running AxManager Server (e.g. 30000).
- `BOOTMODE` (bool): always be true in AxManager.
- `MODPATH` (path): the path where your module files should be installed.
- `TMPDIR` (path): a place where you can temporarily store files.
- `ZIPFILE` (path): your module's installation ZIP.
- `ARCH` (string): the CPU architecture of the device. Value is either arm, arm64, x86, or x64.
- `IS64BIT` (bool): true if $ARCH is either arm64 or x64.
- `API` (int): the API level (Android version) of the device (e.g., 23 for Android 6.0).

## Functions

```
ui_print <msg>
    print <msg> to console
    Avoid using 'echo' as it will not display in custom recovery's console

abort <msg>
    print error message <msg> to console and terminate the installation
    Avoid using 'exit' as it will skip the termination cleanup steps

set_perm <target> <owner> <group> <permission> [context]
    this function is a shorthand for the following commands:
       chown owner:group target    (best effort, a failure is ignored)
       chmod permission target     (always applied; set_perm fails if this fails)
       chcon context target        (only if [context] is set)
    if [context] is not set, the SELinux context is left unchanged

set_perm_recursive <directory> <owner> <group> <dirpermission> <filepermission> [context]
    for all files and symlinks in <directory>, it will call:
       set_perm file owner group filepermission context
    for all directories in <directory> (including itself), it will call:
       set_perm dir owner group dirpermission context
```
