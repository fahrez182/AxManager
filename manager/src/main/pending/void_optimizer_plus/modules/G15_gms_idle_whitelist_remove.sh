#!/system/bin/sh
# G15 — Remove ALL com.google.* from deviceidle whitelist
# Source: Appzuku, Nougat doze tweaks
# Doze (deep sleep) is broken by apps in the whitelist.
# All Google packages are removed, FCM still works thanks to G8.
MODULE_ID="G15_gms_idle_whitelist_remove"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

apply() {
    vopl_apply_or_skip G15_GMS_IDLE_WHITELIST_REMOVE || return 0
    local count=0
    for pkg in $(pm list packages 2>/dev/null | grep -oE 'com\.google\.[a-zA-Z0-9._]+'); do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "$MODULE_ID: $pkg" \
            dumpsys deviceidle whitelist "-$pkg"
        count=$((count + 1))
    done
    vopl_log INFO "$MODULE_ID: removed $count Google packages from idle whitelist"
}

revert() {
    for pkg in $(pm list packages 2>/dev/null | grep -oE 'com\.google\.[a-zA-Z0-9._]+'); do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "revert $MODULE_ID" dumpsys deviceidle whitelist "+$pkg"
    done
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
