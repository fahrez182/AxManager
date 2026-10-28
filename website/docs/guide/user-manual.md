# <Icon name="start" /> User Manual

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
| [Browser](#start-from-the-browser) | Chrome or Edge on a computer, or Chrome on another phone; a USB cable | No |

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

Android 11 or above, with Wi-Fi and no computer: pair once in the Wireless Debugging settings, then start from AxManager. The steps are on **[Start with Wireless Debugging](./start-wireless.md)**.

### Start with TCP debugging

When ADB already listens on a TCP port (for example after `adb tcpip 5555` from a computer, or after a previous activation with TCP mode on), the **TCP Debugging** card shows the port and a **Connect to TCP** button. It starts AxManager without Wi-Fi or a computer.

The switch next to the card's title (TCP mode) changes how Wireless Debugging activation behaves:

- **On** (default): after connecting over Wireless Debugging, AxManager switches ADB to TCP on the **TCP Port** (1024–65535, 5555 unless the device already sets another). **Connect to TCP** can then start AxManager on that port, even without Wi-Fi, until the next reboot. AxManager only switches ADB when it is not on that port already.
- **Off**: if ADB is on TCP, the next Wireless Debugging activation switches it back to USB first.

A changed port applies right away: saving it, or turning TCP mode on, while ADB listens on another TCP port moves ADB to the new port and restarts the server on it. If that switch fails, the restart icon in the port field tries it again. **Stop TCP** switches ADB back to USB.

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

Any device, rooted or not, with a computer and `adb`: run the command AxManager shows, after each reboot. Installing and setting up `adb` is on **[Start with a Computer (adb)](./start-computer.md)**.

### Start from the browser

[Activate from the browser](../activate.md) does what the computer method does, from a web page over USB: no `adb` to install. It works in Chrome or Edge on a computer, or in Chrome on another phone connected with an OTG cable.

1. Enable **USB Debugging** on the device ([how](./start-computer.md#setting-adb)) and connect it with a USB cable.
2. Open [axmanager.app/activate](https://axmanager.app/activate), click **Connect** and pick the device.
3. Allow USB debugging on the device, then click **Activate**.

The page shows the same log as the app's activation and turns green once the server runs. With **TCP mode** on (the switch on the **TCP Debugging** card), it first switches ADB to TCP on that card's port, as Wireless Debugging activation does, so [TCP debugging](#start-with-tcp-debugging) can start AxManager again until the next reboot.

Close anything else using ADB first (an `adb` server, Android Studio, scrcpy): only one program can hold the device. See the page for [requirements](../activate.md#requirements) such as the Windows USB driver.

## Activate on Boot

**Settings → Activate on Boot** (on by default) starts AxManager again after a reboot, using the method it was last started with:

- **Root** or **Start as Shell**: started again through root.
- **ADB** (Wireless Debugging, TCP or OTG), Android 11+ only, **experimental**: AxManager turns Wireless Debugging on and connects to it. This needs the `WRITE_SECURE_SETTINGS` permission, which the server grants AxManager when it runs, and Wi-Fi at boot.
- Started from a computer or the browser, never started, or ADB below Android 11: not started on boot.

Nothing is started in safe mode. While AxManager is stopped, the Home status card shows the last method used and whether boot start is on.

An ADB start after a reboot shows one **Starting AxManager** notification that follows each step, with a step counter: waiting for Wi-Fi, turning on Wireless Debugging, looking up its port, connecting to adbd, starting the server, and waiting for the server to connect. While it waits for Wi-Fi the notification has an **Open Wi-Fi** button, since an app can't switch Wi-Fi on. If a step fails or times out, it says so and offers **Retry**. The notification stays until the device is unlocked, then goes away. If the notification can't be started, AxManager activates without it.

## Stop or restart

While AxManager runs, the power button on the Home status card opens a power menu:

- The **flame** button re-ignites plugins, see [Re-ignite](../plugin/re-ignite.md).
- Slide the switch **up** to shut the server down.
- Slide the switch **down** to restart the server.

Re-ignite, shutdown and restart each run in a sheet with a live log. **Copy log** copies it with each line's time, also while the action is still running, so one that hangs can be reported. **Cancel** stops it, except while the server is being replaced.

With **Stop plugins with the server** on, shutdown and restart stop every plugin first and show it in the log; a restart then re-ignites them in the same sheet.

After you update the app, the Home status card shows the old server being replaced with the new version.
