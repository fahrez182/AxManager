#!/system/bin/sh
OUT=${1:-./parts500}
mkdir -p "$OUT"; i=1
while [ $i -le 500 ]; do n=$(printf "%04d" $i); F="$OUT/$n.sh"
echo "# pipe $n real" > "$F"; echo 'settings get global window_animation_scale 2>/dev/null||true' >> "$F"
yes "sys_${n}_pad=perf_real-0123456789ABCDEF" | head -n 4000 >> "$F"
i=$((i+1)); if [ $((i % 100)) -eq 1 ]; then echo "done $((i-1))/500"; fi; done
du -sh "$OUT"; du -sb "$OUT"
