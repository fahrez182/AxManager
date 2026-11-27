#!/system/bin/sh
MODDIR="${0%/*}"
. $MODDIR/../safe_write.sh

# 护眼模式全局通用
safe_write /sys/devices/platform/mdss/*/eye_care 1
safe_write /sys/devices/platform/mdss/*/saturation 100
