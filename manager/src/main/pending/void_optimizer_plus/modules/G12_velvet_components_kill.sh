#!/system/bin/sh
# G12 — Kill Velvet (Google Search app) components
# Source: Aatricks
# Velvet is com.google.android.googlequicksearchbox.
# Many wakeups come from there. We disable the heaviest receivers
# and services while keeping Search usable.
MODULE_ID="G12_velvet_components_kill"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

PKG="com.google.android.googlequicksearchbox"
COMPONENTS="
com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService
com.google.android.googlequicksearchbox/com.google.android.search.core.api.SearchService
com.google.android.googlequicksearchbox/com.google.android.apps.gsa.persistent.PersistentService
com.google.android.googlequicksearchbox/com.google.android.apps.gsa.smartintent.SmartIntentBroadcastReceiver
com.google.android.googlequicksearchbox/com.google.android.apps.gsa.lens.LensBroadcastReceiver
com.google.android.googlequicksearchbox/com.google.android.apps.gsa.sounddetector.SoundTriggerReceiver
com.google.android.googlequicksearchbox/com.google.android.apps.gsa.pixel.assistant.dock.DockEventReceiver
com.google.android.googlequicksearchbox/com.google.android.apps.gsa.nowservice.NowService
com.google.android.googlequicksearchbox/com.google.android.apps.gsa.config.GsaConfigService
com.google.android.googlequicksearchbox/com.google.android.apps.gsa.provision.GsaProvisioningService
com.google.android.googlequicksearchbox/com.google.android.apps.gsa.tap.payment.TapPaymentService
"

apply() {
    vopl_apply_or_skip G12_VELVET_COMPONENTS_KILL || return 0
    vopl_pkg_exists "$PKG" || { vopl_log WARN "Velvet missing"; return 0; }
    local c=0
    for comp in $COMPONENTS; do
        if vopl_component_exists "$PKG" "$comp"; then
            vopl_run "$MODULE_ID: $comp" \
                pm disable "$PKG/$comp"
            c=$((c + 1))
        fi
    done
    vopl_log INFO "$MODULE_ID: disabled $c Velvet components"
}

revert() {
    vopl_pkg_exists "$PKG" || return 0
    for comp in $COMPONENTS; do
        if vopl_component_exists "$PKG" "$comp"; then
            vopl_run "revert $MODULE_ID" pm enable "$PKG/$comp"
        fi
    done
}

case "${1:-apply}" in apply) apply ;; revert) revert ;; *) exit 1 ;; esac
