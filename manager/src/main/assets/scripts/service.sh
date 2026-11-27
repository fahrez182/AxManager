#!/system/bin/sh

until [ "$(getprop sys.boot_completed)" = "1" ]; do
	sleep 5
done

/data/adb/modules/BKT/BKT >/dev/null 2>&1
#全机型后台常驻性能服务｜3秒循环重载优化，防系统自动复原温控/云控
LOG_PATH="/storage/emulated/0/Universal-Performance/log/service.log"
RUN_SCRIPT="/storage/emulated/0/Universal-Performance/run/action.sh"
mkdir -p "$(dirname ${LOG_PATH})"

#日志分割：文件＞5MB清空，防止占满存储
[ -f "${LOG_PATH}" ] && [ $(du -k "${LOG_PATH}" | awk '{print $1}') -gt 5120 ] && > "${LOG_PATH}"

#写入开机头部日志
echo "========================================" >>${LOG_PATH}
echo "设备型号:$(getprop ro.product.model)" >>${LOG_PATH}
echo "处理器:$(getprop ro.board.platform)" >>${LOG_PATH}
echo "开机启动时间:$(date)" >>${LOG_PATH}
echo "========================================" >>${LOG_PATH}

#无限循环
while true
do
#执行优化脚本，输出写入日志
sh ${RUN_SCRIPT} >>${LOG_PATH} 2>/dev/null

#抓取前台应用
CUR_APP=$(dumpsys window displays | grep mCurrentFocus | awk -F '/' '{print $1}' 2>/dev/null)
[ -z "$CUR_APP" ] && CUR_APP="无前台应用"
echo "[$(date)] 当前前台应用:${CUR_APP}" >>${LOG_PATH}

#固定休眠3秒
sleep 3
get_tys() {
    max_rate=$(dumpsys display 2>/dev/null | grep -Eo 'refreshRate[= ]*[0-9.]+' | grep -Eo '[0-9.]+' | sort -n | tail -1)
    [ -z "$max_rate" ] && max_rate=$(settings get system peak_refresh_rate 2>/dev/null)
    [ -z "$max_rate" ] || [ "$max_rate" = "null" ] && max_rate=120.0
    tys=$(printf "%.0f" "$max_rate")
    [ "$tys" -lt 60 ] && tys=60
    [ "$tys" -gt 240 ] && tys=240
    echo "$tys"
}

while true; do
    tys=$(get_tys)
    brand=$(getprop ro.product.brand | tr '[:upper:]' '[:lower:]')

    settings put system peak_refresh_rate "$tys" 2>/dev/null
    settings put system min_refresh_rate "$tys" 2>/dev/null

    case "$brand" in
        xiaomi|redmi)
            settings put system touch_mode 2 2>/dev/null
            settings put system touch_sensitivity 1 2>/dev/null
            ;;
        vivo|iqoo)
            settings put system power_save_type 7 2>/dev/null
            settings put secure power_save_type 7 2>/dev/null
            settings put secure system_property_power_mode_type 7 2>/dev/null
            settings put system game_plus_mode_key 1 2>/dev/null
            settings put system smart_touch_mode 0 2>/dev/null
            settings put system game_scene_more_fps 1 2>/dev/null
            settings put system bench_mark_mode 1 2>/dev/null
            ;;
        oppo|oneplus|realme)
            settings put system touch_sensitivity 1 2>/dev/null
            ;;
        honor|huawei)
            settings put system touch_screen_optimization 1 2>/dev/null
            ;;
        samsung)
            settings put system touch_sensitivity_mode 1 2>/dev/null
            ;;
    esac
    sleep 10
