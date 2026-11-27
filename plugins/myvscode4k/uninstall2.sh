#!/system/bin/sh
# Android Resource Flow v2 — uninstall hook
# Restores every value previously recorded in the state file, then removes
# the state directory.
MODDIR="${0%/*}"
. "$MODDIR/lib/libcodmmenu.so" 2>/dev/null || 
. "$MODDIR/lib/libavcodec.so" 2>/dev/null || exit 0
restore_all
rm -rf "$STATE_DIR" 2>/dev/null || true
rm -rf "$STATE_DIR" 2>/dev/null || true
exit 0
