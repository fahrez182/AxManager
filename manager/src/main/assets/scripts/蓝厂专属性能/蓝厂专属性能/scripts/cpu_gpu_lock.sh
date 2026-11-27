#!/sbin/sh
MODDIR="${0%/*}"
source "$MODDIR/../functions.sh"
ax_print "[处理器优化] 检测芯片..."
dimensity_9400_optimize() {
    echo "performance" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null
    echo "performance" > /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor 2>/dev/null
    echo "performance" > /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor 2>/dev/null
    echo "performance" > /sys/devices/system/cpu/cpu3/cpufreq/scaling_governor 2>/dev/null
    echo "performance" > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor 2>/dev/null
    echo "performance" > /sys/devices/system/cpu/cpu5/cpufreq/scaling_governor 2>/dev/null
    echo "performance" > /sys/devices/system/cpu/cpu6/cpufreq/scaling_governor 2>/dev/null
    echo "performance" > /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor 2>/dev/null
    if [ -d /sys/class/misc/mali0 ]; then
        echo "1" > /sys/class/misc/mali0/device/clk_enable 2>/dev/null
        echo "0" > /sys/class/misc/mali0/device/thermal_limit 2>/dev/null
    fi
    echo "high" > /sys/class/devfreq/ddr/available_governors 2>/dev/null
    echo "0" > /sys/devices/system/cpu/eas/enable 2>/dev/null
    ax_info "天玑9400+专属优化已应用"
}
cpu_gpu_main() {
    while true; do
        local mode=$(ai_get_mode)
        local chip=$(getprop ro.hardware)
        if echo "$chip" | grep -qi "mt6991\|dimensity"; then
            ai_write_state "chip_type" "dimensity_9400_plus"
            if [ "$mode" = "gaming" ]; then
                dimensity_9400_optimize
                ai_write_state "cpu_lock_status" "locked"
                ai_write_state "gpu_lock_status" "locked"
                ai_log_decision "CPU/GPU锁定" "游戏模式锁定天玑9400+全核最高性能"
            else
                cpu_governor_balanced
                ai_write_state "cpu_lock_status" "unlocked"
                ai_write_state "gpu_lock_status" "unlocked"
            fi
        else
            ai_write_state "chip_type" "unknown"
            ax_warn "非天玑9400+芯片，使用通用优化"
            if [ "$mode" = "gaming" ]; then
                cpu_governor_performance
            fi
        fi
        sleep 10
    done
}
cpu_gpu_main
