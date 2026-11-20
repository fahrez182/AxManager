#!/system/bin/sh



# -----------------------
# Reset all setprop debug properties
# -----------------------
props=$(setprop | awk -F: '{print $1}' | grep "debug.codm")
for p in $props; do
    echo "🔹 Resetting $p"
    setprop "$p" ""
done

# -----------------------
# Reset device_config for CODM
# -----------------------
configs=$(device_config get game_overlay com.garena.game.codm | awk '{print $1}')
for c in $configs; do
    echo "🔹 Resetting device_config $c"
    device_config delete game_overlay com.garena.game.codm "$c"
done

# -----------------------
# Reset content://settings/global entries
# -----------------------
settings=$(content query --uri content://settings/global | grep com.garena.game.codm | awk -F= '{print $1}')
for s in $settings; do
    echo "🔹 Deleting setting $s"
    content delete --uri content://settings/global --where "name='$s'"
done

# -----------------------
# Optional: Clear cache for CodM to avoid leftover effects
# -----------------------
if [ -d "/data/data/com.garena.game.codm/cache" ]; then
    echo "🔹 Clearing CodM cache"
    rm -rf /data/data/com.garena.game.codm/cache/*
fi

