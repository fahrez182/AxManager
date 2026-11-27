#!/system/bin/sh
MODDIR="${0%/*}"
. $MODDIR/../safe_write.sh

# 游戏通用高性能
for i in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    safe_write $i performance
done

# Mali GPU Boost 全局通用
for gpu in /sys/class/mali/*/power_policy /sys/devices/platform/mali*/governor; do
    safe_write $gpu performance
done

# 触控最大响应
for tp in /sys/class/input/*/report_rate /sys/devices/virtual/input/*/report_rate; do
    safe_write $tp 240
done
