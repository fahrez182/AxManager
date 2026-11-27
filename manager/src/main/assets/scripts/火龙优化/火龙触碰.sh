#!/system/bin/sh
MODDIR=${0%/*}

sleep 15

# 后台保活
echo 0 > /sys/module/lowmemorykiller/parameters/enable_lmk 2>/dev/null
echo 4096 > /proc/sys/vm/min_free_kbytes 2>/dev/null
echo 80 > /proc/sys/vm/swappiness 2>/dev/null

# 触控采样率
for f in /sys/class/input/*/poll_rate /sys/devices/virtual/input/*/poll_rate; do
echo 120 > "$f" 2>/dev/null
done

# GPU稳频
echo 0 > /sys/class/kgsl/kgsl-3d0/throttling 2>/dev/null

# 调度优化
echo 90 > /proc/sys/kernel/sched_util_clamp_min 2>/dev/null
echo 1 > /proc/sys/kernel/sched_child_runs_first 2>/dev/null

# 网络低延迟
echo 1 > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null
echo 0 > /proc/sys/net/ipv4/tcp_timestamps 2>/dev/null

# 防断触
echo 1 > /sys/module/msm_ts/parameters/fast_report 2>/dev/null#!/system/bin/sh
MODDIR=${0%/*}
LOG_DIR=/cache/snapdragon_engine
mkdir -p $LOG_DIR

# 等待系统启动完成
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 5
done

# 延迟启动，避开开机高峰期
sleep 30

# 启动守护主进程
sh $MODDIR/底层守护脚本/engine_daemon.sh &
