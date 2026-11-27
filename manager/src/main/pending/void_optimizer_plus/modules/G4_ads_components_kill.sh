#!/system/bin/sh
# G4 — Ads components killer
# Source: Aatricks, MrCarb0n
# Disables ad-related components inside Google packages.
# - Privacy Sandbox (Topics, FLEDGE)
# - AdMob
# - Analytics
MODULE_ID="G4_ads_components_kill"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

# (pkg|component|category)
TARGETS="
com.google.android.gms|com.google.android.gms.ads.measurement.GclidListenerService
com.google.android.gms|com.google.android.gms.ads.jams.BandwidthListenerService
com.google.android.gms|com.google.android.gms.ads.social.GoogleAdMobPersistentService
com.google.android.gms|com.google.android.gms.ads.social.GoogleImaAdService
com.google.android.gms|com.google.android.gms.analytics.AnalyticsService
com.google.android.gms|com.google.android.gms.measurement.AppMeasurementService
com.google.android.gms|com.google.android.gms.ads.phenotype.GmsPhenotypeService
com.google.android.gms|com.google.android.gms.phenotype.service.PhenotypeService
com.google.android.gms|com.google.android.gms.phenotype.background.service.BackgroundPhenotypeService
com.google.android.adservices.api|com.google.android.adservices.api.AdServicesApiService
com.google.android.adservices.api|com.google.android.adservices.api.MeasurementService
com.google.android.adservices.api|com.google.android.adservices.api.TopicsService
com.google.android.adservices.api|com.google.android.adservices.api.AdIdProviderService
com.google.android.adservices|com.google.android.adservices.AdServicesService
com.google.android.adservices|com.google.android.adservices.AdServicesCommonService
com.google.android.adservices|com.google.android.adservices.signalservice.SignalsService
com.google.android.gms.ads|com.google.android.gms.ads.AdMobPersistentService
com.google.android.gms.measurement|com.google.android.gms.measurement.AppMeasurementJobService
com.google.android.gms.measurement|com.google.android.gms.measurement.AppMeasurementService
"

apply() {
    vopl_apply_or_skip G4_ADS_COMPONENTS_KILL || return 0
    local c=0
    echo "$TARGETS" | while IFS='|' read -r pkg comp; do
        [ -z "$pkg" ] && continue
        vopl_pkg_exists "$pkg" || continue
        if vopl_component_exists "$pkg" "$comp"; then
            vopl_run "$MODULE_ID: $pkg/$comp" pm disable "$pkg/$comp"
            c=$((c + 1))
        fi
    done
    vopl_log INFO "$MODULE_ID: scanned all ads components"
}

revert() {
    echo "$TARGETS" | while IFS='|' read -r pkg comp; do
        [ -z "$pkg" ] && continue
        vopl_pkg_exists "$pkg" || continue
        if vopl_component_exists "$pkg" "$comp"; then
            vopl_run "revert $MODULE_ID" pm enable "$pkg/$comp"
        fi
    done
    vopl_log INFO "$MODULE_ID: re-enabled ads components"
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
