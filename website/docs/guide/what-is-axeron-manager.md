# What is Axeron Manager?

Axeron Manager (AxManager) is an open experiment to build a more inclusive Android “manager.”
Instead of being strictly tied to root access, AxManager focuses on ADB/Non-Root mode so it can run on a wider range of devices. At the same time, its architecture remains flexible — allowing optional root execution, support for third-party plugins, and even a Web-based Shell interface.

### Why Axeron Manager?

- Most “manager” apps in Android rely heavily on Root or custom kernels.

- Many users and developers want advanced control, but can’t (or don’t want to) root their devices.

- AxManager fills this gap by making Non-Root/ADB first-class citizens, while still offering optional root features if available.

### Key Capabilities

- Shell Executor: Run commands directly in ADB or Root context.

- Plugin Support: Extend functionality through unrooted modules. [Learn More](../plugin/what-is-plugin)

- Web-based Shell UI: Interact with the device over a browser interface.

- Hybrid Execution: Works the same in both Non-Root and Root environments.

### Philosophy

AxManager is not just a utility but also a learning ground. The project experiments with Android internals, IPC (Inter-Process Communication), and new ways to provide system-level features without breaking device integrity.
