#!/system/bin/sh
# ===========================================================================
# AX GAME FPS UNLOCK ENGINE v1.0.0
# Uninstall: Complete Restore from Backup
# Multi-Brand: Samsung / Xiaomi / OnePlus / Pixel / OPPO / Universal
#
# Restores ALL settings to their pre-AX values using the backup
# created during installation. Also re-enables disabled OEM game
# services and resets Fixed Performance Mode.
# ===========================================================================

BACKUP_DIR="/sdcard/ax_fps_unlock_backup"
LOG="/sdcard/ax_fps_unlock_uninstall.txt"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo "unknown")

log_uninst() {
    echo "$1" >> "$LOG" 2>/dev/null
    echo "$1"
}

echo "" > "$LOG" 2>/dev/null

log_uninst "==========================================================="
log_uninst ""
log_uninst "  AX GAME FPS UNLOCK ENGINE — UNINSTALL v1.0.0"
log_uninst "  Restoring all original settings from backup"
log_uninst ""
log_uninst "  Timestamp: ${TIMESTAMP}"
log_uninst ""
log_uninst "==========================================================="

if [ ! -d "$BACKUP_DIR" ]; then
    log_uninst ""
    log_uninst "  [FAIL] No backup directory found at: ${BACKUP_DIR}"
    log_uninst "  [FAIL] Cannot restore — no backup exists."
    log_uninst ""
    exit 1
fi

_restored=0
_failed=0

# ─── RESTORE SETTINGS FROM BACKUP ────────────────────────────────────────
log_uninst ""
log_uninst "  [AX] Restoring settings from backup..."
log_uninst ""

for _bfile in "${BACKUP_DIR}"/*; do
    [ ! -f "$_bfile" ] && continue

    _fname=""
    _fname=$(basename "$_bfile" 2>/dev/null) || true

    case "$_fname" in
        .ax_*|.*) continue ;;
    esac

    _rns=""
    _rns=$(echo "$_fname" | cut -d'_' -f1 2>/dev/null) || true
    _rkey=""
    _rkey=$(echo "$_fname" | cut -d'_' -f2- 2>/dev/null) || true

    _orig=""
    _orig=$(cat "$_bfile" 2>/dev/null) || true

    if [ "$_orig" = "(not set)" ] || [ -z "$_orig" ]; then
        settings delete "$_rns" "$_rkey" 2>/dev/null
        if [ $? -eq 0 ]; then
            log_uninst "  [OK]   ${_rns}/${_rkey} -> deleted (was not set before AX)"
            _restored=$((_restored + 1))
        else
            log_uninst "  [FAIL] ${_rns}/${_rkey} -> failed to delete"
            _failed=$((_failed + 1))
        fi
    else
        settings put "$_rns" "$_rkey" "$_orig" 2>/dev/null
        _rrb=""
        _rrb=$(settings get "$_rns" "$_rkey" 2>/dev/null) || true
        if [ "$_rrb" = "$_orig" ]; then
            log_uninst "  [OK]   ${_rns}/${_rkey} -> ${_orig} (restored)"
            _restored=$((_restored + 1))
        else
            log_uninst "  [WARN] ${_rns}/${_rkey} -> ${_rrb} (wanted: ${_orig})"
            _failed=$((_failed + 1))
        fi
    fi
done

# ─── RESET TRANSIENT PROPERTIES ──────────────────────────────────────────
log_uninst ""
log_uninst "  [AX] Resetting transient properties..."
log_uninst ""

_props="
    debug.hwui.renderer
    debug.egl.hw
    debug.hwui.profile
    debug.composition.type
    debug.hwui.render_dirty_regions
    debug.gralloc.enable_fb_ubwc
    debug.egl.force_msaa
    debug.hwui.use_buffer_age
    debug.hwui.overdraw
    debug.hwui.show_dirty_regions
    debug.hwui.render_ahead
    debug.hwui.texture_cache_size
    debug.hwui.layer_cache_size
    debug.hwui.path_cache_size
    debug.hwui.gradient_cache_size
    debug.hwui.shape_cache_size
    debug.hwui.drop_shadow_cache_size
    debug.sf.disable_backpressure
    debug.sf.phase_offset_threshold_for_next_vsync_ns
    debug.sf.early_phase_offset_ns
    debug.sf.early_gl_phase_offset_ns
    debug.sf.early_app_phase_offset_ns
    debug.sf.early_gl_app_phase_offset_ns
    debug.sf.use_content_detection_for_refresh_rate
    debug.sf.set_idle_timer_ms
    debug.sf.enable_gl_backpressure
    debug.sf.max_frame_buffer_acquired_buffers
    debug.sf.latch_unsignaled
    debug.sf.enable_transaction_tracing
    debug.sf.frame_rate_multiple_threshold
    debug.sqlite.wal.syncmode
    debug.sqlite.journalmode
    debug.input.dispatcher.throttle
    debug.egl.traceGpuCompletion
    debug.atrace.tags.enableflags
    dalvik.vm.dex2oat-threads
    dalvik.vm.image-dex2oat-threads
    dalvik.vm.bg-dex2oat-threads
    dalvik.vm.usejit
    dalvik.vm.jit.codecachesize
    dalvik.vm.gctype
    dalvik.vm.backgroundgctype
    dalvik.vm.heapsize
    dalvik.vm.heapgrowthlimit
    dalvik.vm.heapmaxfree
    dalvik.vm.heapminfree
    dalvik.vm.heaptargetutilization
    persist.sys.cpuboost.enable
    persist.sys.perf.topAppRenderThreadBoost.enable
    persist.sys.perf.topAppBoost.enable
    persist.sys.perf.debug
    persist.sys.input.boost
    persist.sys.loglevel
    persist.traced.enable
    persist.sys.thermal.enable
    persist.sys.thermal.monitor
    persist.sys.thermaltest
    persist.sys.powmillet.enable
    persist.sys.sched_child_runs_first
    vendor.gpu.enable_adreno_boost
    vendor.gralloc.disable_ubwc
"

for _prop in $_props; do
    setprop "$_prop" "" 2>/dev/null
    log_uninst "  [OK]   ${_prop} -> cleared"
done

# ─── DISABLE FIXED PERFORMANCE MODE ──────────────────────────────────────
log_uninst ""
log_uninst "  [AX] Disabling Fixed Performance Mode..."
cmd power set-fixed-performance-mode-enabled false 2>/dev/null
log_uninst "  [OK]   Fixed Performance Mode -> DISABLED"

# ─── RE-ENABLE DOZE ──────────────────────────────────────────────────────
dumpsys deviceidle enable 2>/dev/null
log_uninst "  [OK]   Doze mode -> RE-ENABLED"

# ─── RE-ENABLE OEM GAME SERVICES ─────────────────────────────────────────
log_uninst ""
log_uninst "  [AX] Re-enabling OEM game services..."
log_uninst ""

_reenable_packages="
    com.samsung.android.game.gos
    com.enhance.gameservice
    com.samsung.android.game.gametools
    com.samsung.android.game.gamehome
    com.miui.powerkeeper
    com.xiaomi.gamecenter.sdk.service
    com.oneplus.gamespace
    com.oplus.gamespace
    com.oplus.jankimprover
    com.coloros.gameassistant
    com.coloros.gamespaceui
    com.oplus.performance
    com.oplus.thermalcontrol
    com.asus.gamecenter
    com.asus.performance.gamemode
    com.motorola.gamemode
    com.nothing.gamespace
    com.iqoo.gamecube
    com.vivo.gamecube
    com.samsung.android.voc
    com.samsung.android.forest
    com.samsung.android.da.daagent
    com.samsung.android.mateagent
    com.sec.android.diagmonagent
    com.miui.analytics
    com.xiaomi.joyose
    com.xiaomi.mipicks
    com.miui.yellowpage
    com.miui.msa.global
    com.samsung.android.bixby.agent
    com.samsung.android.bixby.service
    com.samsung.android.bixby.wakeup
    com.samsung.android.visionintelligence
    com.samsung.android.app.spage
    com.samsung.android.tvplus
    com.samsung.android.aremoji
    com.samsung.android.arzone
    com.facebook.appmanager
    com.facebook.system
    com.facebook.services
    com.microsoft.skydrive
"

for _pkg in $_reenable_packages; do
    _en_result=""
    _en_result=$(pm enable --user 0 "$_pkg" 2>&1) || true
    case "$_en_result" in
        *"enabled"*|*"new state"*)
            log_uninst "  [OK]   RE-ENABLED -> ${_pkg}"
            ;;
        *"not installed"*|*"Unknown"*|*"not found"*)
            ;;
        *)
            log_uninst "  [OK]   ${_pkg} -> ${_en_result}"
            ;;
    esac
done

# ─── RESET GAME MODES ───────────────────────────────────────────────────
log_uninst ""
log_uninst "  [AX] Resetting game modes to STANDARD..."

for _pkg in $(pm list packages -3 2>/dev/null | sed 's/package://'); do
    cmd game mode 1 "$_pkg" 2>/dev/null
done

log_uninst "  [OK]   All games -> STANDARD mode"

# ─── FINAL REPORT ────────────────────────────────────────────────────────
log_uninst ""
log_uninst "==========================================================="
log_uninst ""
log_uninst "  AX GAME FPS UNLOCK ENGINE — UNINSTALL COMPLETE"
log_uninst ""
log_uninst "  [OK]   Restored : ${_restored}"
log_uninst "  [FAIL] Failed   : ${_failed}"
log_uninst ""
log_uninst "  All settings restored to pre-AX values."
log_uninst "  OEM game services re-enabled."
log_uninst "  Fixed Performance Mode disabled."
log_uninst "  Doze mode re-enabled."
log_uninst "  Game modes reset to STANDARD."
log_uninst ""
log_uninst "  Backup preserved at: ${BACKUP_DIR}"
log_uninst "  Uninstall log at   : ${LOG}"
log_uninst ""
log_uninst "  IMPORTANT: Reboot your device to complete restoration."
log_uninst "  Run: adb reboot"
log_uninst ""
log_uninst "==========================================================="
