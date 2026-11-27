#!/system/bin/sh
# AxDarkGov2Service - verified apply, Orbiter style
MODDIR=${0%/*}
OK=0
FAIL=0
_apply(){
settings put "$2" "$1" "$3" 2>/dev/null
N=$(settings get "$2" "$1" 2>/dev/null)
if [ "$N" = "$3" ]; then OK=$((OK+1)); else FAIL=$((FAIL+1)); fi
}
_apply window_animation_scale global 0.5
_apply transition_animation_scale global 0.5
_apply animator_duration_scale global 0.5
sysctl -w vm.dirty_ratio=10 2>/dev/null || FAIL=$((FAIL+1))
sysctl -w vm.swappiness=30 2>/dev/null || FAIL=$((FAIL+1))
sysctl -w kernel.sched_migration_cost_ns=500000 2>/dev/null || true
for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > "$g" 2>/dev/null || true; done
echo "[AxDarkGov2] OK=$OK FAIL=$FAIL" >> "$MODDIR/service.log" 2>/dev/null || true
