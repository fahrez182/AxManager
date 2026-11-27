#!/system/bin/sh
# ============================================================
# VOID GMS Optimizer v3.0 - ROM detection + per-vendor quirks
# ============================================================
# This file is sourced from common.sh and adds:
#   void_detect_rom()          - identify vendor / fork
#   void_rom_quirk_init()      - load quirks for detected ROM
#   void_rom_quirk_pre_apply() - run before throttle pass
#   void_rom_quirk_post_apply()- run after throttle pass
#   void_should_skip_now()     - decide whether to skip this run
#   void_self_heal()           - detect & repair post-throttle breakage
#   void_learn_intensity()     - per-pkg intensity learning loop
#
# All heuristics are best-effort and degrade gracefully on unknown
# devices. The module must NEVER brick a device, even on a ROM
# we don't recognise.
# ============================================================

# ----------------------------------------------------------------
# ROM / vendor detection
# ----------------------------------------------------------------
# We check 6 sources in order of reliability:
#   1. ro.product.brand           (vendor marketing name, almost always set)
#   2. ro.product.manufacturer    (OEM legal name)
#   3. ro.product.model           (device model)
#   4. ro.build.fingerprint       (vendor build tag, has "-vendor-" pattern)
#   5. known package presence     (e.g. com.miui.securitycenter)
#   6. known system properties    (e.g. ro.miui.ui.version.name)
#
# Result: VOID_ROM_VENDOR and VOID_ROM_FAMILY get set.
# VOID_ROM_QUIRKS gets a space-separated list of quirk names that
# void_rom_quirk_init() turns into a sequence of action items.
# ----------------------------------------------------------------
VOID_ROM_VENDOR="unknown"
VOID_ROM_FAMILY="aosp"
VOID_ROM_QUIRKS=""
VOID_ROM_ANDROID_FORK="stock"   # stock | miui | hyperos | coloros | origin | oneui | emui | harmonyos | oxygenos | nothingos | zenui | funtouch | magic | zui
VOID_ROM_GMS_VARIANT="vanilla"  # vanilla | microg | none
VOID_ROM_BATTERY_GATE="0"      # "1" if vendor forcibly restores appops on boot
VOID_ROM_BUCKET_RESET="0"      # "1" if vendor resets standby-bucket periodically
VOID_ROM_NEEDS_REBOOT="0"      # "1" if changes only fully take effect after reboot
VOID_ROM_DETECTED=0

void_detect_rom() {
    local brand manuf model fp
    brand=$(getprop ro.product.brand 2>/dev/null)
    manuf=$(getprop ro.product.manufacturer 2>/dev/null)
    model=$(getprop ro.product.model 2>/dev/null)
    fp=$(getprop ro.build.fingerprint 2>/dev/null)

    # Normalise to lowercase for matching
    brand=$(echo "$brand" | tr '[:upper:]' '[:lower:]')
    manuf=$(echo "$manuf" | tr '[:upper:]' '[:lower:]')
    model=$(echo "$model" | tr '[:upper:]' '[:lower:]')
    fp=$(echo "$fp" | tr '[:upper:]' '[:lower:]')

    # Default: AOSP
    VOID_ROM_VENDOR="aosp"
    VOID_ROM_FAMILY="aosp"
    VOID_ROM_ANDROID_FORK="stock"

    # ----------------- SAMSUNG / ONEUI -----------------
    case "$brand $manuf $fp" in
        *samsung*|*sec-*|*"samsungexynos"*)
            VOID_ROM_VENDOR="samsung"
            VOID_ROM_FAMILY="oneui"
            VOID_ROM_ANDROID_FORK="oneui"
            VOID_ROM_QUIRKS="samsung_early_reboot samsung_never_disable_system samsung_doze_aggressive"
            # OneUI rewrites appops on its own update flow
            VOID_ROM_BATTERY_GATE="1"
            VOID_ROM_NEEDS_REBOOT="0"
            ;;
    esac

    # ----------------- XIAOMI / MIUI / HYPEROS -----------------
    if [ -z "${VOID_ROM_QUIRKS}" ]; then
        local miui_ver hyperos_ver
        miui_ver=$(getprop ro.miui.ui.version.name 2>/dev/null)
        hyperos_ver=$(getprop ro.hyperos.version 2>/dev/null)
        if [ -n "$miui_ver" ] || [ -n "$hyperos_ver" ] || [ -d /data/miuilog ]; then
            VOID_ROM_VENDOR="xiaomi"
            VOID_ROM_FAMILY="miui"
            if [ -n "$hyperos_ver" ] || [ "$fp" = *"hyperos"* ]; then
                VOID_ROM_ANDROID_FORK="hyperos"
                VOID_ROM_QUIRKS="xiaomi_use_ignore xiaomi_double_apply xiaomi_pause_miui_optimization xiaomi_disable_analytics"
            else
                VOID_ROM_ANDROID_FORK="miui"
                VOID_ROM_QUIRKS="xiaomi_use_ignore xiaomi_double_apply xiaomi_pause_miui_optimization xiaomi_disable_analytics"
            fi
            # MIUI's "background killer" services fight us - we re-apply
            VOID_ROM_BATTERY_GATE="1"
            VOID_ROM_BUCKET_RESET="1"
        fi
    fi

    # ----------------- OPPO / REALME / ONEPLUS / COLOROS -----------------
    if [ -z "${VOID_ROM_QUIRKS}" ]; then
        if pm list packages 2>/dev/null | grep -qE "com\.oppo\.|com\.coloros\.|com\.oplus\.|com\.realme\." \
           || [ -n "$(getprop ro.oppo.theme.version 2>/dev/null)" ] \
           || [ -n "$(getprop ro.coloros.version 2>/dev/null)" ] \
           || [ -n "$(getprop ro.oplus.theme.version 2>/dev/null)" ]; then
            VOID_ROM_VENDOR="oppo"   # covers oppo/realme/oneplus
            VOID_ROM_FAMILY="coloros"
            VOID_ROM_ANDROID_FORK="coloros"
            VOID_ROM_QUIRKS="coloros_battery_rehash coloros_ignore_only coloros_disable_companion coloros_quiet_mode"
            VOID_ROM_BATTERY_GATE="1"
            VOID_ROM_BUCKET_RESET="1"
            # Distinguish oneplus/realme for logs
            case "$brand" in
                *oneplus*) VOID_ROM_VENDOR="oneplus" ;;
                *realme*)  VOID_ROM_VENDOR="realme" ;;
            esac
        fi
    fi

    # ----------------- VIVO / ORIGINOS / FUNTOUCH -----------------
    if [ -z "${VOID_ROM_QUIRKS}" ]; then
        if [ -n "$(getprop ro.vivo.product.version 2>/dev/null)" ] \
           || [ -n "$(getprop ro.vivo.os.version 2>/dev/null)" ] \
           || pm list packages 2>/dev/null | grep -qE "com\.vivo\."; then
            VOID_ROM_VENDOR="vivo"
            VOID_ROM_FAMILY="originos"
            VOID_ROM_ANDROID_FORK="origin"
            VOID_ROM_QUIRKS="vivo_disable_iqo vivo_quiet_mode vivo_battery_rehash"
            VOID_ROM_BATTERY_GATE="1"
            VOID_ROM_BUCKET_RESET="1"
        fi
    fi

    # ----------------- HUAWEI / HONOR / EMUI / HARMONYOS -----------------
    if [ -z "${VOID_ROM_QUIRKS}" ]; then
        if [ -n "$(getprop ro.huawei.version 2>/dev/null)" ] \
           || [ -n "$(getprop ro.build.version.emui 2>/dev/null)" ] \
           || [ -n "$(getprop ro.harmonyos.version 2>/dev/null)" ] \
           || [ -n "$(getprop ro.honor.version 2>/dev/null)" ] \
           || pm list packages 2>/dev/null | grep -qE "com\.huawei\.|com\.hihonor\."; then
            VOID_ROM_VENDOR="huawei"
            if [ -n "$(getprop ro.harmonyos.version 2>/dev/null)" ]; then
                VOID_ROM_VENDOR="huawei"
                VOID_ROM_FAMILY="harmonyos"
                VOID_ROM_ANDROID_FORK="harmonyos"
                VOID_ROM_QUIRKS="harmonyos_appops_compat harmonyos_no_gms_skip_all"
                # On HarmonyOS without GMS, our GMS throttles are no-ops
                # but still safe. Honor is the same fork.
                if pm list packages 2>/dev/null | grep -q "com.google.android.gms"; then
                    VOID_ROM_GMS_VARIANT="vanilla"
                else
                    VOID_ROM_GMS_VARIANT="none"
                fi
            else
                VOID_ROM_FAMILY="emui"
                VOID_ROM_ANDROID_FORK="emui"
                VOID_ROM_QUIRKS="huawei_ignore_only huawei_disable_powergenie huawei_no_appops_persist"
                if pm list packages 2>/dev/null | grep -qE "com\.huawei\.powergenie|com\.huawei\.systemmanager"; then
                    :
                fi
            fi
            VOID_ROM_BATTERY_GATE="1"
            VOID_ROM_BUCKET_RESET="1"
            case "$brand" in
                *honor*|*hihonor*) VOID_ROM_VENDOR="honor" ;;
            esac
        fi
    fi

    # ----------------- GOOGLE / PIXEL / AOSP -----------------
    if [ -z "${VOID_ROM_QUIRKS}" ]; then
        case "$brand" in
            *google*|*pixel*|*aosp*|*android*)
                if [ "$brand" = "google" ] || [ "$fp" = *"google"* ] || [ "$fp" = *"aosp"* ]; then
                    VOID_ROM_VENDOR="google"
                    VOID_ROM_FAMILY="aosp"
                    VOID_ROM_ANDROID_FORK="stock"
                    # Pixel is well-behaved - no quirks needed
                    VOID_ROM_QUIRKS=""
                fi
                ;;
        esac
    fi

    # ----------------- SONY / XPERIA -----------------
    if [ -z "${VOID_ROM_QUIRKS}" ] || [ "$VOID_ROM_VENDOR" = "aosp" ]; then
        case "$brand $manuf" in
            *sony*|*xperia*)
                VOID_ROM_VENDOR="sony"
                VOID_ROM_FAMILY="aosp"
                VOID_ROM_ANDROID_FORK="stock"
                VOID_ROM_QUIRKS="sony_disable_aldn sony_disable_stamina"
                ;;
        esac
    fi

    # ----------------- MOTOROLA -----------------
    if [ "$VOID_ROM_VENDOR" = "aosp" ]; then
        case "$brand" in
            *motorola*|*moto*|*lenovo*)
                if [ "$brand" = "motorola" ] || pm list packages 2>/dev/null | grep -qE "com\.moto\."; then
                    VOID_ROM_VENDOR="motorola"
                    VOID_ROM_FAMILY="aosp"
                    VOID_ROM_ANDROID_FORK="stock"
                    VOID_ROM_QUIRKS="moto_disable_motodisplay"
                fi
                ;;
        esac
    fi

    # ----------------- NOKIA / HMD -----------------
    if [ "$VOID_ROM_VENDOR" = "aosp" ]; then
        case "$brand" in
            *nokia*|*hmd*|*fih*)
                VOID_ROM_VENDOR="nokia"
                VOID_ROM_FAMILY="aosp"
                VOID_ROM_ANDROID_FORK="stock"
                # Nokia's Android One builds are very clean
                VOID_ROM_QUIRKS=""
                ;;
        esac
    fi

    # ----------------- ASUS / ROG -----------------
    if [ "$VOID_ROM_VENDOR" = "aosp" ]; then
        case "$brand" in
            *asus*)
                VOID_ROM_VENDOR="asus"
                VOID_ROM_FAMILY="zenui"
                VOID_ROM_ANDROID_FORK="zenui"
                VOID_ROM_QUIRKS="asus_disable_power_master asus_disable_game_genie"
                ;;
        esac
    fi

    # ----------------- NOTHING -----------------
    if [ "$VOID_ROM_VENDOR" = "aosp" ]; then
        case "$brand" in
            *nothing*)
                VOID_ROM_VENDOR="nothing"
                VOID_ROM_FAMILY="aosp"
                VOID_ROM_ANDROID_FORK="nothingos"
                # NothingOS is mostly AOSP, no quirks needed
                VOID_ROM_QUIRKS=""
                ;;
        esac
    fi

    # ----------------- LENOVO / TAB -----------------
    if [ "$VOID_ROM_VENDOR" = "aosp" ]; then
        case "$brand" in
            *lenovo*)
                if [ "$model" = *"tb-"* ] || [ "$manuf" = "lenovo" ]; then
                    VOID_ROM_VENDOR="lenovo"
                    VOID_ROM_FAMILY="zui"
                    VOID_ROM_ANDROID_FORK="zui"
                    VOID_ROM_QUIRKS="lenovo_disable_auto_launch"
                fi
                ;;
        esac
    fi

    # ----------------- HTC -----------------
    if [ "$VOID_ROM_VENDOR" = "aosp" ]; then
        case "$brand" in
            *htc*)
                VOID_ROM_VENDOR="htc"
                VOID_ROM_FAMILY="sense"
                VOID_ROM_ANDROID_FORK="stock"
                VOID_ROM_QUIRKS="htc_disable_sense_companion"
                ;;
        esac
    fi

    # ----------------- GMS variant detection (always run) -----------------
    # microG replaces GMS - we should be less aggressive
    if pm list packages 2>/dev/null | grep -qE "org\.microg\.gms|com\.google\.android\.gms\.microg"; then
        VOID_ROM_GMS_VARIANT="microg"
    elif ! pm list packages 2>/dev/null | grep -qE "^package:com\.google\.android\.gms$"; then
        # No GMS at all
        if [ "$VOID_ROM_VENDOR" != "huawei" ]; then
            VOID_ROM_GMS_VARIANT="none"
        fi
    fi

    # Honor (post-split) reports as hihonor / HONOR
    if [ -z "$VOID_ROM_QUIRKS" ] && [ "$VOID_ROM_VENDOR" = "unknown" ]; then
        # Last-ditch: try to classify by fingerprint
        case "$fp" in
            *samsung*) VOID_ROM_VENDOR="samsung"; VOID_ROM_FAMILY="oneui"; VOID_ROM_ANDROID_FORK="oneui" ;;
            *xiaomi*)  VOID_ROM_VENDOR="xiaomi";  VOID_ROM_FAMILY="miui";  VOID_ROM_ANDROID_FORK="miui" ;;
            *oppo*|*realme*) VOID_ROM_VENDOR="oppo"; VOID_ROM_FAMILY="coloros"; VOID_ROM_ANDROID_FORK="coloros" ;;
            *huawei*|*honor*) VOID_ROM_VENDOR="huawei"; VOID_ROM_FAMILY="emui"; VOID_ROM_ANDROID_FORK="emui" ;;
            *) VOID_ROM_VENDOR="aosp"; VOID_ROM_FAMILY="aosp"; VOID_ROM_ANDROID_FORK="stock" ;;
        esac
    fi

    export VOID_ROM_VENDOR VOID_ROM_FAMILY VOID_ROM_ANDROID_FORK
    export VOID_ROM_QUIRKS VOID_ROM_GMS_VARIANT
    export VOID_ROM_BATTERY_GATE VOID_ROM_BUCKET_RESET VOID_ROM_NEEDS_REBOOT

    VOID_ROM_DETECTED=1
    vlog INFO "ROM: vendor=$VOID_ROM_VENDOR family=$VOID_ROM_FAMILY fork=$VOID_ROM_ANDROID_FORK gms=$VOID_ROM_GMS_VARIANT quirks='$VOID_ROM_QUIRKS'"
}

# ----------------------------------------------------------------
# ROM quirk appliers
# ----------------------------------------------------------------
# Each quirk is a small named function. void_rom_quirk_init() runs
# the pre-apply quirks, void_rom_quirk_post_apply() runs the post
# ones. They are independent and order-independent except where
# noted.
# ----------------------------------------------------------------

# --- Generic helpers used by quirks ---

# Adjust throttle mode from "deny" to "ignore" for ROMs that need it
void_rom_use_ignore_only() {
    # We just log here; the throttle functions already use "ignore"
    # in FULL mode. This quirk is informational - the v2.5 module
    # already does the right thing.
    vlog INFO "quirk: use_ignore_only active (deny -> ignore for all appops)"
}

# Run a command, log result, never fail
_qrun() {
    "$@" 2>/dev/null
    vlog INFO "quirk: $*"
}

# ============== SAMSUNG / ONEUI ==============
void_quirk_samsung_early_reboot() {
    # OneUI applies appops only on next service start for some apps.
    # We force-stop the app to make appops take effect immediately.
    local pkg
    for pkg in com.google.android.gms com.samsung.android.bixby.agent com.samsung.android.bixby.wakeup; do
        package_exists "$pkg" && _qrun am force-stop "$pkg"
    done
    vlog INFO "quirk[samsung]: force-stopped GMS/Bixby to flush appops"
}

void_quirk_samsung_never_disable_system() {
    # Samsung packages we should NEVER pm-disable (would brick features)
    local samsung_safe="com.samsung.android.app.spage com.samsung.android.bixby.agent com.samsung.android.bixby.wakeup com.samsung.android.dialer com.samsung.android.messaging com.samsung.android.app.clockpack com.samsung.android.smartmirroring"
    # The targets.conf can opt out; we just log for the user
    vlog INFO "quirk[samsung]: protecting samsung system apps from disable"
    # We don't actually mutate targets here; the per-target check
    # honours category. Just an informational hint.
}

void_quirk_samsung_doze_aggressive() {
    # OneUI doze can be more aggressive than AOSP
    _qrun settings put global device_idle_constants "inactive_to=600000,sensing_to=180000"
    vlog INFO "quirk[samsung]: tuned doze constants for OneUI"
}

# ============== XIAOMI / MIUI / HYPEROS ==============
void_quirk_xiaomi_use_ignore() {
    void_rom_use_ignore_only
    # MIUI PowerKeeper will UNDO "deny" - we already use ignore so we're good
}

void_quirk_xiaomi_double_apply() {
    # Apply throttles, then re-apply after 3s because MIUI reverts
    # some on the first pass.
    vlog INFO "quirk[xiaomi]: double-apply scheduled (3s after main pass)"
}

void_quirk_xiaomi_pause_miui_optimization() {
    # Pause MIUI's "background killer" so it doesn't immediately
    # unkill what we just throttled.
    _qrun cmd appops set com.miui.powerkeeper RUN_IN_BACKGROUND ignore
    _qrun cmd appops set com.miui.powerkeeper WAKE_LOCK ignore
    _qrun cmd appops set com.miui.powerkeeper START_FOREGROUND ignore
    # Also: Battery Saver / Security Center
    _qrun cmd appops set com.miui.securitycenter RUN_IN_BACKGROUND ignore
    _qrun cmd appops set com.miui.daemon RUN_IN_BACKGROUND ignore
    vlog INFO "quirk[xiaomi]: MIUI powerkeeper + securitycenter + daemon neutralised"
}

void_quirk_xiaomi_disable_analytics() {
    # Disable MIUI Analytics ("User Experience Program")
    _qrun settings put global mipush_activity_enabled 0 2>/dev/null
    _qrun settings put global mipush_event_tracing 0 2>/dev/null
    _qrun settings put global upload_log_to_miui 0 2>/dev/null
    _qrun settings put global user_experience_plan_enabled 0 2>/dev/null
    vlog INFO "quirk[xiaomi]: MIUI analytics disabled"
}

# ============== OPPO / COLOROS / ONEPLUS / REALME ==============
void_quirk_coloros_battery_rehash() {
    # ColorOS stores its own battery whitelist outside of appops.
    # The re-hash is triggered by writing to this file (ColorOS-specific
    # path may not exist on all devices - we fail soft).
    _qrun touch /data/coloros/battery/rehash 2>/dev/null
    _qrun touch /data/oppo/battery/rehash 2>/dev/null
    vlog INFO "quirk[coloros]: requested battery whitelist rehash"
}

void_quirk_coloros_ignore_only() {
    void_rom_use_ignore_only
}

void_quirk_coloros_disable_companion() {
    # Disable HeyTapCloud, HeytapSearch, etc
    local pkg
    for pkg in com.coloros.weather2 com.coloros.safecenter com.coloros.oppoguardelf com.heytap.cloud com.heytap.browser com.heytap.market com.heytap.music; do
        package_exists "$pkg" && _qrun cmd appops set "$pkg" RUN_IN_BACKGROUND ignore \
                                      && _qrun cmd appops set "$pkg" WAKE_LOCK ignore \
                                      && _qrun cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND ignore
    done
    vlog INFO "quirk[coloros]: HeyTap suite throttled"
}

void_quirk_coloros_quiet_mode() {
    # ColorOS has a per-app "quiet mode" that is separate from appops.
    # We can't easily toggle it without a ContentProvider, but we can
    # at least put the app in background-restricted state.
    vlog INFO "quirk[coloros]: quiet mode hint logged (no public API)"
}

# ============== VIVO / ORIGINOS ==============
void_quirk_vivo_disable_iqo() {
    # iQOO phones ship with iQOO service that fights throttling
    _qrun cmd appops set com.iqoo.powersaving RUN_IN_BACKGROUND ignore 2>/dev/null
    _qrun cmd appops set com.iqoo.engineermode RUN_IN_BACKGROUND ignore 2>/dev/null
    vlog INFO "quirk[vivo]: iQOO powersaving neutralised"
}

void_quirk_vivo_quiet_mode() {
    vlog INFO "quirk[vivo]: quiet mode hint logged"
}

void_quirk_vivo_battery_rehash() {
    _qrun touch /data/vivo/battery/rehash 2>/dev/null
    vlog INFO "quirk[vivo]: battery whitelist rehash requested"
}

# ============== HUAWEI / HONOR ==============
void_quirk_huawei_ignore_only() {
    void_rom_use_ignore_only
}

void_quirk_huawei_disable_powergenie() {
    # PowerGenie is the real culprit - it's a foreground service that
    # aggressively kills background apps
    _qrun pm disable-user --user 0 com.huawei.powergenie 2>/dev/null
    _qrun pm disable-user --user 0 com.huawei.systemmanager 2>/dev/null
    vlog INFO "quirk[huawei]: PowerGenie + SystemManager disabled"
}

void_quirk_huawei_no_appops_persist() {
    # EMUI on some builds drops appops after reboot. We schedule a
    # "persistence file" that the service.sh reads on each boot.
    mkdir -p /data/adb/modules/void_gms_optimizer/state 2>/dev/null
    {
        echo "needs_reapply_on_boot=1"
        echo "last_apply=$(date '+%Y-%m-%d %H:%M:%S')"
        echo "reason=huawei_emui_appops_drift"
    } > /data/adb/modules/void_gms_optimizer/state/emui-persist.txt 2>/dev/null
    vlog INFO "quirk[huawei]: persistence hint saved for boot-time re-apply"
}

# ============== HARMONYOS ==============
void_quirk_harmonyos_appops_compat() {
    # HarmonyOS uses a different appops schema. We log and degrade.
    vlog INFO "quirk[harmonyos]: appops mapping may differ; results best-effort"
}

void_quirk_harmonyos_no_gms_skip_all() {
    # If no GMS, our gms.ads/gms.measurement throttles are useless
    # (they were already not installed). The other Google apps
    # (Search, Maps) might or might not be installed. Caller checks
    # package_exists() so we just log.
    if [ "$VOID_ROM_GMS_VARIANT" = "none" ]; then
        vlog INFO "quirk[harmonyos]: no GMS detected, Google-app targets will be skipped at runtime"
    fi
}

# ============== SONY ==============
void_quirk_sony_disable_aldn() {
    # Sony's "Ambient Light Display Notification" (always-on display)
    # can keep CPU awake. We don't disable AOD (UX choice) but log.
    vlog INFO "quirk[sony]: AOD hint logged (UX decision left to user)"
}

void_quirk_sony_disable_stamina() {
    _qrun settings put global stamina_mode_enabled 0 2>/dev/null
    vlog INFO "quirk[sony]: Stamina mode disabled"
}

# ============== MOTOROLA ==============
void_quirk_moto_disable_motodisplay() {
    # Moto Display ambient notifications
    _qrun cmd appops set com.motorola.ambientdisplay RUN_IN_BACKGROUND ignore 2>/dev/null
    vlog INFO "quirk[moto]: Moto Display throttled"
}

# ============== ASUS / ZENUI ==============
void_quirk_asus_disable_power_master() {
    _qrun cmd appops set com.asus.powerwidget RUN_IN_BACKGROUND ignore 2>/dev/null
    _qrun pm disable-user --user 0 com.asus.weathertime 2>/dev/null
    vlog INFO "quirk[asus]: Power Master neutralised"
}

void_quirk_asus_disable_game_genie() {
    _qrun cmd appops set com.asus.gamecenter RUN_IN_BACKGROUND ignore 2>/dev/null
    vlog INFO "quirk[asus]: Game Genie throttled"
}

# ============== NOTHING ==============
# (no quirks - NothingOS is AOSP-clean)

# ============== LENOVO / ZUI ==============
void_quirk_lenovo_disable_auto_launch() {
    _qrun settings put global auto_launch_app_enabled 0 2>/dev/null
    vlog INFO "quirk[lenovo]: ZUI auto-launch disabled"
}

# ============== HTC ==============
void_quirk_htc_disable_sense_companion() {
    _qrun cmd appops set com.htc.sense.companion RUN_IN_BACKGROUND ignore 2>/dev/null
    _qrun cmd appops set com.htc.weather RUN_IN_BACKGROUND ignore 2>/dev/null
    vlog INFO "quirk[htc]: HTC Sense companion throttled"
}

# ----------------------------------------------------------------
# void_rom_quirk_init()  - run BEFORE the main throttle pass
# ----------------------------------------------------------------
void_rom_quirk_init() {
    [ "$VOID_ROM_DETECTED" = "1" ] || void_detect_rom

    local q
    for q in $VOID_ROM_QUIRKS; do
        case "$q" in
            samsung_never_disable_system)        void_quirk_samsung_never_disable_system ;;
            xiaomi_pause_miui_optimization)      void_quirk_xiaomi_pause_miui_optimization ;;
            xiaomi_disable_analytics)            void_quirk_xiaomi_disable_analytics ;;
            xiaomi_use_ignore)                   void_quirk_xiaomi_use_ignore ;;
            xiaomi_double_apply)                 void_quirk_xiaomi_double_apply ;;
            coloros_disable_companion)           void_quirk_coloros_disable_companion ;;
            coloros_ignore_only)                 void_quirk_coloros_ignore_only ;;
            coloros_quiet_mode)                  void_quirk_coloros_quiet_mode ;;
            vivo_disable_iqo)                    void_quirk_vivo_disable_iqo ;;
            vivo_quiet_mode)                     void_quirk_vivo_quiet_mode ;;
            huawei_ignore_only)                  void_quirk_huawei_ignore_only ;;
            huawei_disable_powergenie)           void_quirk_huawei_disable_powergenie ;;
            huawei_no_appops_persist)            void_quirk_huawei_no_appops_persist ;;
            harmonyos_appops_compat)             void_quirk_harmonyos_appops_compat ;;
            harmonyos_no_gms_skip_all)           void_quirk_harmonyos_no_gms_skip_all ;;
            sony_disable_aldn)                   void_quirk_sony_disable_aldn ;;
            sony_disable_stamina)                void_quirk_sony_disable_stamina ;;
            moto_disable_motodisplay)            void_quirk_moto_disable_motodisplay ;;
            asus_disable_power_master)           void_quirk_asus_disable_power_master ;;
            asus_disable_game_genie)             void_quirk_asus_disable_game_genie ;;
            lenovo_disable_auto_launch)          void_quirk_lenovo_disable_auto_launch ;;
            htc_disable_sense_companion)         void_quirk_htc_disable_sense_companion ;;
            # Quirks that are post-only or no-op go here
            samsung_doze_aggressive|samsung_early_reboot|coloros_battery_rehash|vivo_battery_rehash) ;;
            *) vlog INFO "quirk: unknown '$q' (no-op)" ;;
        esac
    done
}

# ----------------------------------------------------------------
# void_rom_quirk_post_apply() - run AFTER the main throttle pass
# ----------------------------------------------------------------
void_rom_quirk_post_apply() {
    [ "$VOID_ROM_DETECTED" = "1" ] || return 0

    local q
    for q in $VOID_ROM_QUIRKS; do
        case "$q" in
            samsung_early_reboot)        void_quirk_samsung_early_reboot ;;
            samsung_doze_aggressive)     void_quirk_samsung_doze_aggressive ;;
            coloros_battery_rehash)      void_quirk_coloros_battery_rehash ;;
            vivo_battery_rehash)         void_quirk_vivo_battery_rehash ;;
            xiaomi_double_apply)
                # Re-apply the appops of all THROTTLED packages after a short
                # delay to defeat MIUI's first-pass undo. We do it inline
                # (synchronous) so service.sh doesn't have to fork.
                sleep 3
                vlog INFO "quirk[xiaomi]: double-apply pass (post 3s delay)"
                if [ -f "$VOID_TARGETS_FILE" ]; then
                    local pkg
                    while IFS='|' read -r pkg cat lvl op notes; do
                        case "$pkg" in ""|\#*) continue ;; esac
                        pkg=$(echo "$pkg" | tr -d ' \t\r\n')
                        [ -z "$pkg" ] && continue
                        package_exists "$pkg" || continue
                        # Re-apply throttle (same op as before)
                        case "$op" in
                            THROTTLE|RESTRICT) void_throttle_package "$pkg" "FULL" ;;
                            TRIM)               void_throttle_package "$pkg" "LITE" ;;
                        esac
                    done < "$VOID_TARGETS_FILE"
                fi
                ;;
        esac
    done
}

# ----------------------------------------------------------------
# void_should_skip_now() - decide whether to apply at all
# Returns 0 = skip (with reason in VOID_SKIP_REASON)
# Returns 1 = apply
# Side-effect: may modify PROFILE_LEVEL (auto-degrade if conditions bad)
# ----------------------------------------------------------------
VOID_SKIP_REASON=""
void_should_skip_now() {
    VOID_SKIP_REASON=""

    # 1. Battery / power state
    local level status
    level=$(dumpsys battery 2>/dev/null | awk -F: '/level:/ {gsub(/[^0-9]/,"",$2); print $2; exit}')
    status=$(dumpsys battery 2>/dev/null | grep -oE 'AC powered|USB powered|Wireless powered|Discharging' | head -1)

    if [ "$status" != "Discharging" ] && [ -n "$status" ]; then
        VOID_SKIP_REASON="charging ($status)"
        return 0
    fi

    if [ -n "$level" ] && [ "$level" -ge 0 ] 2>/dev/null; then
        # Critical battery: skip and degrade future runs
        if [ "$level" -lt 10 ]; then
            VOID_SKIP_REASON="critical battery ($level%)"
            # Mark so service.sh knows to stay quiet until charging
            mkdir -p /data/adb/modules/void_gms_optimizer/state 2>/dev/null
            date '+%Y-%m-%d %H:%M:%S' > /data/adb/modules/void_gms_optimizer/state/last-skip-battery.txt 2>/dev/null
            return 0
        fi
        # Low battery: apply but degrade to lite
        if [ "$level" -lt 20 ]; then
            vlog INFO "should_skip: low battery ($level%), auto-degrading to LITE"
            PROFILE_LEVEL=1
        fi
    fi

    # 2. Screen state
    local wake
    wake=$(dumpsys power 2>/dev/null | grep -oE 'mWakefulness=[A-Za-z]+' | head -1 | cut -d= -f2)
    case "$wake" in
        Asleep|Dream|"")
            # Good - user not actively using phone
            ;;
        Awake)
            # Phone is being used - degrade to LITE
            vlog INFO "should_skip: screen on, auto-degrading to LITE"
            PROFILE_LEVEL=1
            ;;
    esac

    # 3. Recent crash from our throttles
    if [ -f /data/adb/modules/void_gms_optimizer/state/last-self-heal.txt ]; then
        local heal_age_s
        heal_age_s=$(( $(date +%s) - $(date -d "$(cat /data/adb/modules/void_gms_optimizer/state/last-self-heal.txt 2>/dev/null | head -1)" +%s 2>/dev/null || echo 0) ))
        # If we self-healed in the last 30 min, stay in degraded mode
        if [ "$heal_age_s" -lt 1800 ] 2>/dev/null; then
            vlog INFO "should_skip: recent self-heal (${heal_age_s}s ago), staying LITE"
            PROFILE_LEVEL=1
        fi
    fi

    return 1
}

# ----------------------------------------------------------------
# void_self_heal() - detect & repair breakage from previous apply
# ----------------------------------------------------------------
# We check for 3 failure modes that the v2.x module couldn't see:
#   1. FCM broken (Google Play Services or push recipients failing)
#   2. App crashloop caused by our throttle
#   3. Settings put global that we did, reverted by the OS or vendor
# We repair what we can and log everything.
# ----------------------------------------------------------------
VOID_SELF_HEAL_LOG="$VOID_STATS_DIR/self-heal.log"

void_self_heal() {
    mkdir -p /data/adb/modules/void_gms_optimizer/state 2>/dev/null
    local healed=0
    local out=""

    # === 1. FCM health check ===
    # We can't directly time FCM delivery without an active push
    # server, but we CAN check that GMS itself is responding and
    # that no push-related appops are unexpectedly "deny".
    if [ "$VOID_ROM_GMS_VARIANT" = "vanilla" ] && package_exists com.google.android.gms; then
        local gms_bg gms_rab gms_wl
        gms_bg=$(cmd appops get com.google.android.gms RUN_IN_BACKGROUND 2>/dev/null | awk '{print $NF}')
        gms_rab=$(cmd appops get com.google.android.gms RUN_ANY_IN_BACKGROUND 2>/dev/null | awk '{print $NF}')
        gms_wl=$(cmd appops get com.google.android.gms WAKE_LOCK 2>/dev/null | awk '{print $NF}')

        # FCM requires RUN_ANY_IN_BACKGROUND = allow OR ignore
        # If we somehow set it to deny (PROTECT_FCM should have caught this
        # but the user might have toggled it off), FCM dies.
        if [ "$gms_rab" = "deny" ]; then
            out="${out}[HEAL] FCM at risk: GMS RUN_ANY_IN_BACKGROUND=deny. Fixing to ignore.\n"
            cmd appops set com.google.android.gms RUN_ANY_IN_BACKGROUND ignore 2>/dev/null
            healed=$((healed + 1))
        fi
        # And RUN_IN_BACKGROUND=allow is the FCM-friendly setting
        if [ "$gms_bg" = "deny" ]; then
            out="${out}[HEAL] FCM at risk: GMS RUN_IN_BACKGROUND=deny. Fixing to ignore.\n"
            cmd appops set com.google.android.gms RUN_IN_BACKGROUND ignore 2>/dev/null
            healed=$((healed + 1))
        fi
    fi

    # === 2. Crash detection ===
    # dumpsys dropbox lists recent app crashes. If anything we
    # throttled crashed in the last re-apply window, we relax.
    if [ -f /data/adb/modules/void_gms_optimizer/state/last-apply.txt ]; then
        local last_apply
        last_apply=$(cat /data/adb/modules/void_gms_optimizer/state/last-apply.txt 2>/dev/null)
        # Quick scan: look for "data_app_crash" entries referencing GMS
        local crash
        crash=$(dumpsys dropbox --print 2>/dev/null | \
                awk '/data_app_crash/ && /com.google.android.gms/ {print}' | \
                tail -1)
        if [ -n "$crash" ]; then
            out="${out}[HEAL] Recent GMS crash detected. Relaxing GMS to FGM mode.\n"
            void_throttle_package com.google.android.gms FGM 2>/dev/null
            healed=$((healed + 1))
        fi
    fi

    # === 3. Settings drift ===
    # We restore the most-likely-to-drift settings
    if [ "$(settings get global adaptive_battery_management_enabled 2>/dev/null)" != "1" ]; then
        settings put global adaptive_battery_management_enabled 1 2>/dev/null
        out="${out}[HEAL] adaptive_battery drifted, restored\n"
        healed=$((healed + 1))
    fi

    if [ "$healed" -gt 0 ]; then
        printf '%s [%s] %b\n' "$(date '+%Y-%m-%d %H:%M:%S')" "self-heal" "$out" >> "$VOID_SELF_HEAL_LOG" 2>/dev/null
        date '+%Y-%m-%d %H:%M:%S' > /data/adb/modules/void_gms_optimizer/state/last-self-heal.txt 2>/dev/null
        vlog INFO "self-heal: $healed issue(s) fixed"
    fi
    return 0
}

# ----------------------------------------------------------------
# void_learn_intensity() - per-pkg intensity learning
# ----------------------------------------------------------------
# We use the last 3 metric snapshots to decide whether to:
#   - intensify  (pkg is still active despite throttle)
#   - hold       (throttle is working, keep current level)
#   - relax      (pkg is dead, lighten the throttle)
# Decision is stored in /data/adb/modules/.../state/intensity-<pkg>.txt
# and consumed on next apply via VOID_PKG_INTENSITY_<pkg> env.
# ----------------------------------------------------------------
VOID_INTENSITY_DIR="/data/adb/modules/void_gms_optimizer/state/intensity"
void_learn_intensity() {
    [ -f "$VOID_TARGETS_FILE" ] || return 0
    mkdir -p "$VOID_INTENSITY_DIR" 2>/dev/null

    local pkg
    while IFS='|' read -r pkg cat lvl op notes; do
        case "$pkg" in ""|\#*) continue ;; esac
        pkg=$(echo "$pkg" | tr -d ' \t\r\n')
        [ -z "$pkg" ] && continue
        package_exists "$pkg" || continue

        local procs
        procs=$(pidof "$pkg" 2>/dev/null | wc -l)
        local state_file="$VOID_INTENSITY_DIR/${pkg}.txt"
        local current="hold"
        [ -f "$state_file" ] && current=$(cat "$state_file" 2>/dev/null)

        # Heuristic: if package is alive (procs > 0) more than 50% of
        # the time, intensify. If it's been dead for 3+ runs, relax.
        case "$current" in
            hold)
                [ "$procs" -gt 0 ] && current="hold_alive" || current="hold_dead"
                ;;
            hold_alive)
                if [ "$procs" -eq 0 ]; then
                    current="relax"
                fi
                ;;
            hold_dead)
                if [ "$procs" -gt 0 ]; then
                    current="intensify"
                fi
                ;;
            intensify)
                # Don't go further; we can't RESTRICT what we already disabled
                :
                ;;
            relax)
                if [ "$procs" -gt 0 ]; then
                    current="hold"
                fi
                ;;
        esac

        echo "$current" > "$state_file" 2>/dev/null
    done < "$VOID_TARGETS_FILE"

    vlog INFO "learn_intensity: updated per-pkg intensity states"
}

# Returns 0 (intensify) / 1 (hold) / 2 (relax) for a given pkg
void_pkg_intensity() {
    local pkg="$1"
    local f="$VOID_INTENSITY_DIR/${pkg}.txt"
    local s="hold"
    [ -f "$f" ] && s=$(cat "$f" 2>/dev/null)
    case "$s" in
        intensify) return 0 ;;
        relax)     return 2 ;;
        *)         return 1 ;;
    esac
}
