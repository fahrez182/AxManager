#!/system/bin/sh
MODDIR=${0%/*}

# Enable the core refresh rate switching logic in SurfaceFlinger
resetprop ro.surface_flinger.refresh_rate_switching true
# Enable content-based frame rate detection (e.g., for 24fps video)
resetprop ro.surface_flinger.use_content_detection_for_refresh_rate true
# Set a short idle timer to drop to a lower rate when the UI is static
resetprop ro.surface_flinger.set_idle_timer_ms 80
# Keep the display in peak refresh rate longer after touch interactions
resetprop ro.surface_flinger.set_touch_timer_ms 200
# Keep the display in peak refresh rate when power is not in a normal state
resetprop ro.surface_flinger.set_display_power_timer_ms 1000
