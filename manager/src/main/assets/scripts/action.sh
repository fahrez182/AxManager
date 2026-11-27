#!/system/bin/sh
 echo "【更新】+红米/小米锁帧率的问题"
 sleep 0.5
 echo "【更新】欧/加/真/+测试中 、(解除游戏锁频)"
 sleep 0.5
 echo "【新增】 中兴 / 努比亚 / 红魔性能优化"
 sleep 0.5

 echo "【作者】欧ouge数码"
 sleep 0.3
 
 echo "这款模块免费，不许倒卖"
 sleep 0.3
 
echo "性能服务状态:运行中"
echo ""
echo "[+]自动识别SOC
全局优化CPU/GPU/云控/..."
sleep 0.3
SOC=$(getprop ro.board.platform)
BRAND=$(getprop ro.product.brand | tr '[A-Z]' '[a-z]')
sleep 0.3

echo "[√]${BRAND} | ${SOC} 检测设备"
echo "- Android 版本 : $(getprop ro.build.version.release)"
sleep 0.3
echo "- 内核版本: $(uname -r)"
sleep 0.3
echo "- 处理器 : $(getprop ro.product.board)"
sleep 0.3
echo "- CPU 信息: $(getprop ro.hardware)"
sleep 0.3
echo "- 内存大小: $(free | grep Mem | awk '{print $2}')"
sleep 1.0

#=====1 固定CPU优化｜0~7八核通用，无for无ls，永不卡死=====
echo ">> 开始CPU优化"
echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null
echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq 2>/dev/null

echo performance > /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor 2>/dev/null
echo 0 > /sys/devices/system/cpu/cpu1/cpufreq/scaling_min_freq 2>/dev/null

echo performance > /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor 2>/dev/null
echo 0 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq 2>/dev/null

echo performance > /sys/devices/system/cpu/cpu3/cpufreq/scaling_governor 2>/dev/null
echo 0 > /sys/devices/system/cpu/cpu3/cpufreq/scaling_min_freq 2>/dev/null

echo performance > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor 2>/dev/null
echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq 2>/dev/null

echo performance > /sys/devices/system/cpu/cpu5/cpufreq/scaling_governor 2>/dev/null
echo 0 > /sys/devices/system/cpu/cpu5/cpufreq/scaling_min_freq 2>/dev/null

echo performance > /sys/devices/system/cpu/cpu6/cpufreq/scaling_governor 2>/dev/null
echo 0 > /sys/devices/system/cpu/cpu6/cpufreq/scaling_min_freq 2>/dev/null

echo performance > /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor 2>/dev/null
echo 0 > /sys/devices/system/cpu/cpu7/cpufreq/scaling_min_freq 2>/dev/null
echo "【OK】CPU优化完成"
sleep 0.5

#=====2 GPU优化=====
echo ">> 开始GPU优化"
if [ -d /sys/class/kgsl/kgsl-3d0 ];then
    echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null
    cat /sys/class/kgsl/kgsl-3d0/max_gpuclk > /sys/class/kgsl/kgsl-3d0/gpuclk 2>/dev/null
    echo 0 > /sys/class/kgsl/kgsl-3d0/disable_ebuf 2>/dev/null
fi
find /sys/class/mali* -name devfreq 2>/dev/null | while read gpath;do
    echo performance > $gpath/governor 2>/dev/null
done
echo "【OK】GPU优化完成"
sleep 0.5

#=====3 温控封堵=====
echo ">> 开始温控封堵优化"
find /sys/devices/virtual/thermal -path "*thermal_zone*" 2>/dev/null | while read tpath;do
echo disabled > $tpath/mode 2>/dev/null
done
[ -f /sys/module/msm_thermal/parameters/enabled ] && echo N > /sys/module/msm_thermal/parameters/enabled 2>/dev/null
[ -f /sys/module/mtk_thermal/parameters/enable ] && echo 0 > /sys/module/mtk_thermal/parameters/enable 2>/dev/null
echo "【OK】温控封堵完成"
sleep 0.5

#=====全品牌云控分区=====
echo "=====全机型云控拦截优化启动====="
sleep 0.5

#==========二、红魔 nubia/redmagic 云控&负优化禁用（免Root pm disable-user）==========
if [[ $BRAND == "nubia" || $BRAND == "redmagic" ]];then
echo "[红魔] 开始冻结云控、负优化、广告管控套件"
sleep 0.3
# 1. 红魔核心云控、性能远程调度（关键，解除官方降频、温控云策略）
pm disable-user --user 0 com.nubia.optimize.service 2>/dev/null
pm disable-user --user 0 com.nubia.cloudcontrol 2>/dev/null
pm disable-user --user 0 com.redmagic.performance.cloud 2>/dev/null
pm disable-user --user 0 com.nubia.thermal.remote 2>/dev/null

# 2. 广告、统计、用户体验收集组件
pm disable-user --user 0 com.nubia.analytics 2>/dev/null
pm disable-user --user 0 com.nubia.advertise 2>/dev/null
pm disable-user --user 0 com.nubia.userstat 2>/dev/null
pm disable-user --user 0 com.nubia.report 2>/dev/null

# 3. 负优化后台、智能省电限制帧率/性能模块
pm disable-user --user 0 com.nubia.powersaver 2>/dev/null
pm disable-user --user 0 com.nubia.smartpower 2>/dev/null
pm disable-user --user 0 com.nubia.framecontrol 2>/dev/null
pm disable-user --user 0 com.redmagic.gameoptimizer 2>/dev/null

# 4. 推送、远程参数下发、系统策略更新
pm disable-user --user 0 com.nubia.push 2>/dev/null
pm disable-user --user 0 com.nubia.config.sync 2>/dev/null
pm disable-user --user 0 com.nubia.system.patch 2>/dev/null

# 强制停止已禁用常驻进程
am force-stop com.nubia.optimize.service 2>/dev/null
am force-stop com.nubia.cloudcontrol 2>/dev/null
am force-stop com.nubia.powersaver 2>/dev/null

echo "[红魔] 云控拦截、负优化禁用执行完成"
fi

#=============一、小米/红米 HyperOS/MIUI 负优化禁用【性能云控+广告+后台管控】=============
if [[ $BRAND == "xiaomi" || $BRAND == "redmi" ]];then
echo "[小米/红米] 开始冻结负优化管控套件"
# 【保留】温控、帧率、省电核心服务，不禁用！避免锁60帧

#MSA广告、数据统计、远程参数推送
pm disable-user --user 0 com.miui.msa.global 2>/dev/null
pm disable-user --user 0 com.miui.analytics 2>/dev/null
pm disable-user --user 0 com.xiaomi.ab 2>/dev/null
pm disable-user --user 0 com.xiaomi.micloud.sdk 2>/dev/null

#负优化场景引擎、智能省电后台（非帧率核心，可禁）
pm disable-user --user 0 com.miui.powercenter 2>/dev/null
pm disable-user --user 0 com.xiaomi.joyose 2>/dev/null
pm disable-user --user 0 com.miui.systemAdSolution 2>/dev/null

#快应用、资讯推送冗余组件
pm disable-user --user 0 com.miui.hybrid 2>/dev/null
pm disable-user --user 0 com.mi.globalminusscreen 2>/dev/null

# 只停止广告/冗余进程，不杀温控、帧率服务
am force-stop com.xiaomi.joyose 2>/dev/null
echo "✅广告&冗余服务禁用完毕"
sleep 0.3
echo "【优化了锁60帧的问题】"


sleep 0.3
echo "【迅雷】搜索: 欧ouge宝库，持续更新"
echo "这款模块免费，不许倒卖"
fi
sleep 0.3


#====================2.OPPO/realme/一加 禁云控====================
if [[ $BRAND == "oppo" || $BRAND == "realme" || $BRAND == "oneplus" ]];then
echo "【OPPO/真我/一加】开始冻结ColorOS性能云控"
settings put global persist.oplus.power.cloud 0 2>/dev/null
settings put global persist.oplus.thermal.cloud 0 2>/dev/null
settings put global coloros_battery_cloud_ctrl 0 2>/dev/null

pm disable-user --user 0 com.oplus.cosa 2>/dev/null
pm disable-user --user 0 com.oplus.athena 2>/dev/null
pm disable-user --user 0 com.oplus.powermonitor 2>/dev/null
pm disable-user --user 0 com.coloros.sceneservice 2>/dev/null
pm disable-user --user 0 com.oplus.battery.cloud 2>/dev/null
pm disable-user --user 0 com.oplus.statistics 2>/dev/null
am force-stop com.oplus.cosa 2>/dev/null
echo "✅OPPO/一加/真我全系云控全部禁用完成"
 
 sleep 0.3

# 1.核心调度锁频服务（潮汐、COSA雅典娜、功耗监控）
pm disable-user --user 0 com.oplus.cosa 2>/dev/null
pm disable-user --user 0 com.oplus.athena 2>/dev/null
pm disable-user --user 0 com.oplus.powermonitor 2>/dev/null
pm disable-user --user 0 com.coloros.sceneservice 2>/dev/null

# 2.云端远程温控、云端电池策略（远程下发锁频规则源头）
pm disable-user --user 0 com.oplus.battery.cloud 2>/dev/null
pm disable-user --user 0 com.oplus.statistics 2>/dev/null
pm disable-user --user 0 com.oplus.thermalengine 2>/dev/null
pm disable-user --user 0 com.oplus.thermalcontrol 2>/dev/null

# 3.【游戏锁频关键新增】游戏空间、游戏功耗限制、帧率管控组件（免Root可禁用）
pm disable-user --user 0 com.oplus.gamepower 2>/dev/null
pm disable-user --user 0 com.coloros.gameassistant 2>/dev/null
pm disable-user --user 0 com.oplus.gamemonitor 2>/dev/null
pm disable-user --user 0 com.oplus.gameoptimize 2>/dev/null

# 4.全局省电、智能功耗调度组件
pm disable-user --user 0 com.coloros.powersaver 2>/dev/null
pm disable-user --user 0 com.oplus.smartpower 2>/dev/null

# 5.无关广告组件
pm disable-user --user 0 com.coloros.adservice 2>/dev/null
pm disable-user --user 0 com.oplus.quicksearch 2>/dev/null

# 强制终止正在运行的锁频进程
am force-stop com.oplus.cosa com.oplus.athena com.oplus.thermalengine com.oplus.gamepower 2>/dev/null

# 免Root全局强制开启性能模式，关闭省电策略
settings put system power_save_mode 0
settings put global performance_mode 1
settings put secure game_performance_strategy 0
settings put global power_saver_enable 0

echo "【新增】 测试中 、解除游戏锁频、"
sleep 0.2

 echo "【迅雷】搜索: 欧ouge宝库，持续更新"
 
 
 echo "这款模块免费，不许倒卖"

 fi
sleep 0.2

#====================3.vivo/iQOO 新增免ROOT禁用云控====================
if [[ $BRAND == "vivo" || $BRAND == "iqoo" ]];then
echo "【vivo/iQOO】拦截OriginOS云端性能云控"
#关闭云端省电、远程温控开关
settings put global persist.vivo.power.cloud_ctrl 0 2>/dev/null
settings put global persist.vivo.thermal.cloud_limit 0 2>/dev/null
settings put global vivo_battery_cloud_saver 0 2>/dev/null

#!/system/bin/sh
echo "=====批量禁用全机型负优化系统组件====="
SOC=$(getprop ro.board.platform)
BRAND=$(getprop ro.product.brand | tr '[A-Z]' '[a-z]')
sleep 0.2

#=============一、小米/红米 HyperOS/MIUI 负优化禁用【性能云控+广告+后台管控】=============
if [[ $BRAND == "xiaomi" || $BRAND == "redmi" ]];then
 "✅广告&冗余服务禁用完毕"
 
 sleep 0.3
#MSA广告、数据统计、远程参数推送
pm disable-user --user 0 com.miui.msa.global 2>/dev/null
pm disable-user --user 0 com.miui.analytics 2>/dev/null
pm disable-user --user 0 com.xiaomi.ab 2>/dev/null
pm disable-user --user 0 com.xiaomi.micloud.sdk 2>/dev/null
#负优化场景引擎、智能省电后台
pm disable-user --user 0 com.miui.powercenter 2>/dev/null
pm disable-user --user 0 com.xiaomi.joyose 2>/dev/null
pm disable-user --user 0 com.miui.systemAdSolution 2>/dev/null
#快应用、资讯推送冗余组件
pm disable-user --user 0 com.miui.hybrid 2>/dev/null
pm disable-user --user 0 com.mi.globalminusscreen 2>/dev/null
am force-stop com.xiaomi.thermal com.xiaomi.joyose 2>/dev/null
echo "✅小米全系负优化服务禁用完毕"

sleep 0.3

 echo "【迅雷】搜索: 欧ouge宝库，持续更新"
 
   echo "这款模块免费，不许倒卖"
 
fi
sleep 0.2

#=============三、vivo/iQOO OriginOS 蓝厂性能云控+智能调度负优化=============
if [[ $BRAND == "vivo" || $BRAND == "iqoo" ]];then
echo "[vivo/iQOO] 冻结Multi-Turbo后台管控、云端温控组件"
#性能限制、温控引擎、功耗守卫
pm disable-user --user 0 com.vivo.thermal.engine 2>/dev/null
pm disable-user --user 0 com.vivo.powerguard 2>/dev/null
pm disable-user --user 0 com.vivo.batterysaver.cloud 2>/dev/null
pm disable-user --user 0 com.vivo.scenectrl 2>/dev/null
#日志收集、广告推送、场景智能降频
pm disable-user --user 0 com.android.bbklog 2>/dev/null
pm disable-user --user 0 com.vivo.analytics 2>/dev/null
pm disable-user --user 0 com.vivo.pushservice 2>/dev/null
pm disable-user --user 0 com.bbk.iqoo.logsystem 2>/dev/null
am force-stop com.vivo.thermal.engine com.vivo.powerguard 2>/dev/null
echo "✅OriginOS负优化后台禁用完毕"

sleep 0.3

 echo "【迅雷】搜索: 欧ouge宝库，持续更新"
 
  echo "这款模块免费，不许倒卖"

fi
sleep 0.2

#=============四、通用安卓负优化组件(全机型生效：谷歌适配、健康管控)=============
echo "[通用] 禁用安卓原生耗电管控组件"
pm disable-user --user 0 com.google.android.apps.wellbeing 2>/dev/null
pm disable-user --user 0 com.android.traceur 2>/dev/null
pm disable-user --user 0 com.android.providers.userdictionary 2>/dev/null

echo "[√]${BRAND}-${SOC} 全部负优化软件禁用完成"

#====================4、全机型通用底层内核优化【ROOT生效】====================
echo ">> 开始系统内核底层调度优化"
#1.内存vm内核参数优化，减少频繁杀后台、页面抖动
echo 128 > /proc/sys/vm/min_free_kbytes 2>/dev/null
echo 30 > /proc/sys/vm/swappiness 2>/dev/null
echo 50 > /proc/sys/vm/vfs_cache_pressure 2>/dev/null
echo 0 > /proc/sys/vm/oom_kill_allocating_task 2>/dev/null
echo 1 > /proc/sys/vm/pagecache_limit 2>/dev/null

#2.IO磁盘调度优化，闪存读写提速、关闭磁盘节能
for dev in /sys/block/*/queue;do
echo noop > $dev/scheduler 2>/dev/null
echo 256 > $dev/nr_requests 2>/dev/null
echo 0 > $dev/depth 2>/dev/null
done

#3.关闭内核CPU深度休眠（阻止空闲大核自动深度掉频休眠）
find /sys/devices/system/cpu/cpu*/cpuidle/state* -name disable 2>/dev/null | xargs -I {} echo 1 > {} 2>/dev/null

#4.高通(QCOM/真我/小米骁龙)专属底层优化
if [[ $SOC =~ qcom|sun ]];then
echo ">> 高通平台专属底层优化"
#关闭GPU动态调压DCVS、功耗门限
[ -d /sys/class/kgsl/kgsl-3d0/pwrrail ] && echo 0 > /sys/class/kgsl/kgsl-3d0/pwrrail/disable_dcvs 2>/dev/null
#关闭高通CPU功耗协处理器降频管控
echo performance > /sys/devices/platform/soc/soc:qcom,cpubw/governor 2>/dev/null
echo performance > /sys/devices/platform/soc/soc:qcom,devfreq-l3/governor 2>/dev/null
#关闭高通温控功耗节流
echo 0 > /sys/module/msm_thermal/parameters/thermal_throttle 2>/dev/null
fi

#5.联发科平台专属底层优化
if [[ $SOC =~ mt|mtk ]];then
echo ">> 联发科平台专属底层优化"
find /sys/class/mali*/governor -name governor 2>/dev/null | xargs -I {} echo performance > {} 2>/dev/null
echo 0 > /sys/module/mtk_thermal/parameters/throttle_enable 2>/dev/null
fi
echo "【OK】内核底层调度优化完成"
sleep 0.3

#====================5、免ROOT系统全局优化（全机型通用，无ROOT也生效）====================
#强制GPU渲染
settings put global force_gpu_rendering 1 2>/dev/null
#关闭系统后台冻结、省电待机限制
settings put global app_standby_enabled 0 2>/dev/null
settings put global auto_restrict_background 0 2>/dev/null
settings put global cached_apps_freezer 0 2>/dev/null
settings put global low_power 0 2>/dev/null

#====GPU调试层+进阶渲染开关====
#开启GPU调试层（开发者选项开关打开）
settings put global enable_gpu_debug_layers 1 2>/dev/null
settings put global enable_gpu_debug_layers_apps "" 2>/dev/null
#停用HW硬件叠加层，GPU负载释放
settings put global debug.hwui.disable_hw_layers 1 2>/dev/null
#开启4倍MSAA抗锯齿，画面更顺滑
settings put global persist.hwui.msaa 1 2>/dev/null

#====================6、全品牌批量禁用负优化后台软件(追加)====================
#小米追加
if [[ $BRAND == "xiaomi" || $BRAND == "redmi" ]];then
pm disable-user --user 0 com.xiaomi.joyose 2>/dev/null
pm disable-user --user 0 com.miui.powercenter 2>/dev/null
pm disable-user --user 0 com.miui.systemAdSolution 2>/dev/null
pm disable-user --user 0 com.miui.hybrid 2>/dev/null
fi
#OPPO/真我追加
if [[ $BRAND == "oppo" || $BRAND == "realme" || $BRAND == "oneplus" ]];then
pm disable-user --user 0 com.coloros.adservice 2>/dev/null
pm disable-user --user 0 com.oplus.quicksearch 2>/dev/null
pm disable-user --user 0 com.oplus.thermalengine 2>/dev/null
fi
#vivo追加
if [[ $BRAND == "vivo" || $BRAND == "iqoo" ]];then
pm disable-user --user 0 com.android.bbklog 2>/dev/null
pm disable-user --user 0 com.vivo.pushservice 2>/dev/null
pm disable-user --user 0 com.bbk.iqoo.logsystem 2>/dev/null
fi
#通用安卓负优化组件
pm disable-user --user 0 com.google.android.apps.wellbeing 2>/dev/null
pm disable-user --user 0 com.android.traceur 2>/dev/null

#冻结vivo核心性能云控包
pm disable-user --user 0 com.vivo.thermal.engine 2>/dev/null
pm disable-user --u

#自动识别品牌，适配GPU调试层开关
if [[ $BRAND == "realme" || $BRAND == "oppo" || $BRAND == "oneplus" ]];then
    #ColorOS/真我/一加 专属参数
    settings put global persist.graphics.gpu_debug_layer_enable 1 2>/dev/null
    settings put global persist.graphics.gpu_debug_app_list "" 2>/dev/null
elif [[ $BRAND == "xiaomi" || $BRAND == "redmi" ]];then
    #小米HyperOS原生参数
    settings put global enable_gpu_debug_layers 1 2>/dev/null
    settings put global enable_gpu_debug_layers_apps "" 2>/dev/null
elif [[ $BRAND == "vivo" || $BRAND == "iqoo" ]];then
    #vivo OriginOS参数
    settings put global persist.vulkan.debug.layer_enable 1 2>/dev/null
fi

#通用GPU渲染优化（全机型通用不变）
settings put global debug.hwui.disable_hw_layers 1 2>/dev/null
settings put global persist.hwui.msaa 1 2>/dev/null
get_tys() {
    max_rate=$(dumpsys display 2>/dev/null | grep -Eo 'refreshRate[= ]*[0-9.]+' | grep -Eo '[0-9.]+' | sort -n | tail -1)
    [ -z "$max_rate" ] && max_rate=$(settings get system peak_refresh_rate 2>/dev/null)
    [ -z "$max_rate" ] || [ "$max_rate" = "null" ] && max_rate=120.0
    tys=$(printf "%.0f" "$max_rate")
    [ "$tys" -lt 60 ] && tys=60
    [ "$tys" -gt 240 ] && tys=240
    echo "$tys"
}

brand=$(getprop ro.product.brand | tr '[:upper:]' '[:lower:]')
echo "品牌：$brand"

settings put global window_animation_scale 0.35
settings put global transition_animation_scale 0.35
settings put global animator_duration_scale 0.35

settings put global force_gpu_rendering 1
setprop debug.sf.hw 1 2>/dev/null
echo "GPU 加速~"

tys=$(get_tys)
settings put system peak_refresh_rate "$tys"
settings put system min_refresh_rate "$tys"
setprop persist.graphics.fps.limit "$tys" 2>/dev/null
echo "帧率锁死 $tys Hz"

setprop debug.sf.vsync_reactor 0 2>/dev/null
echo "垂直同步关闭"

case "$brand" in
    xiaomi|redmi)
        settings put system touch_mode 2
        settings put system touch_sensitivity 1
        pm disable --user 0 com.miui.analytics 2>/dev/null
        pm disable --user 0 com.miui.msa.global 2>/dev/null
        echo "红米触控&广告冻结"
        ;;
    vivo|iqoo)
        settings put system power_save_type 7
        settings put secure power_save_type 7
        settings put secure system_property_power_mode_type 7
        settings put system game_plus_mode_key 1
        settings put system smart_touch_mode 0
        settings put system game_scene_more_fps 1
        settings put system bench_mark_mode 1
        settings put secure vivo_sync_game_switch_status "*"
        settings put system vivo_game_gyro_dealy_promotion "*"
        settings put system vivo_game_gyro_anti_shake_promotion "*"
        pm disable --user 0 com.vivo.pem 2>/dev/null
        pm disable --user 0 com.vivo.eas 2>/dev/null
        echo "vivoiqoo 增强plus哦😮"
        ;;
    oppo|oneplus|realme)
        settings put system touch_sensitivity 1
        settings put system power_mode high
        pm disable --user 0 com.oplus.cosa 2>/dev/null
        pm disable --user 0 com.oplus.athena 2>/dev/null
        echo "OPPO 性能&触控"
        ;;
    honor|huawei)
        settings put system touch_screen_optimization 1
        settings put system power_mode 2
        pm disable --user 0 com.huawei.powergenie 2>/dev/null
        echo "华为触控&性能"
        ;;
    samsung)
        settings put system touch_sensitivity_mode 1
        settings put global sem_perf_mode 1
        pm disable --user 0 com.samsung.android.game.gametools 2>/dev/null
        echo "三星触控&性能"
        ;;
    *)
        settings put global high_performance_mode 1
        echo "通用性能增强"
        ;;
esac

svc nfc disable 2>/dev/null
pm disable --user 0 com.android.printspooler 2>/dev/null
echo "NFC

TYSNB="/data/user_de/0/com.android.shell/axeron/plugins/xnPro+/service.sh"
if [ -f "$TYSNB" ]; then
    pkill -f "$TYSNB" 2>/dev/null
    nohup sh "$TYSNB" >/dev/null 2>&1 &
    echo "暴力 PID $!"
fi
echo "撸完了"




 sleep 0.3

 echo "【迅雷】搜索: 欧ouge宝库，持续更新"
