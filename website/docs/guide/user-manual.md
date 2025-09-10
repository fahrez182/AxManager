# User Manual

[[toc]]

## Start Axeron Manager

### Start with Root

::: info
For now you can only start root mode using commands
:::

### Start with Wireless Debugging

Starting with wireless debugging works on Android 11 or above. This startup method does not require a connection to a computer. Due to system limitations, the startup steps need to be performed again after each reboot.

#### Enable Wireless Debugging

1. Search how to enable "**Developer Options**" for your device model
2. Enable "**Developer Options**" and "**USB Debugging**"
3. Enter "**Wireless Debugging**"
4. Enable "**Wireless Debugging**"

#### Start & Pairing (only needs once)

1. Start in AxManager
2. [Enable "**Wireless Debugging**"](#enable-wireless-debugging)
3. Tap "**Pair device with pairing code**" in "**Wireless Debugging**"
4. Enter pairing code in AxManager's notification until "**Pairing successfully**"
5. Back to AxManager and wait until connected (On some devices you need to click "**Start**" again)

### Start by connecting to a computer

This boot method works on unrooted devices running Android 10 and below. Unfortunately, this startup method requires a computer. Due to system limitations, the boot steps need to be performed again after each reboot.

#### What is `adb`?

Android Debug Bridge (`adb`) is a versatile command-line tool that lets you communicate with a device. The `adb command` facilitates a variety of device actions, such as installing and debugging apps, and it provides access to a Unix shell that you Can use to run a variety of commands on a device.

See [Android Developer](https://developer.android.com/tools/adb) for more information.

#### Install `adb`

1. Download "**SDK Platform Tools**" provided by Google and extract it to any folder

   - Windows
   - Linux(opens new window)
   - Mac(opens new window)

2. Open the folder, right click to select

   - Windows 10: Open PowerShell windows here (hold down Shift to show this option)
   - Windows 7: Open command window here (hold down Shift to show this option0
   - Mac or Linux: Open Terminal

3. Enter `adb`, if success, you can see a long list of content instead of the prompt not finding adb.

::: tip
Please do not close this window. The "**terminal**" mentioned later refers to this window (if you closed the window, please go back to [step 2](#install-adb))
If you use PowerShell or Linux/Mac, all `adb` should be replaced with `./adb`
:::

#### Setting `adb`

To use `adb` you first need to turn on USB debugging on your device, usually by following these steps:

1. Search how to enable "**Developer Options**" for your device model

2. Enable "**Developer Options**" and "**USB Debugging**"

3. Connect the device to the computer and type `adb devices` in the terminal.

4. At this time, the dialog "**Allow debugging**" will appear on the device, check "**Always allow**" and confirm.

5. Enter `adb devices` again in the terminal. If there is no problem, you will see something like the following.

   ```
   List of devices attached
   XXX     device
   ```

#### Start AxManager

Copy the command and paste into the terminal. If there is no problem, you will see that AxManager has started successfully in AxManager app.
