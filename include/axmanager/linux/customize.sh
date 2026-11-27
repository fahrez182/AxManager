#!/system/bin/sh
# AxLinuxEmu installer - LF only
SKIPUNZIP=1
ARCH=$(getprop ro.product.cpu.abi)
SDK=$(getprop ro.build.version.sdk)
ui_print "- AxLinuxEmu v2 installing"
ui_print "- arch: $ARCH sdk: $SDK"
if [ "$ARCH" != "arm64-v8a" ]; then
  abort "! need arm64 device"
fi
if [ "$SDK" -lt 35 ]; then
  ui_print "! warn: optimal on SDK 36 Android 16 16K page"
fi
mkdir -p $AXERONDIR/plugins/AxLinuxEmu
mkdir -p $AXERONDIR/logs
set_perm_recursive $MODPATH/system/bin 0 0 0755 0755
ui_print "- done. reboot service or re-ignite"
