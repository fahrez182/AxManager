#!/system/bin/sh

# Boost System Props & Settings via resetprop
resetprop debug.hwc.dynThreshold 0.0
resetprop debug.sf.frame_rate_multiple_threshold 0
resetprop debug.app.performance_restricted false
resetprop debug.sf.early_phase_offset_ns -9999999999
resetprop debug.sf.early_app_phase_offset_ns -9999999999
resetprop debug.sf.early_gl_phase_offset_ns -9999999999
resetprop debug.sf.early_gl_app_phase_offset_ns -9999999999
resetprop debug.sf.high_fps_early_phase_offset_ns -9999999999
resetprop debug.sf.high_fps_early_gl_phase_offset_ns -9999999999
resetprop debug.sf.high_fps_late_app_phase_offset_ns -9999999999
resetprop debug.sf.disable_backpressure 1
resetprop debug.sf.latch_unsignaled 1
resetprop debug.sf.use_phase_offsets_as_durations 1
resetprop debug.renderengine.backend skiaglthreaded
resetprop debug.hwui.renderer skiagl
resetprop debug.hwui.use_vulkan true
resetprop debug.hwui.render_dirty_regions false
resetprop debug.hwui.disable_scissor_opt true
resetprop debug.hwui.force_dark false
resetprop debug.mediatek.high_frame_rate_sf_set_big_core_fps_threshold 0
resetprop debug.sf.enable_hwc_vds 1
resetprop debug.systemuicompilerfilter speed
resetprop debug.enabletr true
resetprop debug.egl.hw 1
resetprop debug.gralloc.gfx_ubwc_disable 0
resetprop debug.mdpcomp.logs 0
resetprop debug.performance.tuning 1
resetprop debug.sf.hw 1
resetprop debug.gr.swapinterval 0
resetprop debug.egl.swapinterval 0
resetprop debug.sf.early.app.duration 500000
resetprop debug.sf.early.sf.duration 500000
resetprop debug.sf.late.app.duration 500000
resetprop debug.sf.late.sf.duration 500000
resetprop debug.choreographer.vsync_skipped_frame_limit 0
resetprop debug.choreographer.skip_warning true
resetprop debug.choreographer.vsync_enable false
resetprop debug.vsync.enable false
resetprop debug.fps.limit 0
resetprop debug.fps.divisor 1
resetprop debug.gpu.framesync.enable false
resetprop debug.sf.vsync_reactor.ignore_present_timestamps true
resetprop debug.hwui.target_frame_duration 0
resetprop debug.hwui.fps_divisor 0
resetprop debug.hwui.force_hwc_copy_for_virtual_displays true
resetprop debug.gpu.wait_for_vsync false
resetprop debug.gpu.disable_idle_timer true
resetprop debug.gpu.turbo_mode true
resetprop debug.performance.turbo true
resetprop debug.hwui.static_text_cache_size 64
resetprop debug.renderengine.fps_timing true
resetprop debug.sf.buffer_state_layer_updates false
resetprop debug.sf.show_frame_rate false
resetprop debug.sf.update_frequency 0
resetprop debug.sf.high_fps.late.sf.duration -9999999999
resetprop debug.sf.high_fps.late.app.duration -9999999999
resetprop debug.sf.high_fps.early.sf.duration -9999999999
resetprop debug.sf.high_fps.early.app.duration -9999999999
resetprop debug.sf.high_fps.earlyGl.sf.duration -9999999999
resetprop debug.sf.high_fps.earlyGl.app.duration -9999999999
resetprop debug.sf.high_fps.hwc.min.duration -9999999999
resetprop debug.sf.set_touch_timer_ms 0
resetprop debug.sf.enable_fb_ubwc 1
resetprop debug.gralloc.enable_fb_ubwc 1
resetprop debug.gralloc.gfx_ubwc_disable 0
resetprop debug.gralloc.map_fb_memory 0
resetprop debug.gralloc.disable_ahardware_buffer 1
resetprop debug.sf.region_sampling_timer_timeout_ns 0
resetprop debug.sf.region_sampling_period_ns 0

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
