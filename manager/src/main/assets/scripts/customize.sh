#!/system/bin/sh
sleep 2
ui_print "Author:"
ui_print "Telegram: @RiProG | Channel: @RiOpSo | Group: @RiOpSoDisc"
ui_print "Support me on Sociabuzz:"
ui_print "http://sociabuzz.com/RiProG/tribe"
sleep 2
if [ ! -z "$AXERON" ]; then
	ui_print "----------------------------------------"
	ui_print "⚠️  WARNING:"
	ui_print "- AXERON mode detected."
	ui_print "- This module runs with SU privileges."
	ui_print "- Module will disable or remove itself automatically after reboot."
	ui_print "----------------------------------------"
	sleep 2
fi
ui_print "Extracting module files"
architecture=$(uname -m)
if [ -f "$MODPATH/BKT" ]; then
	ui_print "Default binary detected. Skipping architecture detection."
else
	ui_print "- Detected architecture: $architecture"
	case "$architecture" in
	armv7l | armv8l)
		ui_print "- Architecture $architecture is supported."
		ui_print "- Installation continues."
		cp "$MODPATH/BKT_arm" "$MODPATH/BKT"
		;;
	aarch64)
		ui_print "- Architecture $architecture is supported."
		ui_print "- Installation continues."
		cp "$MODPATH/BKT_arm64" "$MODPATH/BKT"
		;;
	*)
		ui_print "- Architecture $architecture is not supported."
		ui_print "- Installation aborted."
		rm -rf "$MODPATH"
		exit 1
		;;
	esac
fi
rm "$MODPATH/BKT_arm"
rm "$MODPATH/BKT_arm64"
chmod +x "$MODPATH/BKT"
info() {
echo "  _____ _    _ ____  _   _ "
echo " | ____| |  | | __ )| | | |"
echo " |  __| | |__| |  _ \| |_| |"
echo " | |__ |  __  | |_) |  _  |"
echo " |_____|_|  |_|____/|_| |_|"
sleep 0.3
echo " 全品牌通用全能性能优化模块"
sleep 0.3
echo " CPU/GPU满血|温控屏蔽|触控增强|禁用厂商云控"
sleep 0.3
echo " 模块安装中，请稍候..."
sleep 0.3
echo
echo "【作者】欧ouge数码"
sleep 0.3
 
 echo "这款模块免费，不许倒卖"
 sleep 0.3

}

ui_print "***************************************"
ui_print "- 设备型号 : $(getprop ro.product.board)"
sleep 0.2
ui_print "- 制造商  : $(getprop ro.product.manufacturer)"
sleep 0.2
ui_print "- Android 版本 : $(getprop ro.build.version.release)"
sleep 0.2
ui_print "- 内核版本: $(uname -r) "
sleep 0.2
ui_print "- 处理器  : $(getprop ro.product.board) "
sleep 0.2
ui_print "- CPU 信息: $(getprop ro.hardware) "
sleep 0.2
ui_print "- 内存大小: $(free | grep Mem | awk '{print $2}') "
sleep 0.2
ui_print "***************************************"

#安装弹窗通知
send_notification() {
cmd notification post -S bigtext -t「通用全能性能模块」「安装完成」「CPU/GPU满血、温控屏蔽、触控优化、云控禁用全部配置完毕」 >/dev/null 2>&1
}
#创建通用外置工作目录
init_folder(){
mkdir -p /storage/emulated/0/Universal-Performance/run/
mkdir -p /storage/emulated/0/Universal-Performance/log/
cp ${MODDIR}/action.sh /storage/emulated/0/Universal-Performance/run/action.sh
cp ${MODDIR}/banner.jpg /storage/emulated/0/Universal-Performance/
}
#首次运行优化脚本
first_run(){
sh /storage/emulated/0/Universal-Performance/run/action.sh >/storage/emulated/0/Universal-Performance/log/install.log 2>&1
}

info
sleep 0.3
init_folder
sleep 0.3
first_run
sleep 0.3
send_notification
echo "✅通用全能模块安装成功！"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ================= CONFLICT =============ui_print "[*] 初始化环境..."

[ -z "$AXERON" ] && ui_print "[!] 非AxManager环境（部分功能受限）"

 ================= CHIP =============opp
    CHIP_RAW=$(cat /sys/devices/soc0/soc_id 2>/dev/null)
[ -z "$CHIP_RAW" ] && CHIP_RAW=$(getprop ro.board.platform)
[ -z "$CHIP_RAW" ] && CHIP_RAW=$(getprop ro.hardware)

case "$CHIP_RAW" in
  *sm*|*sdm*|*msm*|*qcom*|*kona*|*lahaina*|*taro*|*pineapple*)
    CHIP_FAMILY="snapdragon"; ui_print "[+] 骁龙: $CHIP_RAW" ;;
  *mt*|*mediatek*|*dimensity*)
    CHIP_FAMILY="mediatek"; ui_print "[+] 天玑/MTK: $CHIP_RAW" ;;
  *kirin*|*hi*)
    CHIP_FAMILY="kirin"; ui_print "[+] 麒麟: $CHIP_RAW" ;;
  *exynos*|*s5e*|*universal*)
    CHIP_FAMILY="exynos"; ui_print "[+] Exynos: $CHIP_RAW" ;;
  *tensor*)
    CHIP_FAMILY="tensor"; ui_print "[+] Tensor: $CHIP_RAW" ;;
  *)
    CHIP_FAMILY="generic"; ui_print "[?] 通用: $CHIP_RAW" ;;
esac

# ================= BRAND =================
getp(){ getprop "$1" 2>/dev/null | tr '[:upper:]' '[:lower:]'; }

BRAND=$(getp ro.product.brand)
DEVICE=$(getp ro.product.device)
MODEL=$(getp ro.product.model)
CHARS=$(getp ro.build.characteristics)
MANU=$(getp ro.product.manufacturer)

case "$BRAND" in
  *xiaomi*|*redmi*|*poco*) BRAND_FAMILY="xiaomi" ;;
  *oppo*|*realme*|*oneplus*) BRAND_FAMILY="oppo" ;;
  *vivo*|*iqoo*) BRAND_FAMILY="vivo" ;;
  *huawei*|*honor*) BRAND_FAMILY="huawei" ;;
  *samsung*) BRAND_FAMILY="samsung" ;;
  *google*|*pixel*) BRAND_FAMILY="google" ;;
  *asus*|*rog*) BRAND_FAMILY="asus" ;;
  *) BRAND_FAMILY="generic" ;;
esac

ui_print "[+] 设备厂商: $BRAND_FAMILY"

# ================= DEVICE TYPE =================
DEVICE_TYPE="phone"

CHARS=$(getprop ro.build.characteristics)
MODEL=$(getprop ro.product.model)
DEVICE=$(getprop ro.product.device)

if echo "$CHARS $MODEL $DEVICE" | grep -qiE "tablet|pad|tab"; then
    DEVICE_TYPE="tablet"
fi

if [ "$DEVICE_TYPE" = "tablet" ]; then
    ui_print "[+] 平板设备"
else
    ui_print "[+] 手机设备"
fi

# ================= API =================
API=$(getprop ro.build.version.sdk)
ui_print "[*] API: $API"

[ "$API" -lt 21 ] && ui_print "[!] 低版本Android限制"

# ================= CONFLICT =================
echo -e "${CYAN}       ${NC}"ui_print "[*] 扫描冲突模块..."

MODULE_DIR="/data/adb/modules"
CONFLICT=0

[ -d "$MODULE_DIR" ] && for m in "$MODULE_DIR"/*; do
  [ -f "$m/system.prop" ] || continue
  grep -q "persist.sys.power_profile" "$m/system.prop" && CONFLICT=$((CONFLICT+1))
  grep -q "debug.hwui.renderer" "$m/system.prop" && CONFLICT=$((CONFLICT+1))
done

ui_print "[+] 冲突数: $CONFLICT"

# ================= CONFIG =================
CFG_DIR="$MODPATH/config"
mkdir -p "$CFG_DIR"

cat > "$CFG_DIR/hardware.conf" << EOF
CHIP_FAMILY=$CHIP_FAMILY
CHIP_RAW=$CHIP_RAW
BRAND_FAMILY=$BRAND_FAMILY
DEVICE=$DEVICE
MODEL=$MODEL
DEVICE_TYPE=$DEVICE_TYPE
API=$API
CONFLICT_COUNT=$CONFLICT
EOF

tys=$(get_tys)
settings put system peak_refresh_rate "$tys"
settings put system min_refresh_rate "$tys"
echo "帧率已锁 $tys"                      

# ================= CONFIG =================
CFG_DIR="$MODPATH/config"
mkdir -p "$CFG_DIR"

cat > "$CFG_DIR/hardware.conf" << EOF
CHIP_FAMILY=$CHIP_FAMILY
CHIP_RAW=$CHIP_RAW
BRAND_FAMILY=$BRAND_FAMILY
DEVICE=$DEVICE
MODEL=$MODEL
DEVICE_TYPE=$DEVICE_TYPE
API=$API
CONFLICT_COUNT=$CONFLICT
EOF

touch "$CFG_DIR/.init_done"

# ================= PERMISSION =================
chmod 0755 "$MODPATH"/{customize.sh,post-fs-data.sh,service.sh,uninstall.sh} 2>/dev/null
chmod -R 0755 "$MODPATH/bin" "$MODPATH/scripts" 2>/dev/null

ui_print "========================================"
BRAND=$(getprop ro.product.brand)
MODEL=$(getprop ro.product.model)
DEVICE=$(getprop ro.product.device)
SOC=$(getprop ro.board.platform)
SDK=$(getprop ro.build.version.sdk)
RELEASE=$(getprop ro.build.version.release)
e
echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}       ${NC}"
echo -e "${CYAN}       作者：吵吵${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

BRAND=$(getprop ro.product.brand)
MODEL=$(getprop ro.product.model)
DEVICE=$(getprop ro.product.device)
SOC=$(getprop ro.board.platform)
SDK=$(getprop ro.build.version.sdk)
RELEASE=$(getprop ro.build.version.release)

echo -e "${YELLOW}品牌：${NC}${BRAND}"
echo -e "${YELLOW}型号：${NC}${MODEL}"
echo -e "${YELLOW}设备代号：${NC}${DEVICE}"

if [ -n "$SOC" ]; then
    echo -e "${YELLOW}处理器平台：${NC}${SOC}"
else
    SOC=$(getprop ro.hardware)
    [ -n "$SOC" ] && echo -e "${YELLOW}处理器平台：${NC}${SOC}"
fi

echo -e "${YELLOW}Android 版本：${NC}${RELEASE} (SDK ${SDK})"
echo ""

echo -e "${BLUE}--- 屏幕信息 ---${NC}"

MAX_RATE=$(dumpsys display 2>/dev/null | grep -Eo 'refreshRate[= ]*[0-9]+\.[0-9]+' | grep -Eo '[0-9]+\.[0-9]+' | sort -n | tail -1)
if [ -z "$MAX_RATE" ]; then
    MAX_RATE=$(dumpsys display 2>/dev/null | grep -Eo 'PhysicalDisplayInfo.*[0-9]+\.[0-9]+ fps' | grep -Eo '[0-9]+\.[0-9]+' | sort -n | tail -1)
fi
if [ -z "$MAX_RATE" ]; then
    MAX_RATE=$(settings get system peak_refresh_rate 2>/dev/null)
fi
if [ -z "$MAX_RATE" ] || [ "$MAX_RATE" = "null" ]; then
    MAX_RATE=""
fi
echo -e "${YELLOW}最高刷新率：：${NC}${MAX_RATE} 最高Hz"

RESOLUTION=$(wm size 2>/dev/null | grep Physical | awk '{print $3}')
if [ -n "$RESOLUTION" ]; then
    echo -e "${YELLOW}物理分辨率：${NC}${RESOLUTION}"
fi

DPI=$(wm density 2>/dev/null | grep Physical | awk '{print $2}')
if [ -n "$DPI" ]; then
    echo -e "${YELLOW}DPI：${NC}${DPI}"
fi

echo ""

echo ""
echo -e "${GREEN}设备信息硬件完成${NC}"
echo ""

