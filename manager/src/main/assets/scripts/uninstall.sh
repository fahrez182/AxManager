#!/bin/sh
rm /storage/emulated/0/Android/BKT.log
echo "正在执行全量卸载还原｜恢复手机原厂全部性能策略"
#1 删除模块外置缓存目录
rm -rf /storage/emulated/0/Universal-Performance/

#2 恢复小米/OPPO全系列被禁用的云控&温控APP
##小米/红米
pm enable --user 0 com.miui.msa.global 2>/dev/null
pm enable --user 0 com.xiaomi.micloud.sdk 2>/dev/null
pm enable --user 0 com.miui.analytics 2>/dev/null
pm enable --user 0 com.xiaomi.thermal 2>/dev/null
pm enable --user 0 com.xiaomi.ab 2>/dev/null
pm enable --user 0 com.xiaomi.joyose 2>/dev/null

##OPPO/realme/一加
pm enable --user 0 com.oplus.cosa 2>/dev/null
pm enable --user 0 com.oplus.athena 2>/dev/null
pm enable --user 0 com.oplus.battery 2>/dev/null
pm enable --user 0 com.coloros.sceneservice 2>/dev/null
pm enable --user 0 com.oplus.powermonitor 2>/dev/null

##红魔 redmagic/nubia 恢复启用云控、负优化组件
pm enable --user 0 com.nubia.optimize.service 2>/dev/null
pm enable --user 0 com.nubia.cloudcontrol 2>/dev/null
pm enable --user 0 com.redmagic.performance.cloud 2>/dev/null
pm enable --user 0 com.nubia.thermal.remote 2>/dev/null

pm enable --user 0 com.nubia.analytics 2>/dev/null
pm enable --user 0 com.nubia.advertise 2>/dev/null
pm enable --user 0 com.nubia.userstat 2>/dev/null
pm enable --user 0 com.nubia.report 2>/dev/null

pm enable --user 0 com.nubia.powersaver 2>/dev/null
pm enable --user 0 com.nubia.smartpower 2>/dev/null
pm enable --user 0 com.nubia.framecontrol 2>/dev/null
pm enable --user 0 com.redmagic.gameoptimizer 2>/dev/null

pm enable --user 0 com.nubia.push 2>/dev/null
pm enable --user 0 com.nubia.config.sync 2>/dev/null
pm enable --user 0 com.nubia.system.patch 2>/dev/null


##其他品牌温控兜底
pm enable com.miui.powerkeeper 2>/dev/null
pm enable com.miui.thermalcontrol 2>/dev/null
pm enable com.coloros.powersaver 2>/dev/null
pm enable com.coloros.thermalengine 2>/dev/null
pm enable com.vivo.thermal 2>/dev/null
pm enable com.vivo.powerguard 2>/dev/null
pm enable com.honor.power 2>/dev/null
pm enable com.honor.thermalmanager 2>/dev/null

#3 【关键】关闭红米隐藏极致性能模式，恢复原厂性能开关
settings put global persist.hyperos.perf.ultimate_enable 0 2>/dev/null
settings put global persist.miui.power.ultimate_mode 0 2>/dev/null
settings put global persist.hyperos.game_perf_hide 1 2>/dev/null

#4 恢复云端性能管控总开关（打开云控远程策略）
#小米
settings put global persist.miui.power.cloud_ctrl 1 2>/dev/null
settings put global persist.hyperos.thermal.cloud 1 2>/dev/null
settings put global miui_performance_cloud_limit 1 2>/dev/null
#OPPO
settings put global persist.oplus.power.cloud 1 2>/dev/null
settings put global coloros_battery_cloud_ctrl 1 2>/dev/null
settings put global persist.oplus.thermal.cloud 1 2>/dev/null

#5 还原系统省电/后台限制（恢复原厂冻结策略）
settings put global app_standby_enabled 1 2>/dev/null
settings put global auto_restrict_background 1 2>/dev/null
settings put global low_power 1 2>/dev/null
settings put global cached_apps_freezer 1 2>/dev/null

#6 动画&渲染恢复系统默认1.0倍
settings put global force_gpu_rendering 0 2>/dev/null
settings put global animator_duration_scale 1.0 2>/dev/null
settings put global transition_animation_scale 1.0 2>/dev/null
settings put global window_animation_scale 1.0 2>/dev/null

#7 触控恢复出厂原生（解决之前长按异常，系统默认400ms）
settings put secure long_press_timeout 400 2>/dev/null
settings put system pointer_speed 5 2>/dev/null
#开启全套原厂触控滤波
settings put secure touch_filter_enable 1 2>/dev/null
settings put secure touch_smoothing 1 2>/dev/null
settings put secure touch_hal_filter 1 2>/dev/null
settings put secure touch_prediction 1 2>/dev/null
settings put system touch_velocity_filter 1 2>/dev/null
settings put system touch_event_queue_limit 8 2>/dev/null
settings put system touch_release_response 0 2>/dev/null

#恢复vivo/iQOO云控
pm enable --user 0 com.vivo.thermal.engine
pm enable --user 0 com.vivo.powerguard
pm enable --user 0 com.vivo.batterysaver.cloud
pm enable --user 0 com.vivo.scenectrl
pm enable --user 0 com.vivo.analytics
settings put global persist.vivo.power.cloud_ctrl 1
settings put global persist.vivo.thermal.cloud_limit 1
settings put global vivo_battery_cloud_saver 1

if [[ $BRAND == "realme" || $BRAND == "oppo" || $BRAND == "oneplus" ]];then
    settings put global persist.graphics.gpu_debug_layer_enable 0
elif [[ $BRAND == "xiaomi" || $BRAND == "redmi" ]];then
    settings put global enable_gpu_debug_layers 0
elif [[ $BRAND == "vivo" || $BRAND == "iqoo" ]];then
    settings put global persist.vulkan.debug.layer_enable 0
fi
settings put global debug.hwui.disable_hw_layers 0
settings put global persist.hwui.msaa 0
# 还原红魔性能相关系统设置
if [[ $BRAND == "nubia" || $BRAND == "redmagic" ]];then
settings put global app_standby_enabled 1
settings put global auto_sync 1
settings put system foreground_app_memory 0
settings put secure performance_auto_adjust 1
fi


#8 推送系统通知
cmd notification post -t "全性能模块" "卸载成功" "缓存已清空，极致模式关闭、云控/温控/触控/动画全部恢复出厂" >/dev/null 2>&1

echo "✅模块彻底卸载完毕：全部参数回归原厂系统默认"
TYSNB="/data/user_de/0/com.android.shell/axeron/plugins/gamepro"
NBYSG_SH="$TYSNB/service.sh"
pkill -f "$NBYSG_SH" 2>/dev/null

settings delete system peak_refresh_rate 2>/dev/null
settings delete system min_refresh_rate 2>/dev/null

settings delete system touch_mode 2>/dev/null
settings delete system touch_sensitivity 2>/dev/null

settings put system power_save_type 0 2>/dev/null
settings put secure power_save_type 0 2>/dev/null
settings put secure system_property_power_mode_type 0 2>/dev/null
settings put system game_plus_mode_key 0 2>/dev/null
settings put system smart_touch_mode 1 2>/dev/null
settings delete system game_scene_more_fps 2>/dev/null
settings delete system bench_mark_mode 2>/dev/null

settings delete system touch_screen_optimization 2>/dev/null
settings delete system tLOCK_FILE="/data/local/tmp/burnengine_x.lock"
if [ -f "$LOCK_FILE" ]; then
    pid=$(head -1 "$LOCK_FILE" 2>/dev/null | cut -d: -f1)
    [ -n "$pid" ] && kill "$pid" 2>/dev/null
    rm -f "$LOCK_FILE"
fi

for key in game_mode performance_mode high_performance_mode_on \
           POWER_PERFORMANCE_MODE_OPEN performance_mode_auto \
           speed_mode game_bypass_charge oplus_touch_optimization \
   ouch_sensitivity_mode 2>/dev/null

echo "驱动跑完了"
