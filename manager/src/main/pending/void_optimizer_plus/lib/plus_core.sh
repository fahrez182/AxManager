#!/system/bin/sh
# ============================================================
# VOID OPTIMIZER PLUS — shared library
# Used by all google_*.sh modules under void_optimizer_plus/modules/
# ============================================================

# Paths
VOPL_DIR="${VOPL_DIR:-/data/adb/modules/void_gms_optimizer/void_optimizer_plus}"
VOPL_CFG="$VOPL_DIR/config/google_plus.conf"
VOPL_LOG_DIR="/sdcard/Android/data/voidgms/plus/log"
VOPL_LOG="$VOPL_LOG_DIR/google_plus.log"
VOPL_BACKUP="/sdcard/Android/data/voidgms/plus/backup_google"

# Master switch
ENABLE_GOOGLE_PLUS="${ENABLE_GOOGLE_PLUS:-true}"

# G1..G10 tunables (legacy)
G1_GMS_DEEP_DISABLE="${G1_GMS_DEEP_DISABLE:-true}"
G2_GSF_THROTTLE="${G2_GSF_THROTTLE:-true}"
G3_PLAY_STORE_THROTTLE="${G3_PLAY_STORE_THROTTLE:-true}"
G4_ADS_COMPONENTS_KILL="${G4_ADS_COMPONENTS_KILL:-true}"
G5_GOOGLE_TELEMETRY_PKG_DISABLE="${G5_GOOGLE_TELEMETRY_PKG_DISABLE:-true}"
G6_CARRIER_CONFIG_THROTTLE="${G6_CARRIER_CONFIG_THROTTLE:-true}"
G7_GOOGLE_LOCATION_FORCE_OFF="${G7_GOOGLE_LOCATION_FORCE_OFF:-true}"
G8_GOOGLE_FCM_PROTECT="${G8_GOOGLE_FCM_PROTECT:-true}"
G9_GMS_RARE_BUCKET_FOREVER="${G9_GMS_RARE_BUCKET_FOREVER:-true}"
G10_CHECKIN_SERVICE_KILL="${G10_CHECKIN_SERVICE_KILL:-true}"

# G11..G30 tunables (new — 20 functions)
G11_GOOGLE_APPOPS_SENSOR_BLOCK="${G11_GOOGLE_APPOPS_SENSOR_BLOCK:-true}"
G12_VELVET_COMPONENTS_KILL="${G12_VELVET_COMPONENTS_KILL:-true}"
G13_GOOGLE_BATTERY_HISTORY_OFF="${G13_GOOGLE_BATTERY_HISTORY_OFF:-true}"
G14_GOOGLE_TTS_COMPONENTS_KILL="${G14_GOOGLE_TTS_COMPONENTS_KILL:-true}"
G15_GMS_IDLE_WHITELIST_REMOVE="${G15_GMS_IDLE_WHITELIST_REMOVE:-true}"
G16_GOOGLE_BG_DATA_RESTRICT="${G16_GOOGLE_BG_DATA_RESTRICT:-true}"
G17_GOOGLE_JOBSCHEDULER_QUOTA="${G17_GOOGLE_JOBSCHEDULER_QUOTA:-true}"
G18_GOOGLE_BATTERY_SAVER_FORCE="${G18_GOOGLE_BATTERY_SAVER_FORCE:-true}"
G19_GOOGLE_WELLBEING_COMPONENTS_KILL="${G19_GOOGLE_WELLBEING_COMPONENTS_KILL:-true}"
G20_GOOGLE_PARTNER_COMPONENTS_KILL="${G20_GOOGLE_PARTNER_COMPONENTS_KILL:-true}"
G21_GOOGLE_ASSISTANT_COMPONENTS_KILL="${G21_GOOGLE_ASSISTANT_COMPONENTS_KILL:-true}"
G22_GOOGLE_DRIVE_COMPONENTS_KILL="${G22_GOOGLE_DRIVE_COMPONENTS_KILL:-true}"
G23_GOOGLE_WAKELOCK_KILL="${G23_GOOGLE_WAKELOCK_KILL:-true}"
G24_GOOGLE_PACKAGES_DISABLE="${G24_GOOGLE_PACKAGES_DISABLE:-true}"
G25_GOOGLE_SETUPWIZARD_SKIP="${G25_GOOGLE_SETUPWIZARD_SKIP:-true}"
G26_GOOGLE_GMS_UNWANTED_PERMS_REVOKE="${G26_GOOGLE_GMS_UNWANTED_PERMS_REVOKE:-true}"
G27_GOOGLE_THROTTLE_PHONELOCATION="${G27_GOOGLE_THROTTLE_PHONELOCATION:-true}"
G28_GOOGLE_ACCOUNTS_COMPONENTS_KILL="${G28_GOOGLE_ACCOUNTS_COMPONENTS_KILL:-true}"
G29_GOOGLE_PLAY_SERVICES_TELEMETRY_KILL="${G29_GOOGLE_PLAY_SERVICES_TELEMETRY_KILL:-true}"
G30_GOOGLE_AOD_COMPONENTS_KILL="${G30_GOOGLE_AOD_COMPONENTS_KILL:-true}"


# G31..G50 tunables (v5.0 — 20 new modules)
G31_GOOGLE_BACKUP_OFF="${G31_GOOGLE_BACKUP_OFF:-true}"
G32_GOOGLE_SYNC_ADAPTERS_KILL="${G32_GOOGLE_SYNC_ADAPTERS_KILL:-true}"
G33_GOOGLE_PHOTOS_BG_KILL="${G33_GOOGLE_PHOTOS_BG_KILL:-true}"
G34_GOOGLE_MAPS_BG_KILL="${G34_GOOGLE_MAPS_BG_KILL:-true}"
G35_YOUTUBE_BG_KILL="${G35_YOUTUBE_BG_KILL:-true}"
G36_GBOARD_LEARNING_KILL="${G36_GBOARD_LEARNING_KILL:-true}"
G37_PLAY_PROTECT_OFF="${G37_PLAY_PROTECT_OFF:-true}"
G38_WELLBEING_AGGREGATION_OFF="${G38_WELLBEING_AGGREGATION_OFF:-true}"
G39_FEEDBACK_UPLOAD_OFF="${G39_FEEDBACK_UPLOAD_OFF:-true}"
G40_FAMILYLINK_KILL="${G40_FAMILYLINK_KILL:-true}"
G41_FIREBASE_APPOPS_BLOCK="${G41_FIREBASE_APPOPS_BLOCK:-true}"
G42_DOZE_DEEP_CONSTANTS="${G42_DOZE_DEEP_CONSTANTS:-true}"
G43_JOBSCHED_CONSTANTS="${G43_JOBSCHED_CONSTANTS:-true}"
G44_GOOGLE_FGS_BLOCK="${G44_GOOGLE_FGS_BLOCK:-true}"
G45_GOOGLE_STATS_UPLOAD_KILL="${G45_GOOGLE_STATS_UPLOAD_KILL:-true}"
G46_PLAY_AUTOUPDATE_OFF="${G46_PLAY_AUTOUPDATE_OFF:-true}"
G47_CHROME_WEBVIEW_BG_KILL="${G47_CHROME_WEBVIEW_BG_KILL:-true}"
G48_GOOGLE_FORCE_STOP_NONCRIT="${G48_GOOGLE_FORCE_STOP_NONCRIT:-true}"
G49_AM_CONSTANTS_AGGRESSIVE="${G49_AM_CONSTANTS_AGGRESSIVE:-true}"
G50_BATTERY_SAVER_CONSTANTS_AGGRESSIVE="${G50_BATTERY_SAVER_CONSTANTS_AGGRESSIVE:-true}"

VOPL_DRY_RUN="${VOPL_DRY_RUN:-0}"
VOPL_DEBUG="${VOPL_DEBUG:-1}"
VOPL_API=""

# ----------------------------------------------------------------
# FCM-Safe Whitelist
# Component name fragments that must NEVER be disabled
# because they are the only path for Google FCM to push notifications.
# Any module that disables components checks against this list.
# ----------------------------------------------------------------
VOPL_FCM_SAFE_GMS_COMPONENTS="
com.google.android.gms/.gcm.GcmService
com.google.android.gms/.gcm.GcmIntentService
com.google.android.gms/.gcm.NMSTaskService
com.google.android.gms/com.google.android.gms.gcm.GcmService
com.google.android.gms/com.google.android.gms.gcm.GcmIntentService
com.google.android.gms/com.google.android.gms.gcm.NMSTaskService
com.google.android.gms/com.google.android.gms.notifications.service.MessagingNotificationListenerService
com.google.android.gms/com.google.android.gms.checkin.CheckinService
"

# ----------------------------------------------------------------
# vopl_init — call once from entry
# ----------------------------------------------------------------
vopl_init() {
    mkdir -p "$VOPL_LOG_DIR" "$VOPL_BACKUP" 2>/dev/null
    _vopl_load_cfg
    _vopl_detect_api
    vopl_log INFO "init: api=$VOPL_API enabled=$ENABLE_GOOGLE_PLUS dry=$VOPL_DRY_RUN"
}

# ----------------------------------------------------------------
# Load config (if present)
# ----------------------------------------------------------------
_vopl_load_cfg() {
    if [ -f "$VOPL_CFG" ]; then
        # shellcheck disable=SC1090
        . "$VOPL_CFG"
    fi
}

# ----------------------------------------------------------------
# Detect API
# ----------------------------------------------------------------
_vopl_detect_api() {
    local v
    v=$(getprop ro.build.version.sdk 2>/dev/null)
    [ -z "$v" ] && v=$(getprop ro.build.version.release 2>/dev/null)
    case "$v" in
        [4-9]|[1-9][0-9]) VOPL_API=$v ;;
        *) VOPL_API=14 ;;
    esac
}

vopl_api() { echo "$VOPL_API"; }

vopl_need_api() {
    [ "$VOPL_API" -ge "$1" ] || {
        vopl_log INFO "skip (need API $1, have $VOPL_API)"
        return 1
    }
    return 0
}

# ----------------------------------------------------------------
# Logging
# ----------------------------------------------------------------
vopl_log() {
    local level="$1"; shift
    [ -d "$VOPL_LOG_DIR" ] || mkdir -p "$VOPL_LOG_DIR" 2>/dev/null
    {
        printf '[%s] [%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$level" "$*"
    } >> "$VOPL_LOG" 2>/dev/null
}

# ----------------------------------------------------------------
# vopl_pkg_exists
# ----------------------------------------------------------------
# v4.1: cached lookup shared across all G* modules.
VOPL_PM_CACHE=""
vopl_pm_cache_load() {
    [ -n "$VOPL_PM_CACHE" ] && return 0
    VOPL_PM_CACHE="$(pm list packages 2>/dev/null)"
    [ -z "$VOPL_PM_CACHE" ] && VOPL_PM_CACHE="#empty"
}
vopl_pm_cache_reset() { VOPL_PM_CACHE=""; }
vopl_pkg_exists() {
    [ -z "$1" ] && return 1
    vopl_pm_cache_load
    printf '%s\n' "$VOPL_PM_CACHE" | grep -qE "^package:$1$"
}


# ----------------------------------------------------------------
# vopl_google_pkgs (v5.0)
# Enumerate all com.google.* packages from the cached pm list —
# no extra pm forks. Replaces the pattern `pm list packages | grep`.
# ----------------------------------------------------------------
vopl_google_pkgs() {
    vopl_pm_cache_load
    printf '%s\n' "$VOPL_PM_CACHE" | \
        sed -n 's/^package:\(com\.google\.[A-Za-z0-9._]*\)$/\1/p'
}

# vopl_run_sh <desc> "<shell one-liner>" — honours dry-run for
# commands that need shell features (pipes, redirection).
vopl_run_sh() {
    local desc="$1"; shift
    if [ "$VOPL_DRY_RUN" = "1" ]; then
        vopl_log DRY "[DRY] $desc -> $*"
        return 0
    fi
    vopl_log INFO "$desc -> $*"
    sh -c "$*" 2>/dev/null
}

# ----------------------------------------------------------------
# Non-critical Google package filter (v5.0)
# Packages here are considered CRITICAL and must not be
# force-stopped / hard-disabled by G48. Add pkgs to keep alive.
# ----------------------------------------------------------------
VOPL_GOOGLE_CRITICAL="
com.google.android.gms
com.google.android.gsf
com.google.android.gsf.login
com.android.vending
com.google.android.gms.location.history
"

vopl_is_google_critical() {
    local p="$1"
    for c in $VOPL_GOOGLE_CRITICAL; do
        [ "$c" = "$p" ] && return 0
    done
    return 1
}

# ----------------------------------------------------------------
# vopl_run — honours dry-run
# ----------------------------------------------------------------
vopl_run() {
    local desc="$1"; shift
    if [ "$VOPL_DRY_RUN" = "1" ]; then
        vopl_log DRY "[DRY] $desc -> $*"
        return 0
    fi
    vopl_log INFO "$desc -> $*"
    "$@" 2>/dev/null
}

# ----------------------------------------------------------------
# vopl_component_exists <pkg> <component>
# ----------------------------------------------------------------
# v6.0 EFFICIENCY: cache `dumpsys package <pkg>` once per package per
# module run. G1 alone triggered ~45 dumpsys forks; now it triggers 1.
# Cache is stored in a tmp file keyed by pkg name, cleaned on exit.
VOPL_DUMP_CACHE_DIR="${TMPDIR:-/data/local/tmp}/vopl_dump_$$"
mkdir -p "$VOPL_DUMP_CACHE_DIR" 2>/dev/null
trap 'rm -rf "$VOPL_DUMP_CACHE_DIR" 2>/dev/null' EXIT INT TERM

vopl_pkg_dump() {
    local pkg="$1"
    local f="$VOPL_DUMP_CACHE_DIR/${pkg}.dump"
    if [ ! -s "$f" ]; then
        dumpsys package "$pkg" 2>/dev/null > "$f"
        [ -s "$f" ] || echo "#empty" > "$f"
    fi
    cat "$f"
}
vopl_pkg_dump_reset() { rm -f "$VOPL_DUMP_CACHE_DIR/$1.dump" 2>/dev/null; }

vopl_component_exists() {
    local pkg="$1" comp="$2"
    vopl_pkg_dump "$pkg" | grep -q "$pkg/$comp"
}

# ----------------------------------------------------------------
# vopl_batch_disable <pkg> <newline-separated-components>
# v6.0: disables many components of the same package with a single
# dumpsys read and a single grep pass. Honours FCM-safe list and
# dry-run. Returns count of disables attempted.
# ----------------------------------------------------------------
vopl_batch_disable() {
    local pkg="$1" list="$2"
    vopl_pkg_exists "$pkg" || return 0
    local dump; dump=$(vopl_pkg_dump "$pkg")
    local count=0 comp full
    printf '%s\n' "$list" | while IFS= read -r comp; do
        [ -z "$comp" ] && continue
        full="$pkg/$comp"
        if vopl_is_fcm_safe "$pkg" "$comp"; then
            vopl_log INFO "FCM-safe, skip: $full"
            continue
        fi
        if printf '%s' "$dump" | grep -q "$full"; then
            vopl_run "$MODULE_ID: $full" pm disable "$full"
            count=$((count+1))
        fi
    done
    return 0
}

# ----------------------------------------------------------------
# vopl_is_fcm_safe <pkg> <component>
# Returns 0 (true) if the component is in the FCM safe list and
# MUST NOT be disabled.
# ----------------------------------------------------------------
vopl_is_fcm_safe() {
    local pkg="$1" comp="$2"
    local full="$pkg/$comp"
    local safe
    for safe in $VOPL_FCM_SAFE_GMS_COMPONENTS; do
        if [ "$safe" = "$full" ]; then
            return 0
        fi
    done
    return 1
}

# ----------------------------------------------------------------
# vopl_safe_disable <pkg> <component>
# Disables a component only if it's NOT in the FCM-safe list.
# This is the new helper for G11..G30 — guarantees FCM stays alive.
# ----------------------------------------------------------------
vopl_safe_disable() {
    local pkg="$1" comp="$2"
    if vopl_is_fcm_safe "$pkg" "$comp"; then
        vopl_log INFO "FCM-safe, skipping disable: $pkg/$comp"
        return 0
    fi
    if vopl_component_exists "$pkg" "$comp"; then
        vopl_run "$MODULE_ID: $pkg/$comp" pm disable "$pkg/$comp"
    fi
}

# ----------------------------------------------------------------
# vopl_apply_or_skip — gate by master flag
# ----------------------------------------------------------------
vopl_apply_or_skip() {
    local flag="$1"
    if [ "$ENABLE_GOOGLE_PLUS" != "true" ]; then
        vopl_log INFO "ENABLE_GOOGLE_PLUS=false, skipping $flag"
        return 1
    fi
    eval "val=\$$flag"
    if [ "$val" != "true" ]; then
        vopl_log INFO "$flag disabled in config, skipping"
        return 1
    fi
    return 0
}

# G51..G60 tunables (v6.0)
G51_NEARBY_SHARE_BG_KILL="${G51_NEARBY_SHARE_BG_KILL:-true}"
G52_GOOGLE_HEALTH_SERVICES_DISABLE="${G52_GOOGLE_HEALTH_SERVICES_DISABLE:-true}"
G53_GOOGLE_LENS_BG_KILL="${G53_GOOGLE_LENS_BG_KILL:-true}"
G54_NOW_PLAYING_KILL="${G54_NOW_PLAYING_KILL:-true}"
G55_PERSONAL_SAFETY_KILL="${G55_PERSONAL_SAFETY_KILL:-true}"
G56_GOOGLE_ONE_BACKUP_KILL="${G56_GOOGLE_ONE_BACKUP_KILL:-true}"
G57_GOOGLE_RECORDER_KILL="${G57_GOOGLE_RECORDER_KILL:-true}"
G58_GOOGLE_FILES_BG_KILL="${G58_GOOGLE_FILES_BG_KILL:-true}"
G59_GMAIL_BG_THROTTLE="${G59_GMAIL_BG_THROTTLE:-true}"
G60_DEVICECONFIG_GOOGLE_FLAGS_OFF="${G60_DEVICECONFIG_GOOGLE_FLAGS_OFF:-true}"
