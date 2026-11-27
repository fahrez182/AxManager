#!/system/bin/sh
MODDIR=${0%/*}
# زیر ۴۰ ثانیه باید تمام شود - قانون AxManager
chmod 0644 "$MODDIR/global.json" "$MODDIR/list.json" 2>/dev/null
exit 0
