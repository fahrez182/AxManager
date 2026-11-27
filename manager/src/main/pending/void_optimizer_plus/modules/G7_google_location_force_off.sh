#!/system/bin/sh
# G7 — Google location force off
# Source: Aatricks
# Force OS location mode to battery-saving (2 = no GPS).
# Also remove Google location from idle whitelist.
MODULE_ID="G7_google_location_force_off"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

PKGS="
com.google.android.gms.location
com.google.android.location
com.google.android.gms
"

apply() {
    vopl_apply_or_skip G7_GOOGLE_LOCATION_FORCE_OFF || return 0
    vopl_run "$MODULE_ID: loc_mode" \
        settings put secure location_mode 2
    for pkg in $PKGS; do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "$MODULE_ID: $pkg" \
            cmd appops set "$pkg" ACCESS_FINE_LOCATION ignore
        vopl_run "$MODULE_ID: $pkg" \
            cmd appops set "$pkg" ACCESS_COARSE_LOCATION ignore
        vopl_run "$MODULE_ID: $pkg" \
            dumpsys deviceidle whitelist "-$pkg"
    done
    vopl_log INFO "$MODULE_ID: location force-off applied"
}

revert() {
    vopl_run "revert $MODULE_ID" settings put secure location_mode 3
    for pkg in $PKGS; do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "revert $MODULE_ID" cmd appops reset "$pkg"
        vopl_run "revert $MODULE_ID" dumpsys deviceidle whitelist "+$pkg"
    done
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
