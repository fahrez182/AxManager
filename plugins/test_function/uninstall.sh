#!/system/bin/sh
# Runs when the plugin is removed (on the next re-ignite after you tap Uninstall), before its
# folder is deleted. Output goes to $AXERONDIR/logs/<id>.uninstall.log; 40s limit.

MODDIR=$(cd "${0%/*}" && pwd)
. "$MODDIR/common/lib.sh"

dump_env
check "AXERON_PLUGIN is the plugin id" [ "$AXERON_PLUGIN" = "$PLUGIN_ID" ]

# Proof that this ran, kept after the plugin folder is gone.
date '+%Y-%m-%d %H:%M:%S' > "$AXERONDIR/logs/$PLUGIN_ID.uninstalled"

# Left running on purpose: AxManager stops whatever uninstall.sh leaves behind.
# Check with: axtest procs (from another plugin) or ps -A | grep 'sleep 3600'
setsid sleep 3600 </dev/null >/dev/null 2>&1 &
say "- left pid $! running; AxManager should stop it right after this script"
