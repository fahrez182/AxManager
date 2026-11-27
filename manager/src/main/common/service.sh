#!/system/bin/sh
# service.sh - late_start service, root-agnostic (Magisk/KernelSU/APatch)

until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 1; done
sleep 5

SDK=$(getprop ro.build.version.sdk)
LOG=/data/local/tmp/refresh_thermal_lock.log
echo "$(date) SDK=$SDK boot" > "$LOG"

MAX_HZ=$(dumpsys display 2>/dev/null | grep -oE 'fps=[0-9]+\.[0-9]+' | sed 's/fps=//' | sort -rn | head -n1 | cut -d. -f1)
[ -z "$MAX_HZ" ] && MAX_HZ=$(dumpsys display 2>/dev/null | grep -oE 'refreshRate=[0-9]+\.[0-9]+' | sed 's/refreshRate=//' | sort -rn | head -n1 | cut -d. -f1)
[ -z "$MAX_HZ" ] && MAX_HZ=120
echo "MAX_HZ=$MAX_HZ" >> "$LOG"

# --- Peak/min refresh rate lock: stable Settings.System keys, API 34-37 ---
settings put system peak_refresh_rate "${MAX_HZ}.0"
settings put system min_refresh_rate "${MAX_HZ}.0"

if [ "$SDK" -ge 35 ]; then
    # Android 15/16/17 Beta: DisplayModeDirector device_config surface
    device_config put display_manager peak_refresh_rate_default "${MAX_HZ}.0" 2>/dev/null
    device_config put display_manager refresh_rate_synchronization_enabled false 2>/dev/null
else
    # Android 14 legacy device_config namespace/key
    device_config put display_manager peak_refresh_rate "${MAX_HZ}.0" 2>/dev/null
fi

# --- Thermal dimming mitigation: force ThermalManagerService status to NONE ---
# so PowerManager/BrightnessThrottler never trips a dim/clamp response.
# Kernel-level hardware thermal shutdown is independent and unaffected.
(
    while true; do
        cmd thermal override-status 0 2>/dev/null
        sleep 30
    done
) &

# --- Neutralize AOSP BrightnessThrottler thermal clamp maps ---
device_config put display_manager brightness_throttling_data "" 2>/dev/null
device_config put display_manager brightness_throttling_data_0 "" 2>/dev/null

if [ "$SDK" -ge 36 ]; then
    # Android 16/17 Beta: split thermal brightness clamper config key
    device_config put display_manager thermal_brightness_throttling_data "" 2>/dev/null
fi

echo "$(date) applied" >> "$LOG"
