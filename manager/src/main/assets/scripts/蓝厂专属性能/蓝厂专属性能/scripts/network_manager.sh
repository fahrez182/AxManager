#!/sbin/sh
MODDIR="${0%/*}"
source "$MODDIR/../functions.sh"
ax_print "[网络优化] 启动..."
network_main() {
    while true; do
        local mode=$(ai_get_mode)
        echo "1" > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null
        echo "0" > /proc/sys/net/ipv4/tcp_timestamps 2>/dev/null
        echo "1" > /proc/sys/net/ipv4/tcp_tw_reuse 2>/dev/null
        echo "1" > /proc/sys/net/ipv4/tcp_tw_recycle 2>/dev/null
        echo "0" > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null
        echo "1" > /proc/sys/net/ipv4/tcp_no_metrics_save 2>/dev/null
        echo "10" > /proc/sys/net/ipv4/tcp_fin_timeout 2>/dev/null
        echo "3" > /proc/sys/net/ipv4/tcp_keepalive_probes 2>/dev/null
        echo "10" > /proc/sys/net/ipv4/tcp_keepalive_intvl 2>/dev/null
        echo "524288" > /proc/sys/net/core/rmem_max 2>/dev/null
        echo "524288" > /proc/sys/net/core/wmem_max 2>/dev/null
        echo "262144" > /proc/sys/net/core/rmem_default 2>/dev/null
        echo "262144" > /proc/sys/net/core/wmem_default 2>/dev/null
        echo "65536" > /proc/sys/net/core/netdev_max_backlog 2>/dev/null
        echo "4096" > /proc/sys/net/core/somaxconn 2>/dev/null
        cmd wifi set-wifi-enabled enabled 2>/dev/null
        settings put global wifi_sleep_policy 2 2>/dev/null
        settings put global mobile_data_always_on 1 2>/dev/null
        if [ "$mode" = "gaming" ] || [ "$mode" = "game_window" ]; then
            cmd netpolicy set restrict-background false 2>/dev/null
            echo "1" > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null
            echo "3" > /proc/sys/net/ipv4/tcp_fastopen_key 2>/dev/null
            ai_write_state "network_profile" "gaming"
        else
            ai_write_state "network_profile" "balanced"
        fi
        echo "1" > /proc/sys/net/ipv4/tcp_ecn 2>/dev/null
        local ping_loss=$(ping -c 1 8.8.8.8 2>/dev/null | grep loss | awk -F',' '{print $3}' | awk '{print $1}' | tr -d '%')
        if [ -n "$ping_loss" ] && [ "$ping_loss" -gt 5 ]; then
            ai_log_decision "网络补偿" "丢包${ping_loss}%，触发CPU补偿"
            echo "1" > /proc/sys/kernel/sched_boost 2>/dev/null
        fi
        sleep 8
    done
}
network_main
