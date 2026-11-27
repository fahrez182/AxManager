#!/system/bin/sh
MODDIR="${0%/*}"
. $MODDIR/../safe_write.sh

# 全局触控采样率拉满
for tp in /sys/class/input/*/report_rate /sys/devices/virtual/input/*/report_rate; do
    safe_write $tp 240
done
