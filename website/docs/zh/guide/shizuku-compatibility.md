# <Icon name="swap" /> Shizuku 兼容

AxManager 可以代替 [Shizuku](https://github.com/RikkaApps/Shizuku)：基于 [Shizuku-API](https://github.com/RikkaApps/Shizuku-API) 构建的应用可以连接到 AxManager 的服务，获得同样的 shell 或 root 权限，而无需运行 Shizuku。

[[toc]]

## 开启 {#turn-it-on}

1. [启动 AxManager](./user-manual.md#start-axeron-manager)。
2. 打开**设置 → AX-权限接管**（默认关闭）。

之后，AxManager 会把它的 Shizuku 兼容服务提供给使用 Shizuku-API 的应用，包括已经在运行的应用。该开关按模式分别保存（root 和 shell 使用[各自独立的文件夹](../plugin/environment.md#working-folder)），服务重启后仍然有效。

## 授权应用 {#granting-apps}

当应用请求 Shizuku 权限时，AxManager 会显示自己的授权请求：**始终允许**、**仅本次允许**或**拒绝**。之后可以在**特权**页面修改（**Shizuku** 筛选会列出基于 Shizuku-API 构建的应用），每个应用可设为 **Allowed**、**Ask** 或 **Denied**（参见 [Axeron permission](./privilege.md#axeron-permission)）。

Shizuku 应用本身没有"询问"这一设置，因此它的操作对应的是 **Ask**，而不是 **Denied**：

- 在 Shizuku 应用中关闭某个应用，会把它设为 **Ask**：该应用不再被允许，但仍可再次请求。
- 在 Shizuku 应用的授权对话框中选择**拒绝**，只会拒绝这一次请求，不会被记住。

只有在 AxManager 的**特权**页面才能把应用设为 **Denied**，此时它的请求会被直接拒绝，不显示对话框。

::: info 开启 AX-Stealth Mode 时
你未允许的 Shizuku 客户端根本拿不到该服务：对它而言，Shizuku 没有运行，请求也不会显示对话框。请在**特权**中（或在 Shizuku 应用自己的列表中）允许它，它会立即获得服务，无需重启。参见 [AX-Stealth Mode](./app-tour.md#environment)。
:::

## 需要安装 Shizuku 应用的情况 {#apps-that-need-the-shizuku-app-installed}

一些较旧的 Shizuku 客户端（例如旧版 ZArchiver）只有在**安装了** Shizuku 应用（`moe.shizuku.privileged.api`）时才会尝试连接。如果这类应用提示找不到 Shizuku：

- 安装 Shizuku 应用即可，**不需要**启动它；由 AxManager 代为提供服务。
- 开启接管且未安装 Shizuku 应用时，**设置**中 AX-权限接管下方会显示这一提示。

## Shizuku 应用中显示的内容 {#what-the-shizuku-app-shows}

如果安装了 Shizuku 应用，它也会连接到 AxManager：

- 它会显示服务**正在运行**，版本为 `13.<AxManager 版本号>`（例如 `13.30000`）。点号后面的部分是 AxManager 的版本号，可以据此判断是 AxManager 在提供服务。
- 由于这个数字与任何真实的 Shizuku 版本都不匹配，Shizuku 应用会显示**需要更新**的提示。这是预期行为，无需更新任何东西。
- 它的应用列表会显示已有授权记录的应用，以及基于 Shizuku-API 构建的应用。AxManager 本身不会出现在列表中；请在 AxManager 的**特权**页面管理其他应用。

## 停止 {#stopping}

在 Shizuku 应用中点击**停止**不会停止 AxManager。它会关闭 **AX-权限接管**（并关闭 Shizuku 应用），效果与在 AxManager 设置中关闭该开关相同：

- 已经持有该服务的应用此后会被拒绝；它们通过该服务启动的用户服务会被移除。
- 只有 AxManager 和 Shizuku 应用可以切换该开关。其他 Shizuku 客户端尝试切换会被拒绝。
- Shizuku 应用只有通过该接管、并且作为通过 Shizuku-API 连接的那个应用时才拥有它的权限：仅仅冒用它的包名的应用，得到的并不比其他客户端多。

如需重新开启，请在 AxManager 的设置中打开。
