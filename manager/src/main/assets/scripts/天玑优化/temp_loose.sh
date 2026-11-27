#!/system/bin.sh
MODDIR="${0%/*}"
. $MODDIR/../safe_write.sh

# 宽松温控策略
for tz in /sys/class/thermal/thermal_zone*/policy; do
    safe_write $tz performance
done
