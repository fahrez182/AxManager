#!/system/bin/sh
OUT=${1:-./parts200M}
SIZE_MB=${2:-200}
mkdir -p "$OUT"
if [ "$SIZE_MB" -ge 400 ]; then LINES=8000; else LINES=4000; fi
i=1
while [ $i -le 1000 ]; do
n=$(printf "%04d" $i)
F="$OUT/$n.sh"
echo "# OMNI60 pipe $n real" > "$F"
echo 'settings get global window_animation_scale 2>/dev/null || true' >> "$F"
yes "sys_${n}_pad=perf_72_48_real-0123456789ABCDEF0123456789" | head -n $LINES >> "$F"
i=$((i+1))
if [ $((i % 100)) -eq 1 ]; then echo "done $((i-1))/1000"; fi
done
du -sh "$OUT"
du -sb "$OUT"
