#!/system/bin/sh
MODDIR="${0%/*}"
. $MODDIR/../safe_write.sh

# 严格温控省电
for tz in /sys/class/thermal/thermal_zone*/policy; do
    safe_write $tz balanced
done
