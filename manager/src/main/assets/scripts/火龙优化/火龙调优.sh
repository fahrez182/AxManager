#!/system/bin/sh

# 环境检测
if [ "$AXERON" != "true" ] && [ -z "$KSU" ] && [ -z "$MAGISK_VER" ]; then
    ui_print " "
    ui_print "    环境检测失败"
    ui_print "    请在 AxManager / Root 环境下使用"
    ui_print " "
    exit 1
fi

# ==============================
# 超强兼容 · 全骁龙处理器检测
# 只要是高通骁龙，全部通过
# ==============================
is_soc_qualcomm=0

# 初始化高通芯片标记
is_soc_qualcomm=0

# 多维度读取SOC硬件信息
hardware=$(getprop ro.hardware)
platform=$(getprop ro.board.platform)
board=$(getprop ro.product.board)
soc=$(getprop ro.soc.model)
chipset=$(getprop ro.chipset)
qcom_platform=$(getprop ro.qualcomm.platform)

all_soc_info="$hardware $platform $board $soc $chipset $qcom_platform"

echo "$all_soc_info" | grep -qiE \
'qcom|qualcomm|msm|sm[0-9]|kalama|pineapple|sapphire|volcano|lahaina|taro|naples|holi|yupik|bengal|kona|laha' \
&& is_soc_qualcomm=1
SOC=$(cat /proc/cpuinfo | grep -i "Qualcomm" | head -n 1)
if [ -z "$SOC" ]; then
    SOC_MODEL=$(getprop ro.soc.model)
    if [ -z "$(echo $SOC_MODEL | grep -iE 'SM|QCOM|Snapdragon')" ]; then
        ui_print "警告: 未检测到骁龙处理器，可能存在兼容性问题。"
    fi
fi

# 设置权限
ui_print "正在设置脚本权限..."
chmod 0755 $MODPATH/service.sh
chmod 0755 $MODPATH/底层守护脚本/*.sh
chmod 0755 $MODPATH/核心功能脚本/*.sh
chmod 0755 $MODPATH/日志调试脚本/*.sh
chmod 0755 $MODPATH/适配兼容脚本/*.sh

