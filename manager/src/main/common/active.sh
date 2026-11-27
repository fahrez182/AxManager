#!/system/bin/sh

# Boost System Props & Settings
setprop debug.hwc.dynThreshold 0.0
setprop debug.sf.frame_rate_multiple_threshold 0
setprop debug.app.performance_restricted false
setprop debug.sf.early_phase_offset_ns -9999999999
setprop debug.sf.early_app_phase_offset_ns -9999999999
setprop debug.sf.early_gl_phase_offset_ns -9999999999
setprop debug.sf.early_gl_app_phase_offset_ns -9999999999
setprop debug.sf.high_fps_early_phase_offset_ns -9999999999
setprop debug.sf.high_fps_early_gl_phase_offset_ns -9999999999
setprop debug.sf.high_fps_late_app_phase_offset_ns -9999999999
setprop debug.sf.disable_backpressure 1
setprop debug.sf.latch_unsignaled 1
setprop debug.sf.use_phase_offsets_as_durations 1
setprop debug.renderengine.backend skiaglthreaded
setprop debug.hwui.renderer skiagl
setprop debug.hwui.use_vulkan true
setprop debug.hwui.render_dirty_regions false
setprop debug.hwui.disable_scissor_opt true
setprop debug.hwui.force_dark false
setprop debug.mediatek.high_frame_rate_sf_set_big_core_fps_threshold 0
setprop debug.sf.enable_hwc_vds 1
setprop debug.systemuicompilerfilter speed
setprop debug.enabletr true
setprop debug.egl.hw 1
setprop debug.gralloc.gfx_ubwc_disable 0
setprop debug.mdpcomp.logs 0
setprop debug.performance.tuning 1
setprop debug.sf.hw 1
setprop debug.gr.swapinterval 0
setprop debug.egl.swapinterval 0
setprop debug.sf.early.app.duration 500000
setprop debug.sf.early.sf.duration 500000
setprop debug.sf.late.app.duration 500000
setprop debug.sf.late.sf.duration 500000
setprop debug.choreographer.vsync_skipped_frame_limit 0
setprop debug.choreographer.skip_warning true
setprop debug.choreographer.vsync_enable false
setprop debug.vsync.enable false
setprop debug.fps.limit 0
setprop debug.fps.divisor 1
setprop debug.gpu.framesync.enable false
setprop debug.sf.vsync_reactor.ignore_present_timestamps true
setprop debug.hwui.target_frame_duration 0
setprop debug.hwui.fps_divisor 0
setprop debug.hwui.force_hwc_copy_for_virtual_displays true
setprop debug.gpu.wait_for_vsync false
setprop debug.gpu.disable_idle_timer true
setprop debug.gpu.turbo_mode true
setprop debug.performance.turbo true
setprop debug.hwui.static_text_cache_size 64
setprop debug.renderengine.fps_timing true
setprop debug.sf.buffer_state_layer_updates false
setprop debug.sf.show_frame_rate false
setprop debug.sf.update_frequency 0
setprop debug.sf.high_fps.late.sf.duration -9999999999
setprop debug.sf.high_fps.late.app.duration -9999999999
setprop debug.sf.high_fps.early.sf.duration -9999999999
setprop debug.sf.high_fps.early.app.duration -9999999999
setprop debug.sf.high_fps.earlyGl.sf.duration -9999999999
setprop debug.sf.high_fps.earlyGl.app.duration -9999999999
setprop debug.sf.high_fps.hwc.min.duration -9999999999
setprop debug.sf.set_touch_timer_ms 0
setprop debug.sf.enable_fb_ubwc 1
setprop debug.gralloc.enable_fb_ubwc 1
setprop debug.gralloc.gfx_ubwc_disable 0
setprop debug.gralloc.map_fb_memory 0
setprop debug.gralloc.disable_ahardware_buffer 1
setprop debug.perf_event_mlock_kb 32768
setprop debug.perf_event_max_sample_rate 1000000
setprop debug.sf.region_sampling_timer_timeout_ns 0
setprop debug.sf.region_sampling_period_ns 0

# Settings
settings put secure touch_blocking_period 0.0
settings put secure tap_duration_thershold 0.0
settings put system pointer_speed 7
settings put secure long_press_timeout 50
settings put secure multi_press_timeout 0
settings put system peak_refresh_rate 1
settings put system user_refresh_rate 1
settings put system max_refresh_rate 165
settings put system min_refresh_rate 60

# Activity Manager Optimized
settings put global activity_manager_constants \
"bg_max_cached_processes=0,fg_max_cached_processes=1,cached_max_age=300000,max_cached_processes=1,max_cached_empty_processes=0,max_service_process_time=300000,max_empty_cached_processes=0,fg_service_compaction_threshold=4096,compaction_time_bg=300000,compaction_time_fg=60000,compact_full_rss_throttle=512,compact_throttle_1=4096,compact_throttle_2=8192,compact_statsd_sample_rate=1.0"

# Output UBWC status for verification
getprop | grep ubwc
getprop | grep alloc

# Notifikasi Sukses
cmd notification post -S bigtext -t 'SUCCESS🔥' 'Game Engine Optimizer' "Module telah diterapkan dengan sukses! Performa maksimal diaktifkan ⚡"
