#!/system/bin/sh
# Runs when you tap Action on the plugin card. AxManager starts it from the plugin's folder
# with system/bin first in PATH and AXERON_PLUGIN set; the output appears on screen.

MODDIR=$(cd "${0%/*}" && pwd)
. "$MODDIR/common/lib.sh"

report_begin action
dump_env

section "action"
check "AXERON_PLUGIN is the plugin id" [ "$AXERON_PLUGIN" = "$PLUGIN_ID" ]
check "working directory is the plugin folder" [ "$(pwd)" = "$MODDIR" ]
check "system/bin is first in PATH" [ "${PATH%%:*}" = "$MODDIR/system/bin" ]

section "system/bin and system.prop"
check "axtest resolves" command -v axtest
info "axtest ping" "$(axtest ping)"
check "axtest is also linked in AXERONXBIN" [ -L "$AXERONXBIN/axtest" ]
check "system.prop applied (debug.axeron.test_function=loaded)" \
  [ "$(getprop debug.axeron.test_function)" = loaded ]

section "boot stages"
check "post-fs-data.sh ran" [ -f "$RUN_DIR/post-fs-data.stamp" ]
check "service.sh ran" [ -f "$REPORT_DIR/service.txt" ]
check "post-fs-data log exists" [ -f "$AXERONDIR/logs/$PLUGIN_ID.post-fs-data.log" ]
if [ -f "$RUN_DIR/heartbeat" ]; then
  info "heartbeat age (written every 10s)" "$(( $(date +%s) - $(cat "$RUN_DIR/heartbeat") ))s"
fi

section "process lifecycle"
info "heartbeat daemon (expected: alive, marked)" "$(pid_state heartbeat)"
info "clean-env child (expected: alive, no marker)" "$(pid_state clean-env)"
info "install-time daemon (expected: gone after re-ignite)" "$(pid_state install-daemon)"
info "WebUI daemon (expected: alive if started from the WebUI)" "$(pid_state webui-daemon)"
info "WebUI background job, no setsid (expected: alive if started from the WebUI)" "$(pid_state webui-bg)"
say "processes carrying AXERON_PLUGIN=$PLUGIN_ID:"
marked_processes | while read -r line; do say "  $line"; done

section "server settings"
check "TMPDIR exists" [ -d "$TMPDIR" ]
# Which build is deployed: the installer that keeps cache/ uses cache/install, and the server
# that creates cache/ at start is newer than the one that didn't.
info "cache/ listing" "$(ls -ld "$AXERONDIR/cache" "$AXERONDIR/cache/install" 2>&1 | tr '\n' ';')"
info "installer keeps TMPDIR (functions.sh uses cache/install)" \
  "$(grep -q 'TMPDIR="$TMPDIR/install"' "$AXERONBIN/functions.sh" 2>/dev/null && echo yes || echo 'no: old functions.sh')"
info "server uptime" "$(ps -o etime= -p "$(pidof axeron_server 2>/dev/null | cut -d' ' -f1)" 2>/dev/null | tr -d ' ')"
check "TMPDIR is writable" sh -c ': > "$1/.probe" && rm "$1/.probe"' probe "$TMPDIR"
info "Stop plugins with the server" \
  "$([ -f "$AXERONDIR/ax_stop_plugins_with_server" ] && echo on || echo off)"
info "AX-Permission Intercept (Shizuku)" \
  "$([ -f "$AXERONDIR/ax_perm_companion" ] && echo on || echo off)"
info "busybox" "$(busybox 2>&1 | head -n 1)"
info "logs/" "$(ls "$AXERONDIR/logs" 2>/dev/null | tr '\n' ' ')"

section "failures in all reports"
grep -h '^\[FAIL\]' "$REPORT_DIR"/*.txt 2>/dev/null || say "none"
