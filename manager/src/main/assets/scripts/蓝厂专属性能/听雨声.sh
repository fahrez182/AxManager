# --- 别瞎机巴刷，仅限 vivo/iQOO 蓝厂设备 ---
BRAND=$(getprop ro.product.brand | tr '[:upper:]' '[:lower:]')
MODEL=$(getprop ro.product.model)

echo "[*] 当前设备: $BRAND $MODEL"

if [[ "$BRAND" != "vivo" && "$BRAND" != "iqoo" ]]; then
    echo "[!] 你这根本不是蓝厂机器 ，你执行你老风呢($BRAND)"
    echo "[!] 别硬执行，等下直接负优化了,等下容易开不了机和性能下降,脚本已强制拦截。"
    exit 1
fi
echo 耐心等待项目走完
pm disable --user 0 com.vivo.gamewatch
sleep 3
echo 正在伪装基准评测环境：解除底层发热降频锁
settings put system bench_mark_mode 1
sleep 3
echo 正在彻底粉碎省电策略：阻断一切限频省电行为
settings put secure power_save_mode 5
settings put secure power_save_type 5
settings put system power_save_type 5
am force-stop com.iqoo.powersaving
echo 正在解锁高帧率白名单：尝试强解游戏场景更多帧率选项
settings put system game_scene_more_fps 1

echo 正在关闭启用标准游戏提速模式：全局游戏魔盒提速
settings put system game_standard_promotion_mode 0

echo 正在强制激活鹰眼显示增强：开启底层游戏画质滤镜
settings put system gamecube_hawkeye_effective 1
echo 正在注入底层温控覆写：强行锁死系统温控状态为绝对凉爽
cmd thermalservice override-status 0

echo 正在强制开启开发者固定性能模式：锁定CPU和GPU频率上限
cmd power set-fixed-performance-mode-enabled true

echo 正在关闭自适应省电行为：防止系统后台偷偷限速
cmd power set-adaptive-power-saver-enabled false
am force-stop com.tencent.mm
settings put system realtime_blur_state 0
settings put system game_thermalservice_switch 1
settings put system is_game_mode=0
echo 关闭游戏魔盒云配置更新
settings put system gamecube_update_8_1_data 0
settings put system thermal_policy_mode 0

service call thermalservice 11 i32 0
echo "关闭vivo iQOO手机自带的省电动态防御模式"

# 1. 彻底禁用动态省电阈值限制
settings put global dynamic_power_savings_enabled 0
settings put global dynamic_power_savings_disable_threshold 0

# 2. 强行关闭电池自动优化（防止系统在后台偷偷限制你的应用）
settings put global adaptive_battery_management_enabled 0

# 3. 清空唤醒锁限制，防止基站信号频繁打断CPU调度
# 注意：这会略微增加功耗，但能换来极致稳定性
cmd appops set 1001 WAKE_LOCK allow
cmd appops set 1000 WAKE_LOCK allow
settings put system thermal_policy_mode 0
service call thermalservice 11 i32 0
echo "正在注入极致渲染相位偏移..."

# 将偏移量从 20ms 下调至 1ms，大幅减少渲染延迟
setprop debug.sf.early_app_phase_offset_ns 1000000
setprop debug.sf.early_gl_app_phase_offset_ns 1000000
setprop debug.sf.early_gl_phase_offset_ns 1000000
setprop debug.sf.early_phase_offset_ns 1000000

# 开启 GL 背压机制，确保渲染管线不堆积
setprop debug.sf.enable_gl_backpressure 1

# 锁定信号锁存，减少显示撕裂风险
setprop debug.sf.latch_unsignaled 1
echo 开启电量守护和智慧引擎隐藏极致性能
settings put system pem_power_start_performance 1
settings put secure bbk_power_strategy_type 5
settings put secure abeapps_power_strategy_type 5
settings put secure abe_power_strategy_type 5
settings put system emMonsterState 1
settings put system pem_clear_flags 1
settings put system pem_down_brightness_percent 0.8
settings put system pem_htp_level 0
settings put system pem_htp_thred 50-52-58-1000-1000-1000
settings put system pem_vlog_1500_exist 1
settings put system pempsmstate 1
echo 开启电量守护隐藏高亮度
settings put system brightness_for_pem 740
sleep 5
echo 强制开启vivo iQOO日常场景中120hz
settings put system peak_refresh_rate 120.0
settings put system min_refresh_rate 120.0
echo 开启通用调整动画减少性能开销
settings put global window_animation_scale 0.5
settings put global transition_animation_scale 0.5
settings put global animator_duration_scale 0.5
echo 关闭数据收集和报错上报
settings put global send_action_report 0
sleep 3
echo 开启隐藏游戏模式
settings put system game_plus_mode_key 1
echo 关闭自适应充电
settings put global optimize_charge_stop_battery 0
echo 关闭电池自适应温控保护
settings put global enabled_high_temperature_battery 0
settings put global disable_battery_temperature_protect 0
settings put global enabled_high_temp_battery_saver 0
settings put global enabled_high_temp_battery 0
echo 关闭外置消息打扰,开启防误处,打造你专属的游戏天堂
settings put system gamecube_shorten_status_bar_trigger_area_on 1
settings put system gamecube_shield_bottom_button_on 1
settings put system gamecube_block_notification_on 1
echo 临时解除game WATCH的帧率动态限制
settings put system gamewatch_game_target_fps 120
echo 关闭游戏魔盒云控更新减少cpu占用温控升级限制
settings put global gamecube_update_competition_system 0
echo 关闭游戏超分可让老机型减少负担
settings put global vsr_value_from_gamecube 0   
echo 尝试覆盖游戏魔盒游戏策略,这条指令必须单独小窗运行
settings put system current_game_package ""
echo 关闭系统属性电源省电（自创有可能是玄学）
settings put system system_property_power_save_type 0
echo 开启系统属性超级性能,真实有效
settings put system system_property_power_mode_type 7
echo 尝试临时关闭vivo iQOO低电量弹窗
settings put system supportLowBatteryNotification 0
echo 防止vivo iQOO抽风，开启省电模式在后台把网络拉入低级黑名单［玄学］
settings put global firewall_reject_wifi_uids ""
echo 关闭gpu预读缓存，针对内存小的可以缓解负担，防止内存压力过高,也可以降低老
机形负载
settings put secure adreno_prefetch_cache_size 0
echo 重启系统界面让配置生效
sleep 1.6
cmd activity crash com.android.systemui
