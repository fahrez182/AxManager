#!/system/bin/sh
MODDIR="${0%/*}"
. $MODDIR/../safe_write.sh

# 稳定调度 + 最小频率保障
safe_write /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor schedutil
safe_write /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq 1600000
