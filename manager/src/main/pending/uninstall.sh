#!/system/bin/sh
# VOID GMS Optimizer v2.0 - uninstall
# Reverts all changes before removing files.

MODDIR="${0%/*}"
[ ! -d "$MODDIR" ] && MODDIR="$(cd "$(dirname "$0")" && pwd)"
[ -z "$MODDIR" ] || [ "$MODDIR" = "." ] && MODDIR="$(pwd)"

# shellcheck source=./common/common.sh
. "$MODDIR/common/common.sh" 2>/dev/null || . "$(dirname "$0")/common/common.sh"

ENV="adb"
[ "$AXERON" = "true" ] && ENV="axmanager"
export ENV

echo "[VOID GMS] Restoring factory defaults..."
void_restore_all 2>/dev/null
echo "[VOID GMS] Removing user data..."
rm -rf "/sdcard/Android/data/voidgms" 2>/dev/null
echo "[VOID GMS] Removing module-local backups..."
rm -rf "$MODDIR/backup" 2>/dev/null
echo "[VOID GMS] Cleaning tmp metric files..."
rm -f /tmp/.voidgms_* 2>/dev/null
rm -f /tmp/.voidgms_ps_* 2>/dev/null
echo "[VOID GMS] Uninstall complete. Reboot recommended."
