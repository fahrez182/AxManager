#!/system/bin/sh
# ============================================================
# VOID GMS Optimizer v4.0 - Customize (Install) Script
# ============================================================
# ADB-only mode: NO root, NO Magisk/KSU/APatch branches,
# NO /proc writes. Everything goes through framework APIs.
# ============================================================

MODDIR="${0%/*}"
MODPATH="$MODDIR"

# ----------------------------------------------------------------
# Detect environment
# ----------------------------------------------------------------
ENV="adb"
[ "$AXERON" = "true" ] && ENV="axmanager"

# ----------------------------------------------------------------
# Install screen
# ----------------------------------------------------------------
ui_print " "
ui_print "==============================================="
ui_print "  VOID GMS Optimizer v5.0"
ui_print "  Google Services Battery & Drain Killer"
ui_print "  50 reversible modules · FCM-safe"
ui_print "==============================================="
ui_print " "
ui_print "  Environment  : $ENV"
ui_print "  No root      : 100% ADB / framework APIs"
ui_print "  Profile      : OPTIMAL (single, pre-calibrated)"
ui_print "  Compatibility: AXManager"
ui_print " "

# ----------------------------------------------------------------
# Runtime directories
# ----------------------------------------------------------------
mkdir -p "$MODPATH/config"
mkdir -p "$MODPATH/common"
# Note: runtime logs go to /sdcard/Android/data/voidgms/ (created on first run)
# We intentionally do NOT create a /log or /system dir inside MODPATH.

# ----------------------------------------------------------------
# Default user configuration
# ----------------------------------------------------------------
CONFIG="$MODPATH/config/voidgms.conf"
if [ ! -f "$CONFIG" ]; then
cat > "$CONFIG" << 'EOF'
# VOID GMS Optimizer v4.0 - user configuration
# Edit on-device: <MODDIR>/config/voidgms.conf
# Changes take effect on next boot / next action.
#
# v4.0 ships with a single OPTIMAL profile. The 30 Google+ modules
# in void_optimizer_plus/ are gated by google_plus.conf (30 flags).
# The two config files are independent — the v4 OPTIMAL profile
# already picks the right balance for the main module.

# Keep push notifications arriving fast (recommended).
PROTECT_FCM=true

# Run the optimizer on every boot.
AUTO_OPTIMIZE_ON_BOOT=true

# Reclaim cached processes with `am kill-all` on re-apply.
AGGRESSIVE_KILL=true

# Skip OPT apps (Maps, Photos, Music) used in the last N minutes.
VOID_FG_GRACE_MIN=10

# Re-apply loop interval (seconds) — used by service.sh.
VOID_REAPPLY_INTERVAL=21600

# Debug logging (writes to /sdcard/Android/data/voidgms/log/).
DEBUG_LOG=true

# Sample wakeups/network/CPU before & after each run.
VOID_ENABLE_METRICS=true

# Trim hint level: LOW | MODERATE | COMPLETE.
VOID_TRIM_LEVEL=COMPLETE

# Apps that must NEVER be throttled.
WHITELIST_PACKAGES="\
com.android.systemui \
com.android.launcher \
com.android.launcher3 \
com.android.launcher4 \
com.google.android.apps.nexuslauncher \
com.android.inputmethod.latin \
com.android.phone \
com.android.nfc \
com.android.settings \
com.google.android.inputmethod.latin \
com.samsung.android.honeyboard"
EOF
ui_print "- Config: PROTECT_FCM=true · AGGRESSIVE_KILL=true · OPTIMAL profile"
fi

# ----------------------------------------------------------------
# Void Optimizer Plus — Google+ config (idempotent)
# 30 modules, all enabled by default. Set any to false in this
# file to skip that specific module. FCM-safe list is hard-coded
# in plus_core.sh — no GCM/FcmService is ever disabled.
# ----------------------------------------------------------------
GPLUS_CFG="$MODPATH/void_optimizer_plus/config/google_plus.conf"
if [ ! -f "$GPLUS_CFG" ]; then
    mkdir -p "$MODPATH/void_optimizer_plus/config"
    cat > "$GPLUS_CFG" << 'VGEOF'
# VOID OPTIMIZER PLUS — Google modules config
# v4.0: 30 modules (G1..G30), all enabled by default.
# Set any flag to false to skip that specific module.
# Master switch:
ENABLE_GOOGLE_PLUS=true

# G1..G10 — original 10
G1_GMS_DEEP_DISABLE=true
G2_GSF_THROTTLE=true
G3_PLAY_STORE_THROTTLE=true
G4_ADS_COMPONENTS_KILL=true
G5_GOOGLE_TELEMETRY_PKG_DISABLE=true
G6_CARRIER_CONFIG_THROTTLE=true
G7_GOOGLE_LOCATION_FORCE_OFF=true
G8_GOOGLE_FCM_PROTECT=true
G9_GMS_RARE_BUCKET_FOREVER=true
G10_CHECKIN_SERVICE_KILL=true

# G11..G20 — new 10
G11_GOOGLE_APPOPS_SENSOR_BLOCK=true
G12_VELVET_COMPONENTS_KILL=true
G13_GOOGLE_BATTERY_HISTORY_OFF=true
G14_GOOGLE_TTS_COMPONENTS_KILL=true
G15_GMS_IDLE_WHITELIST_REMOVE=true
G16_GOOGLE_BG_DATA_RESTRICT=true
G17_GOOGLE_JOBSCHEDULER_QUOTA=true
G18_GOOGLE_BATTERY_SAVER_FORCE=true
G19_GOOGLE_WELLBEING_COMPONENTS_KILL=true
G20_GOOGLE_PARTNER_COMPONENTS_KILL=true

# G21..G30 — new 10
G21_GOOGLE_ASSISTANT_COMPONENTS_KILL=true
G22_GOOGLE_DRIVE_COMPONENTS_KILL=true
G23_GOOGLE_WAKELOCK_KILL=true
G24_GOOGLE_PACKAGES_DISABLE=true
G25_GOOGLE_SETUPWIZARD_SKIP=true
G26_GOOGLE_GMS_UNWANTED_PERMS_REVOKE=true
G27_GOOGLE_THROTTLE_PHONELOCATION=true
G28_GOOGLE_ACCOUNTS_COMPONENTS_KILL=true
G29_GOOGLE_PLAY_SERVICES_TELEMETRY_KILL=true
G30_GOOGLE_AOD_COMPONENTS_KILL=true

# Daily re-apply (hours). 0 = disable. job is owned by module UID
# and survives reboots via cmd jobscheduler. No root required.
VOPL_SCHEDULE_HOURS=24
VGEOF
    ui_print "- vOP+: 50 modules enabled (G1..G50) · daily re-apply = 24h"
fi

# ----------------------------------------------------------------
# Target manifest
# v4.0: single OPTIMAL profile — every non-SAFE row is applied.
# The MIN_PROFILE column is preserved for documentation but no
# longer gates application. OPT rows are skipped only when the
# user is actively using the app (VOID_FG_GRACE_MIN).
# ----------------------------------------------------------------
TARGETS_FILE="$MODPATH/config/targets.conf"
if [ ! -f "$TARGETS_FILE" ]; then
cat > "$TARGETS_FILE" << 'EOF'
# VOID GMS Optimizer v4.0 - target manifest
# Format: PACKAGE | CATEGORY | MIN_PROFILE | DEFAULT_OP | NOTES
#
# CATEGORY:
#   DRAIN  pure battery drain
#   TEL    telemetry / ads / analytics
#   LOC    location services
#   SYNC   sync adapters
#   SETUP  setup / config services
#   OPT    optional Google apps (skipped if used in last N min)
#   SAFE   never touch
#
# DEFAULT_OP:
#   THROTTLE  RUN_IN_BACKGROUND ignore + WAKE_LOCK deny/ignore
#   RESTRICT  THROTTLE + standby bucket = rare
#   TRIM      send-trim-memory hints + light throttle
#   DISABLE   pm disable-user --user 0 <pkg>
#   KEEP      do nothing
#
# v4.0: MIN_PROFILE column is documentation only. All non-SAFE
# rows are applied under the single OPTIMAL profile.
EOF
cat >> "$TARGETS_FILE" << 'EOF'

# === GOOGLE PLAY SERVICES (the elephant in the room) ===
com.google.android.gms|DRAIN|1|THROTTLE|Google Play Services - #1 offender. FCM protected via PROTECT_FCM flag
com.google.android.gms.location|LOC|1|TRIM|Fused Location Provider - heavy CPU when triggered
com.google.android.gms.policy_sidecar_aps|TEL|1|THROTTLE|Telemetry sidecar
com.google.android.gsf|DRAIN|1|THROTTLE|Google Services Framework - checkin loops
com.google.android.gsf.login|SYNC|1|THROTTLE|GSF login service
com.google.android.tts|TEL|1|RESTRICT|Google TTS - constant wake-ups for non-voice users
com.google.android.providers.media.module|DRAIN|1|THROTTLE|MediaProvider - indexer

# === ADS & TELEMETRY ===
com.google.android.adservices.api|TEL|1|THROTTLE|Privacy Sandbox / Topics / FLEDGE
com.google.android.adservices|TEL|1|THROTTLE|Older Ads Services
com.google.android.gms.ads|TEL|1|THROTTLE|AdMob runtime
com.google.android.gms.analytics|TEL|1|THROTTLE|Google Analytics
com.google.android.gms.measurement|TEL|1|THROTTLE|Google Analytics for Firebase
com.google.android.apps.turbo|TEL|1|THROTTLE|Performance tuner - notorious battery drain
com.google.android.apps.wellbeing|TEL|1|THROTTLE|Digital Wellbeing background
com.google.android.configupdater|SETUP|1|THROTTLE|Config updater (runs constantly)
com.google.android.onetimeinitializer|SETUP|3|DISABLE|One-time initializer (no longer needed)
com.google.android.partnersetup|SETUP|3|DISABLE|Carrier config setup

# === SEARCH & ASSISTANT ===
com.google.android.googlequicksearchbox|DRAIN|1|THROTTLE|Google Search - NlPCollectorWakeLock
com.google.android.apps.googleassistant|TEL|1|THROTTLE|Assistant background
com.google.android.tachyon|TEL|1|THROTTLE|Google Duo / Meet (if installed)

# === SYNC ADAPTERS ===
com.google.android.syncadapters.contacts|SYNC|1|THROTTLE|Contacts sync
com.google.android.syncadapters.calendar|SYNC|1|THROTTLE|Calendar sync
com.google.android.syncadapters.people|SYNC|2|THROTTLE|People sync

# === LOCATION-DEPENDENT ===
com.google.android.location|LOC|1|RESTRICT|System Location Manager
com.google.android.cellbroadcastservice|LOC|2|RESTRICT|Cell broadcast receiver

# === OPTIONAL APPS (skipped if used recently) ===
com.google.android.apps.docs|OPT|2|RESTRICT|Google Docs (if unused)
com.google.android.apps.docs.editors.sheets|OPT|2|RESTRICT|Google Sheets (if unused)
com.google.android.apps.docs.editors.slides|OPT|2|RESTRICT|Google Slides (if unused)
com.google.android.apps.maps|OPT|2|RESTRICT|Google Maps (if unused)
com.google.android.apps.photos|OPT|2|RESTRICT|Google Photos (if unused)
com.google.android.apps.youtube.music|OPT|2|RESTRICT|YT Music (if unused)

# === ALWAYS SAFE - DO NOT TOUCH ===
com.android.systemui|SAFE|1|KEEP|SystemUI
com.android.launcher|SAFE|1|KEEP|Pixel Launcher
com.android.launcher3|SAFE|1|KEEP|AOSP Launcher 3
com.android.launcher4|SAFE|1|KEEP|AOSP Launcher 4
com.google.android.apps.nexuslauncher|SAFE|1|KEEP|Nexus Launcher
com.android.inputmethod.latin|SAFE|1|KEEP|Gboard
com.android.phone|SAFE|1|KEEP|Phone app
com.android.settings|SAFE|1|KEEP|Settings
com.google.android.inputmethod.latin|SAFE|1|KEEP|Gboard (com.google variant)
EOF
ui_print "- Targets manifest: ~30 packages, OPTIMAL applies all 50 modules"
fi

# ----------------------------------------------------------------
# Permissions
# ----------------------------------------------------------------
chmod -R 0755 "$MODPATH" 2>/dev/null
find "$MODPATH" -type f -name '*.sh' -exec chmod 0755 {} + 2>/dev/null

# ----------------------------------------------------------------
# Done screen — quick start + sub-commands reference
# ----------------------------------------------------------------
ui_print "- Permissions set"
ui_print " "
ui_print "==============================================="
ui_print "  ✓ Installed — v5.0 OPTIMAL profile ready"
ui_print "==============================================="
ui_print " "
ui_print "  How to use"
ui_print "  ----------"
ui_print " "
ui_print "  AXManager: open module > press ACTION button"
ui_print " "
ui_print "-Done"
