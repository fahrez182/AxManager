#!/system/bin/sh
# G2 — Google Services Framework throttle
# Source: Aatricks, Appzuku
# GSF (com.google.android.gsf) is what phones home to Google.
# Heavy appops + bucket + idle whitelist removal.
MODULE_ID="G2_gsf_throttle"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

GSF_PACKAGES="
com.google.android.gsf
com.google.android.gsf.login
com.google.android.gsf.gservices
"

apply() {
    vopl_apply_or_skip G2_GSF_THROTTLE || return 0
    for pkg in $GSF_PACKAGES; do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "$MODULE_ID: appops $pkg" \
            cmd appops set "$pkg" RUN_IN_BACKGROUND ignore
        vopl_run "$MODULE_ID: appops $pkg" \
            cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND ignore
        vopl_run "$MODULE_ID: appops $pkg" \
            cmd appops set "$pkg" WAKE_LOCK ignore
        if vopl_need_api 28; then
            vopl_run "$MODULE_ID: bucket $pkg" \
                am set-standby-bucket "$pkg" rare
        fi
        vopl_run "$MODULE_ID: idle $pkg" \
            dumpsys deviceidle whitelist "-$pkg"
    done
    vopl_log INFO "$MODULE_ID: applied to $(echo $GSF_PACKAGES | wc -w) packages"
}

revert() {
    for pkg in $GSF_PACKAGES; do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "revert $MODULE_ID" cmd appops reset "$pkg"
        if vopl_need_api 28; then
            vopl_run "revert $MODULE_ID" am set-standby-bucket "$pkg" active
        fi
        vopl_run "revert $MODULE_ID" dumpsys deviceidle whitelist "+$pkg"
    done
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
