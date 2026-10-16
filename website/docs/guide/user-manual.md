# User Manual

[[toc]]

## Start Axeron Manager

AxManager does its work through a background server. Before anything else, start that server with one of the methods below. Open them from the status card on the **Home** screen: tap the card to open the **Activate** screen.

The Activate screen is split in two:

- **This device**: TCP debugging, Wireless Debugging (Android 11+), and Root.
- **Another device**: OTG (use this phone to start AxManager on another phone), and the command for a computer.

| Method | Needs | Survives a reboot |
|---|---|---|
| [Root](#start-with-root) | A rooted device | Yes, with [Activate on Boot](#activate-on-boot) |
| [Wireless Debugging](#start-with-wireless-debugging) | Android 11+, Wi-Fi | Experimental, with [Activate on Boot](#activate-on-boot) |
| [TCP debugging](#start-with-tcp-debugging) | ADB already listening on TCP | No |
| [OTG](#start-from-another-phone-otg) | A second phone and an OTG cable | No |
| [Computer](#start-by-connecting-to-a-computer) | A computer with `adb` | No |

### The activation log

Every method on the Activate screen opens a sheet with a live log of each step: finding the ADB port, connecting, running the starter and its own output, and waiting for the server to connect back. A method only counts as successful when the server actually connects; if the starter ran but the server never connected, the sheet says so and points at the log.

- On success, the sheet closes itself after 3 seconds.
- On failure, it stays open. Use **Copy log** to share what went wrong.
- Closing the sheet does not cancel an attempt that is still running.

### Start with Root

On a rooted device, open **Root** and tap **Start**. AxManager asks for root, then starts the server as `root`.

**Start as Shell** starts the server as the `shell` user instead (through `su 2000`), even though the device is rooted. Use it to run AxManager with ADB-level permissions only. Root mode and shell mode keep [separate folders](../plugin/environment.md#working-folder), so their plugins and settings do not mix.

AxManager can start again automatically after a reboot, see [Activate on Boot](#activate-on-boot). If it does not, check whether your system or a third-party tool restricts AxManager from starting in the background ([Don't kill my app!](https://dontkillmyapp.com/)).

### Start with Wireless Debugging

Starting with wireless debugging works on Android 11 or above. This startup method does not require a connection to a computer. Due to system limitations, the startup steps need to be performed again after each reboot, unless [Activate on Boot](#activate-on-boot) does it for you.

#### Enable Wireless Debugging

1. Search how to enable "**Developer Options**" for your device model
2. Enable "**Developer Options**" and "**USB Debugging**"
3. Enter "**Wireless Debugging**"
4. Enable "**Wireless Debugging**"

::: tip
On the Activate screen, **Instruction** → **Developer** opens the Wireless Debugging settings for you.
:::

#### Start & Pairing (only needs once)

AxManager posts the pairing code field as a notification, so allow notifications first (the button shows **Enable Notification** until you do).

1. Tap **Start Pairing** in AxManager
2. [Enable "**Wireless Debugging**"](#enable-wireless-debugging)
3. Tap "**Pair device with pairing code**" in "**Wireless Debugging**"
4. Enter the pairing code in AxManager's notification until "**Pairing successfully**"
5. Back to AxManager and wait until connected (on some devices you need to tap **Start Pairing** again)

Once paired, **Start Pairing** connects directly. If the connection fails (for example, the device was never paired), AxManager starts the pairing service again and asks you to follow the notification.

::: warning
Do not turn off "**Developer Options**" or "**USB debugging**" while AxManager is running, or it stops. Wireless Debugging also turns itself off when the network changes.
:::

### Start with TCP debugging

When ADB already listens on a TCP port (for example after `adb tcpip 5555` from a computer, or after a previous activation with TCP mode on), the **TCP Debugging** card shows the port and a **Connect to TCP** button. It starts AxManager without Wi-Fi or a computer.

The switch next to the card's title (TCP mode) changes how Wireless Debugging activation behaves:

- **On** (default): after connecting over Wireless Debugging, AxManager switches ADB to TCP on the **TCP Port** (1024–65535, 5555 unless the device already sets another). Later activations can then reuse that port, even without Wi-Fi, until the next reboot.
- **Off**: if ADB is on TCP, the next Wireless Debugging activation switches it back to USB first.

A changed port applies at the next activation. **Stop TCP** switches ADB back to USB.

### Start from another phone (OTG)

AxManager can act as the "computer" for another phone: connect the two phones with an OTG cable and start AxManager on the other one. Tap **Start OTG** on both phones and pick a role on each:

- **Computer**: this phone activates the other phone. It needs USB host (OTG) support.
- **Target**: this phone gets activated. A phone without USB host support can only be the target.

On the target phone:

1. Install AxManager
2. Turn on **USB debugging**
3. Open **Activate via OTG** and choose **Target**; it shows the progress
4. Connect both phones with an OTG cable
5. Tap **Allow** when asked about USB debugging

The computer phone then finds the device, connects, and starts AxManager on it. If the target's AxManager has TCP mode on, its ADB is switched to TCP on the target's own port afterwards.

::: tip USB-C to USB-C cables
The other phone may become the USB host instead. Open the USB notification on either phone and set "**USB controlled by**" so that the computer phone is in control.
:::

### Start by connecting to a computer

This method works on any device, rooted or not. It requires a computer, and the steps need to be performed again after each reboot.

#### What is `adb`?

Android Debug Bridge (`adb`) is a versatile command-line tool that lets you communicate with a device. The `adb` command facilitates a variety of device actions, such as installing and debugging apps, and it provides access to a Unix shell that you can use to run a variety of commands on a device.

See [Android Developer](https://developer.android.com/tools/adb) for more information.

#### Install `adb`

1. Download "**SDK Platform Tools**" provided by Google and extract it to any folder
2. Open the folder, right click to select
   - Windows 10/11: Open PowerShell window here (hold down Shift to show this option)
   - Mac or Linux: Open Terminal
3. Enter `adb`. If it works, you see a long list of content instead of an error saying adb was not found.

::: tip
Please do not close this window. The "**terminal**" mentioned later refers to this window (if you closed the window, please go back to [step 2](#install-adb)).
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

On the Activate screen, open **View Command** under **Start by connecting to a computer**. It shows a command of this form:

```
adb shell /data/app/~~…/frb.axeron.manager-…/lib/arm64/libaxeron.so
```

The exact path depends on your installation, so always copy it from the app (**Copy**, or **Send** to share it to your computer). Paste it into the terminal. If there is no problem, AxManager shows that it has started.

## Activate on Boot

**Settings → Activate on Boot** (on by default) starts AxManager again after a reboot, using the method it was last started with:

- **Root** or **Start as Shell**: started again through root.
- **ADB** (Wireless Debugging, TCP or OTG), Android 11+ only, **experimental**: AxManager turns Wireless Debugging on and connects to it. This needs the `WRITE_SECURE_SETTINGS` permission, which the server grants AxManager when it runs, and Wi-Fi at boot.
- Started from a computer, never started, or ADB below Android 11: not started on boot.

Nothing is started in safe mode. While AxManager is stopped, the Home status card shows the last method used and whether boot start is on.

## Stop or restart

While AxManager runs, the power button on the Home status card opens a power menu:

- The **flame** button re-ignites plugins, see [Re-ignite](../plugin/re-ignite.md).
- Slide the switch **up** to shut the server down.
- Slide the switch **down** to restart the server.

After you update the app, the Home status card shows the old server being replaced with the new version.
