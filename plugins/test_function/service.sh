#!/system/bin/sh
# The igniter starts this in its own session (setsid), with stdin from /dev/null.
# The next line makes it run in BusyBox's standalone mode (every command is an applet):
ASH_STANDALONE=1

MODDIR=$(cd "${0%/*}" && pwd)
. "$MODDIR/common/lib.sh"

report_begin service
dump_env

section "checks"
check "AXERON_PLUGIN is the plugin id" [ "$AXERON_PLUGIN" = "$PLUGIN_ID" ]
check "stdin is /dev/null" [ "$(readlink /proc/$$/fd/0)" = /dev/null ]
check "post-fs-data.sh ran before this" [ -f "$RUN_DIR/post-fs-data.stamp" ]
check "system.prop was applied" [ "$(getprop debug.axeron.test_function)" = loaded ]
check "axtest from system/bin is on PATH" [ "$(command -v axtest)" = "$AXERONXBIN/axtest" ]
info "session id" "$(awk '{print $6}' /proc/$$/stat)"

section "daemons"
# A long-running daemon in its own session. It inherits the marker, so disabling the plugin
# stops it. Its first child clears its environment (env -i, like crond's jobs): it has no
# marker and is found only because it stays in the daemon's session.
setsid sh -c '
  env -i /system/bin/sleep 86400 </dev/null >/dev/null 2>&1 &
  echo $! > "$1/run/clean-env.pid"
  while :; do
    date +%s > "$1/run/heartbeat"
    sleep 10
  done
' heartbeat "$MODDIR" </dev/null >/dev/null 2>&1 &
echo $! > "$RUN_DIR/heartbeat.pid"
sleep 1

info "heartbeat" "$(pid_state heartbeat)"
info "clean-env child" "$(pid_state clean-env)"
