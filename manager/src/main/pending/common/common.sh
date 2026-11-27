#!/system/bin/sh
# ============================================================
# VOID GMS Optimizer v3.0 - Shared library
# ============================================================
# Changelog vs v2.5:
#   - /proc/uid_stat + /proc/wakelocks fallbacks for modern Android
#     (the v2.0 sources are gone on 9+, so wakeups/net always read 0).
#   - "n/a" propagated through metrics delta when a source is missing
#     (no more fake negative/zero deltas in `action.sh` report).
#   - void_apply_profile() condition fix (targets manifest now always
#     loads in service.sh re-apply).
#   - appops restore: ignore invalid sentinel values emitted by
#     `appops get` on some Android builds (n/a, ?, default).
#   - service.sh pidfile ordering fix (mkdir before echo).
#   - pm trim-caches dedup (only one trim call now).
# ============================================================
# Changelog vs v1.2:
#   - `throttle <pkg>` / `unthrottle <pkg>` for per-target control.
#   - `health` reports module state, last run, service status.
#   - `logs` (in action.sh) tails the log with colors.
#   - `trim` (in action.sh) does process trim only, no throttle.
#   - Health snapshot persisted to /sdcard for service.sh to update.
# ============================================================
# (v1.2 history preserved below)
# Changelog vs v1.1:
#   - RUN_ANY_IN_BACKGROUND now uses `ignore` (was `deny`) — fixes FCM
#     breakage on MIUI/HyperOS/ColorOS where `deny` is too aggressive.
#   - Dry-run mode: VOID_DRY_RUN=1 logs every action without applying.
#   - Foreground-aware throttling: targets with MIN_PROFILE=2 (OPT)
#     are skipped if the app is currently in the foreground / was
#     used in the last VOID_FG_GRACE_MIN minutes. Detected via
#     `dumpsys activity recents` (no root needed).
#   - History CSV rotation: keeps the last VOID_HISTORY_KEEP lines.
#   - Banner is now optional / smaller (skipped on install if missing).
# ============================================================

# ----------------------------------------------------------------
# Source ROM-specific logic (vendor detection + quirks + self-heal)
# ----------------------------------------------------------------
# shellcheck source=./rom.sh
. "${MODDIR:-/data/adb/modules/void_gms_optimizer}/common/rom.sh" 2>/dev/null \
    || . "$(dirname "$0")/rom.sh" 2>/dev/null \
    || true

# ----------------------------------------------------------------
# Paths
# ----------------------------------------------------------------
VOID_USER_DIR="/sdcard/Android/data/voidgms"
VOID_LOG_DIR="$VOID_USER_DIR/log"
VOID_STATS_DIR="$VOID_USER_DIR/stats"
VOID_HISTORY="$VOID_STATS_DIR/optimize_history.csv"
VOID_DATA_LOG="$VOID_LOG_DIR/voidgms.log"

# ----------------------------------------------------------------
# Pretty output
# ----------------------------------------------------------------
VOID_RED='\033[0;31m'
VOID_GREEN='\033[0;32m'
VOID_YELLOW='\033[1;33m'
VOID_CYAN='\033[0;36m'
VOID_BOLD='\033[1m'
VOID_NC='\033[0m'

vprint()  { echo "$*"; }
vok()     { echo "[OK]   $*"; }
vwarn()   { echo "[WARN] $*"; }
verr()    { echo "[ERR]  $*"; }

# ----------------------------------------------------------------
# Logging
# ----------------------------------------------------------------
vlog_init() {
    mkdir -p "$VOID_LOG_DIR" "$VOID_STATS_DIR" 2>/dev/null
}

vlog() {
    local level="$1"; shift
    [ "${DEBUG_LOG:-false}" = "true" ] || return 0
    [ -d "$VOID_LOG_DIR" ] || mkdir -p "$VOID_LOG_DIR" 2>/dev/null
    {
        printf '[%s] [%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$level" "$*"
    } >> "$VOID_DATA_LOG" 2>/dev/null
}

# ----------------------------------------------------------------
# Load config (with sane defaults)
# ----------------------------------------------------------------
void_load_config() {
    # v4.0: single profile, no more lite/balanced/aggressive toggle.
    # The module picks the right balance of throttling vs preservation
    # internally and exposes only safety toggles (PROTECT_FCM, FG grace).
    PROFILE="OPTIMAL"
    PROTECT_FCM="${PROTECT_FCM:-true}"
    ENABLE_KERNEL_TWEAKS="${ENABLE_KERNEL_TWEAKS:-false}"
    AUTO_OPTIMIZE_ON_BOOT="${AUTO_OPTIMIZE_ON_BOOT:-true}"
    DEBUG_LOG="${DEBUG_LOG:-true}"
    WHITELIST_PACKAGES="${WHITELIST_PACKAGES:-}"
    # v1.2: aggressive kill (standalone toggle, not a profile)
    AGGRESSIVE_KILL="${AGGRESSIVE_KILL:-true}"
    VOID_FG_GRACE_MIN="${VOID_FG_GRACE_MIN:-10}"
    VOID_HISTORY_KEEP="${VOID_HISTORY_KEEP:-500}"
    VOID_DRY_RUN="${VOID_DRY_RUN:-0}"
    # v2.0: metrics
    VOID_ENABLE_METRICS="${VOID_ENABLE_METRICS:-true}"
    VOID_TRIM_LEVEL="${VOID_TRIM_LEVEL:-COMPLETE}"

    local cfg=""
    [ -f "/data/adb/modules/void_gms_optimizer/config/voidgms.conf" ] && \
        cfg="/data/adb/modules/void_gms_optimizer/config/voidgms.conf"
    [ -z "$cfg" ] && [ -f "$MODDIR/config/voidgms.conf" ] && cfg="$MODDIR/config/voidgms.conf"
    if [ -n "$cfg" ]; then
        # shellcheck disable=SC1090
        . "$cfg"
    fi

    vlog INFO "config loaded: profile=$PROFILE protect_fcm=$PROTECT_FCM dry_run=$VOID_DRY_RUN fg_grace=${VOID_FG_GRACE_MIN}m aggressive_kill=$AGGRESSIVE_KILL"
}

# ----------------------------------------------------------------
# Targets manifest loader
# ----------------------------------------------------------------
void_load_targets() {
    VOID_TARGETS_FILE=""
    [ -f "/data/adb/modules/void_gms_optimizer/config/targets.conf" ] && \
        VOID_TARGETS_FILE="/data/adb/modules/void_gms_optimizer/config/targets.conf"
    [ -z "$VOID_TARGETS_FILE" ] && [ -f "$MODDIR/config/targets.conf" ] && \
        VOID_TARGETS_FILE="$MODDIR/config/targets.conf"
    [ -f "$VOID_TARGETS_FILE" ] || return 1
    return 0
}

# ----------------------------------------------------------------
# Detect Android version
# ----------------------------------------------------------------
VOID_ANDROID_API=0
void_detect_android() {
    local v
    v=$(getprop ro.build.version.sdk 2>/dev/null)
    [ -z "$v" ] && v=$(getprop ro.build.version.release 2>/dev/null)
    case "$v" in
        [4-9])  VOID_ANDROID_API=$v ;;
        1[0-6]) VOID_ANDROID_API=$v ;;
        *)      VOID_ANDROID_API=14 ;;
    esac
    vlog INFO "Android API: $VOID_ANDROID_API"
    # v3.0: detect ROM/vendor so we can apply the right quirks
    type void_detect_rom >/dev/null 2>&1 && void_detect_rom
}

# ----------------------------------------------------------------
# Check if a package exists on the device
# ----------------------------------------------------------------
# v4.1: cached pm-list lookup. package_exists used to fork
# `pm list packages | grep` for every target (30+ * multiple call
# sites = 100+ forks per action). Now it forks once per run.
VOID_PM_CACHE=""
void_pm_cache_load() {
    [ -n "$VOID_PM_CACHE" ] && return 0
    VOID_PM_CACHE="$(pm list packages 2>/dev/null)"
    [ -z "$VOID_PM_CACHE" ] && VOID_PM_CACHE="#empty"
    return 0
}
void_pm_cache_reset() { VOID_PM_CACHE=""; }
package_exists() {
    [ -z "$1" ] && return 1
    void_pm_cache_load
    printf '%s\n' "$VOID_PM_CACHE" | grep -qE "^package:$1$"
}

# ----------------------------------------------------------------
# NEW v1.2 — Foreground-aware check
# Returns 0 if the package was in the foreground or used recently.
# Returns 1 if it's safe to throttle.
# Strategy:
#   1. Top-of-task — `dumpsys activity recents` shows the focused task.
#   2. Usage stats (if available) — last time the app was used.
# ----------------------------------------------------------------
VOID_FG_CACHE=""
void_is_in_foreground() {
    local pkg="$1"
    [ -z "$pkg" ] && return 1
    [ "${VOID_FG_GRACE_MIN:-10}" -le 0 ] && return 1  # grace disabled

    # Use cached recents (refreshed once per run)
    if [ -z "$VOID_FG_CACHE" ]; then
        VOID_FG_CACHE=$(dumpsys activity recents 2>/dev/null \
            | grep -oE 'com\.[a-zA-Z0-9._]+' \
            | sort -u)
    fi

    # Currently in foreground / recent task list?
    if echo "$VOID_FG_CACHE" | grep -qx "$pkg"; then
        vlog INFO "fg-skip: $pkg (in recents)"
        return 0
    fi

    # Check usagestats for last-used timestamp
    local last
    last=$(dumpsys usagestats 2>/dev/null \
        | awk -v p="$pkg" '
            $0 ~ "PackageStats{" p "$" || $0 ~ "package=" p "[, }]" {found=1}
            found && /lastTimeUsed=/ {gsub(/.*lastTimeUsed=/, ""); print; exit}
        ')
    if [ -n "$last" ] && [ "$last" != "0" ]; then
        local now last_s diff_min
        now=$(date +%s)
        last_s=$((last / 1000))
        diff_min=$(( (now - last_s) / 60 ))
        if [ "$diff_min" -lt "${VOID_FG_GRACE_MIN:-10}" ]; then
            vlog INFO "fg-skip: $pkg (used ${diff_min}m ago, grace=${VOID_FG_GRACE_MIN}m)"
            return 0
        fi
    fi
    return 1
}

# ----------------------------------------------------------------
# AppOps helper — supports dry-run.
# ----------------------------------------------------------------
void_run() {
    # void_run <description> <command...>
    local desc="$1"; shift
    if [ "${VOID_DRY_RUN:-0}" = "1" ]; then
        vlog DRY "[DRY] $desc -> $*"
        return 0
    fi
    "$@" 2>/dev/null
    vlog INFO "$desc"
}

# ----------------------------------------------------------------
# Throttle a package using AppOps + standby bucket.
#   mode = "FULL"  = RUN_IN_BACKGROUND ignore + RUN_ANY_IN_BACKGROUND ignore + WAKE_LOCK deny
#   mode = "LITE"  = RUN_IN_BACKGROUND ignore + WAKE_LOCK ignore
#   mode = "FGM"   = keep FCM working (RUN_ANY_IN_BACKGROUND allow, others throttled)
#
# v1.2 CHANGE: FULL now uses `ignore` for RUN_ANY_IN_BACKGROUND (was `deny`).
#   Rationale: `deny` is treated as a hard block by MIUI/HyperOS/ColorOS
#   and breaks FCM / Play Services self-checks. `ignore` lets the package
#   start but the result is discarded — same end-user effect, way safer.
# ----------------------------------------------------------------
void_throttle_package() {
    local pkg="$1"
    local mode="${2:-FULL}"
    [ -z "$pkg" ] && return 1
    package_exists "$pkg" || { vlog INFO "skip $pkg (not installed)"; return 0; }

    case "$mode" in
        FGM)
            void_run "throttle(FGM) $pkg" cmd appops set "$pkg" RUN_IN_BACKGROUND ignore
            void_run "throttle(FGM) $pkg" cmd appops set "$pkg" WAKE_LOCK ignore
            void_run "throttle(FGM) $pkg" cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND allow
            ;;
        LITE)
            void_run "throttle(LITE) $pkg" cmd appops set "$pkg" RUN_IN_BACKGROUND ignore
            void_run "throttle(LITE) $pkg" cmd appops set "$pkg" WAKE_LOCK ignore
            ;;
        FULL|*)
            void_run "throttle(FULL) $pkg" cmd appops set "$pkg" RUN_IN_BACKGROUND ignore
            void_run "throttle(FULL) $pkg" cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND ignore
            void_run "throttle(FULL) $pkg" cmd appops set "$pkg" WAKE_LOCK deny
            void_run "throttle(FULL) $pkg" cmd appops set "$pkg" START_FOREGROUND ignore
            ;;
    esac
    return 0
}

# ----------------------------------------------------------------
# Remove package from battery saver / deviceidle whitelist
# ----------------------------------------------------------------
void_unwhitelist_package() {
    local pkg="$1"
    [ -z "$pkg" ] && return 1
    package_exists "$pkg" || return 0
    void_run "unwhitelist $pkg" dumpsys deviceidle whitelist "-$pkg"
    return 0
}

# ----------------------------------------------------------------
# Set app standby bucket (Android 9+).
# Values: active=10, working_set=20, frequent=30, rare=40, restricted=50
# ----------------------------------------------------------------
void_set_standby_bucket() {
    local pkg="$1"
    local bucket="${2:-rare}"
    [ -z "$pkg" ] && return 1
    [ "$VOID_ANDROID_API" -lt 28 ] && { vlog INFO "skip bucket (API<28): $pkg"; return 0; }
    package_exists "$pkg" || return 0
    void_run "standby-bucket($bucket) $pkg" am set-standby-bucket "$pkg" "$bucket"
    return 0
}

# ----------------------------------------------------------------
# Trim memory of all processes of a given package
# ----------------------------------------------------------------
void_trim_package() {
    local pkg="$1"
    local level="${2:-RUNNING_LOW}"
    [ -z "$pkg" ] && return 1
    package_exists "$pkg" || return 0
    local pid
    for pid in $(pidof "$pkg" 2>/dev/null); do
        void_run "trim($level) $pkg pid=$pid" am send-trim-memory --user 0 "$pid" "$level"
    done
    return 0
}

# ----------------------------------------------------------------
# Disable a package for the current user
# v2.0: now uses reversible variant that backs up state.
# ----------------------------------------------------------------
void_disable_package() {
    local pkg="$1"
    [ -z "$pkg" ] && return 1
    package_exists "$pkg" || return 0
    void_disable_pkg_reversible "$pkg"
    return 0
}

# ----------------------------------------------------------------
# v1.3 NEW: Apply throttle to a SINGLE package (per-target control).
#   void_throttle_one <pkg>           -> FULL throttle
#   void_throttle_one <pkg> fgm       -> FGM mode (keeps FCM alive)
#   void_throttle_one <pkg> lite      -> LITE throttle
# Honours PROTECT_FCM for com.google.android.gms.
# ----------------------------------------------------------------
void_throttle_one() {
    local pkg="$1"
    local mode="${2:-full}"
    [ -z "$pkg" ] && { vlog ERR "throttle_one: empty pkg"; return 1; }
    package_exists "$pkg" || { vlog WARN "throttle_one: $pkg not installed"; return 1; }

    # GMS-specific protection
    if [ "$pkg" = "com.google.android.gms" ] && [ "$PROTECT_FCM" = "true" ] && [ "$mode" != "fgm" ]; then
        vlog INFO "throttle_one: $pkg auto-switched to FGM (PROTECT_FCM=true)"
        mode="fgm"
    fi

    case "$mode" in
        fgm)
            void_run "throttle_one(FGM) $pkg" cmd appops set "$pkg" RUN_IN_BACKGROUND ignore
            void_run "throttle_one(FGM) $pkg" cmd appops set "$pkg" WAKE_LOCK ignore
            void_run "throttle_one(FGM) $pkg" cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND allow
            void_run "unwhitelist $pkg" dumpsys deviceidle whitelist "-$pkg"
            ;;
        lite)
            void_run "throttle_one(LITE) $pkg" cmd appops set "$pkg" RUN_IN_BACKGROUND ignore
            void_run "throttle_one(LITE) $pkg" cmd appops set "$pkg" WAKE_LOCK ignore
            ;;
        full|*)
            void_run "throttle_one(FULL) $pkg" cmd appops set "$pkg" RUN_IN_BACKGROUND ignore
            void_run "throttle_one(FULL) $pkg" cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND ignore
            void_run "throttle_one(FULL) $pkg" cmd appops set "$pkg" WAKE_LOCK deny
            void_run "throttle_one(FULL) $pkg" cmd appops set "$pkg" START_FOREGROUND ignore
            void_run "unwhitelist $pkg" dumpsys deviceidle whitelist "-$pkg"
            ;;
    esac
    return 0
}

# ----------------------------------------------------------------
# v1.3 NEW: Revert throttle for a SINGLE package.
# Resets AppOps to default and re-whitelists the package.
# ----------------------------------------------------------------
void_unthrottle_one() {
    local pkg="$1"
    [ -z "$pkg" ] && { vlog ERR "unthrottle_one: empty pkg"; return 1; }
    package_exists "$pkg" || { vlog WARN "unthrottle_one: $pkg not installed"; return 1; }

    void_run "appops reset $pkg" cmd appops reset "$pkg"
    [ "$VOID_ANDROID_API" -ge 28 ] && \
        void_run "bucket active $pkg" am set-standby-bucket "$pkg" active
    void_run "whitelist +$pkg" dumpsys deviceidle whitelist "+$pkg"
    vlog INFO "unthrottle_one: $pkg reverted"
    return 0
}

# ----------------------------------------------------------------
# v1.3 NEW: Health snapshot.
# Writes VOID_HEALTH_FILE for the service.sh to refresh and
# for `sh action.sh health` to read.
# ----------------------------------------------------------------
void_write_health() {
    local status="$1"
    local extra="$2"
    mkdir -p "$VOID_STATS_DIR" 2>/dev/null
    {
        printf 'status=%s\n' "$status"
        printf 'last_run=%s\n' "$(date '+%Y-%m-%d %H:%M:%S')"
        printf 'profile=%s\n' "${PROFILE:-OPTIMAL}"
        printf 'protect_fcm=%s\n' "${PROTECT_FCM:-true}"
        printf 'android_api=%s\n' "${VOID_ANDROID_API:-?}"
        printf 'pid=%s\n' "$$"
        [ -n "$extra" ] && printf 'extra=%s\n' "$extra"
    } > "$VOID_HEALTH_FILE" 2>/dev/null
}

# ----------------------------------------------------------------
# v1.3 NEW: Standalone process trim (no throttle).
# Sends RUNNING_LOW trim hint to up to 200 non-whitelisted PIDs.
# ----------------------------------------------------------------
void_trim_all() {
    local limit="${1:-200}"
    local level="${2:-RUNNING_LOW}"
    local ps_tmp="/tmp/.voidgms_trim_$$"
    local pid name trim_count=0

    if type void_enum_pids >/dev/null 2>&1; then
        void_enum_pids "$ps_tmp" 2>/dev/null
    else
        : > "$ps_tmp"
        for d in /proc/[0-9]*; do
            [ -r "$d/cmdline" ] || continue
            pid="${d##*/}"
            name=$(tr '\0' ' ' < "$d/cmdline" 2>/dev/null | awk '{print $1}')
            name="${name##*/}"
            [ -z "$name" ] && continue
            [ "$pid" -le 1 ] 2>/dev/null && continue
            printf '%s\t%s\n' "$pid" "$name" >> "$ps_tmp"
        done
    fi

    [ -s "$ps_tmp" ] && while IFS="$(printf '\t')" read -r pid name; do
        [ -z "$pid" ] && continue
        [ "$pid" -le 1 ] 2>/dev/null && continue
        case " $WHITELIST_PACKAGES " in
            *" $name "*|*" ${name}. "*) continue ;;
        esac
        am send-trim-memory --user 0 "$pid" "$level" 2>/dev/null \
            && trim_count=$((trim_count + 1))
        [ "$trim_count" -ge "$limit" ] && break
    done < "$ps_tmp"
    rm -f "$ps_tmp" 2>/dev/null
    return "$trim_count"
}

# ----------------------------------------------------------------
# Apply ONE target line from targets.conf
# v1.2: honours foreground grace for OPT-category targets
# v3.0: honours per-pkg intensity learning from previous runs
# ----------------------------------------------------------------
void_apply_target() {
    local pkg="$1"
    local category="$2"
    local min_profile="$3"   # accepted for backward compat, no longer gates
    local default_op="$4"

    [ -z "$pkg" ] && return 1
    [ "$category" = "SAFE" ] && return 0

    case " $WHITELIST_PACKAGES " in
        *" $pkg "*|*" ${pkg%.} "*) vlog INFO "whitelisted: $pkg"; return 0 ;;
    esac

    # v4.0: single OPTIMAL profile — the min_profile field is preserved
    # in targets.conf for documentation but no longer gates apply.
    # v1.2: don't throttle OPT apps that the user is actively using
    if [ "$category" = "OPT" ] && [ "${VOID_FG_GRACE_MIN:-10}" -gt 0 ]; then
        if void_is_in_foreground "$pkg"; then
            return 0
        fi
    fi

    if [ "$pkg" = "com.google.android.gms" ] && [ "$PROTECT_FCM" = "true" ]; then
        # Backup first so restore is faithful
        void_backup_pkg_appops "$pkg"
        void_throttle_package "$pkg" "FGM"
        void_unwhitelist_package "$pkg"
        return 0
    fi

    # v3.0: per-pkg intensity from previous runs
    # If learning said "intensify", escalate THROTTLE/RESTRICT -> RESTRICT/DISABLE
    # If "relax", drop to LITE throttle or skip
    local effective_op="$default_op"
    if type void_pkg_intensity >/dev/null 2>&1; then
        void_pkg_intensity "$pkg"
        local intensity=$?   # 0=intensify 1=hold 2=relax
        case "$intensity" in
            0)  # intensify
                case "$default_op" in
                    THROTTLE) effective_op="RESTRICT" ;;
                    TRIM)     effective_op="THROTTLE" ;;
                    *)        : ;;
                esac
                vlog INFO "intensity: $pkg escalating $default_op -> $effective_op"
                ;;
            2)  # relax
                case "$default_op" in
                    DISABLE)  effective_op="RESTRICT" ;;   # don't actually disable
                    RESTRICT) effective_op="THROTTLE" ;;
                    THROTTLE) effective_op="TRIM" ;;
                esac
                vlog INFO "intensity: $pkg relaxing $default_op -> $effective_op"
                ;;
        esac
    fi

    case "$effective_op" in
        THROTTLE)
            void_backup_pkg_appops "$pkg"
            void_throttle_package "$pkg" "FULL"
            void_unwhitelist_package "$pkg"
            ;;
        RESTRICT)
            void_backup_pkg_appops "$pkg"
            void_throttle_package "$pkg" "FULL"
            void_unwhitelist_package "$pkg"
            void_set_standby_bucket "$pkg" "rare"
            # v2.0: escalated trim - COMPLETE for restricted apps
            void_trim_escalated "$pkg" "RESTRICT"
            ;;
        TRIM)
            void_backup_pkg_appops "$pkg"
            void_trim_package "$pkg" "MODERATE"
            void_throttle_package "$pkg" "LITE"
            ;;
        DISABLE)
            void_disable_package "$pkg"
            ;;
        KEEP) ;;
        *) vlog WARN "unknown op: $effective_op for $pkg" ;;
    esac

    return 0
}

# ----------------------------------------------------------------
# Walk the targets manifest and apply the current profile
# ----------------------------------------------------------------
void_apply_profile() {
    # FIX v2.5: condition was inverted. The previous code asked
    # `[ -z "$VOID_TARGETS_FILE" ] && [ -f "$VOID_TARGETS_FILE" ]`
    # which can never be true (a file can't be both empty and exist),
    # so void_load_targets never re-ran when called from contexts
    # that only seeded the file later.
    [ -n "$VOID_TARGETS_FILE" ] || void_load_targets
    [ -f "$VOID_TARGETS_FILE" ] || { vlog ERR "no targets file"; return 1; }

    local count=0
    local applied=0
    local pkg cat lvl op notes

    while IFS='|' read -r pkg cat lvl op notes; do
        case "$pkg" in ""|\#*) continue ;; esac
        pkg=$(echo "$pkg" | tr -d ' \t\r\n')
        cat=$(echo "$cat" | tr -d ' \t\r\n')
        lvl=$(echo "$lvl" | tr -d ' \t\r\n')
        op=$(echo "$op"   | tr -d ' \t\r\n')
        [ -z "$pkg" ] && continue
        count=$((count + 1))
        if void_apply_target "$pkg" "$cat" "${lvl:-1}" "${op:-KEEP}"; then
            applied=$((applied + 1))
        fi
    done < "$VOID_TARGETS_FILE"

    vlog INFO "profile applied: $applied/$count targets (profile=$PROFILE)"
    return 0
}

# ----------------------------------------------------------------
# Global tweaks — v1.2: am kill-all is now gated by AGGRESSIVE_KILL
# ----------------------------------------------------------------
void_apply_global_tweaks() {
    vlog INFO "applying global battery tweaks"

    void_run "adaptive_battery" settings put global adaptive_battery_management_enabled 1
    void_run "forced_app_standby" settings put global forced_app_standby_enabled 1
    void_run "device_idle_constants" settings put global device_idle_constants "inactive_to=300000"
    void_run "dynamic_power_savings" settings put global dynamic_power_savings_enabled 1
    void_run "background_check" settings put global background_check_enabled 1
    void_run "app_standby" settings put global app_standby_enabled 1
    void_run "gcm_network_manager" settings put global gcm_network_manager_enabled 1

    void_run "trim-caches" pm trim-caches 128G
    void_run "idle-maintenance" cmd activity idle-maintenance

    if [ "${AGGRESSIVE_KILL:-false}" = "true" ]; then
        void_run "am kill-all (aggressive)" am kill-all
    else
        vlog INFO "am kill-all skipped (set AGGRESSIVE_KILL=true to enable)"
    fi

    vlog INFO "global tweaks applied"
    return 0
}

# ----------------------------------------------------------------
# Restore everything to factory defaults
# v2.0: uses reversible restore (reads backup files) so user-set
# appops overrides that were in place before the module ran are
# preserved instead of being wiped by `appops reset`.
# ----------------------------------------------------------------
void_restore_all() {
    vlog INFO "restoring defaults"

    [ -f "$VOID_TARGETS_FILE" ] || void_load_targets
    [ -n "$VOID_BACKUP_DIR" ] || void_backup_init

    local pkg cat lvl op notes
    while IFS='|' read -r pkg cat lvl op notes; do
        case "$pkg" in ""|\#*) continue ;; esac
        pkg=$(echo "$pkg" | tr -d ' \t\r\n')
        [ -z "$pkg" ] && continue
        package_exists "$pkg" || continue

        # Reversible appops restore (falls back to reset if no backup)
        void_restore_pkg_appops "$pkg"
        # Always restore whitelist + active bucket regardless of backup
        void_run "whitelist +$pkg" dumpsys deviceidle whitelist "+$pkg"
        void_enable_pkg_reversible "$pkg"
    done < "$VOID_TARGETS_FILE"

    # Revert every global setting the module touched, in order.
    void_run "restore adaptive_battery" settings put global adaptive_battery_management_enabled 1
    void_run "restore forced_app_standby" settings put global forced_app_standby_enabled 0
    void_run "restore device_idle_constants" settings delete global device_idle_constants
    void_run "restore dynamic_power_savings" settings put global dynamic_power_savings_enabled 0
    void_run "restore background_check" settings put global background_check_enabled 1

    vlog INFO "defaults restored"
    return 0
}

# ----------------------------------------------------------------
# History rotation — keep only the last N lines
# ----------------------------------------------------------------
void_rotate_history() {
    [ -f "$VOID_HISTORY" ] || return 0
    local keep="${VOID_HISTORY_KEEP:-500}"
    local lines
    lines=$(wc -l < "$VOID_HISTORY" 2>/dev/null)
    [ -z "$lines" ] && return 0
    if [ "$lines" -gt "$keep" ]; then
        local tmp="${VOID_HISTORY}.tmp"
        tail -n "$keep" "$VOID_HISTORY" > "$tmp" 2>/dev/null \
            && mv "$tmp" "$VOID_HISTORY"
        vlog INFO "history rotated: kept $keep of $lines lines"
    fi
}

# ----------------------------------------------------------------
# Memory snapshot
# ----------------------------------------------------------------
# IMPORTANT: we report `free_mb` based on MemAvailable (not MemFree).
# MemFree alone is misleading on Android because the kernel keeps most
# RAM in reclaimable caches; MemAvailable is what the OOM killer and
# the framework actually consider usable. Reporting free_mb from
# MemFree made the "RAM freed" delta look frozen at 0 even after a
# successful trim, which users perceived as a bug (it was).
# ----------------------------------------------------------------
void_mem_stats() {
    local mi
    mi=$(cat /proc/meminfo 2>/dev/null)
    VOID_MEM_TOTAL=$(echo "$mi"   | awk '/^MemTotal:/ {print $2}')
    VOID_MEM_FREE=$(echo "$mi"    | awk '/^MemFree:/ {print $2}')
    VOID_MEM_AVAIL=$(echo "$mi"   | awk '/^MemAvailable:/ {print $2}')
    VOID_MEM_CACHED=$(echo "$mi"  | awk '/^Cached:/ {print $2}')
    # Fallback for very old kernels without MemAvailable (API < 19)
    [ -z "$VOID_MEM_AVAIL" ] && VOID_MEM_AVAIL=$((VOID_MEM_FREE + VOID_MEM_CACHED))
    # Guard against empty /proc/meminfo
    : "${VOID_MEM_TOTAL:=0}"
    : "${VOID_MEM_FREE:=0}"
    : "${VOID_MEM_AVAIL:=0}"
    VOID_MEM_USED=$((VOID_MEM_TOTAL - VOID_MEM_AVAIL))
    [ "$VOID_MEM_USED" -lt 0 ] && VOID_MEM_USED=0
    [ "$VOID_MEM_TOTAL" -le 0 ] && VOID_MEM_TOTAL=1
    VOID_MEM_PCT=$((VOID_MEM_USED * 100 / VOID_MEM_TOTAL))
    VOID_MEM_TOTAL_MB=$((VOID_MEM_TOTAL / 1024))
    VOID_MEM_USED_MB=$((VOID_MEM_USED / 1024))
    # Report the *usable* free, not raw MemFree. This is what users care
    # about and what makes the before/after delta actually move.
    VOID_MEM_FREE_MB=$((VOID_MEM_AVAIL / 1024))
    VOID_MEM_AVAIL_MB=$((VOID_MEM_AVAIL / 1024))
}

# ----------------------------------------------------------------
# Main entrypoint
# ----------------------------------------------------------------
void_optimize_all() {
    vlog_init
    void_load_config
    void_load_targets
    void_detect_android
    void_detect_env
    void_backup_init

    vlog INFO "===== OPTIMIZE START (profile=$PROFILE dry=$VOID_DRY_RUN env=$VOID_ENV) ====="

    # v3.0: skip if conditions are bad (charging, low battery, screen on)
    if type void_should_skip_now >/dev/null 2>&1; then
        if void_should_skip_now; then
            vlog INFO "skip: $VOID_SKIP_REASON"
            void_write_health "skipped" "skip: $VOID_SKIP_REASON"
            return 0
        fi
        vlog INFO "skip check passed"
    fi

    # v3.0: self-heal from previous apply's breakage (FCM, crashes, settings)
    if type void_self_heal >/dev/null 2>&1; then
        void_self_heal
    fi

    # v3.0: pre-apply vendor quirks (pause MIUI powerkeeper, etc)
    if type void_rom_quirk_init >/dev/null 2>&1; then
        void_rom_quirk_init
    fi

    # Snapshot metrics BEFORE applying throttles
    void_metrics_snapshot "before"

    void_apply_profile
    void_apply_global_tweaks

    # v3.0: post-apply vendor quirks (re-apply on MIUI, rehash ColorOS battery, etc)
    if type void_rom_quirk_post_apply >/dev/null 2>&1; then
        void_rom_quirk_post_apply
    fi

    # v3.0: learn per-pkg intensity for next run
    if type void_learn_intensity >/dev/null 2>&1; then
        void_learn_intensity
    fi

    void_rotate_history
    void_write_health "ok" "applied by void_optimize_all"
    date '+%Y-%m-%d %H:%M:%S' > /data/adb/modules/void_gms_optimizer/state/last-apply.txt 2>/dev/null

    vlog INFO "===== OPTIMIZE DONE ====="

    mkdir -p "$VOID_STATS_DIR" 2>/dev/null
    void_mem_stats
    {
        printf '%s,%s,%s,%dMB,%d%%,%s\n' \
            "$(date '+%Y-%m-%d %H:%M:%S')" \
            "optimize" \
            "$PROFILE" \
            "$VOID_MEM_FREE_MB" \
            "$VOID_MEM_PCT" \
            "${VOID_ENV:-adb}"
    } >> "$VOID_HISTORY" 2>/dev/null

    return 0
}

# ----------------------------------------------------------------
# Show status
# ----------------------------------------------------------------
void_show_status() {
    void_load_config
    void_load_targets
    void_detect_android

    echo "${VOID_CYAN}============================================================${VOID_NC}"
    echo "${VOID_CYAN}  VOID GMS Optimizer v1.2 - STATUS${VOID_NC}"
    echo "${VOID_CYAN}============================================================${VOID_NC}"
    echo ""
    printf "  Android API      : %s\n" "$VOID_ANDROID_API"
    printf "  Profile          : %s\n" "$PROFILE"
    printf "  Protect FCM      : %s\n" "$PROTECT_FCM"
    printf "  Aggressive kill  : %s\n" "$AGGRESSIVE_KILL"
    printf "  FG grace (OPT)   : %s min\n" "$VOID_FG_GRACE_MIN"
    printf "  Auto on boot     : %s\n" "$AUTO_OPTIMIZE_ON_BOOT"
    printf "  Dry-run          : %s\n" "$VOID_DRY_RUN"
    printf "  Environment      : %s\n" "${ENV:-adb}"
    echo ""
    echo "${VOID_BOLD}Per-target status:${VOID_NC}"

    local pkg cat lvl op notes
    while IFS='|' read -r pkg cat lvl op notes; do
        case "$pkg" in ""|\#*) continue ;; esac
        pkg=$(echo "$pkg" | tr -d ' \t\r\n')
        cat=$(echo "$cat" | tr -d ' \t\r\n')
        lvl=$(echo "$lvl" | tr -d ' \t\r\n')
        [ -z "$pkg" ] && continue
        if ! package_exists "$pkg"; then
            printf "  ${VOID_YELLOW}[skip]${VOID_NC} %s  (not installed)\n" "$pkg"
            continue
        fi
        local bg rab wl bucket gated
        bg=$(cmd appops get "$pkg" RUN_IN_BACKGROUND 2>/dev/null | awk '{print $NF}')
        rab=$(cmd appops get "$pkg" RUN_ANY_IN_BACKGROUND 2>/dev/null | awk '{print $NF}')
        wl=$(cmd appops get "$pkg" WAKE_LOCK 2>/dev/null | awk '{print $NF}')
        bucket=""
        [ "$VOID_ANDROID_API" -ge 28 ] && bucket=$(am get-standby-bucket "$pkg" 2>/dev/null | awk '{print $NF}')
        gated=" "
        printf "  %s  bg=%s  rab=%s  wl=%s  bucket=%s\n" \
            "$pkg" "${bg:-allow}" "${rab:-allow}" "${wl:-allow}" "${bucket:-n/a}"
    done < "$VOID_TARGETS_FILE"
    echo ""
}

# ----------------------------------------------------------------
# Walk the manifest, return list of PIDs (for trim stage)
# ----------------------------------------------------------------
void_enum_pids() {
    local out="$1"
    : > "$out" 2>/dev/null
    for d in /proc/[0-9]*; do
        [ -r "$d/cmdline" ] || continue
        local pid name
        pid="${d##*/}"
        name=$(tr '\0' ' ' < "$d/cmdline" 2>/dev/null | awk '{print $1}')
        name="${name##*/}"
        [ -z "$name" ] && continue
        [ "$pid" -le 1 ] 2>/dev/null && continue
        printf '%s\t%s\n' "$pid" "$name" >> "$out"
    done
}

# ============================================================
# v2.0 NEW: Environment detection
# Distinguishes between adb (shell), axmanager, magisk, ksu, apatch.
# Many operations (inotify hooks, /proc writes, certain appops) only
# work in some environments. We detect up front and store in
# VOID_ENV so downstream code can gate.
# ============================================================
void_detect_env() {
    # AXManager sets AXERON=true in the module environment
    if [ "${AXERON:-false}" = "true" ]; then
        VOID_ENV="axmanager"
    # Magisk exposes these paths/props
    elif [ -d "/data/adb/magisk" ] || [ -n "${MAGISK_VER:-}" ]; then
        VOID_ENV="magisk"
    # KernelSU
    elif [ -d "/data/adb/ksu" ] || [ -n "${KSU_VER:-}" ]; then
        VOID_ENV="ksu"
    # APatch
    elif [ -d "/data/adb/ap" ] || [ -n "${APATCH_VER:-}" ]; then
        VOID_ENV="apatch"
    else
        VOID_ENV="adb"
    fi
    export VOID_ENV
    vlog INFO "environment: $VOID_ENV"
}

# ============================================================
# v2.0 NEW: UID resolver
# Returns the Linux UID(s) for a package name. Empty if not installed.
# We need UIDs to attribute /proc/uid_stat/* traffic.
# ============================================================
VOID_UID_CACHE=""
void_pkg_uid() {
    local pkg="$1"
    [ -z "$pkg" ] && echo "" && return 1
    # cmd package list packages -U prints "package:<name> uid:<n>"
    cmd package list packages -U 2>/dev/null \
        | awk -v p="$pkg" -F'[: ]' '$2==p {print $4; exit}'
}

# ============================================================
# v2.5 REWORKED: Metric snapshot
#
# v2.0 used /proc/wakelocks + /proc/uid_stat/<uid>/{tcp_rcv,tcp_snd}.
# Both paths are gone on modern Android:
#   - /proc/wakelocks was removed in Android 8 (P).
#   - /proc/uid_stat was removed in Android 7+ in most vendor kernels.
# The result: net_rx / net_tx / wakeups always read 0 and the "delta"
# never moved, which users perceived as a fake metric.
#
# v2.5 replaces them with sources that actually exist on stock
# Android 9+ without root:
#   - wakeups:  /proc/wakelocks -> /d/wakeup_sources -> dumpsys power
#               -> mark as "n/a" instead of lying with 0.
#   - network:  /proc/uid_stat  -> /proc/net/tcp + /proc/net/tcp6
#               (we aggregate by uid from the line trailer).
#   - cpu:      /proc/<pid>/stat field 14+15 (still works).
#   - procs:    pidof <pkg> count (unchanged).
# ============================================================
VOID_METRIC_FILE="$VOID_STATS_DIR/metrics.csv"
VOID_METRIC_PRE="$VOID_STATS_DIR/metrics.before"
VOID_METRIC_NA="n/a"

# Best-effort wakeup count. Prefers readable kernel sources,
# falls back to dumpsys, finally to "n/a".
# We filter to lines mentioning Google-ish services so the number
# is comparable across runs.
_void_read_wakeups() {
    if [ -r /proc/wakelocks ]; then
        awk '
            /Google|gms|gsf|search|assistant|tachyon|turbo|wellbeing|cellbroadcast|syncadapters|location/ {
                for (i=1; i<=NF; i++) if ($i=="count:") { sum += $(i+1)+0 }
            }
            END { print sum+0 }
        ' /proc/wakelocks 2>/dev/null && return 0
    fi
    if [ -r /d/wakeup_sources ]; then
        awk '
            /Google|gms|gsf|search|assistant|tachyon|turbo|wellbeing|cellbroadcast|syncadapters|location/ {
                for (i=1; i<=NF; i++) if ($i=="active_count:") { sum += $(i+1)+0 }
            }
            END { print sum+0 }
        ' /d/wakeup_sources 2>/dev/null && return 0
    fi
    # dumpsys power can be huge; we only scan the Wake Locks section
    local w
    w=$(dumpsys power 2>/dev/null \
        | awk '
            /Wake Locks:/ {flag=1; next}
            flag && /Google|gms|gsf|search|assistant|tachyon|turbo|wellbeing/ {count++}
            flag && /^[^ ]/ && !/Wake Locks:/ {flag=0}
            END { print count+0 }
        ')
    if [ -n "$w" ] && [ "$w" -gt 0 ] 2>/dev/null; then
        echo "$w"
        return 0
    fi
    echo "$VOID_METRIC_NA"
}

# Best-effort network byte counter for a given UID.
# Prefers /proc/uid_stat (legacy). If missing, parses /proc/net/tcp{,6}
# and sums the line trailer's rx_queue / tx_queue (per-socket, not bytes,
# but consistent within a single kernel). We also accept a third fallback:
# `cmd network stats` (Android 11+) for the actual byte counter.
#
# We do NOT use cmd network stats as the primary because it requires
# a manifest query, takes ~1-2s, and isn't always present on AOSP.
_void_read_net_bytes() {
    local uid="$1" sum=0
    [ -z "$uid" ] && echo 0 && return 0
    if [ -d "/proc/uid_stat/$uid" ]; then
        local rx tx
        rx=$(cat "/proc/uid_stat/$uid/tcp_rcv" 2>/dev/null || echo 0)
        tx=$(cat "/proc/uid_stat/$uid/tcp_snd" 2>/dev/null || echo 0)
        echo "$rx $tx"
        return 0
    fi
    # Fallback: scan /proc/net/tcp{,6}. The "uid:" trailer is reliably
    # the second-to-last field on every Android kernel we've seen.
    # This gives us a per-socket queue length, not bytes. We mark the
    # metric as "proxy" in the log so the user knows the unit changed.
    local f
    for f in /proc/net/tcp /proc/net/tcp6; do
        [ -r "$f" ] || continue
        local v
        v=$(awk -v u="$uid" '
            NR>1 {
                # trailer looks like: "uid:1234 inode:..."
                for (i=1; i<=NF; i++) {
                    if ($i ~ /^uid:/) {
                        gsub(/uid:/, "", $i)
                        if ($i+0 == u+0) { sum += 1 }
                    }
                }
            }
            END { print sum+0 }
        ' "$f" 2>/dev/null)
        sum=$((sum + v))
    done
    # Returned as "rx_proxy tx_proxy" - same placeholder convention
    # used by the legacy path so the column shape is stable.
    echo "$sum $sum"
}

void_metrics_snapshot() {
    local label="$1"   # "before" | "after"
    [ -z "$label" ] && { vlog WARN "metrics_snapshot needs a label"; return 1; }
    [ "${VOID_ENABLE_METRICS:-true}" = "true" ] || { vlog INFO "metrics disabled"; return 0; }
    [ -f "$VOID_TARGETS_FILE" ] || void_load_targets

    mkdir -p "$VOID_STATS_DIR" 2>/dev/null
    local out="${VOID_METRIC_FILE%.csv}.${label}.tmp"

    # ---- 1. Wakeup count (best-effort) ----
    local wakeups
    wakeups=$(_void_read_wakeups)

    # ---- 2. Network bytes for all UIDs of target packages ----
    # We track two columns:
    #   net_rx_bytes, net_tx_bytes -> byte counts when /proc/uid_stat is present
    #   net_rx_proxy, net_tx_proxy -> socket count from /proc/net/tcp fallback
    # If even the proxy is 0 we still report 0 honestly; the user can
    # tell from the source banner which method was used.
    local net_rx=0 net_tx=0 net_proxy=0 used_proxy=0
    local pkg uid pair
    while IFS='|' read -r pkg cat lvl op notes; do
        case "$pkg" in ""|\#*|"") continue ;; esac
        pkg=$(echo "$pkg" | tr -d ' \t\r\n')
        [ -z "$pkg" ] && continue
        uid=$(void_pkg_uid "$pkg")
        [ -z "$uid" ] && continue
        pair=$(_void_read_net_bytes "$uid")
        if [ -d "/proc/uid_stat/$uid" ]; then
            net_rx=$((net_rx + $(echo "$pair" | awk '{print $1}')))
            net_tx=$((net_tx + $(echo "$pair" | awk '{print $2}')))
        else
            used_proxy=1
            net_proxy=$((net_proxy + $(echo "$pair" | awk '{print $1}')))
        fi
    done < "$VOID_TARGETS_FILE"

    # ---- 3. CPU jiffies for all running PIDs of target packages ----
    local cpu_jiffies=0
    while IFS='|' read -r pkg cat lvl op notes; do
        case "$pkg" in ""|\#*|"") continue ;; esac
        pkg=$(echo "$pkg" | tr -d ' \t\r\n')
        [ -z "$pkg" ] && continue
        package_exists "$pkg" || continue
        for pid in $(pidof "$pkg" 2>/dev/null); do
            # /proc/<pid>/stat: field 14 = utime, 15 = stime (jiffies)
            local j
            j=$(awk '{print $14+$15}' "/proc/$pid/stat" 2>/dev/null)
            [ -n "$j" ] && cpu_jiffies=$((cpu_jiffies + j))
        done
    done < "$VOID_TARGETS_FILE"

    # ---- 4. Active process count for Google apps ----
    local proc_count=0
    while IFS='|' read -r pkg cat lvl op notes; do
        case "$pkg" in ""|\#*|"") continue ;; esac
        pkg=$(echo "$pkg" | tr -d ' \t\r\n')
        [ -z "$pkg" ] && continue
        if pidof "$pkg" >/dev/null 2>&1; then
            proc_count=$((proc_count + 1))
        fi
    done < "$VOID_TARGETS_FILE"

    {
        printf 'label=%s\n' "$label"
        printf 'timestamp=%s\n' "$(date '+%Y-%m-%d %H:%M:%S')"
        printf 'wakeups=%s\n' "$wakeups"
        printf 'net_rx=%s\n' "$net_rx"
        printf 'net_tx=%s\n' "$net_tx"
        printf 'net_proxy=%s\n' "$net_proxy"
        printf 'net_source=%s\n' "$([ "$used_proxy" = "1" ] && echo proxy || echo uid_stat)"
        printf 'cpu_jiffies=%s\n' "$cpu_jiffies"
        printf 'proc_count=%s\n' "$proc_count"
    } > "$out" 2>/dev/null

    # Persist "before" snapshot for delta in next run
    if [ "$label" = "before" ]; then
        cp "$out" "$VOID_METRIC_PRE" 2>/dev/null
    fi

    vlog INFO "metrics($label): wakeups=$wakeups rx=$net_rx tx=$net_tx proxy=$net_proxy src=$([ "$used_proxy" = "1" ] && echo proxy || echo uid_stat) cpu=$cpu_jiffies procs=$proc_count"
}

# ============================================================
# v2.5 REWORKED: Compute delta between before/after snapshots
# Returns formatted strings via globals:
#   VOID_DELTA_WAKEUPS, VOID_DELTA_NET_RX, VOID_DELTA_NET_TX,
#   VOID_DELTA_NET_PROXY, VOID_DELTA_CPU, VOID_DELTA_PROCS
#
# The CSV row written to $VOID_METRIC_FILE now has 13 columns:
#   ts, profile, env,
#   wake_b, wake_a, d_wake,
#   net_b, net_a, d_net,           (bytes if uid_stat, else proxy sockets)
#   net_src,                       ("uid_stat" or "proxy")
#   cpu_b, cpu_a
# v2.0 readers that only count 11 columns will still parse the first 11.
# ============================================================
void_metrics_delta() {
    [ "${VOID_ENABLE_METRICS:-true}" = "true" ] || { vlog INFO "metrics disabled"; return 0; }
    local before="${VOID_METRIC_PRE}"
    local after="${VOID_METRIC_FILE%.csv}.after.tmp"
    [ -f "$before" ] || { vlog INFO "no pre-snapshot; skipping delta"; return 0; }
    [ -f "$after" ]  || { vlog INFO "no post-snapshot; skipping delta"; return 0; }

    local bw aw br ar bt at bc ac bp ap npr_b npr_a src_b src_a
    bw=$(grep '^wakeups='    "$before" | cut -d= -f2)
    aw=$(grep '^wakeups='    "$after"  | cut -d= -f2)
    br=$(grep '^net_rx='     "$before" | cut -d= -f2)
    ar=$(grep '^net_rx='     "$after"  | cut -d= -f2)
    bt=$(grep '^net_tx='     "$before" | cut -d= -f2)
    at=$(grep '^net_tx='     "$after"  | cut -d= -f2)
    bc=$(grep '^cpu_jiffies=' "$before" | cut -d= -f2)
    ac=$(grep '^cpu_jiffies=' "$after"  | cut -d= -f2)
    bp=$(grep '^proc_count='  "$before" | cut -d= -f2)
    ap=$(grep '^proc_count='  "$after"  | cut -d= -f2)
    npr_b=$(grep '^net_proxy=' "$before" | cut -d= -f2)
    npr_a=$(grep '^net_proxy=' "$after"  | cut -d= -f2)
    src_a=$(grep '^net_source=' "$after" | cut -d= -f2)

    : "${bw:=0}" "${aw:=0}" "${br:=0}" "${ar:=0}" "${bt:=0}" "${at:=0}" "${bc:=0}" "${ac:=0}" "${bp:=0}" "${ap:=0}" "${npr_b:=0}" "${npr_a:=0}"

    # If either value is non-numeric (e.g. "n/a" for wakeups on
    # kernels without /proc/wakelocks), keep the delta as "n/a"
    # instead of computing a meaningless 0 or -N.
    _void_isnum() { case "$1" in ''|*[!0-9]*) return 1;; *) return 0;; esac; }
    if _void_isnum "$aw" && _void_isnum "$bw"; then
        VOID_DELTA_WAKEUPS=$((aw - bw))
    else
        VOID_DELTA_WAKEUPS="n/a"
    fi
    if _void_isnum "$ar" && _void_isnum "$br"; then
        VOID_DELTA_NET_RX=$((ar - br))
    else
        VOID_DELTA_NET_RX="n/a"
    fi
    if _void_isnum "$at" && _void_isnum "$bt"; then
        VOID_DELTA_NET_TX=$((at - bt))
    else
        VOID_DELTA_NET_TX="n/a"
    fi
    if _void_isnum "$npr_a" && _void_isnum "$npr_b"; then
        VOID_DELTA_NET_PROXY=$((npr_a - npr_b))
    else
        VOID_DELTA_NET_PROXY="n/a"
    fi
    VOID_DELTA_CPU=$((ac - bc))
    VOID_DELTA_PROCS=$((ap - bp))

    # Append a CSV row to the permanent history
    {
        printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' \
            "$(date '+%Y-%m-%d %H:%M:%S')" \
            "${PROFILE:-OPTIMAL}" \
            "${VOID_ENV:-adb}" \
            "$bw" "$aw" "$VOID_DELTA_WAKEUPS" \
            "$br" "$ar" "$VOID_DELTA_NET_RX" \
            "${src_a:-uid_stat}" \
            "$bc" "$ac" "$VOID_DELTA_CPU"
    } >> "$VOID_METRIC_FILE" 2>/dev/null

    vlog INFO "metrics(delta): d_wake=$VOID_DELTA_WAKEUPS d_rx=$VOID_DELTA_NET_RX d_tx=$VOID_DELTA_NET_TX d_proxy=$VOID_DELTA_NET_PROXY d_cpu=$VOID_DELTA_CPU d_procs=$VOID_DELTA_PROCS"
}

# ============================================================
# v2.0 NEW: Escalated trim
# Sends TRIM hints at the right level for the right app:
#   - COMPLETE for never-used apps in RESTRICT bucket
#   - MODERATE for OPT apps (don't kill caches mid-session)
#   - LOW for everything else
# ============================================================
void_trim_escalated() {
    local pkg="$1"
    [ -z "$pkg" ] && return 1
    package_exists "$pkg" || return 0

    local level="RUNNING_LOW"
    # OPT apps: gentle trim so the user doesn't notice
    # but everything else gets COMPLETE to actually reclaim memory
    case "$2" in
        OPT)    level="RUNNING_MODERATE" ;;
        *)      level="COMPLETE" ;;
    esac

    for pid in $(pidof "$pkg" 2>/dev/null); do
        void_run "trim-escalated($level) $pkg pid=$pid" \
            am send-trim-memory --user 0 "$pid" "$level"
    done
}

# ============================================================
# v2.0 NEW: Backup of AppOps state BEFORE throttle
# Writes to /data/adb/modules/void_gms_optimizer/backup/appops-<pkg>.txt
# Restore uses this instead of `appops reset` (which loses user overrides).
# ============================================================
VOID_BACKUP_DIR=""
void_backup_init() {
    # Backup dir lives in module dir (root writable on Magisk/KSU/APatch,
    # world-readable; ADB-mode also has access via $MODDIR).
    VOID_BACKUP_DIR="$MODDIR/backup"
    mkdir -p "$VOID_BACKUP_DIR" 2>/dev/null
}

void_backup_pkg_appops() {
    local pkg="$1"
    [ -z "$pkg" ] && return 1
    [ -n "$VOID_BACKUP_DIR" ] || void_backup_init
    package_exists "$pkg" || return 0
    local out="$VOID_BACKUP_DIR/appops-${pkg}.txt"
    {
        # Snapshot the 4 ops the module touches
        for op in RUN_IN_BACKGROUND RUN_ANY_IN_BACKGROUND WAKE_LOCK START_FOREGROUND; do
            printf '%s=%s\n' "$op" \
                "$(cmd appops get "$pkg" "$op" 2>/dev/null | awk '{print $NF}')"
        done
        printf 'standby_bucket=%s\n' \
            "$(am get-standby-bucket "$pkg" 2>/dev/null | awk '{print $NF}')"
    } > "$out" 2>/dev/null
    vlog INFO "backup: $pkg -> $out"
}

void_restore_pkg_appops() {
    local pkg="$1"
    [ -z "$pkg" ] && return 1
    [ -n "$VOID_BACKUP_DIR" ] || void_backup_init
    local f="$VOID_BACKUP_DIR/appops-${pkg}.txt"
    if [ ! -f "$f" ]; then
        vlog INFO "no backup for $pkg, falling back to appops reset"
        void_run "appops reset $pkg" cmd appops reset "$pkg"
        return 0
    fi
    # Parse and restore
    while IFS='=' read -r k v; do
        case "$k" in
            RUN_IN_BACKGROUND|RUN_ANY_IN_BACKGROUND|WAKE_LOCK|START_FOREGROUND)
                # FIX v2.5: skip empty / placeholder values that some
                # Android builds emit on `appops get` (n/a, ?, default)
                # which would otherwise be set literally and brick the op.
                case "$v" in
                    ""|"n/a"|"?"|"default"|"unknown") continue ;;
                esac
                void_run "restore $k=$v $pkg" \
                    cmd appops set "$pkg" "$k" "$v"
                ;;
            standby_bucket)
                case "$v" in
                    ""|"n/a"|"?"|"default"|"unknown") continue ;;
                esac
                [ "$VOID_ANDROID_API" -ge 28 ] && \
                    void_run "restore bucket=$v $pkg" \
                    am set-standby-bucket "$pkg" "$v"
                ;;
        esac
    done < "$f"
    rm -f "$f"
    vlog INFO "restored: $pkg"
}

# ============================================================
# v2.0 NEW: Reversible package disable
# Backup enabled/disabled state to a sentinel file before disabling,
# restore reads the sentinel to know what to do.
# Without this, restore blindly does `pm enable` which may flip a
# package the user had manually disabled before the module ran.
# ============================================================
void_disable_pkg_reversible() {
    local pkg="$1"
    [ -z "$pkg" ] && return 1
    package_exists "$pkg" || return 0
    [ -n "$VOID_BACKUP_DIR" ] || void_backup_init

    # Record current state. pm list packages -e shows enabled packages.
    if pm list packages -e 2>/dev/null | grep -qE "^package:$pkg$"; then
        echo "enabled" > "$VOID_BACKUP_DIR/disable-${pkg}.state"
    else
        echo "disabled" > "$VOID_BACKUP_DIR/disable-${pkg}.state"
    fi

    if [ "${VOID_DRY_RUN:-0}" = "1" ]; then
        vlog DRY "[DRY] reversible-disable $pkg"
        return 0
    fi
    pm disable-user --user 0 "$pkg" 2>/dev/null || pm disable "$pkg" 2>/dev/null
    vlog INFO "disabled: $pkg (reversible)"
}

void_enable_pkg_reversible() {
    local pkg="$1"
    [ -z "$pkg" ] && return 1
    [ -n "$VOID_BACKUP_DIR" ] || void_backup_init

    local f="$VOID_BACKUP_DIR/disable-${pkg}.state"
    local was_enabled="enabled"
    [ -f "$f" ] && was_enabled=$(cat "$f" 2>/dev/null)

    if [ "${VOID_DRY_RUN:-0}" = "1" ]; then
        vlog DRY "[DRY] reversible-enable $pkg (was=$was_enabled)"
        return 0
    fi

    case "$was_enabled" in
        enabled)  pm enable "$pkg" 2>/dev/null ;;
        disabled) : ;; # user had it disabled, leave it
        *)        pm enable "$pkg" 2>/dev/null ;;
    esac
    rm -f "$f"
    vlog INFO "enabled: $pkg (was=$was_enabled)"
}
