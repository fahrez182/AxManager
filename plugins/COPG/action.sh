#!/system/bin/sh
MODDIR=${0%/*}
echo "COPG 7.7.2 - AXERONDIR=$AXERONDIR"
echo "MODDIR=$MODDIR"
ls -l "$MODDIR/controller" 2>&1
echo "--- procs ---"
ps -A 2>/dev/null | grep -i copg || ps 2>/dev/null | grep -i copg || echo "no proc"
