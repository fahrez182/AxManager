#!/system/bin/sh
# G11 — Block dangerous sensors for Google packages via appops
# Source: Aatricks (sensors permission lockdown)
# Blocks CAMERA, RECORD_AUDIO, READ_CONTACTS, READ_CALENDAR, READ_SMS,
# READ_CALL_LOG, READ_PHONE_STATE, BODY_SENSORS, ACTIVITY_RECOGNITION
# for every com.google.* package.
# Reversible: appops reset.
MODULE_ID="G11_google_appops_sensor_block"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

OPPS="
CAMERA
RECORD_AUDIO
READ_CONTACTS
READ_CALENDAR
READ_SMS
READ_CALL_LOG
READ_PHONE_STATE
BODY_SENSORS
ACTIVITY_RECOGNITION
"

apply() {
    vopl_apply_or_skip G11_GOOGLE_APPOPS_SENSOR_BLOCK || return 0
    vopl_need_api 29 || return 0
    local count=0
    for pkg in $(vopl_google_pkgs); do
        vopl_pkg_exists "$pkg" || continue
        for op in $OPPS; do
            vopl_run "$MODULE_ID: $pkg/$op" \
                cmd appops set "$pkg" "$op" ignore
        done
        count=$((count + 1))
    done
    vopl_log INFO "$MODULE_ID: blocked 9 ops on $count Google packages"
}

revert() {
    vopl_need_api 29 || return 0
    for pkg in $(vopl_google_pkgs); do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "revert $MODULE_ID" cmd appops reset "$pkg"
    done
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
