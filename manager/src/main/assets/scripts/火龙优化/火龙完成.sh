#!/system/bin/sh
CURRENT_PROCESSOR=$(getprop ro.product.board)
CURRENT_ANDROID=$(getprop ro.build.version.release)
CURRENT_KERNEL=$(uname -r)
BRAND=$(getprop ro.product.brand | tr '[:upper:]' '[:lower:]')
DEVICE=$(getprop ro.product.device)
PLATFORM=$(getprop ro.board.platform)

echo "processor=$CURRENT_PROCESSOR"
echo "android=$CURRENT_ANDROID"
echo "kernel=$CURRENT_KERNEL"
echo "brand=$BRAND"
echo "device=$DEVICE"
echo "platform=$PLATFORM"

exit 0
