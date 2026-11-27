#!/system/bin/sh
# G20 — Kill Google Partner Setup + Google Contacts Sync
# Source: Aatricks, MrCarb0n
# com.google.android.partnersetup, com.google.android.syncadapters
# These run partner spam and contacts sync in background.
MODULE_ID="G20_google_partner_components_kill"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

declare -A PKGS_COMPONENTS
PKGS_COMPONENTS["com.google.android.partnersetup"]="
com.google.android.partnersetup/com.google.android.partnersetup.PartnersetupService
com.google.android.partnersetup/com.google.android.partnersetup.receiver.PhoneStateChangedReceiver
com.google.android.partnersetup/com.google.android.partnersetup.receiver.PackageChangeReceiver
com.google.android.partnersetup/com.google.android.partnersetup.receiver.BootCompletedReceiver
com.google.android.partnersetup/com.google.android.partnersetup.receiver.ConnectivityChangedReceiver
"
PKGS_COMPONENTS["com.google.android.syncadapters"]="
com.google.android.syncadapters/com.google.android.syncadapters.SyncService
com.google.android.syncadapters/com.google.android.syncadapters.contacts.ContactsSyncAdapterService
com.google.android.syncadapters/com.google.android.syncadapters.calendar.CalendarSyncAdapterService
"

apply() {
    vopl_apply_or_skip G20_GOOGLE_PARTNER_COMPONENTS_KILL || return 0
    local c=0
    for pkg in "${!PKGS_COMPONENTS[@]}"; do
        vopl_pkg_exists "$pkg" || continue
        for comp in ${PKGS_COMPONENTS[$pkg]}; do
            if vopl_component_exists "$pkg" "$comp"; then
                vopl_run "$MODULE_ID: $pkg/$comp" \
                    pm disable "$pkg/$comp"
                c=$((c + 1))
            fi
        done
    done
    vopl_log INFO "$MODULE_ID: disabled $c partner/sync components"
}

revert() {
    for pkg in "${!PKGS_COMPONENTS[@]}"; do
        vopl_pkg_exists "$pkg" || continue
        for comp in ${PKGS_COMPONENTS[$pkg]}; do
            if vopl_component_exists "$pkg" "$comp"; then
                vopl_run "revert $MODULE_ID" pm enable "$pkg/$comp"
            fi
        done
    done
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
