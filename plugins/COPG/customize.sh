#!/system/bin/sh
# COPG customize.sh - دوحالته: Magisk/KSU/APatch + AxManager
if [ -n "$AXERONDIR" ] || [ -n "$AXERON_PLUGIN" ]; then
  AX_MODE=true
else
  AX_MODE=false
fi
ui_print "  ▌  C O P G"
ui_print "  ▌  Device Spoofer 7.7.2"
if $AX_MODE; then
  ui_print "  ✓  Environment AxManager"
  [ -z "$ARCH" ] && ARCH="arm64"
  [ -z "$MODPATH" ] && MODPATH="$AXERONDIR/plugins/COPG"
else
  if ! $BOOTMODE; then
    abort "Recovery flashing not supported"
  fi
  if [ "$API" -lt 26 ]; then
    abort "Requires Android 9.0+"
  fi
  if command -v magisk >/dev/null 2>&1; then
    ui_print "  ✓  Root Magisk"
  elif command -v ksud >/dev/null 2>&1; then
    ui_print "  ✓  Root KernelSU"
  elif command -v apd >/dev/null 2>&1; then
    ui_print "  ✓  Root APatch"
  else
    abort "No root solution found"
  fi
fi
if [ "$ARCH" = "arm64" ] || [ "$IS64BIT" = "true" ]; then
  rm -f "$MODPATH/controller_x86_64" 2>/dev/null
  if [ -f "$MODPATH/controller_arm64" ]; then
    mv -f "$MODPATH/controller_arm64" "$MODPATH/controller" 2>/dev/null
  fi
  if [ -f "$MODPATH/copg_lic_arm64" ] && [ ! -f "$MODPATH/copg_lic" ]; then
    mv -f "$MODPATH/copg_lic_arm64" "$MODPATH/copg_lic" 2>/dev/null
  fi
  ui_print "  ✓  ABI arm64-v8a"
else
  ui_print "  !  ABI $ARCH - only arm64 tested"
fi
set_perm_recursive "$MODPATH" 0 0 0755 0644 2>/dev/null
chmod 0755 "$MODPATH/controller" "$MODPATH/copg_lic" "$MODPATH/service.sh" 2>/dev/null
chmod 0755 "$MODPATH/micshare/mapinject_arm64" 2>/dev/null
chmod 0755 "$MODPATH/micshare/mapinject" 2>/dev/null
chmod 0755 "$MODPATH/proxy/copg_proxy.sh" 2>/dev/null
chmod 0644 "$MODPATH/micshare/libmicshare_arm64.so" 2>/dev/null
ui_print "  ✓  Install done"
