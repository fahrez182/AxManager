# FAQ

[[toc]]

Many manufacturers have made modifications to the Android system that prevent AxManager from working properly.

## Start via wireless debugging: keeps showing "Searching for pairing service"

Please allow AxManager to run in the background.

Searching for pairing service requires access to the local network, and many manufacturers disable network access for apps as soon as they become invisible. You can search the web for how to allow apps to `run in the background` on your device.

## Start via wireless debugging: immediately fail after tapping "Enter pairing code"

::: info MIUI (Xiaomi, POCO)
Switch notification style to "Android" from "Notification" - "Notification shade" in system settings.
:::

## Start via wireless debugging/Start by connecting to a computer: the permission of adb is limited

::: info MIUI (Xiaomi, POCO)
Enable "USB debugging (Security options)" in "Developer options". Note that this is a separate option from "USB debugging".
:::

::: info ColorOS (OPPO & OnePlus)
Disable "Permission monitoring" in "Developer options".
:::

::: info Flyme (Meizu)
Disable "Flyme payment protection" in "Developer options".
:::

## Start via wireless debugging/Start by connecting to a computer: AxManager randomly stops

::: info All devices

- Make sure Shizuku can run in the background.

- Do not disable "USB debugging" and "Developer options".

- Change the USB usage mode to "Charge only" in the "Developer options".

  - On Android 8, the option is "Select USB configuration" - "Charge only".
  - On Android 9+, the option is "Default USB configuration" - "No data transfer".

- (Android 11+) Enable "Disable adb authorization timeout" option

:::

::: info EMUI (Huawei)
Enable "Allow ADB debugging options in 'Charge only' mode" in "Developer options".
:::

::: info MIUI (Xiaomi, POCO)
Do not use the scan feature in MIUI's "Security" app, since it will disable "Developer options".
:::

::: info Sony
Don't click the dialog shows after connecting the USB, because it will change USB usage mode.
:::
