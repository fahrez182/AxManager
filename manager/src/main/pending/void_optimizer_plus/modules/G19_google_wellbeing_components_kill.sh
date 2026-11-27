#!/system/bin/sh
# G19 — Kill Google Wellbeing / Digital Wellbeing components
# Source: Aatricks
# com.google.android.apps.wellbeing and com.google.android.apps.safety
# run a lot of background sensors. We disable the heaviest.
MODULE_ID="G19_google_wellbeing_components_kill"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

declare -A PKGS_COMPONENTS
PKGS_COMPONENTS["com.google.android.apps.wellbeing"]="
com.google.android.apps.wellbeing/com.google.android.apps.wellbeing.activity.WellbeingActivity
com.google.android.apps.wellbeing/com.google.android.apps.wellbeing.notification.WellbeingNotificationService
com.google.android.apps.wellbeing/com.google.android.apps.wellbeing.dashboard.DashboardService
com.google.android.apps.wellbeing/com.google.android.apps.wellbeing.sleep.SleepService
com.google.android.apps.wellbeing/com.google.android.apps.wellbeing.account.WellbeingAccountService
"
PKGS_COMPONENTS["com.google.android.apps.safety"]="
com.google.android.apps.safety/com.google.android.apps.safety.SafetyService
com.google.android.apps.safety/com.google.android.apps.safety.receiver.SafetyBootReceiver
com.google.android.apps.safety/com.google.android.apps.safety.receiver.PackageChangeReceiver
"

apply() {
    vopl_apply_or_skip G19_GOOGLE_WELLBEING_COMPONENTS_KILL || return 0
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
    vopl_log INFO "$MODULE_ID: disabled $c wellbeing components"
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
