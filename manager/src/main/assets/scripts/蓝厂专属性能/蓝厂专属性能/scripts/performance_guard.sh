#!/sbin/sh
MODDIR="${0%/*}"
source "$MODDIR/../functions.sh"
guard_main() {
    while true; do
        local modules="game_detector.sh thermal_manager.sh frame_manager.sh network_manager.sh touch_manager.sh audio_manager.sh battery_manager.sh memory_manager.sh ai_brain.sh"
        for mod in $modules; do
            if ! pgrep -f "$mod" > /dev/null 2>&1; then
                ax_warn "守护进程丢失: $mod，重新启动..."
                nohup sh "$MODDIR/$mod" > /dev/null 2>&1 &
                ai_log_decision "进程守护" "$mod 崩溃重启"
            fi
        done
        local mode=$(ai_get_mode)
        if [ "$mode" = "gaming" ]; then
            local gov=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null)
            if [ "$gov" != "performance" ]; then
                cpu_governor_performance
                ai_log_decision "稳态维持" "CPU调度被覆盖，已恢复"
            fi
            local min_hz=$(settings get system min_refresh_rate 2>/dev/null)
            if [ "$min_hz" != "144" ] && [ "$min_hz" != "120" ]; then
                frame_lock_high
                ai_log_decision "稳态维持" "帧率被覆盖，已恢复"
            fi
        fi
        thermal_disable
        sleep 10
    done
}
guard_main
