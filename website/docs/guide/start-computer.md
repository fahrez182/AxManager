# <Icon name="usb" /> Start with a Computer (adb)

This method works on any device, rooted or not. It requires a computer, and the steps need to be performed again after each reboot. To skip installing `adb`, [activate from the browser](../activate.md) instead.

## What is `adb`?

Android Debug Bridge (`adb`) is a versatile command-line tool that lets you communicate with a device. The `adb` command facilitates a variety of device actions, such as installing and debugging apps, and it provides access to a Unix shell that you can use to run a variety of commands on a device.

See [Android Developer](https://developer.android.com/tools/adb) for more information.

## Install `adb`

1. Download "**SDK Platform Tools**" provided by Google and extract it to any folder
2. Open the folder, right click to select
   - Windows 10/11: Open PowerShell window here (hold down Shift to show this option)
   - Mac or Linux: Open Terminal
3. Enter `adb`. If it works, you see a long list of content instead of an error saying adb was not found.

::: tip
Please do not close this window. The "**terminal**" mentioned later refers to this window (if you closed the window, please go back to [step 2](#install-adb)).
If you use PowerShell or Linux/Mac, all `adb` should be replaced with `./adb`
:::

## Setting `adb`

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

## Start AxManager

On the Activate screen, open **View Command** under **Start by connecting to a computer**. It shows a command of this form:

```
adb shell /data/app/~~…/frb.axeron.manager-…/lib/arm64/libaxeron.so
```

The exact path depends on your installation, so always copy it from the app (**Copy**, or **Send** to share it to your computer). Paste it into the terminal. If there is no problem, AxManager shows that it has started.
