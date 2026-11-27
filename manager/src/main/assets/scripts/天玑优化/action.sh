#!/system/bin/sh
# ============================================================
# POWER BY SASUKETWEAKS
# 联发科优化器
# ============================================================

echo " "
echo "══════════════════════════════════════════════════════════"
echo "      [⚡] 联发科芯片优化开始 [⚡]                         "
echo "══════════════════════════════════════════════════════════"
echo " "

# ============================================================
# MTK 专用硬件调优（核心逻辑）
# ============================================================
echo "[🔧] 提升联发科性能..."

# MTK GPU 特定优化（Mali GPU）
# 这些路径是联发科设备的标准路径
setprop debug.mali.enable 1
setprop debug.mali.hang 0
setprop debug.mali.egl 1
setprop vendor.mali.platform.plugin 1

# MTK 专用性能提示
setprop persist.vendor.sys.hwui.renderer skiagl
setprop vendor.gpu.platform.plugin 1
setprop debug.hwui.renderer skiagl
setprop debug.hwui.level 1

# 强制高性能电源策略
# 告知 MTK 性能模块提升时钟频率
setprop vendor.powerhal.init 1
setprop vendor.powerhal.svc.sched 1
setprop vendor.powerhal.svc.irq 1
setprop vendor.powerhal.svc.power 1

# 尝试通过 dumpsys 触发性能提示（在某些 MTK ROM 上有效）
dumpsys android.os.IPowerManagerService enable 1 2>/dev/null

# 强制 GPU 渲染
settings put global force_gpu_rendering 1
settings put global hardware_acceleration_enabled 1

# 禁用省电功能
settings put global low_power_mode_stickiness_off_when_plugged 1
settings put global adaptive_battery_management_enabled 0
settings put global app_standby_enabled 0

# 立即提升游戏包优先级
# 通知系统这些应用需要高资源
cmd activity schedule-app-start com.garena.game.codm
cmd activity schedule-app-start com.mobile.legends

# MTK 视频加速
setprop video.accelerate.hw 1
setprop video.accelerate.mtk 1
setprop mtk.decode.accelerate 1

# 禁用 MTK 省电功能（防止降频）
setprop persist.vendor.pms_stable 1
setprop persist.vendor.pms_enable 1
setprop persist.sys.mtk_pms 0

# ============================================================
# FPS 稳定器（无掉帧逻辑）
# ============================================================
echo "[📊] 稳定帧率..."

# 强制 GPU 渲染（减少联发科 big.LITTLE 架构的 CPU 负载）
settings put global force_gpu_rendering 1
settings put global hardware_acceleration_enabled 1

# 禁用帧缓冲翻转（在 Mali GPU 上平滑视觉效果）
setprop debug.gr.swapinterval 0
setprop debug.gr.numframebuffers 4
setprop debug.sf.hw 1
setprop debug.egl.hw 1

# 禁用 HWUI 过度绘制（减少 GPU 负载）
setprop debug.hwui.show_dirty_regions false
setprop debug.hwui.use_partial_updates false
setprop debug.hwui.disable_draw_defer true

# ============================================================
# MTK 温控缓解（减少热降频）
# ============================================================
echo "[❄️] 管理联发科温控限制..."

# 这些设置尝试通过系统属性放松温控策略
setprop vendor.thermal.config game
setprop persist.vendor.thermal.config game
setprop debug.thermal.game 1
setprop persist.sys.thermal.mode 1

# 禁用标准温控钩子
settings put global thermal_disabled 1 2>/dev/null

# ============================================================
# 内存管理
# ============================================================
echo "[🧹] 优化联发科内存..."

# 联发科设备从激进缓存清理中获益显著
sync
echo 3 > /proc/sys/vm/drop_caches 2>/dev/null

# 如果可访问，清除 ZRAM
swapoff /dev/block/zram0 2>/dev/null
swapon /dev/block/zram0 2>/dev/null

# 激进后台清理
am kill-all
settings put global app_standby_enabled 0
settings put global adaptive_battery_management_enabled 0

# ============================================================
# 延迟和触控优化
# ============================================================
echo "[⚡] 降低延迟和触控响应..."

# 移除动画
settings put global window_animation_scale 0
settings put global transition_animation_scale 0
settings put global animator_duration_scale 0

# MTK 触控加速提示
setprop persist.sys.mtk.touch_boost 1
setprop debug.input_latency 0
settings put system pointer_speed 7

# ============================================================
# FPS 稳定器（新增）
# ============================================================
echo "[🔋] FPS 稳定器 - 联发科..."

# 锁定 GPU 频率提示（联发科专用）
setprop vendor.gpu.freq.min 0
setprop vendor.gpu.freq.max 0

# 禁用 FPS 抖动
setprop debug.sf.latch_unsignaled 0
setprop debug.sf.early_phase_offset_ns 500000
setprop debug.sf.late_phase_offset_ns 1500000

# 强制平滑帧率控制
setprop persist.sys.sf.comp_peer_standby 0
setprop debug.gr.num_buffers 4

# ============================================================
# 快速多任务优化（新增）
# ============================================================
echo "[🚀] 提升多任务性能..."

# MTK 调度器优化
setprop vendor.perf.ioprio 1
setprop persist.vendor.ioprio 1

# 增加缓存应用限制
settings put global activity_manager_constants max_cached_processes=64

# 禁用应用冷冻（更快应用恢复）
settings put global cached_apps_freezer disabled

# 提升 UI 线程优先级
setprop debug.hwui.render_dirty_regions true
setprop debug.hwui.text_large_cache_height 2048
setprop debug.hwui.text_large_cache_width 2048

# 为 CODM 设置游戏渲染覆盖（降低分辨率换取帧率）
cmd device_config put game_overlay com.garena.game.codm "mode=2,downscaleFactor=0.55,fps=120"

# ============================================================
# 游戏模式激活（CODM 和 MLBB）
# ============================================================
echo " "
echo "══════════════════════════════════════════════════════════"
echo "      [🎮] 为联发科激活游戏模式 [🎮]                      "
echo "══════════════════════════════════════════════════════════"

# CODM 联发科优先级
am set-standby-bucket com.garena.game.codm exempted
cmd appops set com.garena.game.codm RUN_IN_BACKGROUND allow
cmd appops set com.garena.game.codm RUN_ANY_IN_BACKGROUND allow
cmd appops set com.garena.game.codm COARSE_LOCATION allow

# MLBB 联发科优先级
am set-standby-bucket com.mobile.legends exempted
cmd appops set com.mobile.legends RUN_IN_BACKGROUND allow
cmd appops set com.mobile.legends RUN_ANY_IN_BACKGROUND allow
cmd appops set com.mobile.legends COARSE_LOCATION allow

# 最终提示
echo " "
echo "══════════════════════════════════════════════════════════"
echo "      [✅] 联发科优化完成 [✅]                            "
echo "══════════════════════════════════════════════════════════"
