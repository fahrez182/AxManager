# <Icon name="start" /> Start with Wireless Debugging

Starting with wireless debugging works on Android 11 or above. This startup method does not require a connection to a computer. Due to system limitations, the startup steps need to be performed again after each reboot, unless [Activate on Boot](./user-manual.md#activate-on-boot) does it for you.

It needs a Wi-Fi network. If Wi-Fi is off, AxManager opens the Wi-Fi panel and waits until a network is up, because an app can't turn Wi-Fi on itself. It always goes through Wireless Debugging, even when ADB already listens on a TCP port; to start from that port without Wi-Fi, use [TCP debugging](./user-manual.md#start-with-tcp-debugging).

## Enable Wireless Debugging

1. Search how to enable "**Developer Options**" for your device model
2. Enable "**Developer Options**" and "**USB Debugging**"
3. Enter "**Wireless Debugging**"
4. Enable "**Wireless Debugging**"

::: tip
On the Activate screen, **Instruction** → **Developer** opens the Wireless Debugging settings for you.
:::

A line at the top of the Activate screen shows whether AxManager can write the **Secure** and **System** settings (**Granted** or **Not granted**). When it can, it switches Developer options, USB debugging and Wireless Debugging on together each time it activates, so the steps above are only needed while it says **Not granted**.

## Start & Pairing (only needs once)

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
