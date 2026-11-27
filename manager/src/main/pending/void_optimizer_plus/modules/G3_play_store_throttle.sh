#!/system/bin/sh
# G3 — Play Store (Vending) throttle
# Source: kibotu, Appzuku
# Play Store is one of the heaviest Google apps. We keep it functional
# (you can still open it) but stop it from background-checking for updates.
MODULE_ID="G3_play_store_throttle"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

PKG="com.android.vending"

apply() {
    vopl_apply_or_skip G3_PLAY_STORE_THROTTLE || return 0
    vopl_pkg_exists "$PKG" || { vopl_log WARN "Play Store missing"; return 0; }
    vopl_run "$MODULE_ID: appops" \
        cmd appops set "$PKG" RUN_IN_BACKGROUND ignore
    vopl_run "$MODULE_ID: appops" \
        cmd appops set "$PKG" RUN_ANY_IN_BACKGROUND ignore
    vopl_run "$MODULE_ID: appops" \
        cmd appops set "$PKG" WAKE_LOCK ignore
    if vopl_need_api 28; then
        vopl_run "$MODULE_ID: bucket" \
            am set-standby-bucket "$PKG" rare
    fi
    vopl_run "$MODULE_ID: idle" \
        dumpsys deviceidle whitelist "-$PKG"
    vopl_log INFO "$MODULE_ID: applied"
}

revert() {
    vopl_pkg_exists "$PKG" || return 0
    vopl_run "revert $MODULE_ID" cmd appops reset "$PKG"
    if vopl_need_api 28; then
        vopl_run "revert $MODULE_ID" am set-standby-bucket "$PKG" active
    fi
    vopl_run "revert $MODULE_ID" dumpsys deviceidle whitelist "+$PKG"
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
