#!/system/bin/sh
# G10 — Checkin service killer (the #1 GMS drain)
# Source: php.cn GMS deep-dive, XDA GMS Battery Drain fix, MrCarb0n
# CheckinService is what causes GMS to spin for 10+ minutes on first
# boot (CPU 60-80% holding wake lock while network is unreachable).
# Disable it permanently + kill any pending checkin intents.
MODULE_ID="G10_checkin_service_kill"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

PKG="com.google.android.gms"
COMPONENTS="
com.google.android.gms/.checkin.CheckinService
com.google.android.gms/.checkin.CheckinApiService
com.google.android.gms/com.google.android.gms.checkin.CheckinService
com.google.android.gms/com.google.android.gms.checkin.CheckinApiService
com.google.android.gms/com.google.android.gms.checkin.CheckinBroadcastReceiver
com.google.android.gms/com.google.android.gms.checkin.CheckinTrampoline
"

apply() {
    vopl_apply_or_skip G10_CHECKIN_SERVICE_KILL || return 0
    vopl_pkg_exists "$PKG" || return 0
    local c=0
    for comp in $COMPONENTS; do
        if vopl_component_exists "$PKG" "$comp"; then
            vopl_run "$MODULE_ID: $comp" \
                pm disable "$PKG/$comp"
            c=$((c + 1))
        fi
    done
    # Also kill any running checkin process
    vopl_run "$MODULE_ID: kill checkin" \
        pkill -9 -f "com.google.android.gms.*checkin" 2>/dev/null
    vopl_log INFO "$MODULE_ID: killed $c checkin components"
}

revert() {
    vopl_pkg_exists "$PKG" || return 0
    for comp in $COMPONENTS; do
        if vopl_component_exists "$PKG" "$comp"; then
            vopl_run "revert $MODULE_ID" pm enable "$PKG/$comp"
        fi
    done
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
