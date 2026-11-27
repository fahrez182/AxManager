#!/system/bin/sh
echo "mode threads time_ms temp cap" > bench.csv
getprop ro.product.model > hwinfo.txt 2>/dev/null || true
cat /proc/cpuinfo | head -n 10 >> hwinfo.txt 2>/dev/null || true
cat /proc/meminfo | head -n 5 >> hwinfo.txt 2>/dev/null || true
for m in old ultra; do
echo $m > /data/local/tmp/axelden_mode 2>/dev/null || true
if [ "$m" = "old" ]; then TH=4; else TH=8; fi
S=$(date +%s%N)
sh ./service.sh 2>/dev/null || sh $AXERONDIR/plugins/axelden/service.sh 2>/dev/null || true
E=$(date +%s%N)
MS=$(( (E - S) / 1000000 ))
T=$(cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || echo 0)
C=$(cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo 0)
echo "$m $TH $MS $T $C" >> bench.csv
echo "$m $TH $MS $T $C"
done
cat bench.csv
cat hwinfo.txt
