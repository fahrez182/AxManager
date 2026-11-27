#!/system/bin/sh
# G14 — Kill Google TTS / SpeechServices components
# Source: Aatricks
# com.google.android.tts and com.google.android.speech.services
# run scheduled jobs even when not in use.
MODULE_ID="G14_google_tts_components_kill"
. "$(dirname "$0")/../lib/plus_core.sh"
vopl_init

declare -A PKGS_COMPONENTS
PKGS_COMPONENTS["com.google.android.tts"]="
com.google.android.tts/com.google.android.tts.service.GoogleTTSService
com.google.android.tts/com.google.android.tts.speech.ConversionService
com.google.android.tts/com.google.android.tts.speech.femaletts.FemaleTtsService
com.google.android.tts/com.google.android.tts.speech.maletts.MaleTtsService
"
PKGS_COMPONENTS["com.google.android.speech.services"]="
com.google.android.speech.services/com.google.android.speech.speechservices.SpeechService
com.google.android.speech.services/com.google.android.speech.speechservices.SpeechServicesConnection
com.google.android.speech.services/com.google.android.speech.speechservices.SpeechWakeUpService
"

apply() {
    vopl_apply_or_skip G14_GOOGLE_TTS_COMPONENTS_KILL || return 0
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
    vopl_log INFO "$MODULE_ID: disabled $c TTS components"
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
