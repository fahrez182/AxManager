# Re-ignite (Reboot system)

::: info TL/DR
**Re-ignite** is a system that restarts a plugin/module to apply updates — similar to rebooting a rooted module on your phone, but for **AxManager**.
:::

[[toc]]

## Background — Re-ignite Concept

In **root modules**, rebooting is an essential process to activate or update a module so that changes can fully take effect at the system level. However, this approach is not feasible for **Plugins**, which operate entirely within the **userspace** and **do not have access** to the **mount process during boot**.

As a workaround, I developed the **_Re-ignite_** concept — a mechanism that _re-initializes_ the plugin directly, performing several internal manipulations to simulate the effect of a reboot, but limited only to the plugin itself. This allows the plugin to refresh and apply updates without requiring a full system reboot.

::: warning Read this
This concept is still under development. If you have a more efficient approach, feel free to submit a _pull request_. This idea was born out of a technical _limitation_, so please understand that it may still have several shortcomings.
:::
