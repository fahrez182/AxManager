#!/system/bin/sh
# Reverts ONLY the Google+ modules (G1..G30).
# Safe to call from action.sh:  sh action.sh restore-google
VOPL_DIR="${0%/*}"
[ ! -d "$VOPL_DIR" ] && VOPL_DIR="$(cd "$(dirname "$0")" && pwd)"
[ -z "$VOPL_DIR" ] || [ "$VOPL_DIR" = "." ] && VOPL_DIR="$(pwd)"

. "$VOPL_DIR/lib/plus_core.sh"
vopl_init
vopl_log INFO "=== revert_google.sh START ==="

for m in G1_gms_deep_disable G2_gsf_throttle G3_play_store_throttle \
         G4_ads_components_kill G5_google_telemetry_pkg_disable \
         G6_carrier_config_throttle G7_google_location_force_off \
         G8_google_fcm_protect G9_gms_rare_bucket_forever \
         G10_checkin_service_kill \
         G11_google_appops_sensor_block \
         G12_velvet_components_kill \
         G13_google_battery_history_off \
         G14_google_tts_components_kill \
         G15_gms_idle_whitelist_remove \
         G16_google_bg_data_restrict \
         G17_google_jobscheduler_quota \
         G18_google_battery_saver_force \
         G19_google_wellbeing_components_kill \
         G20_google_partner_components_kill \
         G21_google_assistant_components_kill \
         G22_google_drive_components_kill \
         G23_google_wakelock_kill \
         G24_google_packages_disable \
         G25_google_setupwizard_skip \
         G26_google_gms_unwanted_perms_revoke \
         G27_google_throttle_phonelocation \
         G28_google_accounts_components_kill \
         G29_google_play_services_telemetry_kill \
         G30_google_aod_components_kill \
         G31_google_backup_off G32_google_sync_adapters_kill G33_google_photos_bg_kill \
         G34_google_maps_bg_kill G35_youtube_bg_kill G36_gboard_learning_kill \
         G37_play_protect_off G38_wellbeing_aggregation_off G39_feedback_upload_off \
         G40_familylink_kill G41_firebase_appops_block G42_doze_deep_constants \
         G43_jobsched_constants G44_google_fgs_block G45_google_stats_upload_kill \
         G46_play_autoupdate_off G47_chrome_webview_bg_kill G48_google_force_stop_noncrit \
         G49_am_constants_aggressive G50_battery_saver_constants; do
    script="$VOPL_DIR/modules/${m}.sh"
    [ -f "$script" ] || continue
    printf '  [plus-revert] %-40s ... ' "$m"
    if sh "$script" revert >/dev/null 2>&1; then
        echo "ok"
    else
        echo "skip/fail"
    fi
done

vopl_log INFO "=== revert_google.sh DONE ==="
