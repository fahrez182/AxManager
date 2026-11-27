#!/system/bin/sh
# ============================================================
# VOID OPTIMIZER PLUS — Boot trigger
# ============================================================
# Re-applies all 30 Google modules on BOOT_COMPLETED.
# Hooked into service.sh, but can also be invoked standalone:
#   sh boottrigger.sh
# This is the second half of the "no root, runs itself" path:
#   1. service.sh runs on boot, calls entry.sh + scheduler.sh set
#   2. every N hours, jobscheduler fires; boottrigger does the
#      same work entry.sh would do.
# Both paths are idempotent: re-running is safe.
# ============================================================

VOPL_DIR="${0%/*}"
[ ! -d "$VOPL_DIR" ] && VOPL_DIR="$(cd "$(dirname "$0")" && pwd)"
[ -z "$VOPL_DIR" ] || [ "$VOPL_DIR" = "." ] && VOPL_DIR="$(pwd)"

. "$VOPL_DIR/lib/plus_core.sh"
vopl_init

# Re-apply all 30 modules (entry.sh runs the same logic)
if [ -f "$VOPL_DIR/entry.sh" ]; then
    vopl_log INFO "boottrigger: launching entry.sh"
    sh "$VOPL_DIR/entry.sh" >/dev/null 2>&1
    vopl_log INFO "boottrigger: entry.sh done"
else
    vopl_log ERR "boottrigger: entry.sh missing"
    exit 1
fi

# Also (re)register the daily schedule, so a re-flash + reboot
# doesn't lose the job. This is safe to call on every boot.
if [ -x "$VOPL_DIR/scheduler.sh" ] || [ -f "$VOPL_DIR/scheduler.sh" ]; then
    vopl_log INFO "boottrigger: refreshing schedule"
    sh "$VOPL_DIR/scheduler.sh" set >/dev/null 2>&1
    vopl_log INFO "boottrigger: schedule refreshed"
fi

exit 0
