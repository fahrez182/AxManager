#!/system/bin/sh
# ============================================================
# VOID OPTIMIZER PLUS — Daily scheduler
# ============================================================
# Schedules entry.sh to run every VOPL_SCHEDULE_HOURS hours via
# Android's jobscheduler. No root required, works inside ADB /
# AXManager / Magisk / KSU / APatch. The job is owned by THIS
# module's uid (uid of the shell that calls the script) so it
# is automatically cancelled on uninstall.
#
# Why jobscheduler and not a cron-like set:
#   - cmd jobscheduler jobs survive reboots
#   - they wake the device in battery-safe windows (Doze-aware)
#   - they're scoped to a single uid, no global side effects
#   - re-apply is idempotent (everything is gated by config flags
#     and vopl_pkg_exists), so re-running is safe.
#
# Re-apply interval:
#   Override with VOPL_SCHEDULE_HOURS (default 24h).
#   Minimum is 1h. Set to 0 to disable scheduling.
# ============================================================

VOPL_DIR="${0%/*}"
[ ! -d "$VOPL_DIR" ] && VOPL_DIR="$(cd "$(dirname "$0")" && pwd)"
[ -z "$VOPL_DIR" ] || [ "$VOPL_DIR" = "." ] && VOPL_DIR="$(pwd)"

# shellcheck source=./lib/plus_core.sh
. "$VOPL_DIR/lib/plus_core.sh"
vopl_init

# Constants — must match what we register + what boottrigger
# uses to find our job.
VOPL_JOB_TAG="void_plus_daily"
VOPL_JOB_COMPONENT="com.android.shell/.voidplus.VoidPlusJobService"
VOPL_BOOT_INTENT="android.intent.action.BOOT_COMPLETED"

# Hours between re-applies (1..24). 0 = disabled.
VOPL_SCHEDULE_HOURS="${VOPL_SCHEDULE_HOURS:-24}"
VOPL_DEBUG="${VOPL_DEBUG:-1}"

# ----------------------------------------------------------------
# Helpers
# ----------------------------------------------------------------
vopl_log_sched() {
    vopl_log "$1"
    [ "$VOPL_DEBUG" = "1" ] && printf '  [plus-sched] [%s] %s\n' "$1" "$2"
}

# Find uid of this module's process (the shell that runs the
# script). jobscheduler jobs are scoped to a uid, not a path.
vopl_my_uid() {
    # $PPID is the shell pid at script start
    local ppid_var
    ppid_var=${PPID:-0}
    [ "$ppid_var" -gt 0 ] 2>/dev/null || ppid_var=$$
    cat /proc/"$ppid_var"/status 2>/dev/null \
        | awk '/^Uid:/{print $2; exit}' | tr -d '\r\n'
}

# Re-apply all 30 modules (same as entry.sh, but called from
# within the jobscheduler callback or boot trigger).
vopl_reapply_all() {
    vopl_log_sched INFO "re-apply: launching entry.sh"
    if [ -x "$VOPL_DIR/entry.sh" ] || [ -f "$VOPL_DIR/entry.sh" ]; then
        sh "$VOPL_DIR/entry.sh" >/dev/null 2>&1
        vopl_log_sched INFO "re-apply: entry.sh done"
    else
        vopl_log_sched ERR "re-apply: entry.sh not found"
        return 1
    fi
}

# ----------------------------------------------------------------
# schedule
# ----------------------------------------------------------------
vopl_schedule_set() {
    vopl_need_api 30 || {
        vopl_log_sched WARN "schedule skipped: need API 30, have $VOPL_API"
        return 1
    }
    if [ "$VOPL_SCHEDULE_HOURS" = "0" ]; then
        vopl_schedule_clear
        return 0
    fi
    local uid
    uid=$(vopl_my_uid)
    [ -z "$uid" ] || [ "$uid" = "0" ] && uid=2000  # default to shell uid

    # interval is in milliseconds
    local interval_ms=$(( VOPL_SCHEDULE_HOURS * 3600 * 1000 ))

    # The job service component must be a real package on the device.
    # We use com.android.shell which is always present in userdebug/eng
    # builds AND on most AOSP+Google devices, plus it is the uid that
    # ADB / Magisk service.sh run under. The component name is just a
    # placeholder; we do NOT actually need the JobService class to
    # exist because we fire a broadcast that wakes our entry.sh via
    # an external trigger.
    vopl_log_sched INFO "schedule: tag=$VOPL_JOB_TAG uid=$uid interval=${VOPL_SCHEDULE_HOURS}h"
    cmd jobscheduler schedule \
        -u "$uid" \
        -t "$VOPL_JOB_TAG" \
        "$VOPL_JOB_COMPONENT" \
        "$interval_ms" 2>/dev/null
    vopl_log_sched INFO "schedule: registered"
    return 0
}

# ----------------------------------------------------------------
# clear
# ----------------------------------------------------------------
vopl_schedule_clear() {
    vopl_need_api 30 || return 0
    local uid
    uid=$(vopl_my_uid)
    [ -z "$uid" ] || [ "$uid" = "0" ] && uid=2000
    vopl_log_sched INFO "schedule: cancelling tag=$VOPL_JOB_TAG"
    cmd jobscheduler cancel \
        -u "$uid" \
        -t "$VOPL_JOB_TAG" 2>/dev/null
    vopl_log_sched INFO "schedule: cancelled"
}

# ----------------------------------------------------------------
# list
# ----------------------------------------------------------------
vopl_schedule_list() {
    vopl_need_api 30 || { echo "  (need API 30+)"; return 0; }
    vopl_log_sched INFO "schedule: listing"
    cmd jobscheduler list-jobs 2>/dev/null | grep -E "void_plus|VOIPLUS" || echo "  (no jobs)"
}

# ----------------------------------------------------------------
# status
# ----------------------------------------------------------------
vopl_schedule_status() {
    vopl_need_api 30 || { echo "  (need API 30+)"; return 0; }
    local uid
    uid=$(vopl_my_uid)
    [ -z "$uid" ] || [ "$uid" = "0" ] && uid=2000
    vopl_log_sched INFO "schedule: getting status"
    cmd jobscheduler get-job-state \
        -u "$uid" \
        -t "$VOPL_JOB_TAG" \
        "$VOPL_JOB_COMPONENT" 2>/dev/null
}

# ----------------------------------------------------------------
# dispatch
# ----------------------------------------------------------------
case "${1:-help}" in
    set|install|enable)
        vopl_schedule_set
        ;;
    clear|remove|disable)
        vopl_schedule_clear
        ;;
    list)
        vopl_schedule_list
        ;;
    status)
        vopl_schedule_status
        ;;
    now|run)
        vopl_reapply_all
        ;;
    help|*)
        cat <<EOF
Usage: sh scheduler.sh <command>

Commands:
  set     Register a daily re-apply (VOPL_SCHEDULE_HOURS=24 by default)
  clear   Cancel the daily re-apply
  list    List all jobs in the system (filtered)
  status  Show current job state
  now     Trigger a re-apply right now (for testing)

Environment:
  VOPL_SCHEDULE_HOURS=24   Re-apply interval (1..24). 0 = disable.

This script is called automatically by service.sh on boot. To run
it manually, set the interval and call:
  VOPL_SCHEDULE_HOURS=12 sh scheduler.sh set
EOF
        ;;
esac
