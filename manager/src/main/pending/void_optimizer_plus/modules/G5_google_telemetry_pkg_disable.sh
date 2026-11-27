#!/system/bin/sh
# G5 — Google telemetry/sync packages disable
# Source: UAD-NG, debloat_samsung, MrCarb0n
# Disables (not uninstalls) the whole Google telemetry packages
# that exist for "one-time setup" or "wellbeing" purposes.
# They're not needed for FCM or core Play Services.
MODULE_ID="G5_google_telemetry_pkg_disable"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

PKGS="
com.google.android.apps.turbo
com.google.android.apps.wellbeing
com.google.android.configupdater
com.google.android.onetimeinitializer
com.google.android.partnersetup
com.google.android.apps.tips
com.google.android.apps.subscriptions.red
com.google.android.gesture.service
com.google.android.overlay.googleconfig
com.google.android.overlay.modules.documentsui
com.google.android.overlay.modules.maps
com.google.android.overlay.modules.permissioncontroller
com.google.android.overlay.modules.settings
com.google.android.setupwizard
com.google.android.tts
com.google.android.ext.services
com.google.android.ext.shared
com.google.android.cellbroadcastservice
com.google.android.cellbroadcastreceiver
com.google.android.feedback
com.google.android.printservice.recommendation
"

apply() {
    vopl_apply_or_skip G5_GOOGLE_TELEMETRY_PKG_DISABLE || return 0
    local c=0
    for pkg in $PKGS; do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "$MODULE_ID: $pkg" \
            pm disable-user --user 0 "$pkg"
        c=$((c + 1))
    done
    vopl_log INFO "$MODULE_ID: disabled $c google telemetry packages"
}

revert() {
    for pkg in $PKGS; do
        vopl_pkg_exists "$pkg" || continue
        vopl_run "revert $MODULE_ID" \
            pm enable "$pkg"
    done
    vopl_log INFO "$MODULE_ID: re-enabled all"
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
