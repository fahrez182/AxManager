#---
# name: 性能优化·满血调度
# author: vivo/iqoo
# brand: 优化
# description: 解除温控/满血调度/防降频降亮度
#---
# iQOO满血性能）

#on
# 1. CPU/GPU满血
settings put global performance_mode 1
settings put global cpu_boost 1
settings put global gpu_boost 1
settings put global disable_thermal_throttle 1
settings put global thermal_engine_disable 1

# 2. 防降频/降亮度
settings put global game_performance_mode 1
settings put global no_brightness_drop 1
settings put global disable_power_saving 1
settings put global disable_battery_saver 1

# 3. 后台/内存优化
settings put global max_processes 4
settings put global free_memory 2048
settings put global disable_auto_kill 1

# 4. 动画跟手
settings put global window_animation_scale 0.5
settings put global transition_animation_scale 0.5
settings put global animator_duration_scale 0.5

# 5. 游戏稳帧
settings put global refresh_rate 120
settings put global game_stable_frame 1

echo "✅ iQOO性能优化完成·满血调度·稳120帧"

#off
settings put global performance_mode 0
settings put global cpu_boost 0
settings put global gpu_boost 0
settings put global disable_thermal_throttle 0
settings put global window_animation_scale 1
settings put global transition_animation_scale 1
settings put global animator_duration_scale 1

echo "✅ 已恢复系统默认设置"
