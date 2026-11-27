#!/system/bin/sh
# G18 — Force Google battery-saver heuristics off
# Source: Aatricks
# Disable adaptive battery exception logic that keeps Google packages warm.
# Settings: device_idle_temp_limit lowered, jobs constrained, alarmmanager throttled.
MODULE_ID="G18_google_battery_saver_force"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

apply() {
    vopl_apply_or_skip G18_GOOGLE_BATTERY_SAVER_FORCE || return 0
    vopl_need_api 30 || return 0
    vopl_run "$MODULE_ID: adaptive battery" \
        settings put global adaptive_battery_management_enabled 0
    vopl_run "$MODULE_ID: app standby" \
        settings put global app_standby_enabled 1
    vopl_run "$MODULE_ID: app standby refactor" \
        settings put global app_standby_refactor 1
    vopl_log INFO "$MODULE_ID: adaptive battery disabled, app standby enforced"
}

revert() {
    vopl_need_api 30 || return 0
    vopl_run "revert $MODULE_ID" settings put global adaptive_battery_management_enabled 1
    vopl_log INFO "$MODULE_ID: reverted"
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
