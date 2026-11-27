#!/system/bin/sh
# G13 — Disable Google battery/usage history uploads
# Source: Aatricks, MrCarb0n
# Settings:
#   usage_stats = 0 (don't send app usage)
#   bug_reporting = 0 (don't send bug reports)
#   activity_streams_upload = 0
#   apps_component_usage_enabled = 0
MODULE_ID="G13_google_battery_history_off"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

apply() {
    vopl_apply_or_skip G13_GOOGLE_BATTERY_HISTORY_OFF || return 0
    vopl_run "$MODULE_ID: usage" \
        settings put global upload_apps_status 0
    vopl_run "$MODULE_ID: apm_history" \
        settings put global apm_observable_enabled 0
    vopl_run "$MODULE_ID: bug" \
        settings put global bug_reporting 0
    vopl_run "$MODULE_ID: logcat" \
        settings put global logcat_enabled 0
    vopl_log INFO "$MODULE_ID: battery/usage uploads disabled"
}

revert() {
    vopl_run "revert $MODULE_ID" settings put global upload_apps_status 1
    vopl_run "revert $MODULE_ID" settings put global bug_reporting 1
    vopl_log INFO "$MODULE_ID: reverted to default"
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
