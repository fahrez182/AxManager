#!/system/bin/sh
MODDIR=${0%/*}
if [ -n "$AXERONDIR" ]; then
  rm -rf "$MODDIR" 2>/dev/null
else
  rm -rf /data/adb/modules/COPG 2>/dev/null
fi
rm -rf /data/adb/copg_state 2>/dev/null
if [ -f "$INFO" ]; then
  while read LINE; do
    [ -f "$LINE~" ] && mv -f "$LINE~" "$LINE" || rm -f "$LINE"
  done < "$INFO"
  rm -f "$INFO"
fi
