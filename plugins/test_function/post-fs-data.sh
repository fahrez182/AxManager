#!/system/bin/sh
# Runs once per boot (and again after the plugin was disabled or updated), before service.sh.
# It blocks the igniter for at most 40s; its output goes to $AXERONDIR/logs/<id>.post-fs-data.log.

MODDIR=$(cd "${0%/*}" && pwd)
. "$MODDIR/common/lib.sh"

report_begin post-fs-data
dump_env

section "checks"
check "AXERON_PLUGIN is the plugin id" [ "$AXERON_PLUGIN" = "$PLUGIN_ID" ]
check "stdin is /dev/null" [ "$(readlink /proc/$$/fd/0)" = /dev/null ]
check "output goes to logs/$PLUGIN_ID.post-fs-data.log" \
  [ "$(readlink /proc/$$/fd/1)" = "$AXERONDIR/logs/$PLUGIN_ID.post-fs-data.log" ]
info "service.sh daemon right now (expected: gone or no pid file)" "$(pid_state heartbeat)"

date +%s > "$RUN_DIR/post-fs-data.stamp"

# Touch $MODDIR/slow-post-fs-data to see the 40s limit: the igniter moves on to the next
# plugin and starts service.sh while this keeps sleeping.
if [ -f "$MODDIR/slow-post-fs-data" ]; then
  say "- slow mode: sleeping 50s, past the 40s limit"
  sleep 50
  say "- slow mode: finished at $(date '+%H:%M:%S')"
fi
