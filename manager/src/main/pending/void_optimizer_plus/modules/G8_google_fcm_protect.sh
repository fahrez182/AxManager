#!/system/bin/sh
# G8 — Google FCM protect
# Source: void-gms-optimizer PROTECT_FCM logic
# After all the G-modules run, RE-APPLY FGM mode to com.google.android.gms:
#   RUN_IN_BACKGROUND=ignore
#   RUN_ANY_IN_BACKGROUND=allow  <-- this is what keeps FCM alive
#   WAKE_LOCK=ignore
# This must run LAST in the chain.
MODULE_ID="G8_google_fcm_protect"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

PKG="com.google.android.gms"

apply() {
    vopl_apply_or_skip G8_GOOGLE_FCM_PROTECT || return 0
    vopl_pkg_exists "$PKG" || return 0
    vopl_run "$MODULE_ID: FCM protect $PKG" \
        cmd appops set "$PKG" RUN_IN_BACKGROUND ignore
    vopl_run "$MODULE_ID: FCM protect $PKG" \
        cmd appops set "$PKG" RUN_ANY_IN_BACKGROUND allow
    vopl_run "$MODULE_ID: FCM protect $PKG" \
        cmd appops set "$PKG" WAKE_LOCK ignore
    vopl_log INFO "$MODULE_ID: FCM-safe mode applied to GMS"
}

revert() {
    vopl_pkg_exists "$PKG" || return 0
    vopl_run "revert $MODULE_ID" cmd appops reset "$PKG"
    vopl_log INFO "$MODULE_ID: GMS reset to defaults"
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
