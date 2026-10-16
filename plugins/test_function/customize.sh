# Sourced (not executed) by AxManager's installer after the zip is extracted to $MODPATH and
# every file got the default owner and mode 0755. Never call exit here: use abort.
# SKIPUNZIP is not set, so the default extraction runs; see README.md for SKIPUNZIP=1.

MODDIR=$MODPATH
. "$MODPATH/common/lib.sh"

report_begin customize

# Build the zip with "python build.py --abort" to see abort(): it prints the message, deletes
# $MODPATH so nothing half-installed stays behind, and the install fails.
if unzip -l "$ZIPFILE" 2>/dev/null | grep -q 'abort-test'; then
  abort "! abort-test file found in the zip: aborting on purpose"
fi

section "installer variables"
for v in BOOTMODE MODPATH ZIPFILE TMPDIR ARCH ABI ABI32 IS64BIT API MODID MODNAME MODAUTH; do
  eval "kv $v \"\$$v\""
done
dump_env

section "checks"
check "AXERON is true" [ "$AXERON" = true ]
check "BOOTMODE is true" [ "$BOOTMODE" = true ]
check "MODID matches module.prop id" [ "$MODID" = "$(grep_prop id "$MODPATH/module.prop")" ]
check "AXERONVER >= axeronPlugin" [ "$AXERONVER" -ge "$(grep_prop axeronPlugin "$MODPATH/module.prop")" ]
check "AXERON_PLUGIN is set to the plugin id" [ "$AXERON_PLUGIN" = "$MODID" ]
check "ZIPFILE exists" [ -f "$ZIPFILE" ]
check "TMPDIR is writable" sh -c ': > "$1/.probe" && rm "$1/.probe"' probe "$TMPDIR"
check "README.md was extracted (the installer deletes it after this script)" [ -f "$MODPATH/README.md" ]
info "type ls (BusyBox standalone makes this an applet)" "$(type ls 2>&1)"
info "busybox" "$(busybox 2>&1 | head -n 1)"

section "extract by hand from ZIPFILE"
# The default extraction already put extras/ in $MODPATH; this shows reading the zip directly.
mkdir -p "$TMPDIR/extras"
unzip -o "$ZIPFILE" 'extras/*' -d "$TMPDIR" >&2
check "unzip extras/ to TMPDIR" [ -f "$TMPDIR/extras/arch/generic.txt" ]
ARCH_FILE="$TMPDIR/extras/arch/$ARCH.txt"
[ -f "$ARCH_FILE" ] || ARCH_FILE="$TMPDIR/extras/arch/generic.txt"
cp "$ARCH_FILE" "$MODPATH/arch.txt"
info "picked for ARCH=$ARCH" "$(cat "$MODPATH/arch.txt")"
rm -rf "$MODPATH/extras"
check "extras/ removed from MODPATH" [ ! -d "$MODPATH/extras" ]

section "set_perm / set_perm_recursive"
# 0:0 like a Magisk module: chown is best effort, so in shell mode it fails quietly and the
# mode is still applied.
set_perm "$MODPATH/system/bin/axtest" 0 0 0755
check "set_perm applied 0755 to system/bin/axtest" [ "$(stat -c %a "$MODPATH/system/bin/axtest")" = 755 ]
info "axtest owner after set_perm 0 0" "$(stat -c %u:%g "$MODPATH/system/bin/axtest")"

set_perm_recursive "$MODPATH/webroot" "$(id -u)" "$(id -g)" 0755 0644
check "set_perm_recursive: webroot dir 0755" [ "$(stat -c %a "$MODPATH/webroot")" = 755 ]
check "set_perm_recursive: webroot/index.html 0644" [ "$(stat -c %a "$MODPATH/webroot/index.html")" = 644 ]

: > "$MODPATH/run/context-test"
set_perm "$MODPATH/run/context-test" "$(id -u)" "$(id -g)" 0600 u:object_r:system_file:s0
check "set_perm with a context still returns 0" [ $? -eq 0 ]
info "context-test SELinux label (chcon needs root)" "$(ls -Z "$MODPATH/run/context-test" 2>/dev/null | awk '{print $1}')"

section "process started at install"
# Tagged with AXERON_PLUGIN by the installer, so the next re-ignite (this install sets the
# update flag) must stop it. The action shows whether it is gone.
setsid sh -c 'sleep 3600' </dev/null >/dev/null 2>&1 &
echo $! > "$MODPATH/run/install-daemon.pid"
info "started" "$(pid_state install-daemon)"

ui_print ""
ui_print "- Report: $REPORT_DIR/customize.txt"
ui_print "- Re-ignite, then run the Action or open the WebUI to see the rest."
