# ================================================
# COPG Module Installation Script
# ================================================

INSTALL_SUCCESS=true

# ── clean install UI ────────────────────────────────────────────────────────
# A single consistent visual language: a header rail, aligned "glyph label value"
# rows, thin dividers, one restrained glyph set (✓ success · step ! warn ✗ fail).
BAR="  ──────────────────────────────"
hr()   { ui_print "$BAR"; }
sp()   { ui_print " "; }
hdr()  { ui_print "  ▌  $1"; }
kv()   { ui_print "$(printf '  %s  %-10s %s' "$1" "$2" "$3")"; }   # glyph, label, value
ok()   { ui_print "  ✓  $1"; }
step() { ui_print "  ›  $1"; }
note() { ui_print "  ·  $1"; }
warn() { ui_print "  !  $1"; }
bad()  { ui_print "  ✗  $1"; }
sub()  { ui_print "     $1"; }                                     # indented detail line

print_failure_and_exit() {
  sp
  bad "Installation aborted"
  hr
  exit 1
}

grep_prop() {
  local PROP_FILE="$1"
  local PROP_NAME="$2"
  if [ -f "$PROP_FILE" ]; then
    grep "^${PROP_NAME}=" "$PROP_FILE" | cut -d'=' -f2- | head -n 1
  else
    echo ""
  fi
}

print_header() {
  local v c meta=""
  if [ -f "$MODPATH/module.prop" ]; then
    v=$(grep_prop "$MODPATH/module.prop" "version")
    c=$(grep_prop "$MODPATH/module.prop" "versionCode")
    [ -n "$v" ] && meta="$v" && [ -n "$c" ] && meta="$v ($c)"
  fi
  sp
  hdr "C O P G"
  if [ -n "$meta" ]; then
    hdr "Device & App Spoofer   ·   $meta"
  else
    hdr "Device & App Spoofer"
  fi
  hr
}

check_zygisk() {
  ZYGISK_NEXT_PATH="/data/adb/modules/zygisksu"
  REZYGISK_PATH="/data/adb/modules/rezygisk"

  DETECTED_ROOT_SOLUTIONS=""
  ROOT_SOLUTION_COUNT=0

  if command -v apd >/dev/null; then
    DETECTED_ROOT_SOLUTIONS="$DETECTED_ROOT_SOLUTIONS APatch"
    ROOT_SOLUTION_COUNT=$((ROOT_SOLUTION_COUNT + 1))
    ROOT_SOLUTION="APatch"
    MANAGER_NAME="APatch Manager"
  fi

  if command -v ksud >/dev/null; then
    DETECTED_ROOT_SOLUTIONS="$DETECTED_ROOT_SOLUTIONS KernelSU"
    ROOT_SOLUTION_COUNT=$((ROOT_SOLUTION_COUNT + 1))
    if [ $ROOT_SOLUTION_COUNT -eq 1 ]; then
      ROOT_SOLUTION="KernelSU"
      MANAGER_NAME="KernelSU Manager"
    fi
  fi

  if command -v magisk >/dev/null; then
    DETECTED_ROOT_SOLUTIONS="$DETECTED_ROOT_SOLUTIONS Magisk"
    ROOT_SOLUTION_COUNT=$((ROOT_SOLUTION_COUNT + 1))
    if [ $ROOT_SOLUTION_COUNT -eq 1 ]; then
      ROOT_SOLUTION="Magisk"
      MANAGER_NAME="Magisk Manager"
    fi
  fi

  if [ $ROOT_SOLUTION_COUNT -gt 1 ]; then
    bad "Multiple root solutions found:$DETECTED_ROOT_SOLUTIONS"
    sub "Keep only one installed."
    print_failure_and_exit "zygisk"
  elif [ $ROOT_SOLUTION_COUNT -eq 0 ]; then
    bad "No supported root solution"
    sub "Magisk 26.4+ · KernelSU 0.7.0+ · APatch 1.0.7+"
    print_failure_and_exit "zygisk"
  else
    kv "✓" "Root" "$ROOT_SOLUTION"
  fi

  # ── Per-variant ACTIVE state ──────────────────────────────────────────────
  # A variant counts as active only if its module dir exists AND has no
  # `disable` marker. Each is checked independently, so a disabled variant can
  # never mask another that IS active (e.g. Zygisk Next disabled + ReZygisk on
  # → ReZygisk still wins). `disable` on one says nothing about the other.
  ZN_ACTIVE=false; RZ_ACTIVE=false
  [ -d "$ZYGISK_NEXT_PATH" ] && [ ! -f "$ZYGISK_NEXT_PATH/disable" ] && ZN_ACTIVE=true
  [ -d "$REZYGISK_PATH" ]    && [ ! -f "$REZYGISK_PATH/disable" ]    && RZ_ACTIVE=true
  ZYGISK_INSTALLED=false
  { [ -d "$ZYGISK_NEXT_PATH" ] || [ -d "$REZYGISK_PATH" ]; } && ZYGISK_INSTALLED=true

  # ── Magisk: built-in Zygisk must be OFF ───────────────────────────────────
  # ReZygisk / Zygisk Next replace Magisk's native Zygisk and only load when
  # the built-in one is disabled. If native Zygisk is ON they silently fail to
  # work even though the module dir is present — so block install and tell the
  # user to turn it off (instead of falsely reporting the variant "Active").
  if [ "$ROOT_SOLUTION" = "Magisk" ]; then
    ZYGISK_STATUS=$(magisk --sqlite "SELECT value FROM settings WHERE key='zygisk';" 2>/dev/null)
    if [ "$ZYGISK_STATUS" = "value=1" ]; then
      bad "Magisk built-in Zygisk is enabled"
      sub "Turn it off in Magisk settings (ReZygisk / Zygisk Next need it off)."
      print_failure_and_exit "zygisk"
    fi
  fi

  # ── Require an ACTIVE standalone Zygisk variant ───────────────────────────
  if $RZ_ACTIVE; then
    kv "✓" "Zygisk" "ReZygisk · active"
    return
  elif $ZN_ACTIVE; then
    kv "✓" "Zygisk" "Zygisk Next · active"
    return
  elif $ZYGISK_INSTALLED; then
    bad "Zygisk module is disabled"
    sub "Enable ReZygisk / Zygisk Next in $MANAGER_NAME."
    print_failure_and_exit "zygisk"
  else
    bad "No Zygisk module found"
    sub "Install ReZygisk or Zygisk Next."
    print_failure_and_exit "zygisk"
  fi
}

cleanup_unused_architectures() {
  # Drop zygisk .so's for ABIs this device isn't. The build ships arm64-v8a + x86_64 only
  # (arm32 dropped); on a 64-bit device Zygisk's loader picks per-process. Drop the non-x86_64
  # libs on x86_64 devices, the x86_64 lib on arm64 devices (COPG_ABI set by the probe above).
  [ "$COPG_ABI" = "x86_64" ] || rm -f "$MODPATH/zygisk/x86_64.so" "$MODPATH/zygisk/x86.so" 2>/dev/null
  rm -f "$MODPATH/controller_arm64" "$MODPATH/controller_x86_64" 2>/dev/null
}

# Ask (volume keys, 10s) whether to CARRY the device-wide / system-server spoof state (global.json)
# from the previously installed version. These hooks are the only bootloop source, so the SAFE
# default (timeout, VOL-, or a device with broken volume keys) is to NOT carry → the new install
# starts from the shipped default (system-server master OFF). VOL+ carries the old global.json so
# the spoofs stay ON and arm on the first reboot (no second reboot). The per-app config
# (COPG.json/list.json) is not part of this prompt — it is always carried over on an update (see the
# user-config block below); only the global switch is a per-user choice. A boot-crash guard still disables the layer if
# a boot fails, so carrying is never a one-way bootloop. Mirrors the v5.1 getevent prompt.
CARRY_GLOBAL=false
prompt_global_carry() {
  CARRY_GLOBAL=false
  hdr "Keep device-wide spoofs? (update)"
  note "VOL+ keep · VOL- off (safe) · 10s → off"
  warn "Keeping them can bootloop — off if unsure."
  local T=10 start now el EVENT
  start=$(date +%s)
  while true; do
    now=$(date +%s); el=$((now - start))
    [ "$el" -ge "$T" ] && { CARRY_GLOBAL=false; return; }
    if command -v timeout >/dev/null 2>&1; then
      EVENT=$(timeout 0.3 getevent -lc1 2>/dev/null | tr -d '\r')
    else
      EVENT=$(getevent -lc1 2>/dev/null | tr -d '\r')
    fi
    echo "$EVENT" | grep -q "KEY_VOLUMEUP.*DOWN"   && { CARRY_GLOBAL=true;  return; }
    echo "$EVENT" | grep -q "KEY_VOLUMEDOWN.*DOWN" && { CARRY_GLOBAL=false; return; }
    sleep 0.1
  done
}

print_header

if ! $BOOTMODE; then
  bad "Recovery flashing not supported"
  sub "Install from the Magisk / KernelSU / APatch app."
  print_failure_and_exit "initial"
fi

if [ "$API" -lt 26 ]; then
  bad "Android version too old"
  sub "Requires Android 9.0+ (this device: API $API)."
  print_failure_and_exit "initial"
fi

if $INSTALL_SUCCESS; then
  check_zygisk || {
    INSTALL_SUCCESS=false
  }
fi

if $INSTALL_SUCCESS; then
  kv "✓" "Android" "$(getprop ro.build.version.release) · SDK $API"

  # Controller ABI selection by RUNTIME PROBE, not getprop ABI guessing. Some Android
  # 12+/16 devices leave ro.product.cpu.abilist EMPTY or only partition-scoped, so parsing
  # it misselected or aborted the install. Instead, just RUN each candidate: it exits 0 at
  # once when given an arg (see unified_controller.cpp main), while a wrong-arch ELF cannot
  # exec (ENOEXEC → non-zero). The first that runs IS the device ABI. The .so files stay
  # multi-ABI (Zygisk's loader picks per-process) — only the controller binary is singular.
  if $INSTALL_SUCCESS; then
    CONTROLLER_INSTALLED=false
    # The probe decides the device ABI ONCE. Every other per-ABI binary (copg_lic, micshare)
    # is the same ABI from the same toolchain, so it reuses this verdict — no second probe.
    COPG_ABI=arm64

    # x86_64 FIRST: on an x86_64 emulator with arm translation the arm64 ELF also execs
    # (translated), so probe native x86_64 before arm to avoid selecting a translated controller.
    for CAND in x86_64:x86_64 arm64:arm64-v8a; do
      SRC="$MODPATH/controller_${CAND%%:*}"
      [ -f "$SRC" ] || continue
      chmod 0755 "$SRC" 2>/dev/null
      if "$SRC" --probe >/dev/null 2>&1; then
        mv "$SRC" "$MODPATH/controller" && chmod 0755 "$MODPATH/controller" && {
          kv "✓" "Controller" "${CAND##*:}"
          COPG_ABI="${CAND%%:*}"
          CONTROLLER_INSTALLED=true
          break
        }
      fi
    done

    # Fallback: nothing probed runnable (exec blocked on a noexec mount, etc.) but the
    # arm64 binary is present. Every KernelSU/Zygisk host is ARM, so use it rather than
    # abort — aborting is what blocked users with an empty ro.product.cpu.abilist.
    if ! $CONTROLLER_INSTALLED && [ -f "$MODPATH/controller_arm64" ]; then
      mv "$MODPATH/controller_arm64" "$MODPATH/controller" && chmod 0755 "$MODPATH/controller" && {
        kv "✓" "Controller" "arm64-v8a (fallback)"
        COPG_ABI=arm64
        CONTROLLER_INSTALLED=true
      }
    fi

    rm -f "$MODPATH/controller_arm64" "$MODPATH/controller_x86_64" 2>/dev/null

    if ! $CONTROLLER_INSTALLED; then
      bad "No compatible controller"
      sub "Supported: ARM64 (arm64-v8a) · x86_64."
      print_failure_and_exit "binary"
    fi
  fi

  if $INSTALL_SUCCESS; then
    chmod 0755 "$MODPATH/service.sh" 2>/dev/null
    chmod 0644 "$MODPATH/COPG.json" "$MODPATH/list.json" "$MODPATH/global.json" 2>/dev/null

    # Concurrent Mic (mic-share): pick the device-ABI payload + injector, make the injector executable.
    # CI ships per-ABI suffixed files (mapinject_arm64/_x86_64); the local build ships them flat — the
    # suffixed mv is a no-op there and the flat files just get chmod'd. arm64/x86_64 (audioserver ABI).
    if [ -d "$MODPATH/micshare" ]; then
      MC_ABI="$COPG_ABI"   # reuse the controller probe's verdict (same device ABI)
      mv "$MODPATH/micshare/mapinject_$MC_ABI"     "$MODPATH/micshare/mapinject"     2>/dev/null
      mv "$MODPATH/micshare/libmicshare_$MC_ABI.so" "$MODPATH/micshare/libmicshare.so" 2>/dev/null
      rm -f "$MODPATH/micshare/mapinject_"* "$MODPATH/micshare/libmicshare_"*.so 2>/dev/null
      chmod 0755 "$MODPATH/micshare/mapinject"     2>/dev/null
      chmod 0644 "$MODPATH/micshare/libmicshare.so" 2>/dev/null
    fi

    # Select the copg_lic helper for this ABI. The CI ships copg_lic_<abi> (multi-ABI);
    # the local build.sh ships a single copg_lic. Reuse the controller probe's verdict
    # ($COPG_ABI) — same device ABI, same toolchain — then drop the rest.
    if [ ! -f "$MODPATH/copg_lic" ] && [ -f "$MODPATH/copg_lic_$COPG_ABI" ]; then
      mv "$MODPATH/copg_lic_$COPG_ABI" "$MODPATH/copg_lic"
    fi
    # Fallback: the abilist match above can MISS on some OEM builds (odd ABI strings, empty/short
    # abilist — e.g. a Samsung S25 Ultra hit "copg_lic: not found" → enrollment fails). Never leave
    # the device with no helper: if nothing matched, promote a shipped variant (prefer arm64, the
    # dominant real-device ABI), so licensing works instead of silently breaking.
    if [ ! -f "$MODPATH/copg_lic" ]; then
      for cand in copg_lic_arm64 copg_lic_x86_64; do
        if [ -f "$MODPATH/$cand" ]; then
          mv "$MODPATH/$cand" "$MODPATH/copg_lic"; break
        fi
      done
    fi
    rm -f "$MODPATH/copg_lic_arm64" "$MODPATH/copg_lic_x86_64" 2>/dev/null

    # Licensing helper (licensing branch): the WebUI execs it via ksu.exec, so it MUST be
    # executable. Magisk/KSU reset extracted files to 0644, so set 0755 explicitly (same as
    # the controller). wv.dex is read (by app_process / the module in the zygote domain) so
    # it just needs to be readable with a normal system_file context.
    [ -f "$MODPATH/copg_lic" ] && chmod 0755 "$MODPATH/copg_lic" 2>/dev/null
    [ -f "$MODPATH/wv.dex" ] && chmod 0644 "$MODPATH/wv.dex" 2>/dev/null

    for file in "$MODPATH/COPG.json" "$MODPATH/list.json" "$MODPATH/global.json" \
                "$MODPATH/service.sh" "$MODPATH/copg_lic" \
                "$MODPATH/wv.dex"; do
      if [ -f "$file" ]; then
        chcon u:object_r:system_file:s0 "$file" 2>/dev/null
      fi
    done

    # Final licensing-helper sanity: the WebUI execs copg_lic via ksu.exec, so a missing or
    # non-executable binary makes "Get device code" fail with "Could not read device fingerprint".
    # Flag it loudly at install so a broken PRO-licensing build never ships silently.
    if [ ! -f "$MODPATH/copg_lic" ]; then
      warn "Licensing helper missing — PRO enrollment will not work"
    else
      chmod 0755 "$MODPATH/copg_lic" 2>/dev/null
      chcon u:object_r:system_file:s0 "$MODPATH/copg_lic" 2>/dev/null
      [ -x "$MODPATH/copg_lic" ] || warn "Licensing helper not executable — enrollment may fail"
    fi

    # ── Preserve the device-locked PRO license across UPDATES ────────────────────
    # The license lives at license/license.bin inside the module and is NOT shipped in the
    # update zip (it's per-device, minted by the seller). On a module UPDATE the manager
    # stages the new zip to $MODPATH (…/modules_update/COPG) and swaps it over the live
    # module on reboot — so without this the license is wiped on every update and PRO turns
    # off until re-imported. Carry it forward from (1) the currently-installed module, else
    # (2) a persistent backup outside the module that survives a full uninstall+reinstall
    # (safe: the license is device-locked + signed — useless on any other device, unforgeable).
    LIVE_LIC="/data/adb/modules/COPG/license/license.bin"
    PERSIST_DIR="/data/adb/COPG"
    # One-time migration: the persist + per-app-proxy runtime dir was created lowercase as
    # /data/adb/copg — a stray duplicate of the module's uppercase COPG convention (the served-spoofs
    # log already lives in /data/adb/COPG). Fold the old dir into the uppercase one (license backup +
    # proxy/ config), then drop it, so there is a single /data/adb/COPG. /data is case-sensitive, so
    # the two are genuinely different dirs.
    if [ -d /data/adb/copg ]; then
      mkdir -p "$PERSIST_DIR" 2>/dev/null
      cp -a /data/adb/copg/. "$PERSIST_DIR/" 2>/dev/null
      rm -rf /data/adb/copg 2>/dev/null
      note "Consolidated /data/adb/copg → /data/adb/COPG"
    fi
    PERSIST_LIC="$PERSIST_DIR/license.bin"
    DEST_LIC="$MODPATH/license/license.bin"

    if [ ! -f "$DEST_LIC" ]; then
      mkdir -p "$MODPATH/license" 2>/dev/null
      if [ -f "$LIVE_LIC" ] && [ "$LIVE_LIC" != "$DEST_LIC" ]; then
        cp -f "$LIVE_LIC" "$DEST_LIC" 2>/dev/null && note "PRO license carried over from the installed module"
      elif [ -f "$PERSIST_LIC" ]; then
        cp -f "$PERSIST_LIC" "$DEST_LIC" 2>/dev/null && note "PRO license restored from backup"
      fi
    fi

    # Whatever license ended up in place: fix perms/context (companion reads it in the zygote
    # domain) and refresh the persistent backup so the NEXT update can restore it too.
    if [ -f "$DEST_LIC" ]; then
      chmod 0644 "$DEST_LIC" 2>/dev/null
      chcon u:object_r:system_file:s0 "$DEST_LIC" 2>/dev/null
      mkdir -p "$PERSIST_DIR" 2>/dev/null
      chmod 0755 "$PERSIST_DIR" 2>/dev/null           # shared dir (also holds served.jsonl); files carry their own perms
      cp -f "$DEST_LIC" "$PERSIST_LIC" 2>/dev/null
      chmod 0600 "$PERSIST_LIC" 2>/dev/null
      chcon u:object_r:system_file:s0 "$PERSIST_LIC" 2>/dev/null
    fi

    # ── Device-wide / system-server spoofs (global.json) across UPDATES ──────────
    # Only runs on an UPDATE (a live global.json exists). Per-app config (COPG.json/list.json)
    # is carried over unconditionally by the user-config block below. The
    # global switch is the ONLY bootloop-risky, per-user bit, so ASK: VOL+ carries the old global.json
    # (spoofs stay ON, arm on the first reboot); timeout / VOL- / broken-vol-key device → keep the
    # shipped default (system-server master OFF, safe). The boot-crash guard still backstops a bad carry.
    LIVE_GLOBAL="/data/adb/modules/COPG/global.json"
    if [ -f "$LIVE_GLOBAL" ]; then
      prompt_global_carry
      if [ "$CARRY_GLOBAL" = true ]; then
        cp -f "$LIVE_GLOBAL" "$MODPATH/global.json" 2>/dev/null && note "Device-wide spoofs: KEPT"
      else
        note "Device-wide spoofs: OFF"
      fi
      chmod 0644 "$MODPATH/global.json" 2>/dev/null
      chcon u:object_r:system_file:s0 "$MODPATH/global.json" 2>/dev/null
    fi

    # ── User config (COPG.json / list.json) across UPDATES ───────────────────────
    # The manager stages the new zip to $MODPATH and swaps it over the live module on reboot, so
    # without this every update replaces the user's devices, packages and tags with the shipped
    # defaults. Carry the live copies forward (like the license). Only a non-empty file that looks
    # like a JSON object is carried; a missing/corrupt one falls back to the shipped default.
    LIVE_DIR="/data/adb/modules/COPG"
    if [ "$LIVE_DIR" != "$MODPATH" ]; then
      CARRIED=""
      for f in COPG.json list.json; do
        src="$LIVE_DIR/$f"
        if [ -s "$src" ] && [ "$(tr -d ' \t\r\n' < "$src" | head -c 1)" = "{" ]; then
          cp -f "$src" "$MODPATH/$f" 2>/dev/null && CARRIED="$CARRIED $f"
        fi
        chmod 0644 "$MODPATH/$f" 2>/dev/null
        chcon u:object_r:system_file:s0 "$MODPATH/$f" 2>/dev/null
      done
      [ -n "$CARRIED" ] && ok "Using your existing configuration:$CARRIED"
    fi

    # Boot heartbeat sentinel: write REBOOT so the WebUI shows "reboot to activate" until the NEW
    # .so actually runs (postServerSpecialize overwrites it with this boot's boot_id). A distinct
    # sentinel (not just deleting) lets the WebUI tell "freshly installed, needs reboot" apart from
    # "loaded a previous boot but not this one" (crashed / failed preload → stale boot_id).
    echo REBOOT > /data/system/copg_hb 2>/dev/null
    chmod 0644 /data/system/copg_hb 2>/dev/null
    # Clear the system_server liveness heartbeat so a stale timestamp can't read "alive" before the
    # new .so's heartbeat thread starts. Absent until the next boot arms the sysserver layer.
    rm -f /data/system/copg_ss_hb 2>/dev/null

    # Per-app CPU profiles (CPU/cpuinfo_<key>) bind-mount onto /proc/cpuinfo, so each
    # must read like a normal system file — 0444 + system_file context (wrong context =
    # unreadable mount / tamper signal). CPU/cpuinfo_sd8elite is also the default/fallback
    # profile (replaces the old root cpuinfo_spoof).
    if [ -d "$MODPATH/CPU" ]; then
      chmod 0644 "$MODPATH/CPU/manifest.json" 2>/dev/null
      for file in "$MODPATH/CPU/"cpuinfo_*; do
        [ -f "$file" ] && chmod 0444 "$file" 2>/dev/null
      done
      for file in "$MODPATH/CPU/"*; do
        [ -f "$file" ] && chcon u:object_r:system_file:s0 "$file" 2>/dev/null
      done
    fi

    # GPU profiles (GPU/manifest.json) — reference branch only. Just a readable JSON
    # (strings, not bind-mounted), so plain 0644 is enough; no special context needed.
    if [ -d "$MODPATH/GPU" ]; then
      chmod 0644 "$MODPATH/GPU/manifest.json" 2>/dev/null
    fi

    # SIM/carrier profiles (SIM/manifest.json) — same as GPU: a readable JSON of carrier
    # identity strings, read in the zygote domain. No bind mount → plain 0644 is enough.
    if [ -d "$MODPATH/SIM" ]; then
      chmod 0644 "$MODPATH/SIM/manifest.json" 2>/dev/null
    fi

    # Per-app proxy: the routing scripts must be executable. The hev helper ships ENCRYPTED
    # (bin/<abi>/hev_tproxy.enc, 0644, NOT executable) — copg_lic decrypts + exec's it from a memfd
    # at runtime with the license's K_GPU, so there is no plaintext binary to chmod.
    if [ -d "$MODPATH/proxy" ]; then
      chmod 0755 "$MODPATH/proxy/"*.sh 2>/dev/null
      for b in "$MODPATH/bin/"*/hev_tproxy.enc; do [ -f "$b" ] && chmod 0644 "$b" 2>/dev/null; done
    fi
    # Proxy profiles (PROXY/manifest.json) — a readable JSON list of the user's proxies (like SIM).
    if [ -d "$MODPATH/PROXY" ]; then
      chmod 0644 "$MODPATH/PROXY/manifest.json" 2>/dev/null
    fi

    step "Configured — permissions, CPU library, contexts"

    # Clean up unused architecture files to reduce module size
    cleanup_unused_architectures
  fi

  if $INSTALL_SUCCESS; then
    hr
    ok "Installed — reboot to activate"
    hr
  fi
fi

if ! $INSTALL_SUCCESS; then
  exit 1
fi
