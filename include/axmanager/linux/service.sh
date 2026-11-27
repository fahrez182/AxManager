#!/system/bin/sh
# AxLinuxEmu service.sh - run by AxManager server (shell or root)
MODDIR=${0%/*}
EMU=$MODDIR/system/bin/axemu
LIBDIR=$MODDIR/system/lib
LOGDIR=$AXERONDIR/logs
LOG=$LOGDIR/axemu.log
mkdir -p $LOGDIR
export LD_LIBRARY_PATH=$LIBDIR:$LD_LIBRARY_PATH
export AX_PAGE_SIZE=16384
chmod +x $EMU
# guest x86_64 libs dir - put your lib_x86.so here
GUEST_DIR=/sdcard/AxLinuxEmu
mkdir -p $GUEST_DIR
echo "[AxLinuxEmu] boot $(date)" >> $LOG
# daemon: load all guest .so configs if present
if [ -x $EMU ]; then
  $EMU --page16k --jit-cache --guest-dir $GUEST_DIR >> $LOG 2>&1 &
  echo $! > $LOGDIR/axemu.pid
  echo "[AxLinuxEmu] started pid $(cat $LOGDIR/axemu.pid)" >> $LOG
else
  echo "[AxLinuxEmu] missing axemu host binary arm64" >> $LOG
fi
# keep alive for AxManager lifecycle
# plugins keep running unless Stop plugins with server is ON
