#!/system/bin/sh
MODDIR=${0%/*}
LOG="$MODDIR/service.log"
HIST="/data/local/tmp/axelden_hist.txt"
MODEFILE="/data/local/tmp/axelden_mode"
if [ -f "$MODDIR/disable" ]; then exit 0; fi
if [ -f "$MODDIR/remove" ]; then sh "$MODDIR/revert.sh" 2>/dev/null || true; exit 0; fi
[ -f "$LOG" ] && [ $(stat -c%s "$LOG" 2>/dev/null || echo 0) -gt 51200 ] && rm "$LOG"
OK=0; FAIL=0
_log(){ echo "[OMNI60 $(date +%H:%M:%S)] $1" >> "$LOG" 2>/dev/null || true; }
_apply(){
settings put "$2" "$1" "$3" 2>/dev/null
N=$(settings get "$2" "$1" 2>/dev/null)
if [ "$N" = "$3" ]; then OK=$((OK+1)); else FAIL=$((FAIL+1)); _log "fail $1 got [$N] want [$3]"; fi
}
TEMP=$(cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || echo 35000)
CAP=$(cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo 50)
MODE=$(cat "$MODEFILE" 2>/dev/null || echo ultra)
echo "$TEMP" >> "$HIST" 2>/dev/null || true
tail -n 5 "$HIST" > "$HIST.tmp" 2>/dev/null && mv "$HIST.tmp" "$HIST" 2>/dev/null || true
PREV=$(tail -n 2 "$HIST" 2>/dev/null | head -n 1)
[ -z "$PREV" ] && PREV=$TEMP
PRED=$((TEMP + (TEMP - PREV) * 2))
if [ "$PRED" -gt 45000 ] || [ "$CAP" -le 15 ]; then EFF=balanced; else
if [ "$MODE" = "old" ]; then EFF=balanced; else EFF=performance; fi
fi
_log "start MODE=$MODE EFF=$EFF TEMP=$TEMP PRED=$PRED CAP=$CAP"
mkdir -p /data/local/tmp 2>/dev/null || true
if [ ! -f /data/local/tmp/axelden_backup_global.txt ]; then settings list global > /data/local/tmp/axelden_backup_global.txt 2>/dev/null || true; fi
if [ "$MODE" = "old" ]; then
_apply window_animation_scale global 1.0
_apply transition_animation_scale global 1.0
_apply animator_duration_scale global 1.0
else
_apply window_animation_scale global 0.5
_apply transition_animation_scale global 0.5
_apply animator_duration_scale global 0.5
fi
sysctl -w vm.dirty_ratio=10 2>/dev/null && OK=$((OK+1)) || FAIL=$((FAIL+1))
sysctl -w vm.dirty_background_ratio=5 2>/dev/null || true
sysctl -w vm.swappiness=30 2>/dev/null || true
for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo $EFF > "$g" 2>/dev/null || true; done
_log "OK=$OK FAIL=$FAIL"
(
while true; do
T=$(cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || echo 35000)
C=$(cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo 50)
echo "$T" >> "$HIST" 2>/dev/null || true
tail -n 5 "$HIST" > "$HIST.tmp" 2>/dev/null && mv "$HIST.tmp" "$HIST" 2>/dev/null || true
if [ "$T" -gt 45000 ] || [ "$C" -le 15 ]; then echo balanced > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor 2>/dev/null || true; fi
sleep 60
done
) &
