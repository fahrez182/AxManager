#!/system/bin/sh

until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 2
done

sleep 5
chmod 0644 data/adb/modules/COPG/COPG.json
chcon u:object_r:system_file:s0 /data/adb/modules/COPG/COPG.json

# Boot-crash guard reset for the system_server-resident hooks (FLAG_SECURE / VPN-hide / per-app
# locale). We only get here AFTER sys.boot_completed, i.e. system_server booted fine this cycle, so
# clear the crash counters. If one of those hooks had bootlooped, boot_completed never fires, this
# never runs, the counters keep climbing, and spoof_module stops arming that hook after 3 crash-boots
# → the device recovers on its own. (FLAG_SECURE also self-clears from its own hook; harmless dup.)
for g in copg_fs_bootguard copg_vpn_bootguard copg_lang_bootguard copg_wallpaper_bootguard copg_account_bootguard copg_ssaid_bootguard copg_playsrc_bootguard copg_tensorfeat_bootguard copg_corepatch_bootguard copg_hidedev_bootguard; do
    echo 0 > "/data/system/$g" 2>/dev/null
    chmod 0644 "/data/system/$g" 2>/dev/null
done

# licensing: the GPU license-verify verdict is memoized IN THE COMPANION PROCESS (in memory),
# not on disk — a disk cache in /data/adb is forgeable by root and would unlock GPU with no
# license. So there is nothing to warm here; the first gpu-app launch per boot pays the verify,
# the rest are instant. Remove any stale disk cache left by older builds.
rm -f /data/adb/modules/COPG/.lic_cache 2>/dev/null

# per-app proxy daemon (M2): watches proxy/active.conf and applies kernel TPROXY routing for
# proxy-tagged apps (routing each through its remote SOCKS5 via a local hev-socks5-tproxy helper).
# No-ops when active.conf is absent/empty. PRO — the WebUI only writes active.conf on a valid
# license. Detached so it outlives this script; teardown-first on each (re)start keeps it clean.
MODDIR=/data/adb/modules/COPG
if [ -x "$MODDIR/proxy/copg_proxy.sh" ]; then
    nohup sh "$MODDIR/proxy/copg_proxy.sh" watch >/dev/null 2>&1 &
fi

# Concurrent Mic (mic-share): map-inject libmicshare.so into audioserver so two apps can use the mic
# at once (e.g. a game's voice chat + a screen recorder). The injector maps the payload straight into
# audioserver from the module dir (no dlopen, no /system, systemless); the payload hooks
# AudioPolicyService::updateUidStates_l and skips the silencing recompute while enabled. The hook is
# ALWAYS injected (passthrough when off) and reads /data/system/copg_micshare live → the WebUI toggle
# applies with no reboot. A watchdog re-injects if audioserver restarts (crash / HAL reload). The
# execmem/execmod rules the inline hook needs ship in the module's sepolicy.rule.
if [ -x "$MODDIR/micshare/mapinject" ] && [ -f "$MODDIR/micshare/libmicshare.so" ]; then
    # seed the live toggle from persisted intent (WebUI writes both global.json + the live file)
    if grep -q '"micshare"[[:space:]]*:[[:space:]]*true' "$MODDIR/global.json" 2>/dev/null; then
        echo 1 > /data/system/copg_micshare
    else
        echo 0 > /data/system/copg_micshare
    fi
    chmod 0644 /data/system/copg_micshare 2>/dev/null
    # Inject ONLY when the toggle is on (safe-by-default: an off/unaware user never gets the injection,
    # so a ROM the manual loader is unstable on can't crash their audio), ONCE per audioserver pid (the
    # payload is anonymous in maps — track the pid, not a name), and with a CRASH-GUARD: if audioserver
    # keeps restarting <15s after our inject (a crash-loop, e.g. the split-lib OnePlus report), stop
    # after MC_MAX so audio recovers on its own. Enabling the toggle re-arms within one loop (no reboot).
    (
        MC_LAST=""; MC_T=0; MC_CRASH=0; MC_MAX=3
        while true; do
            EN=$(cat /data/system/copg_micshare 2>/dev/null)
            if [ "$EN" = "1" ] && [ "$MC_CRASH" -lt "$MC_MAX" ]; then
                P=$(pidof audioserver | tr ' ' '\n' | head -1)
                if [ -n "$P" ] && [ "$P" != "$MC_LAST" ]; then
                    NOW=$(date +%s 2>/dev/null || echo 0)
                    [ -n "$MC_LAST" ] && [ "$((NOW - MC_T))" -lt 15 ] && MC_CRASH=$((MC_CRASH + 1))
                    if [ "$MC_CRASH" -lt "$MC_MAX" ] && "$MODDIR/micshare/mapinject" "$P" "$MODDIR/micshare/libmicshare.so" >/dev/null 2>&1; then
                        MC_LAST="$P"; MC_T="$NOW"
                    fi
                fi
            fi
            sleep 5
        done
    ) &
fi

# periodic maintenance tick
if [ -x "$MODDIR/copg_lic" ]; then
    ( while true; do "$MODDIR/copg_lic" tick >/dev/null 2>&1; sleep 21600; done ) &
fi

exec /data/adb/modules/COPG/controller >/dev/null 2>&1 &
