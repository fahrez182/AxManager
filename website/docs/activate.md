---
title: Activate
description: Start AxManager from your browser over USB, without installing adb.
---

# Activate from the browser

Start AxManager from this page over USB. It runs the same command as [Start by connecting to a computer](./guide/user-manual.md#start-by-connecting-to-a-computer), but the browser talks to the device directly, so you don't need to install `adb`.

<ClientOnly>
  <WebActivate />
</ClientOnly>

## Steps

1. Install AxManager on the device.
2. Enable "**Developer Options**" and "**USB Debugging**" on the device ([how](./guide/start-computer.md#setting-adb)).
3. Connect the device with a USB cable, then click **Connect** and pick it from the list.
4. On the device, allow USB debugging when asked (check "**Always allow**").
5. Click **Activate**. The log shows the starter's output, and the status turns green once the server is running.

The steps need to be performed again after each reboot, like starting from a computer.

If **TCP mode** is on in AxManager (the switch on its **TCP Debugging** card), the page first switches the device's ADB to TCP on that card's port, then starts the server, as the app does. ADB restarts for the switch, so the USB connection drops for a moment and the page reconnects by itself (allow USB debugging again if asked). Until the next reboot, the card's **Connect to TCP** can then start AxManager again without a computer.

## Requirements

- A Chromium browser with WebUSB: Chrome or Edge on a computer, or Chrome on Android. Firefox and Safari can't use USB devices.
- Nothing else may hold the device's ADB connection. If you have `adb` installed, run `adb kill-server` first, and close Android Studio, scrcpy and other ADB tools.
- On Windows, the device needs a USB driver that WebUSB can use. If the device doesn't show up or can't be opened, install the [Google USB Driver](https://developer.android.com/studio/run/win-usb) or your device maker's ADB driver.

::: tip
Chrome on Android works too: connect the device to another phone with an OTG cable and open this page on that phone.
:::

## What this page does

After you allow USB debugging, the page:

1. Asks AxManager for its TCP mode and port (`content call --uri content://frb.axeron.manager.server --method getTcpConfig`). When TCP mode is on and ADB isn't on that port yet, it switches ADB to it (`adb tcpip <port>`) and reconnects over USB, which ADB keeps serving.
2. Runs `pm path frb.axeron.manager` to find where AxManager is installed.
3. Runs `libaxeron.so` from that install, the same starter the app shows under **View Command**.
4. Checks that `axeron_server` is running.

Everything runs in your browser. The key the browser uses for USB debugging is stored in this browser only.
