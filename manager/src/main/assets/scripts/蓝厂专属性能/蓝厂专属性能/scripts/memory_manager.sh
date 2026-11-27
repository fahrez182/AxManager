#!/sbin/sh
MODDIR="${0%/*}"
source "$MODDIR/../functions.sh"
memory_main() {
    while true; do
        local current_time=$(date +%s)
        local last_cleanup=$(grep "last_cleanup" "$AI_STATE_FILE" 2>/dev/null | grep -oE "[0-9]+" | head -1)
        [ -z "$last_cleanup" ] && last_cleanup=0
        local diff=$((current_time - last_cleanup))
        local mode=$(ai_get_mode)
        if [ "$diff" -ge 86400 ]; then
            ax_print "[内存清理] 执行每日深度清理..."
            sync; echo "3" > /proc/sys/vm/drop_caches 2>/dev/null
            find /sdcard/ -type d -empty -delete 2>/dev/null
            find /data/data/ -type d -empty -delete 2>/dev/null
            find /data/local/tmp/ -type f -mtime +1 -delete 2>/dev/null
            find /data/log/ -type f -mtime +3 -delete 2>/dev/null
            find /data/vendor/log/ -type f -mtime +3 -delete 2>/dev/null
            find /data/system/dropbox/ -type f -mtime +3 -delete 2>/dev/null
            pm trim-caches 2G 2>/dev/null
            ai_write_state "last_cleanup" "$current_time"
            ai_log_decision "内存清理" "每日深度清理完成"
            ax_print "[内存清理] 完成"
        fi
        if [ "$mode" = "gaming" ] && [ "$diff" -ge 3600 ]; then
            echo "3" > /proc/sys/vm/drop_caches 2>/dev/null
            ai_log_decision "内存清理-游戏前" "游戏前预清理"
        fi
        sleep 3600
    done
}
memory_main
