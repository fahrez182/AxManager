#!/sbin/sh
MODDIR="${0%/*}"
source "$MODDIR/../functions.sh"
scheduler_main() {
    while true; do
        local mode=$(ai_get_mode)
        if [ "$mode" = "gaming" ]; then
            echo "-15" > /proc/$(pidof surfaceflinger)/oom_score_adj 2>/dev/null
            echo "-15" > /proc/$(pidof android.hardware.graphics.composer)/oom_score_adj 2>/dev/null
            for game in com.tencent.tmgp.sgame com.tencent.tmgp.pubgmhd com.miHoYo.Yuanshen; do
                local pid=$(pidof "$game" 2>/dev/null)
                if [ -n "$pid" ]; then
                    echo "-10" > /proc/$pid/oom_score_adj 2>/dev/null
                    echo "F0" > /proc/$pid/task/$pid/cpus_allowed 2>/dev/null
                fi
            done
            echo "0" > /proc/sys/kernel/sched_child_runs_first 2>/dev/null
            echo "1000000" > /proc/sys/kernel/sched_min_granularity_ns 2>/dev/null
            echo "4000000" > /proc/sys/kernel/sched_latency_ns 2>/dev/null
            echo "500000" > /proc/sys/kernel/sched_wakeup_granularity_ns 2>/dev/null
            echo "1" > /proc/sys/kernel/sched_autogroup_enabled 2>/dev/null
            echo "0" > /proc/sys/kernel/sched_cfs_bandwidth_slice_us 2>/dev/null
            ai_write_state "scheduler_status" "aggressive"
        else
            ai_write_state "scheduler_status" "balanced"
        fi
        sleep 10
    done
}
scheduler_main
