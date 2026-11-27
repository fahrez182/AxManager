#---
# name: [ 骁龙调优 V1.0]
# author: 吵吵
# brand: 高通骁龙
# description: 能写的都写了，免root顶天就这样，不好用，不要骂我啊，我即是黑夜，我就是太阳
#---
#on
echo ======================
echo 骁龙调优 v1.0    -    听雨声
echo ======================

echo "开始压榨性能"

for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    if [ -w "$cpu" ]; then
        echo performance > "$cpu"
        echo "$cpu -> performance"
    fi
done

max=$(cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq 2>/dev/null)
if [ -n "$max" ]; then
    for node in /sys/devices/system/cpu/cpu*/cpufreq/scaling_max_freq; do
        [ -w "$node" ] && echo "$max" > "$node"
    done
    for node in /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq; do
        [ -w "$node" ] && echo "$max" > "$node"
    done
    echo "CPU锁最高频 $max 最低也最高 不会降了"
fi

for online in /sys/devices/system/cpu/cpu*/online; do
    [ -w "$online" ] && echo 1 > "$online"
done
echo "全核在线"

for gpu_gov in /sys/class/kgsl/kgsl-3d0/devfreq/governor; do
    if [ -w "$gpu_gov" ]; then
        echo performance > "$gpu_gov"
        echo "$gpu_gov -> performance"
    fi
done

echo 尝试GPU超频 能写进去就赚了
if [ -f /sys/class/kgsl/kgsl-3d0/gpu_available_frequencies ]; then
    gpu_max_freq=$(cat /sys/class/kgsl/kgsl-3d0/gpu_available_frequencies 2>/dev/null | tr ' ' '\n' | sort -n | tail -1)
    if [ -n "$gpu_max_freq" ] && [ -w /sys/class/kgsl/kgsl-3d0/max_gpuclk ]; then
        echo "$gpu_max_freq" > /sys/class/kgsl/kgsl-3d0/max_gpuclk
        echo "GPU已超频至 $gpu_max_freq Hz"
    fi
fi

echo 正在尝试干掉温控[概率]

for trip in /sys/class/thermal/thermal_zone*/trip_point_*_temp; do
    [ -w "$trip" ] && echo 95000 > "$trip"
done
if [ -w /sys/class/thermal/thermal_message/temp_limit ]; then
    echo 0 > /sys/class/thermal/thermal_message/temp_limit
    echo "温控已干掉"
fi

setprop debug.composition.type gpu 2>/dev/null
setprop debug.sf.hw 1 2>/dev/null
settings put global force_gpu_rendering 1 2>/dev/null
echo "GPU渲染全接管"

settings put global high_performance_mode 1 2>/dev/null
setprop persist.sys.performance 1 2>/dev/null
setprop debug.performance.tuning 1 2>/dev/null

echo "尽情享受高通火龙的怒火吧，吼"
#off
echo "正在恢复..."

for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    [ -w "$cpu" ] && echo schedutil > "$cpu"
done
echo "CPU调度恢复"

for node in /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq; do
    [ -w "$node" ] && echo 0 > "$node"
done
echo "最低频率恢复"

for gpu_gov in /sys/class/kgsl/kgsl-3d0/devfreq/governor; do
    [ -w "$gpu_gov" ] && echo msm-adreno-tz > "$gpu_gov"
done
echo "GPU调度恢复"

if [ -w /sys/class/thermal/thermal_message/temp_limit ]; then
    echo 1 > /sys/class/thermal/thermal_message/temp_limit
    echo "温控已还给系统"
fi

settings delete global high_performance_mode 2>/dev/null
settings delete global force_gpu_rendering 2>/dev/null
setprop debug.composition.type "" 2>/dev/null
setprop debug.sf.hw "" 2>/dev/null
setprop persist.sys.performance "" 2>/dev/null
setprop debug.performance.tuning "" 2>/dev/null
echo "属性清完了"

echo 完整恢复成功
