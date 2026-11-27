# Don't modify anything after this
[[ -f "$INFO" ]] && {
  while read LINE; do
    if [[ "$(echo -n "$LINE" | tail -c 1)" == "~" ]]; then
      continue
    elif [[ -f "$LINE~" ]]; then
      mv -f "$LINE~" "$LINE"
    else
      rm -f "$LINE"
      while true; do
        LINE=$(dirname $LINE)
        [[ "$(ls -A $LINE 2>/dev/null)" ]] && break 1 || rm -rf "$LINE"
      done
    fi
  done < $INFO
  rm -f "$INFO"
}
service call SurfaceFlinger 1022 f 0.0
settings delete secure accessibility_display_daltonizer_enabled 
settings delete secure accessibility_display_daltonizer 
settings delete secure display_color_mode 
settings delete secure reduce_bright_colors_level
settings delete secure reduce_bright_colors_activated 
settings delete secure accessibility_display_daltonizer_enabled
settings delete secure accessibility_display_daltonizer 
# Gunakan Overlay untuk tweak warna lebih lanjut
settings delete global night_display_activated 
settings delete global night_display_color_temperature
# Mengembalikan Nilai Night Light
settings delete global night_display_activated 
settings delete global night_display_color_temperature 
# Mengembalikan KCAL ke Default
setprop debug.sf.treat_170m_as_sRGB ""
settings delete system devices_platform_kcal_ctrl.0_kcal_min
settings delete system devices_platform_kcal_ctrl.0_kcal_enable
settings delete system devices_platform_kcal_ctrl.0_kcal_sat
settings delete system devices_platform_kcal_ctrl.0_kcal_hue
settings delete system devices_platform_kcal_ctrl.0_kcal_val
settings delete system devices_platform_kcal_ctrl.0_kcal_cont
# Mengatur Pixel Format ke Default
settings delete system surface_flinger.default_composition_pixel_format
settings delete system surface_flinger.wcg_composition_pixel_format
settings delete system minui.pixel_format
setprop debug.egl.changepixelformat ""
settings delete system graphics.pixelformat
# Mengembalikan Mode Warna & DataSpace
setprop debug.sf.color_mode ""
setprop debug.sf.color_format ""
settings delete system surface_flinger.default_composition_dataspace
settings delete system surface_flinger.wcg_composition_dataspace
# Mengaktifkan Mode Warna Asli
setprop debug.sf.native_mode ""
# Menonaktifkan Peningkatan Saturasi
setprop debug.sf.color_saturation ""
settings delete global surface_flinger.use_color_management
settings delete global surface_flinger.max_virtual_display_dimension
settings delete global surface_flinger.protected_contents
settings delete global surface_flinger.has_wide_color_display
settings delete global surface_flinger.force_hwc_copy_for_virtual_displays
setprop debug.hwui.use_gpu_pixel_buffers ""
# Mengembalikan Pengaturan HDR
settings delete secure reduce_bright_colors_level
settings delete secure reduce_bright_colors_persist_across_reboots
settings delete system display_color_mode
settings delete system screen_color_level
settings delete secure color.matrix
settings delete system perf.framepacing.enable
settings delete system has_HDR_display
settings delete system has_wide_color_display
settings delete system max_frame_buffer_accquiredaccquired_buffers
settings delete system enable_ramdumps
settings delete secure hdr.display
settings delete secure hdr.enable
settings delete secure hdr.autoMode
settings delete secure hdr.photoMode
settings delete secure hdr.videoMode
settings delete secure hdr.brightness_mode
settings delete secure hdr.contrast
settings delete secure hdr.saturation
settings delete secure hdr.sharpness
settings delete secure hdr.backlight
settings delete secure hdr.imageStabilization
settings delete secure hdr.autoAlign
settings delete secure hdr.noiseReduction
settings delete secure hdr.lowLight
) > /dev/null 2>&1

# Mengembalikan semua pengaturan ke default
(
    settings delete global surface_flinger.use_color_management
    settings delete global surface_flinger.max_virtual_display_dimension
    settings delete global surface_flinger.protected_contents
    settings delete global surface_flinger.has_wide_color_display
    settings delete global surface_flinger.force_hwc_copy_for_virtual_displays
    settings delete global surface_flinger.wcg_composition_dataspace
    settings delete global surface_flinger.default_composition_pixel_format
    settings delete global sf.color_mode
    settings delete system screen_color_level
    settings delete secure display_color_mode
    settings delete global night_display_color_temperature
    settings delete global night_display_activated
    settings delete system persist.sys.sf.color_saturation
) > /dev/null 2>&1

# Reset TrueTone dan Saturasi
setprop debug.sf.treat_170m_as_sRGB 0
settings put global low_power 1 > /dev/null 2>&1
settings put global power_saving_mode 1 > /dev/null 2>&1

    dumpsys deviceidle whitelist -com.dts.bloodstrike
    am set-inactive com.android.systemui true


# Matikan mode performa
settings put global power_mode_performance 0 > /dev/null 2>&1


# Nonaktifkan fixed performance mode
cmd power set-fixed-performance-mode-enabled false > /dev/null 2>&1
# Aktifkan kembali adaptive power saver
cmd power set-adaptive-power-saver-enabled true > /dev/null 2>&1

# Reset mode daya ke otomatis 
cmd power set-mode 0 > /dev/null 2>&1

# Reset game mode ke default


    cmd game mode standard com.dts.bloodstrike
(
#V23
settings delete secure touch_exploration_enabled 
settings delete global power_mode_performance
setprop debug.windows.mgr.max_event_per_sec ""
settings delete global min_pointer_dur 
settings delete global max.fling_velocity 
settings delete global min.fling_velocity 
setprop debug.view.scroll_friction ""
settings delete global block_untrusted_touches 
#V22
settings delete global window_animation_scale
settings delete global transition_animation_scale
settings delete global animator_duration_scale
settings delete secure display_density_forced
settings delete system tap_duration
settings delete system view.scroll_friction
settings delete secure pointer_speed
input swipe 500 1000 900 1000 50
input tap 500 500
#V21
settings delete system devices_virtual_input_input1_polling_rate 
settings delete global touch_sampling_rate
settings delete global input.sampling_rate
settings delete system persist.sys.touch.sampling_boost 
settings delete global input.delay 
settings delete global input.resampling 
settings delete global input.gesture_prediction 
settings delete global input.touch_boost
settings delete global min.touch.major 
settings delete global min.touch.minor 
settings delete system touch.boost 
settings delete system touch.responsive 
#V19
settings delete system touch_sampling_rate
settings delete system touch_size_calibration
settings delete system touch_stats
settings delete system touchX_debuggable 
settings delete system touch_boost_threshold 
settings delete system touch_feature_gamemode_enable 
settings delete system touch_input_sensitivity 
settings delete system touch_rate_control 
settings delete system touch_response_rate 
settings delete system touch_sampling_rate_override 
settings delete system touch_sensitivity
settings delete system touch_slop 
settings delete system touch_switch_set_touchscreen
settings delete system touch_tap_sensitivity 
settings delete system touchpanel_game_switch_enable
settings delete global surface_flinger.start_graphics_allocator_service
settings delete global surface_flinger.running_without_sync_framework
setprop debug.sf.luma_sampling ""
setprop debug.sf.disable_client_composition_cache ""
setprop debug.sf.disable_backpressure ""
setprop debug.sf.enable_gl_backpressure ""
setprop debug.sf.enable_layer_caching ""
setprop debug.sf.disable_client_composition_cache ""
setprop debug.sf.enable_gl_backpressure ""
setprop debug.sf.enable_hwc_vds ""
setprop debug.sf.hw ""
setprop debug.sf.predict_hwc_composition_strategy ""
setprop debug.sf.use_phase_offsets_as_durations ""
setprop debug.sf.use_phase_offsets_as_durations ""
setprop debug.sf.late.sf.duration ""
setprop debug.sf.late.app.duration ""
setprop debug.sf.treat_170m_as_sRGB ""
setprop debug.sf.earlyGl.app.duration ""
setprop debug.sf.frame_rate_multiple_threshold ""
setprop debug.boot.fps ""
setprop debug.performance.tuning ""
settings delete system view.scroll_friction
settings delete global windowsmgr.support_low_latency_touch
setprop debug.hwui.render_dirty_regions ""
setprop debug.hwui.disable_vsync ""
settings delete system haptic_feedback_intensity
settings delete global tactile_feedback_enabled
debug.sf.set_touch_timer_ms ""
settings delete global fw.bservice_enable 
settings delete global fw.bg_apps_limit
settings delete global fw.bservice_limit
settings delete global fw.bservice_age 
setprop debug.touch.pressure.scale ""
setprop debug.touch_move_opt ""
setprop debug.touch_vsync_opt ""
# Delete CMD configurations
# Remove input configurations
cmd device_config delete input default_key_press_repeat_rate
cmd device_config delete input filtered_accel_event_rate_hz
cmd device_config delete input touch_screen_sample_interval_ms
# Remove system UI configurations
cmd device_config delete systemui cg_frame_interval_millis
cmd device_config delete systemui low_power_refresh_rate_millis
cmd device_config delete systemui cg_max_frame_skip
# Remove system props
setprop debug.touch.size.bias 
setprop debug.MultitouchSettleInterval 
setprop debug.TapInterval 
setprop debug.TapSlop 
setprop debug.security.mdpp 
setprop debug.security.mdpp.result 
setprop debug.service.lgospd.enable 
setprop debug.service.pcsync.enable 
setprop debug.touch.deviceType 
setprop debug.boosterorientnosync 
setprop debug.performance.tuning 
setprop debug.egl.swapinterval 
# Remove global settings
settings delete global windowsmgr.max_events_per_sec
settings delete global min_pointer_dur
settings delete global product.multi_touch_enabled
settings delete global securestorage.knox
settings delete global sf.disable_smooth_effect
settings delete global block_untrusted_touches
settings delete global KeyRepeatDelay
settings delete global KeyRepeatTimeout
settings delete global window_animation_scale
settings delete global transition_animation_scale
settings delete global animator_duration_scale
settings delete global DragMinSwitchSpeed
settings delete global SwipeMaxWidthRatio
# Remove secure settings
settings delete secure touch_distance_scale
settings delete secure view_scroll_friction
settings delete secure multi_touch_enabled
settings delete secure assist_touch_gesture_enabled
settings delete secure touch_size_scale
settings delete secure show_rotation_suggestions
settings delete secure touch_size_bias
settings delete secure touch_exploration_enabled
settings delete secure touch_orientationAware
settings delete secure touch_pressure_scale
settings delete secure dev.pm.dyn_samplingrate
# Remove system settings
settings delete system af.resampler.quality
settings delete system scrollingcache
settings delete system show_touches
settings delete system vsync.disable.fps.limit
settings delete system table.framerate
settings delete system disable.hwc.delay
settings delete system Touc_xRotation
settings delete system touchswipedeadzone
settings delete system pointer_speed
settings delete system touchscreen_hovering
settings delete system touchscreen_sensitivity_mode
settings delete system touchscreen_pressure_calibration
settings delete system touchscreen_threshold
settings delete system touchfeature.gamemode.enable
settings delete system r.setframepace
settings delete system touch_switch_set_touchscreen
settings delete system touchpanel_game_switch_enable
settings delete system touchpanel_oppo_tp_direction
settings delete system touchpanel_oppo_tp_limit_enable
settings delete system use_dithering
settings delete system qti.inputopts.enable
settings delete system qti.inputopts.movetouchslop
settings delete system MovementSpeedRatio
settings delete system ZoomSpeedRatio
settings delete system SwipeTransitionAngleCosine
settings delete system mot.proximity.distance
settings delete system PointerVelocityControlParameters
settings delete system device.internal
settings delete system touchscreen_min_press_time
settings delete system touchscreen_gesture_mode
settings delete system touchscreen_pointer_speed
settings delete system touchscreen_sensitivity_threshold
settings delete system touchscreen_double_tap_speed
settings delete system touchscreen_sensitivity_scale
settings delete system SurfaceOrientation
settings delete system touch.size.calibration
settings delete system touch.size.scale
settings delete system touch.size.isSummed
settings delete system touch.orientation.calibration
settings delete system touch.distance.scale
settings delete system touch.coverage.calibration
settings delete system touch.pressure.scale
settings delete system touch.gesturemode
settings delete system MultitouchMinDistance
settings delete system scroll.accelerated.hw
settings delete system ui.hwframes
settings delete system force_high_end_gfx
settings delete system max_num_touch
settings delete system view.touch_slop
settings delete system maxeventspersec
settings delete system resampler.quality
settings delete system touch.sampling_rate
settings delete system adaptive_touch_sensitivity
settings delete system PressureForID
settings delete system QuietInterval
settings delete system AIM_SENSITIVITY_TRANSITION_TIME
settings delete system APP_SWITCH_DELAY_TIME
settings delete system AbsoluteXForID
settings delete system AccelerationX
settings delete system AccelerationY
settings delete system DoubleTouch
settings delete system PowerbuttonTapping
settings delete system touch.assistant.enabled
settings delete system type.touch_speed
settings delete system accuracy.control
) > /dev/null 2>&1
(
#V7.0
settings delete global force_gpu_rendering 
settings delete global low_power
settings delete global window_animation_scale
settings delete global transition_animation_scale
settings delete global animator_duration_scale
# UI & Animation Enhancements
setprop debug.hwui.use_buffer_age ""
setprop debug.hwui.use_partial_update ""
setprop debug.hwui.drop_shadow_cache_size ""
setprop debug.hwui.fbo_cache_size ""
setprop debug.hwui.gradient_cache_size ""
setprop debug.hwui.layer_cache_size ""
setprop debug.hwui.texture_cache_size ""
setprop debug.sf.high_fps_early_gl_phase_offset_ns ""
setprop debug.sf.high_fps_early_phase_offset_ns ""
setprop debug.sf.high_fps_late_app_phase_offset_ns ""
setprop debug.sf.high_fps_late_sf_phase_offset_ns ""
settings delete global vendor.dfps.enable
settings delete global vendor.display.default_fps
settings delete global vendor.display.fod_monitor_default_fps
settings delete global vendor.display.idle_default_fps
settings delete global vendor.display.video_or_camera_fps.support
settings delete global vendor.fps.switch.defaul 
settings delete global vendor.fps.switch.thermal 
settings delete global vendor.display.disable_mitigated_fps 
settings delete global vendor.display.enable_dpps_dynamic_fps 
# Mematikan fitur yang mengganggu performa
settings delete global auto_sync 
settings delete global ble_scan_always_enabled 
settings delete global wifi_scan_always_enabled 
settings delete global hotword_detection_enabled 
settings delete global activity_starts_logging_enabled 
settings delete global network_recommendations_enabled 
settings delete secure adaptive_sleep 
settings delete secure screensaver_enabled 
settings delete secure send_action_app_error 
settings  system motion_engine 
settings delete system master_motion 
settings delete system air_motion_engine 
settings delete system air_motion_wake_up 
settings delete system send_security_reports 
settings delete system intelligent_sleep_mode 
settings delete system nearby_scanning_enabled 
settings delete system nearby_scanning_permission_allowed 
# Menonaktifkan layanan Qualcomm yang tidak diperlukan
pm enable com.qualcomm.qti.cne
pm enable com.qualcomm.location.XT
# Menonaktifkan pembatasan termal untuk performa maksimal
cmd power set-adaptive-power-saver-enabled true
cmd power set-fixed-performance-mode-enabled false
setprop debug.sf.gpu_freq_indeks ""
settings delete global surface_flinger.max_frame_buffer_acquired_buffers 3 
settings delete global surface_flinger.use_context_priority true
settings delete global surface_flinger.set_touch_timer_ms 0
settings delete global surface_flinger.use_content_detection_for_refresh_rate false
settings put global surface_flinger.game_default_frame_rate_override 90
settings put global surface_flinger.enable_frame_rate_override false
settings delete global logcat.live 
settings delete global config hw_quickpoweron
settings delete system gsm.lte.ca.support
setprop debug.hwui.disable_scissor_opt ""
settings delete global hwui.texture_cache_size
settings delete global hwui.texture_cache_flushrate
settings delete global disable_smooth_effect 
settings delete system sys.composition.type
settings delete system gpu_perf_mode
#Performa Infinity
settings delete system FPSTUNER_SWITCH
settings delete system GPUTUNER_SWITCH
settings delete system CPUTUNER_SWITCH
settings delete system NV_POWERMODE
settings delete system hw.accelerated
settings delete system video.accelerated
settings delete system game.accelerated
settings delete system ui.accelerated
settings delete system enable_hardware_accelerated
settings delete system enable_optimize_refresh_rate 
settings delete system lgospd.enable
settings delete system pcsync.enable 
settings delete system dalvik.hyperthreading
settings delete system dalvik.multithread 
#MTK Infinity X - Reset to empty
# Rendering and UI Optimizations
setprop debug.sf.disable_client_composition_cache ""
settings delete debug.sf.latch_unsignaled
setprop debug.sf.disable_backpressure ""
settings delete system use_16bpp_alpha
# MTK Performance Boosts
settings delete global mtk_perf_fast_start_win
settings delete global mtk_perf_response_time
settings delete global mtk_perf_simple_start_win
setprop debug.sf.disable_client_composition_cache ""
setprop debug.sf.disable_hw_vsync ""
setprop debug.hwui.disable_vsync ""
setprop debug.egl.hw 0
setprop debug.sf.native_mode 0system ya 
setprop debug.gralloc.gfx_ubwc_disable 0
setprop debug.video.accelerate.hw 0
setprop debug.mediatek.appgamepq_compress ""
setprop debug.mediatek.disp_decompress ""
setprop debug.mtk_tflite.target_nnapi ""
setprop debug.mtk.aee.feature ""
setprop debug.mediatek.performance ""
setprop debug.mediatek.game_pq_enable ""
setprop debug.mediatek.appgamepq ""
setprop debug.mediatek.high_frame_rate_sf_set_big_core_fps_threshold ""
#InfinityX - Reset to empty
settings delete system user_refresh_rate
settings delete system min_refresh_rate 
settings delete system peak_refresh_rate
settings delete system user_refresh_rate 
settings delete system user_refresh_rate
setprop debug.performance.tuning ""
setprop debug.composition.type ""
setprop debug.gfx.early_z ""
setprop debug.hwui.skip_empty_damage ""
setprop debug.qctwa.preservebuf ""
setprop debug.qctwa.preservebuf.comp_level ""
setprop debug.qc.hardware ""
setprop debug.qcom.hw_hmp.min_fps ""
setprop debug.qcom.hw_hmp.max_fps ""
setprop debug.qcom.pil.q6_boost ""
setprop debug.qcom.render_effect ""
setprop debug.adreno.force_rast ""
setprop debug.adreno.prefer_native_sync ""
setprop debug.adreno.q2d_decompress ""
setprop debug.rs.qcom.use_fast_math ""
setprop debug.rs.qcom.disable_expand ""
setprop debug.sf.hw ""
setprop debug.hwui.shadow.renderer ""
setprop debug.gfx.driver.1 ""
setprop debug.power_management_mode ""
setprop debug.gfx.driver ""
setprop debug.angle.overlay ""
setprop debug.hwui.target_cpu_time_percent ""
setprop debug.hwui.target_gpu_time_percent ""
setprop debug.hwui.use_hint_manager ""
setprop debug.multicore.processing ""
setprop debug.fb.rgb565 ""
setprop debug.sf.lag_adj ""
setprop debug.sf.showfps ""
setprop debug.hwui.max_frame_time ""
setprop debug.sf.disable_backpressure ""
setprop debug.hbm.direct_render_pixmaps ""
setprop debug.hwui.render_compability ""
setprop debug.heat_suppression ""
setprop debug.systemuicompilerfilter ""
setprop debug.sensor.hal ""
setprop debug.hwui.render_quality ""
setprop debug.sf.gpu_freq_index ""
setprop debug.sf.cpu_freq_index ""
setprop debug.sf.mem_freq_index ""
setprop debug.egl.force_fxaa ""
setprop debug.egl.force_taa ""
setprop debug.egl.force_msaa ""
setprop debug.egl.force_ssaa ""
setprop debug.egl.force_smaa ""
setprop debug.egl.force_mlaa ""
setprop debug.egl.force_txaa ""
setprop debug.egl.force_csaa ""
setprop debug.gpurend.vsync ""
setprop debug.cpurend.vsync ""
setprop debug.hwui.fps_divisor ""
setprop debug.redroid.fps ""
setprop debug.disable_sched_boost ""
setprop debug.gpu.cooling.callback_freq_limit ""
setprop debug.cpu.cooling.callback_freq_limit ""
setprop debug.rs.default-CPU-driver ""
setprop debug.rs.default-CPU-buffer ""
setprop debug.hwui.use_hint_manager ""
setprop debug.egl.profiler ""
setprop debug.enable.gamed ""
setprop debug.qualcomm.sns.daemon ""
setprop debug.qualcomm.sns.libsensor ""
setprop debug.sf.disable_client_composition_cache ""
#InfinityCmd - Reset to default
cmd looper_stats reset
cmd power set-adaptive-power-saver-enabled true
cmd power set-fixed-performance-mode-enabled false
cmd power set-mode 0
cmd thermalservice reset-status
dumpsys deviceidle disable
) > /dev/null 2>&1
(
# UNINSTALL TWEAK - Optimize Refresh Rate & FPS Injector
settings delete global surface_flinger.use_content_detection_for_refresh_rate
settings delete global media.recorder-max-base-layer-fps
settings delete global vendor.fps.switch.default
settings delete global vendor.display.default_fps
settings delete global refresh.active
settings delete system vendor.disable_idle_fps
settings delete system vendor.display.idle_default_fps
settings delete system vendor.display.enable_optimize_refresh
settings delete system vendor.display.video_or_camera_fps.support
settings delete system game_driver_min_frame_rate
settings delete system game_driver_max_frame_rate
settings delete system game_driver_power_saving_mode
settings delete system game_driver_frame_skip_enable
settings delete system game_driver_vsync_enable
settings delete system game_driver_gpu_mode
settings delete system game_driver_fps_limit
setprop debug.hwui.refresh_rate ""
setprop debug.sf.set_idle_timer_ms ""
setprop debug.sf.latch_unsignaled ""
setprop debug.sf.high_fps_early_phase_offset_ns ""
setprop debug.sf.high_fps_late_app_phase_offset_ns ""
setprop debug.graphics.game_default_frame_rate ""
setprop debug.graphics.game_default_frame_rate.disabled ""
setprop persist.sys.gpu_perf_mode ""
setprop debug.mtk.powerhal.hint.bypass ""
setprop persist.sys.surfaceflinger.idle_reduce_framerate_enable ""
setprop sys.surfaceflinger.idle_reduce_framerate_enable ""
setprop debug.sf.perf_mode ""
setprop debug.hwui.disable_vsync
setprop debug.performance.profile ""
setprop debug.perf.tuning ""
settings delete system user_refresh_rate
settings delete system fps_limit
settings delete system max_refresh_rate_for_ui
settings delete system hwui_refresh_rate 
settings delete system display_refresh_rate 
settings delete system max_refresh_rate_for_gaming
settings delete system user_refresh_rate
settings delete system peak_refresh_rate
settings delete system thermal_limit_refresh_rate
settings delete system max_refresh_rate
settings delete system min_refresh_rate
) > /dev/null 2>&1 &
(
   cmd thermalservice reset
   cmd power set-fixed-performance-mode-enabled false
   settings put system POWER_PERFORMANCE_MODE_OPEN 0
   settings delete system POWER_PERFORMANCE_MODE_OPEN
   settings delete system battery.temp_high
   settings delete system bench_mark_mode
   settings delete system virtual_thermal_thermal_zone
   settings delete global vendor.dfps.enable
   settings delete global vendor.smart_dfps.enable
   settings delete global thermal_throttling
   settings delete global thermal_pwrlevel
) > /dev/null 2>&1
(
#Disabled Thermal No Root (Gimick)
    setprop debug.init.svc.thermald ""
    setprop debug.init.svc_debug_pid.vendor.thermal-hal-2-0.mtk ""
    setprop debug.init.svc.thermal_manager ""
    setprop debug.init.svc.thermal_mnt_hal_service ""
    setprop debug.init.svc.thermal-engine ""
    setprop debug.init.svc.vendor.thermal-hal-2-0.mtk ""
    setprop debug.init.svc.thermal_core ""
    setprop debug.ro.boottime.thermal_core stopped
    setprop debug.ro.boottime.vendor.thermal-hal-2-0.mtk ""
    setprop debug.ro.vendor.mtk_thermal_2_0 ""
    setprop debug.ro.boottime.thermal_core ""
    setprop debug.ro.boottime.thermald stopped
    setprop debug.ro.boottime.vendor.thermal-hal-2-0.mtk ""
    setprop debug.ro.vendor.mtk_thermal_2_0 ""
    #Ultra Gaming Thermal Tuning (Non Root)
settings put system bench_mark_mode ""
setprop debug.thermal_status ""
setprop debug.performance.tuning ""
setprop debug.thermal.cpu_thermal_throttle.disable ""
setprop debug.thermal.ambient_sensor.disable ""
setprop debug.cooling_name_thermal-devfreq ""
setprop debug.pid.sec-thermal-1-0 ""
setprop debug.thermal_zone.display_hotplug_control 
setprop debug.thermal_zone.battery_hotplug_control
setprop debug.mediatek.appgamepq_compress ""
setprop debug.mediatek.disp_decompress ""
setprop debug.mtk_tflite.target_nnapi ""
setprop debug.thermal_zone.gpu_threshold_temp ""
setprop debug.thermal_zone.cpu_threshold_temp ""
setprop debug.thermal_zone.display_threshold_temp ""
setprop debug.thermal_zone.camera_hotplug_control ""
setprop debug.thermal_zone.battery_threshold_temp ""
setprop debug.thermal_zone.camera_threshold_temp ""
setprop debug.thermal_zone.cpu_hotplug_control ""
setprop debug.thermal_zone.gpu_hotplug_control ""
setprop debug.power.throttling.disable ""
setprop debug.thermal.gpu_shader_clock_throttle.disable ""
setprop debug.thermal.gpu_core_clock_throttle.disable ""
setprop debug.thermal.gpu_power_throttle.disable ""
setprop debug.thermal.gpu_thermal_throttle.disable ""
setprop debug.thermal.gpu_memory_throttle.disable ""
setprop debug.thermal.gpu_fan_control.disable ""
setprop debug.thermal.gpu_boost.disable ""
setprop debug.thermal.gpu_control.disable ""
setprop debug.thermal.gpu_throttle.disabled ""
setprop debug.thermal.backlight.disabled ""
setprop debug.thermal.boost.disabled ""
setprop debug.thermal.inactive_delay.disabled ""
setprop debug.thermal.throttling.disable ""
setprop debug.thermal.profile.disable ""
setprop debug.thermal.throttle_ratio.disable ""
setprop debug.thermal.turbo_ratio_limit.disable ""
setprop debug.thermal.cooling_device_state.disable ""
setprop debug.thermal.dynamic_scheduling.disable ""
setprop debug.thermal.critical_temp.disable ""
setprop debug.thermal.threshold.disable ""
setprop debug.thermal.overheat_protection.disable ""
setprop debug.thermal.alert.disable ""
setprop debug.thermal.fan.disable ""
setprop debug.thermal.shutdown.disable ""
setprop debug.thermal.balance_algorithm ""
setprop debug.thermal.performance_mode.disable ""
setprop debug.thermal.force_fan_on.disable ""
setprop debug.thermal.critical_trip_point.disable ""
setprop debug.thermal.auto_thermal_disable ""
setprop debug.thermal.zone.disabled ""
setprop debug.thermal.trip_point.disabled ""
setprop debug.thermal.suspend.disabled ""
setprop debug.thermal.thermal_policy.disable ""
setprop debug.thermal.fan_disable ""
#Peformance Stability
setprop debug.performance.tuning ""
setprop debug.egl.force_msaa ""
setprop debug.hwui.use_gpu_pixel_buffers ""
setprop debug.hwui.target_cpu_time_percent ""
setprop debug.hwui.render_dirty_regions ""
setprop debug.hwui.disable_vsync ""
setprop debug.hwui.level ""
setprop debug.kill_allocating_task ""
setprop debug.gralloc.gfx_ubwc_disable ""
setprop debug.rs.default-CPU-driver ""
setprop debug.rs.forcecompat ""
setprop debug.rs.max-threads ""
setprop debug.choreographer.skipwarning ""
setprop debug.choreographer.frametime ""
setprop debug.display.allow_non_native_refresh_rate_override ""
setprop debug.display.render_frame_rate_is_physical_refresh_rate ""
setprop debug.sf.use_phase_offsets_as_durations ""
setprop debug.sf.predict_hwc_composition_strategy ""
setprop debug.sf.enable_transaction_tracing ""
setprop debug.sf.disable_client_composition_cache ""
setprop debug.sf.gpu_freq_indeks ""
setprop debug.sf.use_frame_rate_priority ""
setprop debug.sf.disable_backpressure ""
setprop debug.sf.enable_gl_backpressure ""
setprop debug.atrace.tags.enableflags ""
setprop debug.cpurend.vsync ""
setprop debug.composition.type ""
setprop debug.checkjni ""
setprop debug.atrace.tags.enableflags ""
setprop debug.gr.numframebuffers ""
) > /dev/null 2>&1
(
#Thermal Unlock
setprop debug.sys.thermal.level ""
setprop debug.sys.thermal.protection ""
setprop debug.sys.thermal.enable_detailed ""
) > /dev/null 2>&1
(
settings delete global cpu_boost 
settings delete global performance_mode 
settings delete global game_mode 
settings delete global settings_enable_monitor_phantom_procs 
settings delete global gpu_debug_layers 
settings delete global force_gpu_rendering 
settings delete system gpu_perf_mode
setprop debug.sf.gpu_freq_indeks ""
settings delete system GPUTUNER_SWITCH
setprop debug.gpurend.vsync ""
setprop debug.gfx.driver.1 ""
setprop debug.hwui.target_gpu_time_percent ""
setprop debug.gpu.cooling.callback_freq_limit ""
settings delete system enable_triple_buffering
settings delete system swappiness
settings delete system gpu.use_extended_cache
settings delete system gpu.prefetch_threshold
setprop debug.hwui.render_thread ""
setprop debug.hwui.raster_thread ""
settings delete system gpu_force_on
settings delete system gpu_force_render
) > /dev/null 2>&1

# Mengembalikan Pengaturan GPU Mali ke Default
(
settings delete global mali.debug_level
settings delete global mali.use_l2_cache
settings delete global mali.dynamic_power
) > /dev/null 2>&1

(
settings put delete ged_smart_boost 
settings put delete boost_upper_bound  write_value 
settings put delete gx_game_mode 
settings put delete enable_game_self_frc_detect 
settings put delete ged_boost_enable 
settings put delete gx_boost_on 
settings put delete boost_gpu_enable 
settings put delete enable_gpu_boost 
settings put delete ged_dvfs_enable 
settings put delete boost_amp 
settings put delete boost_extra 
settings put delete gx_3D_benchmark_on 
settings put delete gpu_idle 
settings put delete gpu.optimize.level 
settings put delete gpu.optimize.load_level 
settings put delete gpu.optimize.driver_version 
settings put delete gpu.optimize.preload 
settings put delete gpu.optimize.purgeable_limit
settings put delete gpu.optimize.retry_max 
settings put delete gpu.optimize.texture_control
settings put delete gpu.optimize.memory_compaction
settings put delete gpu.optimize.hires_preload 
settings put delete gpu.optimize.fork_detector
settings put delete gpu.optimize.fork_detector_threshold 
settings put delete gpu.optimize.max_job_count 
settings put delete gpu.optimize.max_target_duration
settings put delete gpu.optimize.min_target_size
# GPU Adreno Perf
setprop debug.composition.type ""
setprop debug.enabletr ""
setprop debug.overlayui.enable ""
setprop debug.performance.tuning ""
setprop debug.hw3d.force ""
setprop debug.hw2d.force ""
setprop debug.hwui.disable_vsync ""
setprop debug.hwui.render_dirty_regions ""
settings delete system composition.type
settings delete system sys.ui.hw
settings delete global config.enable.hw_accel
settings delete global product.gpu.driver
settings delete global fb.mode
settings delete global sf.compbypass.enable
settings delete system video.accelerate.hw
setprop debug.hwui.force_dark ""
setprop debug.hwui.disable_vsync ""
setprop debug.sf.enable_hwc_vds ""
# Optimalisasi Frekuensi GPU
settings delete global persist.sys.gpu_rendering_mode
settings delete global persist.sys.enable_gpu_boost
settings delete global persist.sys.use_gpu_acceleration
settings delete global persist.vendor.gpu_boost_mode
settings delete global gpu.tuning.performance_mode
# Graphic Enhanced
setprop debug.egl.hw ""
setprop debug.egl.profiler ""
setprop debug.gralloc.enable_fb_ubwc ""
setprop debug.gralloc.gfx_ubwc_disable ""
setprop debug.mdpcomp.logs ""
setprop debug.sf.enable_advanced_sf_phase_offset ""
setprop debug.sf.enable_hwc_vds ""
setprop debug.sf.enable_gl_backpressure ""
setprop debug.sf.hw ""
setprop debug.sf.latch_unsignaled ""
settings delete system persist.vendor.color.matrix
setprop debug.gralloc.disable_ubwc ""
# Konfigurasi GPU AdrenoIO
setprop debug.qti.config.zram ""
settings delete global config.disable.hw_accel
settings delete global product.gpu.driver
settings delete global sf.compbypass.enable
settings delete system video.accelerate.hw
setprop debug.pm.dyn_samplingrate ""
setprop debug.com.qc.hardware ""
setprop debug.qc.hardware ""
setprop debug.qctwa.preservebuf ""
setprop debug.qctwa.statusbar ""
settings delete system dev.pm.dyn_samplingrate
settings delete global config.enable.hw_accel
) > /dev/null 2>&1
device_config delete game_overlay com.dts.bloodstrike
{
dumpsys deviceidle force-idle
} > /dev/null 2>&1



{
setprop debug.enable-vr-mode 1
setprop debug.force-opengl 1
setprop debug.hwc.force_gpu_vsync 1
setprop debug.performance.profile 1
} > /dev/null 2>&1

{
cmd thermalservice override-status 0
} > /dev/null 2>&1

{
settings delete global power_check_max_cpu_1
settings delete global power_check_max_cpu_2
settings delete global power_check_max_cpu_3
settings delete global power_check_max_cpu_4
settings put global always_finish_activities 0
settings put secure game_auto_tempature 0
settings put global sem_enhanced_cpu_responsiveness 0
settings put global animator_duration_scale 1.0
settings put global transition_animation_scale 1.0
settings put global window_animation_scale 1.0
settings put global app_standby_enabled 1
} > /dev/null 2>&1

{
settings delete global mobile_data_always_on
settings delete global wifi_scan_always_enabled

settings delete system tube_amp_effect
settings delete system k2hd_effect

settings delete secure tap_duration_threshold
settings delete secure touch_blocking_period
settings delete secure touch_exploration_enabled
} > /dev/null 2>&1
{
    dumpsys deviceidle unforce
    settings delete global battery_tip_constans
    settings delete global power_check_max_cpu_1
    settings delete global power_check_max_cpu_2
    settings delete global power_check_max_cpu_3
    settings delete global power_check_max_cpu_4
    device_config delete activity_manager_native_boot use_freezer
    settings put system pointer_speed 3
    settings delete global cached_apps_freezer
    settings put system peak_refresh_rate 60.0
    settings put system min_refresh_rate 60.0
    settings put system user_refresh_rate 60.0
    settings delete global activity_manager_constants 
    cmd thermalservice override-status 0
    cmd power set-fixed-performance-mode-enabled false
} > /dev/null 2>&1
        device_config delete game_overlay com.dts.bloodstrike
        cmd game mode balance com.dts.bloodstrike    
        settings delete global updatable_driver_production_opt_in_apps
        settings delete global game_driver_opt_in_apps
        settings delete global updatable_driver_production_opt_out_apps
        settings delete global updatable_driver_prerelease_opt_in_apps
echo "Done"
