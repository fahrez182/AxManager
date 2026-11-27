#!/system/bin/sh
# G6 — Carrier config throttle
# Source: Aatricks
# Carrier config from both Google and AOSP can drain a lot.
MODULE_ID="G6_carrier_config_throttle"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

PKGS="
com.google.android.carrierconfig
com.android.carrierconfig
com.google.android.cellbroadcastservice
com.google.android.cellbroadcastreceiver
com.android.cellbroadcastreceiver
"

apply() {
    vopl_apply_or_skip G6_CARRIER_CONFIG_THROTTLE || return 0
    for pkg in $PKGS; do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "$MODULE_ID: $pkg" \
            cmd appops set "$pkg" RUN_IN_BACKGROUND ignore
        vopl_run "$MODULE_ID: $pkg" \
            cmd appops set "$pkg" WAKE_LOCK ignore
    done
    vopl_log INFO "$MODULE_ID: applied to $(echo $PKGS | wc -w) carrier packages"
}

revert() {
    for pkg in $PKGS; do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "revert $MODULE_ID" cmd appops reset "$pkg"
    done
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
