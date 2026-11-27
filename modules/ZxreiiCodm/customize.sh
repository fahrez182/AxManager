

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 1: DEBUG PROPERTIES - 165 FPS ULTRA LOCK
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

setprop debug.codm.fps 165
setprop debug.codm.fps_target 165
setprop debug.codm.fps_lock 1
setprop debug.codm.fps_cap 165
setprop debug.codm.fps_min 165
setprop debug.codm.fps_max 165
setprop debug.codm.refresh_rate 165
setprop debug.codm.vsync 0
setprop debug.codm.vsync_mode disabled
setprop debug.codm.ultra_smooth 1
setprop debug.codm.max_smoothness 1
setprop debug.codm.extreme_smooth 1
setprop debug.codm.zero_lag 1
setprop debug.codm.no_lag 1
setprop debug.codm.latency 0
setprop debug.codm.input_delay 0
setprop debug.codm.render_delay 0
setprop debug.codm.frame_time 16.67
setprop debug.codm.frame_pacing 1
setprop debug.codm.frame_stabilizer 1
setprop debug.codm.performance_mode 1
setprop debug.codm.high_performance 1
setprop debug.codm.extreme_performance 1
setprop debug.codm.ultra_performance 1
setprop debug.codm.gpu_boost 1
setprop debug.codm.cpu_boost 1
setprop debug.codm.thermal_throttle 0
setprop debug.codm.game_boost 1
setprop debug.codm.vulkan_mode 1
setprop debug.codm.render_driver vulkan

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 2: SYSTEM DEBUG - 165HZ VULKAN PERFORMANCE
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

setprop debug.performance.tuning 1
setprop debug.performance.mode extreme
setprop debug.hwui.renderer vulkan
setprop debug.hwui.disable_vsync true
setprop debug.hwui.render_rate 165
setprop debug.gr.swapinterval 0
setprop debug.gr.numframebuffers 2
setprop debug.egl.swapinterval 0
setprop debug.egl.hw 1
setprop debug.egl.buffcount 2
setprop debug.sf.disable_backpressure 1
setprop debug.sf.latch_unsignaled 1
setprop debug.sf.hw 1
setprop debug.sf.disable_client_composition_cache 1
setprop debug.sf.use_phase_offsets_as_durations 1
setprop debug.sf.late.sf.duration 16666666
setprop debug.sf.late.app.duration 16666666
setprop debug.sf.early.sf.duration 16666666
setprop debug.sf.early.app.duration 16666666
setprop debug.composition.type gpu
setprop debug.enable.sglscale 1
setprop debug.scaling.min 1800000
setprop debug.scaling.max 3000000
setprop debug.refresh_rate 165
setprop debug.vulkan.render 1
setprop debug.vulkan.optimize 1

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 3: GPU VULKAN DEBUG
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

setprop debug.gpu.scheduler_pre.emption 1
setprop debug.gpu.fbs 2
setprop debug.gpu.profiler 0
setprop debug.gpu.systrace 0
setprop debug.gpu.overdraw false
setprop debug.gpu.profile false
setprop debug.gpu.max_fps 165
setprop debug.gpu.boost 1
setprop debug.gpu.high_performance 1
setprop debug.gpu.low_latency 1
setprop debug.gpu.vulkan_mode 1
setprop debug.renderengine.backend vulkanthreaded
setprop debug.hwui.skia_atrace_enabled false
setprop debug.hwui.profile false
setprop debug.hwui.disable_draw_defer true

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 4: DEVICE_CONFIG - CODM 165 FPS VULKAN ULTRA
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

device_config put game_overlay com.garena.game.codm fps 165
device_config put game_overlay com.garena.game.codm fps.min 165
device_config put game_overlay com.garena.game.codm fps.max 165
device_config put game_overlay com.garena.game.codm fps.lock true
device_config put game_overlay com.garena.game.codm mode performance
device_config put game_overlay com.garena.game.codm performance_mode true
device_config put game_overlay com.garena.game.codm extreme_performance true
device_config put game_overlay com.garena.game.codm ultra_performance true
device_config put game_overlay com.garena.game.codm ultra_smooth true
device_config put game_overlay com.garena.game.codm zero_lag true
device_config put game_overlay com.garena.game.codm no_lag true
device_config put game_overlay com.garena.game.codm downscale 0.9
device_config put game_overlay com.garena.game.codm graphics_quality high
device_config put game_overlay com.garena.game.codm texture_quality high
device_config put game_overlay com.garena.game.codm shadow_quality medium
device_config put game_overlay com.garena.game.codm effects_quality high
device_config put game_overlay com.garena.game.codm character_quality high
device_config put game_overlay com.garena.game.codm weapon_quality ultra
device_config put game_overlay com.garena.game.codm environment_quality high
device_config put game_overlay com.garena.game.codm anti_aliasing 2x
device_config put game_overlay com.garena.game.codm post_processing high
device_config put game_overlay com.garena.game.codm bloom on
device_config put game_overlay com.garena.game.codm motion_blur off
device_config put game_overlay com.garena.game.codm depth_of_field on
device_config put game_overlay com.garena.game.codm ragdoll on
device_config put game_overlay com.garena.game.codm frame_stability ultra
device_config put game_overlay com.garena.game.codm frame_pacing ultra
device_config put game_overlay com.garena.game.codm frame_time_target 16.67
device_config put game_overlay com.garena.game.codm frame_variance ultra_low
device_config put game_overlay com.garena.game.codm stutter_elimination true
device_config put game_overlay com.garena.game.codm jitter_reduction extreme
device_config put game_overlay com.garena.game.codm lag_reduction maximum
device_config put game_overlay com.garena.game.codm smoothness maximum
device_config put game_overlay com.garena.game.codm render_driver vulkan
device_config put game_overlay com.garena.game.codm render_backend vulkan
device_config put game_overlay com.garena.game.codm vulkan_render true
device_config put game_overlay com.garena.game.codm vulkan_optimize true
device_config put game_overlay com.garena.game.codm vulkan_async true
device_config put game_overlay com.garena.game.codm vulkan_pipeline_cache true
device_config put game_overlay com.garena.game.codm vulkan_descriptor_pool 1024
device_config put game_overlay com.garena.game.codm vulkan_command_buffer 256
device_config put game_overlay com.garena.game.codm cpu_boost 1
device_config put game_overlay com.garena.game.codm cpu_scheduler performance
device_config put game_overlay com.garena.game.codm cpu_affinity big_cores
device_config put game_overlay com.garena.game.codm cpu_governor performance
device_config put game_overlay com.garena.game.codm cpu_thread_priority high
device_config put game_overlay com.garena.game.codm cpu_game_thread_priority high
device_config put game_overlay com.garena.game.codm cpu_render_thread_priority high
device_config put game_overlay com.garena.game.codm cpu_input_thread_priority high
device_config put game_overlay com.garena.game.codm cpu_background_limit true
device_config put game_overlay com.garena.game.codm cpu_frame_scheduler performance
device_config put game_overlay com.garena.game.codm cpu_power_efficiency false
device_config put game_overlay com.garena.game.codm gpu_boost 1
device_config put game_overlay com.garena.game.codm gpu_scheduler performance
device_config put game_overlay com.garena.game.codm gpu_priority high
device_config put game_overlay com.garena.game.codm gpu_thread_priority high
device_config put game_overlay com.garena.game.codm gpu_low_latency true
device_config put game_overlay com.garena.game.codm gpu_frame_ahead 2
device_config put game_overlay com.garena.game.codm gpu_pipeline performance
device_config put game_overlay com.garena.game.codm gpu_preemption true
device_config put game_overlay com.garena.game.codm gpu_async_compute true
device_config put game_overlay com.garena.game.codm gpu_shader_cache true
device_config put game_overlay com.garena.game.codm gpu_pipeline_cache true
device_config put game_overlay com.garena.game.codm gpu_optimization extreme
device_config put game_overlay com.garena.game.codm render_latency low
device_config put game_overlay com.garena.game.codm render_ahead 2
device_config put game_overlay com.garena.game.codm render_multithread true
device_config put game_overlay com.garena.game.codm render_fast_path true
device_config put game_overlay com.garena.game.codm render_prediction true
device_config put game_overlay com.garena.game.codm render_thread_affinity true
device_config put game_overlay com.garena.game.codm touch_boost 1
device_config put game_overlay com.garena.game.codm touch_latency ultra_low
device_config put game_overlay com.garena.game.codm touch_sampling_rate 240
device_config put game_overlay com.garena.game.codm touch_response_immediate true
device_config put game_overlay com.garena.game.codm touch_gesture_boost true
device_config put game_overlay com.garena.game.codm input_latency zero
device_config put game_overlay com.garena.game.codm input_delay zero
device_config put game_overlay com.garena.game.codm input_pipeline_parallel true
device_config put game_overlay com.garena.game.codm input_prediction high
device_config put game_overlay com.garena.game.codm input_buffer_size 512
device_config put game_overlay com.garena.game.codm gyro_latency low
device_config put game_overlay com.garena.game.codm gyro_sampling_rate 120
device_config put game_overlay com.garena.game.codm memory_optimization aggressive
device_config put game_overlay com.garena.game.codm memory_cache 3072
device_config put game_overlay com.garena.game.codm memory_texture_cache true
device_config put game_overlay com.garena.game.codm memory_shader_cache true
device_config put game_overlay com.garena.game.codm memory_pipeline_cache true
device_config put game_overlay com.garena.game.codm memory_preload_assets true
device_config put game_overlay com.garena.game.codm memory_gc_aggressive true
device_config put game_overlay com.garena.game.codm network_latency low
device_config put game_overlay com.garena.game.codm network_tickrate 165
device_config put game_overlay com.garena.game.codm network_priority gaming
device_config put game_overlay com.garena.game.codm network_jitter_reduction true
device_config put game_overlay com.garena.game.codm network_packet_boost true
device_config put game_overlay com.garena.game.codm network_optimization extreme
device_config put game_overlay com.garena.game.codm thermal_mode performance
device_config put game_overlay com.garena.game.codm thermal_throttle false
device_config put game_overlay com.garena.game.codm thermal_stability ultra
device_config put game_overlay com.garena.game.codm power_mode high_performance
device_config put game_overlay com.garena.game.codm battery_saver false
device_config put game_overlay com.garena.game.codm br_mode performance
device_config put game_overlay com.garena.game.codm br_frame_stability ultra
device_config put game_overlay com.garena.game.codm br_render_distance high
device_config put game_overlay com.garena.game.codm br_texture_streaming fast
device_config put game_overlay com.garena.game.codm br_object_culling aggressive
device_config put game_overlay com.garena.game.codm mp_mode performance
device_config put game_overlay com.garena.game.codm mp_frame_stability ultra
device_config put game_overlay com.garena.game.codm mp_render_quality high
device_config put game_overlay com.garena.game.codm stability_mode ultra
device_config put game_overlay com.garena.game.codm anti_stutter true
device_config put game_overlay com.garena.game.codm frame_drop_prevention true
device_config put game_overlay com.garena.game.codm smooth_frame_delivery true
device_config put game_overlay com.garena.game.codm adaptive_fps false
device_config put game_overlay com.garena.game.codm dynamic_resolution false
device_config put game_overlay com.garena.game.codm quality_scaling false

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 5: CONTENT URI - 165 FPS VULKAN ULTRA
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.fps --bind value:s:165
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.fps_target --bind value:s:165
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.fps_lock --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.fps_cap --bind value:s:165
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.fps_min --bind value:s:165
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.fps_max --bind value:s:165
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.lock_fps --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.frame_rate_control --bind value:s:strict
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.vsync_mode --bind value:s:0
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.refresh_rate --bind value:s:165
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.frame_pacing --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.frame_stabilizer --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.frame_time_target --bind value:s:16.67
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.ultra_smooth --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.max_smoothness --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.extreme_smooth --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.zero_lag --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.no_lag --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.latency --bind value:s:0

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 6: CONTENT URI - HIGH QUALITY VULKAN GRAPHICS
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.graphics_quality --bind value:s:High
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.resolution_scale --bind value:s:0.9
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.render_scale --bind value:s:0.9
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.texture_quality --bind value:s:High
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.shadow_quality --bind value:s:Medium
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.effects_quality --bind value:s:High
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.character_quality --bind value:s:High
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.weapon_quality --bind value:s:Ultra
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.environment_quality --bind value:s:High
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.anti_aliasing --bind value:s:2
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.post_processing --bind value:s:High
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.bloom --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.motion_blur --bind value:s:0
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.depth_of_field --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.ragdoll --bind value:s:1

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 7: CONTENT URI - VULKAN RENDER DRIVER
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.render_driver --bind value:s:vulkan
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.render_backend --bind value:s:vulkan
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.vulkan_render --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.vulkan_optimize --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.vulkan_async --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.vulkan_pipeline_cache --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.vulkan_descriptor_pool --bind value:s:1024
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.vulkan_command_buffer --bind value:s:256

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 8: CONTENT URI - CPU PERFORMANCE
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cpu_boost --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cpu_scheduler --bind value:s:performance
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cpu_affinity --bind value:s:big_cores
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cpu_governor --bind value:s:performance
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cpu_thread_priority --bind value:s:high
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cpu_game_thread_priority --bind value:s:high
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cpu_render_thread_priority --bind value:s:high
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cpu_input_thread_priority --bind value:s:high
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cpu_background_limit --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cpu_frame_scheduler --bind value:s:performance
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cpu_power_efficiency --bind value:s:0
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.thread_priority --bind value:s:high
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.process_priority --bind value:s:high

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 9: CONTENT URI - GPU VULKAN BOOST
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_boost --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_scheduler --bind value:s:performance
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_priority --bind value:s:high
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_thread_priority --bind value:s:high
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_low_latency --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_frame_ahead --bind value:s:2
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_pipeline --bind value:s:performance
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_preemption --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_async_compute --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_shader_cache --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_pipeline_cache --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_optimization --bind value:s:extreme
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gpu_accelerated --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.hardware_accelerated --bind value:s:1

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 10: CONTENT URI - RENDER PIPELINE
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.render_latency --bind value:s:low
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.render_ahead --bind value:s:2
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.render_multithread --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.render_fast_path --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.render_prediction --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.render_thread_affinity --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.render_thread_priority --bind value:s:high
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.multi_core_rendering --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.prediction_enabled --bind value:s:1

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 11: CONTENT URI - ULTRA SMOOTH TOUCH
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.touch_boost --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.touch_latency --bind value:s:ultra_low
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.touch_sampling_rate --bind value:s:240
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.touch_response_immediate --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.touch_gesture_boost --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.input_latency --bind value:s:0
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.input_delay --bind value:s:0
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.input_pipeline_parallel --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.input_prediction --bind value:s:high
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.input_buffer_size --bind value:s:512
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gyro_latency --bind value:s:low
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.gyro_sampling_rate --bind value:s:120
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.low_latency_mode --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.ultra_low_latency --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.high_priority_input --bind value:s:1

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 12: CONTENT URI - MEMORY OPTIMIZATION
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.memory_optimization --bind value:s:aggressive
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.memory_cache --bind value:s:3072
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.memory_texture_cache --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.memory_shader_cache --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.memory_pipeline_cache --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.memory_preload_assets --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.memory_gc_aggressive --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.memory_optimize --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.aggressive_memory --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.cache_clear_on_launch --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.aggressive_gc --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.background_limit --bind value:s:0

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 13: CONTENT URI - NETWORK GAMING
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.network_latency --bind value:s:low
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.network_tickrate --bind value:s:165
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.network_priority --bind value:s:gaming
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.network_jitter_reduction --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.network_packet_boost --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.network_optimization --bind value:s:extreme
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.net_optimize --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.low_ping_mode --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.latency_compensation --bind value:s:0

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 14: CONTENT URI - THERMAL & POWER
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.thermal_mode --bind value:s:performance
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.thermal_throttle --bind value:s:0
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.thermal_stability --bind value:s:ultra
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.power_mode --bind value:s:high_performance
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.battery_saver --bind value:s:0
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.power_save_mode --bind value:s:0
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.high_performance_mode --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.performance_mode --bind value:s:maximum
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.boost_mode --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.sustained_performance --bind value:s:0

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 15: CONTENT URI - BR & MP MODES
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.br_mode --bind value:s:performance
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.br_frame_stability --bind value:s:ultra
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.br_render_distance --bind value:s:high
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.br_texture_streaming --bind value:s:fast
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.br_object_culling --bind value:s:aggressive
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.mp_mode --bind value:s:performance
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.mp_frame_stability --bind value:s:ultra
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.mp_render_quality --bind value:s:high

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SECTION 16: CONTENT URI - ULTRA STABILITY
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

content insert --uri content://settings/global --bind name:s:com.garena.game.codm.stability_mode --bind value:s:ultra
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.anti_stutter --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.anti_lag --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.smooth_frame --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.frame_drop_prevention --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.smooth_frame_delivery --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.jitter_reduction --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.stutter_fix --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.fps_stabilizer --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.adaptive_fps --bind value:s:0
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.dynamic_resolution --bind value:s:0
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.quality_scaling --bind value:s:0
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.smooth_mode --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.game_boost --bind value:s:1
content insert --uri content://settings/global --bind name:s:com.garena.game.codm.ultra_mode --bind value:s:1
