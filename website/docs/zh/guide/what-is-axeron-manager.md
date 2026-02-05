# 什么是 Axeron Manager？

Axeron Manager (AxManager) 是一个开放的实验性项目，旨在构建一个更具包容性的 Android “管理器”。
AxManager 不严格依赖 root 访问权限，而是专注于 ADB/非 Root 模式，使其能够在更广泛的设备上运行。同时，其架构保持灵活性——允许可选的 root 执行、支持第三方插件，甚至提供基于 Web 的 Shell 界面。

### 为什么选择 Axeron Manager？

- Android 中的大多数“管理器”应用都严重依赖 Root 或自定义内核。

- 许多用户和开发者希望获得高级控制，但无法（或不想）对设备进行 root。

- AxManager 通过将非 Root/ADB 作为核心功能来填补这一空白，同时如果可用，仍提供可选的 root 功能。

### 核心能力

- Shell 执行器：在 ADB 或 Root 上下文中直接运行命令。

- 插件支持：通过非 Root 模块扩展功能。[了解更多](../plugin/what-is-plugin)

- 基于 Web 的 Shell UI：通过浏览器界面与设备交互。

- 混合执行：在非 Root 和 Root 环境中以相同的方式工作。

### 设计理念

AxManager 不仅是一个实用工具，也是一个学习平台。该项目探索 Android 内部机制、IPC（进程间通信）以及在保持设备完整性的同时提供系统级功能的新方法。