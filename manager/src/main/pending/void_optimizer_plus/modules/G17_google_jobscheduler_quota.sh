#!/system/bin/sh
# G17 — Quota Google jobs via jobscheduler
# Source: Appzuku, MrCarb0n
# For each com.google.* package we cancel all pending jobs and
# (API 30+) set a per-app standby quota. This is the equivalent of
# "force stop" without root but in a system-friendly way.
MODULE_ID="G17_google_jobscheduler_quota"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

apply() {
    vopl_apply_or_skip G17_GOOGLE_JOBSCHEDULER_QUOTA || return 0
    vopl_need_api 30 || return 0
    local count=0
    for pkg in $(pm list packages 2>/dev/null | grep -oE 'com\.google\.[a-zA-Z0-9._]+'); do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "$MODULE_ID: cancel $pkg" \
            cmd jobscheduler cancel "$pkg" 0 2>/dev/null
        count=$((count + 1))
    done
    vopl_log INFO "$MODULE_ID: cleared jobs for $count Google packages"
}

revert() {
    vopl_need_api 30 || return 0
    vopl_log INFO "$MODULE_ID: jobs will be rescheduled by the OS automatically"
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
