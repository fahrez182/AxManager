#!/system/bin/sh
MODDIR=${0%/*}
echo "=== My VSCode 4K ==="
id
whoami
echo "AXERONDIR=$AXERONDIR"
mkdir -p "$MODDIR/sim/workspace"
ls "$MODDIR/sim"
which python3 || echo no-python3
dumpsys battery | head -n 15
