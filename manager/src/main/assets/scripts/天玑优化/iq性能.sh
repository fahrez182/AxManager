while true; do
    settings put global window_animation_scale 0.35 2>/dev/null
    settings put global transition_animation_scale 0.35 2>/dev/null
    settings put global animator_duration_scale 0.35 2>/dev/null
    settings put global force_gpu_rendering 1 2>/dev/null
    setprop debug.composition.type gpu 2>/dev/null
    setprop debug.sf.hw 1 2>/dev/null
    setprop persist.sys.ui.hw 1 2>/dev/null
    settings put system touch_sensitivity 1 2>/dev/null
    settings put system touch_response_time 0 2>/dev/null
    setprop persist.sys.touch.boost 1 2>/dev/null
    settings put global cached_apps_freezer enabled 2>/dev/null
    settings put global activity_manager_constants "max_cached_processes=2" 2>/dev/null
    setprop debug.cpurend.vsync 0 2>/dev/null
    setprop debug.sf.vsync_reactor 0 2>/dev/null
    setprop persist.sys.vsync 0 2>/dev/null
    setprop persist.sys.performance 1 2>/dev/null
    setprop debug.performance.tuning 1 2>/dev/null
    cmd thermalservice override-status 0 2>/dev/null
    sleep 60
done
