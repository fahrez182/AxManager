#!/system/bin/sh
# ============================================================
# VOID GMS Optimizer v4.0 - Single-action entry point
# ============================================================
# v4.0 simplifies the user model: there is exactly ONE profile
# (OPTIMAL), pre-calibrated. The user only needs to press the
# action button (or run `sh action.sh`). No more profile
# switching, no more lite/balanced/aggressive confusion.
#
# v2.0+ real-metrics reporting (wakeups/network/CPU delta),
# environment detection (adb/magisk/ksu/apatch/axmanager).
#
# Sub-commands:
#   sh action.sh                          -> optimize (default)
#   sh action.sh status                   -> dump current state to file
#   sh action.sh profile                  -> confirm single OPTIMAL profile
#   sh action.sh restore                  -> revert all changes
#   sh action.sh restore-google           -> revert ONLY the Google+ modules
#   sh action.sh google-plus              -> apply ONLY the Google+ modules
#   sh action.sh schedule [hours]         -> register daily re-apply
#   sh action.sh unschedule               -> cancel daily re-apply
#   sh action.sh schedule-status          -> show daily job state
#   sh action.sh schedule-now             -> trigger vOP+ re-apply now
#   sh action.sh health                   -> module + service health
#   sh action.sh logs                     -> tail the log
#   sh action.sh throttle <pkg> [mode]    -> throttle one package
#   sh action.sh unthrottle <pkg>         -> revert throttle for one
#   sh action.sh trim                     -> trim memory only
#   sh action.sh dry-run                  -> show what would be done
# ============================================================

MODDIR="${0%/*}"
[ ! -d "$MODDIR" ] && MODDIR="$(cd "$(dirname "$0")" && pwd)"
[ -z "$MODDIR" ] || [ "$MODDIR" = "." ] && MODDIR="$(pwd)"

# shellcheck source=./common/common.sh
. "$MODDIR/common/common.sh" 2>/dev/null || . "$(dirname "$0")/common/common.sh"

ENV="adb"
[ "$AXERON" = "true" ] && ENV="axmanager"
export ENV

# ----------------------------------------------------------------
# Pretty output helpers
# ----------------------------------------------------------------
stage() { printf "\n  ${VOID_CYAN}%s${VOID_NC} %s\n" "$1" "$2"; }
ok()   { printf "    ${VOID_GREEN}\xe2\x9c\x93${VOID_NC} %s\n" "$*"; }
warn() { printf "    ${VOID_YELLOW}!${VOID_NC} %s\n" "$*"; }
err()  { printf "    ${VOID_RED}\xe2\x9c\x97${VOID_NC} %s\n" "$*"; }

# ----------------------------------------------------------------
# Header
# ----------------------------------------------------------------
clear 2>/dev/null
printf "${VOID_CYAN}============================================================${VOID_NC}\n"
printf "${VOID_CYAN}  VOID GMS Optimizer v4.0 \xe2\x80\x94 Google Services Killer${VOID_NC}\n"
printf "${VOID_CYAN}============================================================${VOID_NC}\n"
printf "\n"

# ----------------------------------------------------------------
# Parse subcommand
# ----------------------------------------------------------------
CMD="${1:-optimize}"
case "$CMD" in
    status)
        vlog_init
        void_load_config
        void_load_targets
        void_detect_android
        status_out="$VOID_USER_DIR/status.txt"
        mkdir -p "$VOID_USER_DIR" 2>/dev/null
        {
            echo "VOID GMS Optimizer v4.0 — STATUS"
            echo "Generated: $(date '+%Y-%m-%d %H:%M:%S')"
            echo "Environment: ${ENV:-adb}"
            echo "Android API: $VOID_ANDROID_API"
            echo "Profile: $PROFILE"
            echo "Protect FCM: $PROTECT_FCM"
            echo ""
            echo "Per-target status:"
            while IFS='|' read -r pkg cat lvl op notes; do
                case "$pkg" in ""|\#*) continue ;; esac
                pkg=$(echo "$pkg" | tr -d ' \t\r\n')
                [ -z "$pkg" ] && continue
                if ! package_exists "$pkg"; then
                    echo "  [skip] $pkg  (not installed)"
                    continue
                fi
                bg=$(cmd appops get "$pkg" RUN_IN_BACKGROUND 2>/dev/null | awk '{print $NF}')
                rab=$(cmd appops get "$pkg" RUN_ANY_IN_BACKGROUND 2>/dev/null | awk '{print $NF}')
                wl=$(cmd appops get "$pkg" WAKE_LOCK 2>/dev/null | awk '{print $NF}')
                bucket=""
                [ "$VOID_ANDROID_API" -ge 28 ] && bucket=$(am get-standby-bucket "$pkg" 2>/dev/null | awk '{print $NF}')
                echo "  $pkg  bg=${bg:-allow}  rab=${rab:-allow}  wl=${wl:-allow}  bucket=${bucket:-n/a}"
            done < "$VOID_TARGETS_FILE"
        } > "$status_out" 2>/dev/null
        ok "Status written to: $status_out"
        exit 0
        ;;
    profile)
        # v4.0: single OPTIMAL profile, no switching. This is a no-op
        # kept so that older shell scripts and muscle memory still work.
        ok "Profile: $PROFILE — pre-calibrated, no switching needed."
        ok "All Google+ throttles are applied. FCM is preserved."
        exit 0
        ;;
    schedule)
        vlog_init
        void_load_config
        stage "[1/1]" "Registering daily re-apply schedule (vOP+)..."
        if [ -f "$MODDIR/void_optimizer_plus/scheduler.sh" ]; then
            HO="${2:-${VOPL_SCHEDULE_HOURS:-24}}"
            VOPL_SCHEDULE_HOURS="$HO" sh "$MODDIR/void_optimizer_plus/scheduler.sh" set
            ok "Daily re-apply scheduled every ${HO}h. Check with: sh action.sh schedule-status"
        else
            err "void_optimizer_plus/scheduler.sh not found"
            exit 1
        fi
        exit 0
        ;;
    unschedule)
        vlog_init
        void_load_config
        stage "[1/1]" "Cancelling daily re-apply schedule (vOP+)..."
        if [ -f "$MODDIR/void_optimizer_plus/scheduler.sh" ]; then
            sh "$MODDIR/void_optimizer_plus/scheduler.sh" clear
            ok "Daily re-apply cancelled."
        else
            err "void_optimizer_plus/scheduler.sh not found"
            exit 1
        fi
        exit 0
        ;;
    schedule-status)
        vlog_init
        if [ -f "$MODDIR/void_optimizer_plus/scheduler.sh" ]; then
            sh "$MODDIR/void_optimizer_plus/scheduler.sh" status
        fi
        exit 0
        ;;
    schedule-now)
        vlog_init
        if [ -f "$MODDIR/void_optimizer_plus/scheduler.sh" ]; then
            sh "$MODDIR/void_optimizer_plus/scheduler.sh" now
            ok "vOP+ re-apply complete."
        fi
        exit 0
        ;;
    restore)
        vlog_init
        void_load_config
        void_load_targets
        void_detect_android
        stage "[1/1]" "Restoring factory defaults..."
        void_restore_all
        ok "Restore complete. Reboot recommended."
        exit 0
        ;;
    restore-google)
        vlog_init
        void_load_config
        void_detect_android
        stage "[1/1]" "Restoring void_optimizer_plus (Google+ modules)..."
        if [ -f "$MODDIR/void_optimizer_plus/revert_google.sh" ]; then
            sh "$MODDIR/void_optimizer_plus/revert_google.sh"
            ok "Google+ modules reverted."
        else
            err "void_optimizer_plus/revert_google.sh not found"
            exit 1
        fi
        exit 0
        ;;
    google-plus)
        vlog_init
        void_load_config
        void_detect_android
        stage "[1/1]" "Running void_optimizer_plus (Google+ modules)..."
        if [ -f "$MODDIR/void_optimizer_plus/entry.sh" ]; then
            sh "$MODDIR/void_optimizer_plus/entry.sh"
            ok "Google+ modules applied."
        else
            err "void_optimizer_plus/entry.sh not found"
            exit 1
        fi
        exit 0
        ;;
    trim)
        vlog_init
        void_load_config
        void_detect_android
        stage "[1/1]" "Trimming process memory (no throttle changes)..."
        trim_count=0
        void_trim_all 200 RUNNING_LOW
        trim_count=$?
        ok "Trim hint sent to $trim_count processes"
        void_write_health "ok" "manual trim: $trim_count processes"
        exit 0
        ;;
    health)
        vlog_init
        void_load_config
        void_load_targets
        void_detect_android
        printf "${VOID_CYAN}============================================================${VOID_NC}\n"
        printf "${VOID_CYAN}  VOID GMS Optimizer v4.0 - HEALTH${VOID_NC}\n"
        printf "${VOID_CYAN}============================================================${VOID_NC}\n"
        printf "\n"
        if [ -d "/data/adb/modules/void_gms_optimizer" ]; then
            printf "  ${VOID_GREEN}\xe2\x9c\x93${VOID_NC} Module installed: /data/adb/modules/void_gms_optimizer\n"
        else
            printf "  ${VOID_RED}\xe2\x9c\x97${VOID_NC} Module NOT installed at /data/adb/modules/void_gms_optimizer\n"
        fi
        if [ -f "/data/adb/modules/void_gms_optimizer/module.prop" ]; then
            local mp="/data/adb/modules/void_gms_optimizer/module.prop"
            local state=""
            [ -f "/data/adb/modules/void_gms_optimizer/disable" ] && state="DISABLED"
            [ -z "$state" ] && [ -f "/data/adb/modules/void_gms_optimizer/remove" ] && state="REMOVED"
            if [ -n "$state" ]; then
                printf "  ${VOID_RED}\xe2\x9c\x97${VOID_NC} Module state: %s\n" "$state"
            else
                printf "  ${VOID_GREEN}\xe2\x9c\x93${VOID_NC} Module state: active\n"
            fi
        fi
        local svc=""
        svc=$(getprop init.svc.void_gms_optimizer 2>/dev/null)
        [ -z "$svc" ] && svc=$(pgrep -f "void_gms_optimizer" 2>/dev/null | head -1)
        if [ -n "$svc" ] && [ "$svc" != "stopped" ]; then
            printf "  ${VOID_GREEN}\xe2\x9c\x93${VOID_NC} Service: running (pid=$svc)\n"
        else
            printf "  ${VOID_YELLOW}!${VOID_NC} Service: not running (sleeping between re-applies)\n"
        fi
        if [ -f "$VOID_HEALTH_FILE" ]; then
            printf "  ${VOID_GREEN}\xe2\x9c\x93${VOID_NC} Last run: "
            grep '^last_run=' "$VOID_HEALTH_FILE" | sed 's/^last_run=//'
            printf "  ${VOID_GREEN}\xe2\x9c\x93${VOID_NC} Last status: "
            grep '^status=' "$VOID_HEALTH_FILE" | sed 's/^status=//'
            local age
            age=$(grep '^last_run=' "$VOID_HEALTH_FILE" | sed 's/^last_run=//' \
                | xargs -I{} date -d "{}" +%s 2>/dev/null)
            if [ -n "$age" ]; then
                local now age_h
                now=$(date +%s)
                age_h=$(( (now - age) / 3600 ))
                if [ "$age_h" -gt 12 ]; then
                    printf "  ${VOID_YELLOW}!${VOID_NC} Last run was %d hours ago (interval is %ss)\n" \
                        "$age_h" "${VOID_REAPPLY_INTERVAL:-21600}"
                fi
            fi
        else
            printf "  ${VOID_YELLOW}!${VOID_NC} Never run. Press ACTION or run: sh action.sh\n"
        fi
        if [ -f "$VOID_HISTORY" ]; then
            local runs
            runs=$(wc -l < "$VOID_HISTORY" 2>/dev/null)
            printf "  ${VOID_GREEN}\xe2\x9c\x93${VOID_NC} History: %s runs recorded\n" "$runs"
            printf "  ${VOID_CYAN}Last 3 runs:${VOID_NC}\n"
            tail -n 3 "$VOID_HISTORY" 2>/dev/null | sed 's/^/    /'
        fi
        printf "\n"
        printf "  ${VOID_CYAN}Config:${VOID_NC}\n"
        printf "    profile=%s\n" "$PROFILE"
        printf "    protect_fcm=%s\n" "$PROTECT_FCM"
        printf "    auto_on_boot=%s\n" "$AUTO_OPTIMIZE_ON_BOOT"
        printf "    aggressive_kill=%s\n" "$AGGRESSIVE_KILL"
        printf "    fg_grace_min=%s\n" "$VOID_FG_GRACE_MIN"
        printf "    reapply_interval=%ss\n" "${VOID_REAPPLY_INTERVAL:-21600}"
        printf "\n"
        exit 0
        ;;
    logs)
        vlog_init
        printf "${VOID_CYAN}============================================================${VOID_NC}\n"
        printf "${VOID_CYAN}  VOID GMS Optimizer v4.0 - LOGS (Ctrl+C to exit)${VOID_NC}\n"
        printf "${VOID_CYAN}============================================================${VOID_NC}\n"
        if [ ! -f "$VOID_DATA_LOG" ]; then
            warn "No log file yet at $VOID_DATA_LOG"
            exit 0
        fi
        tail -n 50 -f "$VOID_DATA_LOG" 2>/dev/null | while IFS= read -r line; do
            case "$line" in
                *"[INFO]"*)  printf "  ${VOID_GREEN}%s${VOID_NC}\n" "$line" ;;
                *"[WARN]"*)  printf "  ${VOID_YELLOW}%s${VOID_NC}\n" "$line" ;;
                *"[ERR]"*)   printf "  ${VOID_RED}%s${VOID_NC}\n" "$line" ;;
                *"[DRY]"*)   printf "  ${VOID_CYAN}%s${VOID_NC}\n" "$line" ;;
                *)           printf "  %s\n" "$line" ;;
            esac
        done
        exit 0
        ;;
    throttle)
        vlog_init
        void_load_config
        PKG="${2:-}"
        MODE="${3:-full}"
        if [ -z "$PKG" ]; then
            err "Usage: sh action.sh throttle <package> [full|lite|fgm]"
            exit 1
        fi
        if ! package_exists "$PKG"; then
            err "Package not installed: $PKG"
            exit 1
        fi
        stage "[1/1]" "Throttling $PKG (mode=$MODE)..."
        void_throttle_one "$PKG" "$MODE"
        ok "Throttled $PKG (mode=$MODE)"
        exit 0
        ;;
    unthrottle)
        vlog_init
        void_load_config
        PKG="${2:-}"
        if [ -z "$PKG" ]; then
            err "Usage: sh action.sh unthrottle <package>"
            exit 1
        fi
        if ! package_exists "$PKG"; then
            err "Package not installed: $PKG"
            exit 1
        fi
        stage "[1/1]" "Reverting throttle for $PKG..."
        void_unthrottle_one "$PKG"
        ok "Reverted $PKG"
        exit 0
        ;;
    dry-run|dryrun|dry)
        vlog_init
        void_load_config
        void_load_targets
        void_detect_android
        VOID_DRY_RUN=1
        export VOID_DRY_RUN
        . "$MODDIR/common/common.sh" 2>/dev/null
        printf "  ${VOID_CYAN}Profile:${VOID_NC}      %s\n" "$PROFILE"
        printf "  ${VOID_CYAN}Mode:${VOID_NC}         ${VOID_YELLOW}DRY-RUN (no changes applied)${VOID_NC}\n"
        printf "  ${VOID_CYAN}FG grace:${VOID_NC}     %s min\n" "$VOID_FG_GRACE_MIN"
        printf "\n"
        stage "[1/2]" "Scanning targets..."
        total=0
        found=0
        while IFS='|' read -r pkg cat lvl op notes; do
            case "$pkg" in ""|\#*) continue ;; esac
            pkg=$(echo "$pkg" | tr -d ' \t\r\n')
            cat=$(echo "$cat" | tr -d ' \t\r\n')
            [ -z "$pkg" ] && continue
            [ "$cat" = "SAFE" ] && continue
            total=$((total + 1))
            package_exists "$pkg" && found=$((found + 1))
        done < "$VOID_TARGETS_FILE"
        ok "Would scan $found of $total targets (OPTIMAL applies all)"
        printf "\n"
        stage "[2/2]" "Actions that WOULD be taken (logged, not applied):"
        printf "\n"
        printf "  ${VOID_BOLD}%-45s %-10s${VOID_NC}\n" "PACKAGE" "OP"
        printf "  %-45s %-10s\n" "---------------------------------------------" "----------"
        while IFS='|' read -r pkg cat lvl op notes; do
            case "$pkg" in ""|\#*) continue ;; esac
            pkg=$(echo "$pkg" | tr -d ' \t\r\n')
            cat=$(echo "$cat" | tr -d ' \t\r\n')
            op=$(echo "$op"   | tr -d ' \t\r\n')
            [ -z "$pkg" ] && continue
            [ "$cat" = "SAFE" ] && continue
            if ! package_exists "$pkg"; then
                printf "  ${VOID_YELLOW}[skip]${VOID_NC} %-45s (not installed)\n" "$pkg"
                continue
            fi
            if [ "$cat" = "OPT" ] && void_is_in_foreground "$pkg"; then
                printf "  ${VOID_CYAN}[fg]${VOID_NC}   %-45s %-10s (in use recently)\n" "$pkg" "${op:-KEEP}"
                continue
            fi
            printf "  ${VOID_GREEN}[would]${VOID_NC} %-45s %-10s\n" "$pkg" "${op:-KEEP}"
        done < "$VOID_TARGETS_FILE"
        printf "\n"
        ok "Dry-run complete. No changes made."
        printf "  ${VOID_CYAN}Tip:${VOID_NC} run ${VOID_BOLD}sh action.sh${VOID_NC} to actually apply\n"
        exit 0
        ;;
    optimize|"")
        CMD="optimize"
        ;;
    help|-h|--help)
        cat <<EOF
Usage: sh action.sh [command]

Commands:
  (default)              Apply the OPTIMAL profile
  status                 Dump per-target optimization state
  profile                Confirm the module uses single OPTIMAL profile
  health                 Module state, last run, service status
  logs                   Tail the log (Ctrl+C to exit)
  throttle <pkg> [mode]  Throttle one package (full|lite|fgm)
  unthrottle <pkg>       Revert throttle for one package
  trim                   Trim process memory only (no throttle)
  restore                Revert all changes
  restore-google         Revert ONLY the Google+ (vOP+) modules
  google-plus            Apply ONLY the Google+ (vOP+) modules
  schedule [hours]       Register daily re-apply (default 24h)
  unschedule             Cancel daily re-apply
  schedule-status        Show current daily job state
  schedule-now           Trigger vOP+ re-apply right now
  dry-run                Show what would be done, without applying

Profile:
  OPTIMAL      pre-calibrated single profile (no switching needed)

Examples:
  sh action.sh
  sh action.sh status
  sh action.sh health
  sh action.sh logs
  sh action.sh trim
  sh action.sh throttle com.google.android.apps.maps full
  sh action.sh unthrottle com.google.android.apps.maps
  sh action.sh restore
  sh action.sh dry-run
EOF
        exit 0
        ;;
    *)
        err "Unknown command: $CMD (try 'help')"
        exit 1
        ;;
esac

# ----------------------------------------------------------------
# Memory snapshot (before)
# ----------------------------------------------------------------
void_mem_stats
start_free_mb=$VOID_MEM_FREE_MB
start_pct=$VOID_MEM_PCT

# ----------------------------------------------------------------
# Load config, show what we're about to do
# ----------------------------------------------------------------
void_load_config
void_load_targets
void_detect_android

printf "  ${VOID_CYAN}Profile:${VOID_NC}      %s\n" "$PROFILE"
printf "  ${VOID_CYAN}Protect FCM:${VOID_NC} %s\n" "$PROTECT_FCM"
printf "  ${VOID_CYAN}Android API:${VOID_NC} %s\n" "$VOID_ANDROID_API"
printf "  ${VOID_CYAN}Environment:${VOID_NC} %s\n" "$ENV"
printf "  ${VOID_CYAN}RAM before:${VOID_NC}  ${start_pct}%% used, ${start_free_mb} MB free\n"
printf "\n"

# ----------------------------------------------------------------
# Stage 1: enumerate targets
# ----------------------------------------------------------------
stage "[1/4]" "Scanning Google services..."
total_targets=0
found_targets=0
while IFS='|' read -r pkg cat lvl op notes; do
    case "$pkg" in ""|\#*) continue ;; esac
    pkg=$(echo "$pkg" | tr -d ' \t\r\n')
    cat=$(echo "$cat" | tr -d ' \t\r\n')
    [ -z "$pkg" ] && continue
    [ "$cat" = "SAFE" ] && continue
    total_targets=$((total_targets + 1))
    if package_exists "$pkg"; then
        found_targets=$((found_targets + 1))
    fi
done < "$VOID_TARGETS_FILE"
ok "Found $found_targets of $total_targets target packages (OPTIMAL applies all)"

# ----------------------------------------------------------------
# Stage 2: apply OPTIMAL
# ----------------------------------------------------------------
stage "[2/4]" "Throttling Google services..."
applied=0
notfound=0
while IFS='|' read -r pkg cat lvl op notes; do
    case "$pkg" in ""|\#*) continue ;; esac
    pkg=$(echo "$pkg" | tr -d ' \t\r\n')
    cat=$(echo "$cat" | tr -d ' \t\r\n')
    lvl=$(echo "$lvl" | tr -d ' \t\r\n')
    op=$(echo "$op"   | tr -d ' \t\r\n')
    [ -z "$pkg" ] && continue
    [ "$cat" = "SAFE" ] && continue
    if ! package_exists "$pkg"; then
        notfound=$((notfound + 1))
        continue
    fi
    # OPTIMAL: no profile gating. All non-SAFE targets are in scope.
    void_apply_target "$pkg" "$cat" "${lvl:-1}" "${op:-KEEP}"
    applied=$((applied + 1))
done < "$VOID_TARGETS_FILE"
ok "Throttled ${applied} packages, ${notfound} not installed"

# ----------------------------------------------------------------
# Stage 3: global tweaks
# ----------------------------------------------------------------
stage "[3/4]" "Applying global battery tweaks..."
void_apply_global_tweaks
ok "Global tweaks applied (adaptive battery, doze, standby buckets, GCM)"

# ----------------------------------------------------------------
# Stage 4: trim everything
# ----------------------------------------------------------------
stage "[4/4]" "Trimming process memory..."
total_trimmed=0
ps_tmp="/tmp/.voidgms_ps_$$"
if ! type void_enum_pids >/dev/null 2>&1; then
    for d in /proc/[0-9]*; do
        [ -r "$d/cmdline" ] || continue
        pid="${d##*/}"
        name=$(tr '\0' ' ' < "$d/cmdline" 2>/dev/null | awk '{print $1}')
        name="${name##*/}"
        [ -z "$name" ] && continue
        [ "$pid" -le 1 ] 2>/dev/null && continue
        printf '%s\t%s\n' "$pid" "$name" >> "$ps_tmp"
    done
else
    void_enum_pids "$ps_tmp" 2>/dev/null
fi

trim_count=0
[ -s "$ps_tmp" ] && while IFS="$(printf '\t')" read -r pid name; do
    [ -z "$pid" ] && continue
    [ "$pid" -le 1 ] 2>/dev/null && continue
    case " $WHITELIST_PACKAGES " in
        *" $name "*|*" ${name}. "*) continue ;;
    esac
    am send-trim-memory --user 0 "$pid" RUNNING_LOW 2>/dev/null \
        && trim_count=$((trim_count + 1))
    [ "$trim_count" -ge 200 ] && break
done < "$ps_tmp"
total_trimmed=$trim_count
rm -f "$ps_tmp" 2>/dev/null
ok "Trim hint sent to $total_trimmed processes"

# ----------------------------------------------------------------
# Final report
# ----------------------------------------------------------------
# Let the kernel settle after trim hints, then average a few samples
# to report a *steady-state* "after" value. Using the peak was misleading:
# it captured a transient spike that didn't match what the user actually
# had available a second later, making before/after look inconsistent.
sleep 3
sum_free_mb=0
sum_pct=0
sample_count=0
total_wait=0
while [ "$total_wait" -lt 6 ]; do
    void_mem_stats
    sum_free_mb=$((sum_free_mb + VOID_MEM_FREE_MB))
    sum_pct=$((sum_pct + VOID_MEM_PCT))
    sample_count=$((sample_count + 1))
    sleep 2
    total_wait=$((total_wait + 2))
done
[ "$sample_count" -lt 1 ] && sample_count=1
end_free_mb=$((sum_free_mb / sample_count))
end_pct=$((sum_pct / sample_count))

void_metrics_snapshot "after"
void_metrics_delta
freed_mb=$((end_free_mb - start_free_mb))
freed_sign=""
if [ "$freed_mb" -gt 0 ]; then
    freed_sign="+"
    freed_color="$VOID_GREEN"
elif [ "$freed_mb" -lt 0 ]; then
    freed_sign=""
    freed_color="$VOID_YELLOW"
else
    freed_color="$VOID_NC"
fi

printf "\n"
printf "${VOID_GREEN}============================================================${VOID_NC}\n"
printf "${VOID_GREEN}  \xe2\x9c\x93 Optimization complete!${VOID_NC}\n"
printf "${VOID_GREEN}============================================================${VOID_NC}\n"
printf "\n"
printf "  ${VOID_CYAN}Profile applied:${VOID_NC}    ${VOID_BOLD}%s${VOID_NC}\n" "$PROFILE"
printf "  ${VOID_CYAN}Packages throttled:${VOID_NC} ${VOID_GREEN}%d${VOID_NC}\n" "$applied"
printf "  ${VOID_CYAN}Processes trimmed:${VOID_NC}  ${VOID_GREEN}%d${VOID_NC}\n" "$total_trimmed"
printf "  ${VOID_CYAN}RAM freed:${VOID_NC}         ${freed_color}${freed_sign}%d MB${VOID_NC}  ${VOID_CYAN}(avg of %d samples)${VOID_NC}\n" "$freed_mb" "$sample_count"
printf "\n"
if [ "$freed_mb" -gt 0 ]; then
    after_color="$VOID_GREEN"
elif [ "$freed_mb" -lt 0 ]; then
    after_color="$VOID_YELLOW"
else
    after_color="$VOID_CYAN"
fi
printf "  ${VOID_CYAN}RAM before:${VOID_NC}  ${start_free_mb} MB free  \xe2\x86\x92  ${VOID_CYAN}RAM after:${VOID_NC}  ${after_color}%d MB free${VOID_NC}\n" \
    "$end_free_mb"
if [ "$start_pct" -ne "$end_pct" ]; then
    printf "  ${VOID_CYAN}(usage:${VOID_NC} %d%% \xe2\x86\x92 %d%%${VOID_CYAN})${VOID_NC}\n" \
        "$start_pct" "$end_pct"
fi
printf "\n"

_void_fmt_bytes() {
    local v="$1"
    case "$v" in
        ""|n/a) echo "n/a"; return 0 ;;
    esac
    local kb=$((v / 1024))
    if [ "$kb" -gt 1024 ] 2>/dev/null; then
        echo "$((kb / 1024)) MB"
    else
        echo "${kb} KB"
    fi
}

if [ -n "${VOID_DELTA_WAKEUPS:-}" ]; then
    d_wake="$VOID_DELTA_WAKEUPS"
    d_rx="$VOID_DELTA_NET_RX"
    d_tx="$VOID_DELTA_NET_TX"
    d_proxy="${VOID_DELTA_NET_PROXY:-n/a}"

    rx_str=$(_void_fmt_bytes "$d_rx")
    tx_str=$(_void_fmt_bytes "$d_tx")

    case "$d_wake" in
        n/a)
            wake_color="$VOID_YELLOW"; wake_sign=""; wake_disp="n/a"
            ;;
        -*)
            wake_color="$VOID_GREEN"; wake_sign="-"; wake_disp="${d_wake#-}"
            ;;
        *)
            wake_color="$VOID_YELLOW"; wake_sign="+"; wake_disp="$d_wake"
            ;;
    esac

    printf "  ${VOID_BOLD}${VOID_CYAN}Real-world impact (Google apps):${VOID_NC}\n"
    printf "    Wakeups:        ${wake_color}${wake_sign}%s${VOID_NC}\n" "$wake_disp"
    printf "    Net RX:         %s\n" "$rx_str"
    printf "    Net TX:         %s\n" "$tx_str"
    if [ "$d_proxy" != "n/a" ] && [ -n "$d_proxy" ]; then
        printf "    Net sockets:    %s (proxy metric)\n" "$d_proxy"
    fi
    printf "    CPU jiffies:    %d\n" "${VOID_DELTA_CPU:-0}"
    printf "\n"
fi

if [ "$PROTECT_FCM" = "true" ]; then
    printf "  ${VOID_GREEN}\xe2\x9c\x93 FCM protected${VOID_NC} \xe2\x80\x94 push notifications will arrive normally\n"
else
    printf "  ${VOID_YELLOW}! FCM NOT protected${VOID_NC} \xe2\x80\x94 push notifications may delay 1-2 minutes\n"
fi
printf "\n"
printf "  ${VOID_CYAN}Status file:${VOID_NC}     ${VOID_BOLD}sh action.sh status${VOID_NC}\n"
printf "  ${VOID_CYAN}Revert:${VOID_NC}          ${VOID_BOLD}sh action.sh restore${VOID_NC}  or uninstall\n"
printf "  ${VOID_CYAN}Revert vOP+:${VOID_NC}     ${VOID_BOLD}sh action.sh restore-google${VOID_NC}\n"
printf "  ${VOID_CYAN}Apply vOP+:${VOID_NC}      ${VOID_BOLD}sh action.sh google-plus${VOID_NC}\n"
printf "  ${VOID_CYAN}Schedule:${VOID_NC}        ${VOID_BOLD}sh action.sh schedule [hours]${VOID_NC}\n"
printf "\n${VOID_CYAN}============================================================${VOID_NC}\n\n"

mkdir -p "$VOID_STATS_DIR" 2>/dev/null
{
    printf '%s,%s,%s,%dMB,%d%%,%d,%d,%s\n' \
        "$(date '+%Y-%m-%d %H:%M:%S')" \
        "$PROFILE" \
        "${ENV:-adb}" \
        "$freed_mb" \
        "$end_pct" \
        "$applied" \
        "$total_trimmed" \
        "fcm=$PROTECT_FCM"
} >> "$VOID_HISTORY" 2>/dev/null

vlog INFO "action complete: profile=$PROFILE applied=$applied trimmed=$total_trimmed freed=${freed_mb}MB"

exit 0
