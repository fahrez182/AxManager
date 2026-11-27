#!/system/bin/sh
# ============================================================
# VOID GMS Optimizer v2.0 - Background service
# ============================================================
# Runs on boot under AXManager / Magisk / KSU / APatch.
# Only does work if AUTO_OPTIMIZE_ON_BOOT=true in config.
# Otherwise, just stays out of the way.
#
# v2.0: detects environment explicitly so logs show which manager
# is hosting the service. v2.0 metrics are recorded per-cycle in
# VOID_METRIC_FILE and survive across re-apply windows.
# ============================================================

MODDIR="${0%/*}"
[ ! -d "$MODDIR" ] && MODDIR="$(cd "$(dirname "$0")" && pwd)"
[ -z "$MODDIR" ] || [ "$MODDIR" = "." ] && MODDIR="$(pwd)"

# Skip if not in a module manager
case "${ENV:-}" in
    axmanager|magisk|ksu|apatch) ;;
    *) ENV=""; exit 0 ;;
esac

# shellcheck source=./common/common.sh
. "$MODDIR/common/common.sh"

ENV="${ENV:-axmanager}"
export ENV

vlog_init
void_load_config

if [ "${AUTO_OPTIMIZE_ON_BOOT:-true}" != "true" ]; then
    vlog INFO "service: AUTO_OPTIMIZE_ON_BOOT=false, exiting"
    exit 0
fi

vlog INFO "service started: env=$ENV profile=$PROFILE"

# Wait for boot completion
while [ "$(getprop sys.boot_completed 2>/dev/null)" != "1" ]; do
    sleep 5
done

# Give the system 15s to settle user services
sleep 15

# Initial optimization
void_optimize_all

# ============================================================
# VOID OPTIMIZER PLUS — Google+ modules
# Auto-activates the new Google-focused modules (G1..G10) right
# after the main optimization. Honours ENABLE_GOOGLE_PLUS in
# <MODDIR>/void_optimizer_plus/config/google_plus.conf.
# Falls back silently if the folder is missing.
# ============================================================
if [ -f "$MODDIR/void_optimizer_plus/entry.sh" ]; then
    vlog INFO "service: launching void_optimizer_plus/entry.sh"
    sh "$MODDIR/void_optimizer_plus/entry.sh" >/dev/null 2>&1
    vlog INFO "service: void_optimizer_plus/entry.sh done"
    # v4.0: register the daily re-apply schedule (no root, jobscheduler)
    if [ -f "$MODDIR/void_optimizer_plus/scheduler.sh" ]; then
        vlog INFO "service: registering void_optimizer_plus daily schedule"
        sh "$MODDIR/void_optimizer_plus/scheduler.sh" set >/dev/null 2>&1
    fi
else
    vlog INFO "service: void_optimizer_plus not present, skipping"
fi

# Mark service as alive (for `sh action.sh health`)
# FIX v2.5: create the run/ dir BEFORE the first pid write.
# The previous order silently dropped the pid on the first echo
# when the directory didn't exist yet, then a second mkdir+write
# papered over it. Both writes are kept for backward compatibility
# (older health scripts only look for the file's existence).
mkdir -p "/data/adb/modules/void_gms_optimizer/run" 2>/dev/null
echo "$$" > "/data/adb/modules/void_gms_optimizer/run/service.pid" 2>/dev/null
void_write_health "ok" "service started, pid=$$"

# Periodic re-apply
PERIOD=${VOID_REAPPLY_INTERVAL:-21600}

while true; do
    vlog INFO "service: sleeping ${PERIOD}s before re-apply"
    sleep "$PERIOD"
    void_optimize_all
    # vOP+ re-apply too (keeps the G* modules in sync)
    if [ -f "$MODDIR/void_optimizer_plus/entry.sh" ]; then
        sh "$MODDIR/void_optimizer_plus/entry.sh" >/dev/null 2>&1
    fi
    # v4.0: re-register schedule in case it was cancelled by reboot
    if [ -f "$MODDIR/void_optimizer_plus/scheduler.sh" ]; then
        sh "$MODDIR/void_optimizer_plus/scheduler.sh" set >/dev/null 2>&1
    fi
    void_write_health "ok" "service re-apply, pid=$$"
done
