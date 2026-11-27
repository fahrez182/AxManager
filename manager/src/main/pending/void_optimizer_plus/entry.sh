#!/system/bin/sh
# ============================================================
# VOID OPTIMIZER PLUS — entry point (v6.0)
# Auto-discovers every G*.sh module under modules/ so new drops
# require no code change. G8_google_fcm_protect ALWAYS runs last
# to guarantee FCM push notifications keep working.
# Per-module timing is logged.
# ============================================================

VOPL_DIR="${0%/*}"
[ ! -d "$VOPL_DIR" ] && VOPL_DIR="$(cd "$(dirname "$0")" && pwd)"
[ -z "$VOPL_DIR" ] || [ "$VOPL_DIR" = "." ] && VOPL_DIR="$(pwd)"

# shellcheck source=./lib/plus_core.sh
. "$VOPL_DIR/lib/plus_core.sh"

vopl_init

# v4.1: single pm-list cache shared with every child module.
vopl_pm_cache_load
export VOPL_PM_CACHE
# v6.0: share the dumpsys cache dir too, so each child module reuses
# the same per-package cache instead of building its own.
export VOPL_DUMP_CACHE_DIR

vopl_log INFO "=== entry.sh START (v6.0) ==="

# --- sort helper: natural order on the G-prefix, keep G8 last ------
_vopl_module_order() {
    ls -1 "$VOPL_DIR/modules/"G*.sh 2>/dev/null | \
        awk -F/ '{print $NF}' | \
        awk '{
            n = $0
            sub(/^G/, "", n)
            sub(/_.*$/, "", n)
            printf "%05d\t%s\n", n, $0
        }' | \
        sort -n | \
        cut -f2- | \
        grep -v '^G8_google_fcm_protect\.sh$'
    # FCM protect always last
    [ -f "$VOPL_DIR/modules/G8_google_fcm_protect.sh" ] && \
        echo "G8_google_fcm_protect.sh"
}

run_module() {
    local script="$VOPL_DIR/modules/$1"
    local name="${1%.sh}"
    [ -f "$script" ] || { printf '  [plus] %-42s ... missing\n' "$name"; return; }
    local t0 t1 dt
    t0=$(date +%s 2>/dev/null); [ -z "$t0" ] && t0=0
    printf '  [plus] %-42s ... ' "$name"
    if sh "$script" apply >/dev/null 2>&1; then
        t1=$(date +%s 2>/dev/null); [ -z "$t1" ] && t1=$t0
        dt=$((t1 - t0))
        echo "ok (${dt}s)"
        vopl_log INFO "$name applied in ${dt}s"
    else
        echo "skip/fail"
        vopl_log WARN "$name skip/fail"
    fi
}

TOTAL=0
for m in $(_vopl_module_order); do
    run_module "$m"
    TOTAL=$((TOTAL+1))
done

vopl_log INFO "=== entry.sh DONE ($TOTAL modules) ==="
