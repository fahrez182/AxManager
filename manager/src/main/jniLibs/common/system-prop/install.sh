#vexiro(maindata)
echo ""
echo "Instalasi Module Please Wait"
sleep 2
echo "Package Name [ com.activision.callofduty.shooter ]"
sleep 2
#Optimize Truetone Saturation IOS 
(
setprop debug.sf.treat_170m_as_sRGB 1
settings put global surface_flinger.use_color_management true
settings put global surface_flinger.max_virtual_display_dimension 8192
settings put global surface_flinger.protected_contents true
settings put global surface_flinger.has_wide_color_display true
settings put global surface_flinger.force_hwc_copy_for_virtual_displays true
) > /dev/null 2>&1

#Mode TRUETONE 
(
settings put global surface_flinger.wcg_composition_dataspace DISPLAY_P3
settings put global surface_flinger.default_composition_pixel_format RGBA_8888
settings put global sf.color_mode 10 
settings put system screen_color_level -9803157
settings put secure display_color_mode 1
settings put global night_display_color_temperature 8000
settings put global night_display_activated 1
settings put system persist.sys.sf.color_saturation 1.5
) > /dev/null 2>&1

# KCAL Settings untuk TrueTone iOS (Lebih Colorful dan Saturasi Maksimal)
(
setprop debug.sf.treat_170m_as_sRGB 1
settings put system devices_platform_kcal_ctrl.0_kcal_enable 1
settings put system devices_platform_kcal_ctrl.0_kcal_min 0
settings put system devices_platform_kcal_ctrl.0_kcal_sat 300
settings put system devices_platform_kcal_ctrl.0_kcal_hue 15
settings put system devices_platform_kcal_ctrl.0_kcal_val 290
settings put system devices_platform_kcal_ctrl.0_kcal_cont 280
settings put system devices_platform_kcal_ctrl.0_kcal_red 255
settings put system devices_platform_kcal_ctrl.0_kcal_green 240
settings put system devices_platform_kcal_ctrl.0_kcal_blue 245
) > /dev/null 2>&1

# Pengaturan Format Pixel HD [RGBA_8888]
(
settings put system surface_flinger.default_composition_pixel_format RGBA_8888
settings put system surface_flinger.wcg_composition_pixel_format RGBA_8888
settings put system minui.pixel_format RGBA_8888
setprop debug.egl.changepixelformat RGBA_8888
settings put system graphics.pixelformat RGBA_8888
) > /dev/null 2>&1

# Mode Warna P3 Display untuk Lebih Vibrant
(
setprop debug.sf.color_mode 9
setprop debug.sf.color_format RGBA_8888
settings put system surface_flinger.default_composition_dataspace DISPLAY_P3
settings put system surface_flinger.wcg_composition_dataspace DISPLAY_P3
) > /dev/null 2>&1
setprop debug.sf.native_mode 0 > /dev/null 2>&1

# Saturasi Maksimal & HDR Support
(
setprop debug.sf.color_saturation 2.2
setprop debug.hwui.use_gpu_pixel_buffers false
settings put secure hdr.display true
settings put secure hdr.enable true
settings put secure hdr.autoMode true
settings put secure hdr.photoMode true
settings put secure hdr.videoMode true
settings put secure hdr.brightness_mode 150
settings put secure hdr.contrast 120
settings put secure hdr.saturation 120
settings put secure hdr.sharpness 120
settings put secure hdr.backlight 120
settings put secure hdr.lowLight true
settings put secure hdr.noiseReduction true
settings put secure hdr.imageStabilization true
settings put global surface_flinger.use_color_management true
settings put system display_color_mode 1
) > /dev/null 2>&1

# Nonaktifkan Native Color Mode untuk Performa Maksimal
(
setprop debug.sf.native_mode 0
) > /dev/null 2>&1

# Tambahan untuk Mengurangi Lag dan Menjaga FPS Stabil
(
settings put global surface_flinger.max_virtual_display_dimension 8192
settings put global surface_flinger.force_hwc_copy_for_virtual_displays true
settings put system enable_ramdumps 0
setprop debug.persist.sys.ui.hw true
setprop debug.persist.sys.gpu.2d.enable true
) > /dev/null 2>&1
settings put global low_power 0 > /dev/null 2>&1
settings put global power_saving_mode 0 > /dev/null 2>&1
dumpsys deviceidle whitelist +com.activision.callofduty.shooter
am set-inactive com.android.systemui false
settings put global power_mode_performance 1 > /dev/null 2>&1
cmd power set-fixed-performance-mode-enabled true > /dev/null 2>&1
cmd power set-adaptive-power-saver-enabled false > /dev/null 2>&1
cmd power set-mode 0 > /dev/null 2>&1
cmd game mode performance com.activision.callofduty.shooter
(
settings put secure touch_exploration_enabled 0
settings put global power_mode_performance
setprop debug.windows.mgr.max_event_per_sec 180
settings put global min_pointer_dur 8 
settings put global max.fling_velocity 12000
settings put global min.fling_velocity 8000
setprop debug.view.scroll_friction 10
settings put global block_untrusted_touches 0
) > /dev/null 2>&1

(
#V22.0
settings put global window_animation_scale 0.25
settings put global transition_animation_scale 0.25
settings put global animator_duration_scale 0.25
settings put system tap_duration 20
settings put secure pointer_speed 10
input gamepad swipe 200 500 600 500 30
input touchpad swipe 200 500 300 500 70
input touchscreen swipe 100 100 500 500 150
input swipe 500 1000 900 1000 30
input gamepad tap 250 450
input touchpad tap 350 650
input touchscreen tap 130 180
input tap 750 1150
) > /dev/null 2>&1

#V21
( 
settings put system devices_virtual_input_input1_polling_rate 240
settings put global touch_sampling_rate 240
settings put global input.sampling_rate 1000
settings put system persist.sys.touch.sampling_boost 1
settings put global input.delay 0
settings put global input.resampling 1
settings put global input.gesture_prediction 0
settings put global input.touch_boost 1
settings put global min.touch.major 0
settings put global min.touch.minor 0
settings put system touch.boost true
settings put system touch.responsive 1
) > /dev/null 2>&1

#V20
(
settings put global touch.pressure.scale 0.1
settings put global touch.size.scale 0.1
settings put global settings_enable_monitor_phantom_procs false
settings put system pointer_speed 5
settings put global surface_flinger.set_idle_timer_ms 0
settings put global surface_flinger.set_touch_timer_ms 0
settings put global surface_flinger.set_display_power_timer_ms 0
setprop debug.sf.latch_unsignaled 1
setprop debug.sf.disable_backpressure 1
setprop debug.sf.multithreaded_present true
settings put global surface_flinger.use_context_priority 1
) > /dev/null 2>&1

(
#V19
settings put system touch_sampling_rate 120
settings put system touch_size_calibration geometric
settings put system touch_stats {"min":1,"max":1}
settings put system touchX_debuggable 1
settings put system touch_boost_threshold 5
settings put system touch_feature_gamemode_enable 1
settings put system touch_input_sensitivity 1
settings put system touch_rate_control 0
settings put system touch_response_rate 1
settings put system touch_sampling_rate_override 1
settings put system touch_sensitivity 1_2
settings put system touch_slop 8
settings put system touch_switch_set_touchscreen 14005
settings put system touch_tap_sensitivity 1
settings put system touchpanel_game_switch_enable 1
) > /dev/null 2>&1

(
#V18
settings put global surface_flinger.start_graphics_allocator_service true
settings put global surface_flinger.running_without_sync_framework true
setprop debug.sf.luma_sampling 0
setprop debug.sf.disable_client_composition_cache 1
setprop debug.sf.disable_backpressure 1
setprop debug.sf.enable_gl_backpressure 0
setprop debug.sf.enable_layer_caching 0
setprop debug.sf.disable_client_composition_cache 1 
setprop debug.sf.enable_gl_backpressure false
setprop debug.sf.enable_hwc_vds 0
setprop debug.sf.hw 0
setprop debug.sf.predict_hwc_composition_strategy 0
setprop debug.sf.use_phase_offsets_as_durations 1
#V17SCREEN.TOUCH
setprop debug.sf.use_phase_offsets_as_durations 1
setprop debug.sf.late.sf.duration 10500000
setprop debug.sf.late.app.duration 16600000
setprop debug.sf.treat_170m_as_sRGB 1
setprop debug.sf.earlyGl.app.duration 16600000
setprop debug.sf.frame_rate_multiple_threshold 120
setprop debug.boot.fps 20
#V16 Faster Touch 
setprop debug.performance.tuning 1
settings put global windowsmgr.support_low_latency_touch true
setprop debug.hwui.render_dirty_regions false
setprop debug.hwui.disable_vsync true
settings put system haptic_feedback_intensity 50
settings put global tactile_feedback_enabled 1
debug.sf.set_touch_timer_ms 100
###
setprop debug.MultitouchSettleInterval 0.01ms
setprop debyg.MultitouchMinDistance 0.01px
setprop debug.TapInterval 0.1ms
settings put global fw.bservice_enable true
settings put global fw.bg_apps_limit 4
settings put global fw.bservice_limit 4
settings put global fw.bservice_age 10000
setprop debug.touch.pressure.scale 0.001
setprop debug.touch_move_opt 1
setprop debug.touch_vsync_opt 1
setprop debug.touch.size.bias 0 
setprop debug.TapSlop1px
settings put global windowsmgr.max_events_per_sec 180
settings put global min_pointer_dur 8
settings put global product.multi_touch_enabled true
settings put global securestorage.knox false
setprop debug.security.mdpp none
setprop debug.security.mdpp.result none
settings put system af.resampler.quality 255
settings put system scrollingcache 3
setprop debug.service.lgospd.enable 0
setprop debug.service.pcsync.enable 0
setprop debug.touch.deviceTypetouchScreen
cmd device_config put input default_key_press_repeat_rate 33
cmd device_config put input filtered_accel_event_rate_hz 240
cmd device_config put input touch_screen_sample_interval_ms 8
cmd device_config put systemui cg_frame_interval_millis 4
cmd device_config put systemui low_power_refresh_rate_millis 0
cmd device_config put systemui low_power_refresh_rate_millis 0
cmd device_config put systemui cg_max_frame_skip 8
settings put system service.touch.tpf 30
settings put system lowThreshold 0
settings put system highThreshold 0
settings put system VirtualKeyQuietTime 0
settings put system KeyRepeatDelay 0
settings put system KeyRepeatTimeout 0
setprop debug.boosterorientnosync 1
settings put global sf.disable_smooth_effect true
settings put secure touch_distance_scale 0
settings put secure view_scroll_friction 0
settings put secure multi_touch_enabled 1
settings put secure assist_touch_gesture_enabled 0
settings put global maximum_obscuring_opacity_for_touch 0.5
settings put system show_touches 0
settings put system vsync.disable.fps.limit 1
settings put system table.framerate 120
setprop debug.touch.deviceType touchScreen
settings put system disable.hwc.delay 1
settings put system Touc_xRotation  360
settings put system touchswipedeadzone 5
settings put secure long_press_timeout 300
settings put secure multi_press_timeout 300
settings put secure touch_size_scale 5
settings put secure show_rotation_suggestions 0
settings put secure touch_size_bias 5
settings put secure touch_exploration_enabled 1
settings put secure touch_orientationAware 1
settings put secure touch_pressure_scale 0.00000125
settings put system touchscreen_hovering 0
settings put system touchscreen_sensitivity_mode 3
settings put system touchscreen_pressure_calibration 1023
settings put system touchscreen_threshold 9
settings put system touchfeature.gamemode.enable true
settings put system r.setframepace 120
settings put system touch_switch_set_touchscreen 14005
settings put system touchpanel_game_switch_enable 1
settings put system touchpanel_oppo_tp_direction 1
settings put system touchpanel_oppo_tp_limit_enable 0
settings put system touchpanel_oplus_tp_limit_enable 0
settings put system touchpanel_oplus_tp_direction 1
settings put system use_dithering 0
settings put system use_dithering false
settings put system qti.inputopts.enable true
settings put system qti.inputopts.movetouchslop 0.1
settings put global DragMinSwitchSpeed 99999.0px/s
settings put global SwipeMaxWidthRatio 1
settings put system MovementSpeedRatio 1
settings put system ZoomSpeedRatio 1
settings put system SwipeTransitionAngleCosine 3.6
settings put system mot.proximity.distance 1
settings put system PointerVelocityControlParameters 1
settings put system device.internal 1
setprop debug.performance.tuning 1
setprop debug.egl.swapinterval 90
settings put secure dev.pm.dyn_samplingrate 1
settings put system touchscreen_sensitivity 10
settings put system touchscreen_min_press_time 50
settings put system touchscreen_hevoring 0
settings put system touchscreen_gesture_mode 1
settings put system touchscreen_sensitivity_threshold 9
settings put system touchscreen_double_tap_speed 75
settings put system touchscreen_sensitivity_scale 1.5
settings put system qti.inputopts.enable true
settings put system qti.inputopts.movetouchslop 0.6
settings put system touch.orientationAware 1
settings put system SurfaceOrientation auto
settings put system touch.size.calibration geometric
settings put system touch.size.isSummed 1
settings put system touch.orientation.calibration auto
settings put system touch.distance.scale auto
settings put system touch.coverage.calibration octagram
settings put system touch.gesturemode spots
settings put system MovementSpeedRatio auto
settings put system pm.dyn_samplingrate 9999999999999999999999999999
settings put system touch.pressure.calibration auto
settings put system scroll.accelerated.hw true
settings put system ui.hwframes 9999999999999999999999999999
settings put system force_high_end_gfx 1
settings put system sf.disable_smooth_effect true
settings put system max_num_touch auto
settings put system maxeventspersec 9999999999999999999999999999
settings put system resampler.quality 255
settings put system touch.sampling rate 720
settings put system adaptive_touch_sensitivity speed
settings put system touch.orientationAware 0
settings put system PressureForID 0.01
settings put system QuietInterval 0.1ms
settings put system MultitouchMinDistance 1px
settings put system AIM_SENSITIVITY_TRANSITION_TIME GRADUAL
settings put system APP_SWITCH_DELAY_TIME false
settings put system AbsoluteXForID SpeedForID
settings put system AccelerationX true
settings put system AccelerationY true
settings put system DoubleTouch OEM
settings put system PowerbuttonTapping 0
settings put system touch.assistant.enabled 0
settings put system type.touch_speed true
settings put system MovementSpeedRatio 0.8
settings put system accuracy.control 100
settings put system view_scroll_friction 10
settings put secure multi_press_timeout 300
settings put global KeyRepeatDelay 0
settings put global KeyRepeatTimeout 0
settings put global LOSS_OF_FOCUS_BY_MOUSE_MOVEMENT DISABLED
settings put global MOUSEX_AIM_LEVEL 95%
) > /dev/null 2>&1
(
#V7.0
settings put global force_gpu_rendering 1
settings put global low_power 0
settings put global window_animation_scale 0.5
settings put global transition_animation_scale 0.5
settings put global animator_duration_scale 0.5
#V6.0
# UI & Animation Enhancements
setprop debug.hwui.use_buffer_age false
setprop debug.hwui.use_partial_update false
setprop debug.hwui.drop_shadow_cache_size 12
setprop debug.hwui.fbo_cache_size 12
setprop debug.hwui.gradient_cache_size 2
setprop debug.hwui.layer_cache_size 48
setprop debug.hwui.texture_cache_size 88
setprop debug.sf.high_fps_early_gl_phase_offset_ns -2000000
setprop debug.sf.high_fps_early_phase_offset_ns -4000000
setprop debug.sf.high_fps_late_app_phase_offset_ns 1000000
setprop debug.sf.high_fps_late_sf_phase_offset_ns -2000000
settings put global vendor.dfps.enable false
settings put global vendor.display.default_fps 120
settings put global vendor.display.fod_monitor_default_fps 120
settings put global vendor.display.idle_default_fps 120
settings put global vendor.display.video_or_camera_fps.support true
settings put global vendor.fps.switch.defaul true
settings put global vendor.fps.switch.thermal true
settings put global vendor.display.disable_mitigated_fps 1
settings put global vendor.display.enable_dpps_dynamic_fps 0
#V5.0
# Mematikan fitur yang mengganggu performa
settings put global auto_sync 0
settings put global ble_scan_always_enabled 0
settings put global wifi_scan_always_enabled 0
settings put global hotword_detection_enabled 0
settings put global activity_starts_logging_enabled 0
settings put global network_recommendations_enabled 0
settings put secure adaptive_sleep 0
settings put secure screensaver_enabled 0
settings put secure send_action_app_error 0
settings put system motion_engine 0
settings put system master_motion 0
settings put system air_motion_engine 0
settings put system air_motion_wake_up 0
settings put system send_security_reports 0
settings put system intelligent_sleep_mode 0
settings put system nearby_scanning_enabled 0
settings put system nearby_scanning_permission_allowed 0
# Menonaktifkan layanan Qualcomm yang tidak diperlukan
pm disable com.qualcomm.qti.cne
pm disable com.qualcomm.location.XT
# Menonaktifkan pembatasan termal untuk performa maksimal
cmd thermalservice override-status 0
cmd power set-adaptive-power-saver-enabled false
cmd power set-fixed-performance-mode-enabled true
#V4.0 Infinity X
setprop debug.sf.gpu_freq_indeks 7
settings put global surface_flinger.max_frame_buffer_acquired_buffers 3 
settings put global surface_flinger.use_context_priority true
settings put global surface_flinger.set_touch_timer_ms 0
settings put global surface_flinger.use_content_detection_for_refresh_rate false
settings put global surface_flinger.game_default_frame_rate_override 90
settings put global surface_flinger.enable_frame_rate_override false
#New
setprop debug.performance.tuning 1
settings put global logcat.live disable
settings put global config hw_quickpoweron true
settings put system gsm.lte.ca.support 1
setprop debug.hwui.disable_scissor_opt true
settings put global hwui.texture_cache_size 24
settings put global hwui.texture_cache_flushrate 0.5
settings put global disable_smooth_effect true
setprop debug.composition.type mdp
settings put system sys.composition.type mdp
settings put system gpu_perf_mode 1
#Performa Infinity
settings put system FPSTUNER_SWITCH true
settings put system GPUTUNER_SWITCH true
settings put system CPUTUNER_SWITCH true
settings put system NV_POWERMODE true
setprop debug.gpurend.vsync false
setprop debug.cpurend.vsync false
settings put system hw.accelerated 1
settings put system video.accelerated 1
settings put system game.accelerated 1
settings put system ui.accelerated 1
settings put system enable_hardware_accelerated true
settings put system enable_optimize_refresh_rate true
settings put system lgospd.enable 0
settings put system pcsync.enable 0
settings put system dalvik.hyperthreading true
settings put system dalvik.multithread true
# Rendering and UI Optimizations
setprop debug.sf.disable_client_composition_cache 1
settings put debug.sf.latch_unsignaled 1
setprop debug.sf.disable_backpressure 1
settings put system use_16bpp_alpha 1
#MTK Infinity X
# MTK Performance Boosts
settings put global mtk_perf_fast_start_win1
settings put global mtk_perf_response_time 1
settings put global mtk_perf_simple_start_win 1
setprop debug.mediatek.appgamepq_compress 1
setprop debug.mediatek.disp_decompress 1
setprop debug.mtk_tflite.target_nnapi 29
setprop debug.mtk.aee.feature 1
setprop debug.mediatek.performance 1
setprop debug.mediatek.game_pq_enable 1
setprop debug.mediatek.appgamepq 2
setprop debug.mediatek.high_frame_rate_sf_set_big_core_fps_threshold 120
#InfinityX
settings put system user_refresh_rate 120
settings put system min_refresh_rate 120
settings put system peak_refresh_rate 120
settings put system user_refresh_rate infinity
settings get system user_refresh_rate
setprop debug.gfx.early_z 1
setprop debug.hwui.skip_empty_damage true
setprop debug.qctwa.preservebuf 1
setprop debug.qctwa.preservebuf.comp_level 3
setprop debug.qc.hardware 1
setprop debug.qcom.hw_hmp.min_fps -1
setprop debug.qcom.hw_hmp.max_fps -1
setprop debug.qcom.pil.q6_boost q
setprop debug.qcom.render_effect 0
setprop debug.adreno.force_rast 1
setprop debug.adreno.prefer_native_sync 1
setprop debug.adreno.q2d_decompress 1
setprop debug.rs.qcom.use_fast_math 1
setprop debug.rs.qcom.disable_expand 1
setprop debug.sf.hw 1
setprop debug.hwui.shadow.renderer monothic
setprop debug.gfx.driver.1 com.qualcomm.qti.gpudrivers.kona.api30
setprop debug.power_management_mode pref_max
setprop debug.gfx.driver 1
setprop debug.angle.overlay FPS:Vulkan*PipelineCache*
setprop debug.hwui.target_cpu_time_percent 300
setprop debug.hwui.target_gpu_time_percent 300
setprop debug.hwui.use_hint_manager true
setprop debug.multicore.processing 1
setprop debug.fb.rgb565 1
setprop debug.sf.lag_adj 0
setprop debug.sf.showfps 0
setprop debug.hwui.max_frame_time 35.55
setprop debug.sf.disable_backpressure 1
setprop debug.hbm.direct_render_pixmaps 1
setprop debug.hwui.render_compability true
setprop debug.heat_suppression 0
setprop debug.systemuicompilerfilter speed
setprop debug.sensor.hal 0
setprop debug.hwui.render_quality high
setprop debug.sf.gpu_freq_index 7
setprop debug.sf.cpu_freq_index 7
setprop debug.sf.mem_freq_index 7
setprop debug.egl.force_fxaa false
setprop debug.egl.force_taa false
setprop debug.egl.force_msaa false
setprop debug.egl.force_ssaa false
setprop debug.egl.force_smaa false
setprop debug.egl.force_mlaa false
setprop debug.egl.force_txaa false
setprop debug.egl.force_csaa false
setprop debug.hwui.fps_divisor -1
setprop debug.redroid.fps 120
setprop debug.disable_sched_boost true
setprop debug.gpu.cooling.callback_freq_limit false
setprop debug.cpu.cooling.callback_freq_limit false
setprop debug.rs.default-CPU-driver 1
setprop debug.rs.default-CPU-buffer 65536
setprop debug.hwui.use_hint_manager 1
setprop debug.egl.profiler 0
setprop debug.enable.gamed false
setprop debug.qualcomm.sns.daemon 0
setprop debug.qualcomm.sns.libsensor 1
setprop debug.sf.disable_client_composition_cache 1
setprop debug.sf.disable_client_composition_cache 1
setprop debug.sf.disable_hw_vsync true
setprop debug.hwui.disable_vsync true
setprop debug.egl.hw 1
setprop debug.sf.native_mode 1
setprop debug.gralloc.gfx_ubwc_disable 1
setprop debug.video.accelerate.hw 1
#InfinityCmd
cmd looper_stats disable
cmd power set-adaptive-power-saver-enabled false
cmd power set-fixed-performance-mode-enabled true
cmd power set-mode 0
cmd thermalservice override-status 0
dumpsys deviceidle enable
dumpsys deviceidle force-idle
dumpsys deviceidle step deep
) > /dev/null 2>&1
(
# Optimize Refresh Rate
settings put global surface_flinger.use_content_detection_for_refresh_rate false
settings put global media.recorder-max-base-layer-fps 120
settings put global vendor.fps.switch.default true
settings put system vendor.disable_idle_fps true
settings put global vendor.display.default_fps 120
settings put system vendor.display.idle_default_fps 120
settings put system vendor.display.enable_optimize_refresh 1
settings put system vendor.display.video_or_camera_fps.support true
setprop debug.hwui.refresh_rate 120
setprop debug.sf.set_idle_timer_ms 500
setprop debug.sf.latch_unsignaled 1
setprop debug.sf.high_fps_early_phase_offset_ns 2000000
setprop debug.sf.high_fps_late_app_phase_offset_ns 500000
settings put system game_driver_min_frame_rate  120
settings put system game_driver_max_frame_rate  120
settings put system game_driver_power_saving_mode 0
settings put system game_driver_frame_skip_enable 0
settings put system game_driver_vsync_enable 0
settings put system game_driver_gpu_mode 1
settings put system game_driver_gpu_mode 1
settings put system game_driver_fps_limit 120
) > /dev/null 2>&1 &

(
#Fps Injector 
setprop debug.graphics.game_default_frame_rate 120
setprop debug.graphics.game_default_frame_rate.disabled false
setprop persist.sys.gpu_perf_mode 1
setprop debug.mtk.powerhal.hint.bypass 1
setprop persist.sys.surfaceflinger.idle_reduce_framerate_enable false
setprop sys.surfaceflinger.idle_reduce_framerate_enable false
setprop debug.sf.perf_mode 1
settings put global refresh.active 1
setprop debug.hwui.disable_vsync true
setprop debug.performance.profile 1
setprop debug.perf.tuning 1
) > /dev/null 2>&1 &
(
#New Tweak Fps Lock
settings put system user_refresh_rate 120
settings put system fps_limit 120
settings put system max_refresh_rate_for_ui 120
settings put system hwui_refresh_rate 120
settings put system display_refresh_rate 120
settings put system max_refresh_rate_for_gaming 120
#Mengatur margin Fps
settings put system fstb_target_fps_margin_high_fps 20
settings put system fstb_target_fps_margin_low_fps 20
settings put system gcc_fps_margin 10
#remove Refresh rate
settings put system tran_low_battery_60hz_refresh_rate.support 0
# Lock refresh rate to 120 Hz
settings put system vendor.display.refresh_rate 120
settings put system user_refresh_rate 1
settings put system sf.refresh_rate120
settings put secure user_refresh_rate 1
settings put secure miui_refresh_rate 120
settings put system min_frame_rate 120
settings put system max_frame_rate 120
settings put system tran_refresh_mode 120
settings put system last_tran_refresh_mode_in_refresh_setting 120
settings put global min_fps 120
settings put global max_fps 120
settings put system tran_need_recovery_refresh_mode 120
settings put system display_min_refresh_rate 120
settings put system min_refresh_rate 120
settings put system max_refresh_rate 120
settings put system peak_refresh_rate 120
settings put secure refresh_rate_mode 120
settings put system thermal_limit_refresh_rate 120
settings put system NV_FPSLIMIT 120
settings put system fps.limit.is.now locked
) > /dev/null 2>&1 &
(
for sensor in cpu0 gpu0 npu0 apu0 dsp0 tpu0 vpu0 isp0 spu0 dpu0 pim0 skin pmic0 ddr0 ufs0 modem0 battery
do
    type=$(echo $sensor | tr a-z A-Z)
    cmd thermalservice inject-temperature $type light $sensor 120000
done
) > /dev/null 2>&1
(
    cmd power set-fixed-performance-mode-enabled true
    settings put system POWER_PERFORMANCE_MODE_OPEN 1
    cmd thermalservice override-status 0
    setprop debug.performance.tuning 1
    setprop debug.sf.perf_mode 1
    setprop debug.thermal.cpu_thermal_throttle.disable 1
    setprop debug.power.throttling.disable 1
    setprop persist.sys.gpu_perf_mode 1
    setprop sys.force_boost_cpu true
    setprop debug.thermal.shutdown.disable 1
    # spoof suhu
    cmd thermalservice inject-temperature CPU light cpu0 120.000
    cmd thermalservice inject-temperature GPU light gpu0 120.000
    setprop debug.gpu.thermal.temp 150
    settings put system battery.temp_high 90
    settings put global vendor.dfps.enable false
    settings put global vendor.smart_dfps.enable false
    settings put system virtual_thermal_thermal_zone false
    settings put global thermal_throttling 0
    settings put global thermal_pwrlevel 0
    #Disabled Thermal No Root (Gimick)
    setprop debug.init.svc.thermald stopped
    setprop debug.init.svc_debug_pid.vendor.thermal-hal-2-0.mtk stopped
    setprop debug.init.svc.thermal_manager stopped
    setprop debug.init.svc.thermal_mnt_hal_service stopped
    setprop debug.init.svc.thermal-engine stopped
    setprop debug.init.svc.vendor.thermal-hal-2-0.mtk stopped
    setprop debug.init.svc.thermal_core stopped
    setprop debug.ro.boottime.thermal_core stopped
    setprop debug.ro.boottime.vendor.thermal-hal-2-0.mtk stopped
    setprop debug.ro.vendor.mtk_thermal_2_0 stopped
    setprop debug.ro.boottime.thermal_core stopped
    setprop debug.ro.boottime.thermald stopped
    setprop debug.ro.boottime.vendor.thermal-hal-2-0.mtk stopped
    setprop debug.ro.vendor.mtk_thermal_2_0 stopped
    #Ultra Gaming Thermal Tuning (Non Root)
settings put system bench_mark_mode 0
setprop debug.thermal_status 0
setprop debug.performance.tuning 3
setprop debug.thermal.cpu_thermal_throttle.disable 1
setprop debug.thermal.ambient_sensor.disable 1
setprop debug.cooling_name_thermal-devfreq 0
setprop debug.pid.sec-thermal-1-0 disabled
setprop debug.thermal_zone.display_hotplug_control 0
setprop debug.thermal_zone.battery_hotplug_control 0
setprop debug.mediatek.appgamepq_compress 0
setprop debug.mediatek.disp_decompress 2
setprop debug.mtk_tflite.target_nnapi 31
setprop debug.thermal_zone.gpu_threshold_temp 95
setprop debug.thermal_zone.cpu_threshold_temp 90
setprop debug.thermal_zone.display_threshold_temp 85
setprop debug.thermal_zone.camera_hotplug_control 0
setprop debug.thermal_zone.battery_threshold_temp 75
setprop debug.thermal_zone.camera_threshold_temp 90
setprop debug.thermal_zone.cpu_hotplug_control 0
setprop debug.thermal_zone.gpu_hotplug_control 0
setprop debug.power.throttling.disable 1
setprop debug.thermal.gpu_shader_clock_throttle.disable 1
setprop debug.thermal.gpu_core_clock_throttle.disable 1
setprop debug.thermal.gpu_power_throttle.disable 1
setprop debug.thermal.gpu_thermal_throttle.disable 1
setprop debug.thermal.gpu_memory_throttle.disable 1
setprop debug.thermal.gpu_fan_control.disable 1
setprop debug.thermal.gpu_boost.disable 0
setprop debug.thermal.gpu_control.disable 1
setprop debug.thermal.gpu_throttle.disabled 1
setprop debug.thermal.backlight.disabled 1
setprop debug.thermal.boost.disabled 0
setprop debug.thermal.inactive_delay.disabled 1
setprop debug.thermal.throttling.disable 1
setprop debug.thermal.profile.disable 1
setprop debug.thermal.throttle_ratio.disable 1
setprop debug.thermal.turbo_ratio_limit.disable 1
setprop debug.thermal.cooling_device_state.disable 1
setprop debug.thermal.dynamic_scheduling.disable 1
setprop debug.thermal.critical_temp.disable 1
setprop debug.thermal.threshold.disable 1
setprop debug.thermal.overheat_protection.disable 1
setprop debug.thermal.alert.disable 1
setprop debug.thermal.fan.disable 1
setprop debug.thermal.shutdown.disable 1
setprop debug.thermal.balance_algorithm -1
setprop debug.thermal.performance_mode.disable 1
setprop debug.thermal.force_fan_on.disable 0
setprop debug.thermal.critical_trip_point.disable 1
setprop debug.thermal.auto_thermal_disable 1
setprop debug.thermal.zone.disabled 1
setprop debug.thermal.trip_point.disabled 1
setprop debug.thermal.suspend.disabled 1
setprop debug.thermal.thermal_policy.disable 1
setprop debug.thermal.fan_disable 1
#Peformance Stability
setprop debug.performance.tuning 1 
setprop debug.egl.force_msaa false
setprop debug.hwui.use_gpu_pixel_buffers 1
setprop debug.hwui.target_cpu_time_percent 10
setprop debug.hwui.render_dirty_regions false
setprop debug.hwui.disable_vsync true
setprop debug.hwui.level 0
setprop debug.kill_allocating_task 0
setprop debug.gralloc.gfx_ubwc_disable 0
setprop debug.rs.default-CPU-driver 1
setprop debug.rs.forcecompat 1
setprop debug.rs.max-threads 8
setprop debug.choreographer.skipwarning 30
setprop debug.choreographer.frametime false
setprop debug.display.allow_non_native_refresh_rate_override 1
setprop debug.display.render_frame_rate_is_physical_refresh_rate 1
setprop debug.sf.use_phase_offsets_as_durations 1
setprop debug.sf.predict_hwc_composition_strategy 0
setprop debug.sf.enable_transaction_tracing false
setprop debug.sf.disable_client_composition_cache 1
setprop debug.sf.gpu_freq_indeks 7
setprop debug.sf.use_frame_rate_priority 1
setprop debug.sf.disable_backpressure 1
setprop debug.sf.enable_gl_backpressure 1
setprop debug.atrace.tags.enableflags 0
setprop debug.cpurend.vsync false
setprop debug.composition.type gpu
setprop debug.checkjni 0
setprop debug.atrace.tags.enableflags 0
setprop debug.gr.numframebuffers 3
) > /dev/null 2>&1
(
#Thermal Unlock
setprop debug.sys.thermal.level 0
setprop debug.sys.thermal.protection 0
setprop debug.sys.thermal.enable_detailed 0
) > /dev/null 2>&1
(
#V4.0
settings put global cpu_boost true
settings put global performance_mode 1
settings put global game_mode 2
settings put global settings_enable_monitor_phantom_procs false
settings put global gpu_debug_layers 1
settings put global force_gpu_rendering 1
) > /dev/null 2>&1

(
#New
settings put system gpu_perf_mode 1
setprop debug.sf.gpu_freq_indeks 7
settings put system GPUTUNER_SWITCH true
setprop debug.gpurend.vsync false
setprop debug.gfx.driver.1 com.qualcomm.qti.gpudrivers.kona.api30
setprop debug.hwui.target_gpu_time_percent 300
setprop debug.gpu.cooling.callback_freq_limit false
settings put system enable_triple_buffering 1
settings put system swappiness 60
settings put system gpu.use_extended_cache true
settings put system gpu.prefetch_threshold 1
setprop debug.hwui.render_thread boost
setprop debug.hwui.raster_thread 4
settings put system gpu_force_on 1
settings put system gpu_force_render true
) > /dev/null 2>&1

(
#Optimize GPU Mali 
settings put global mali.debug_level 0
settings put global mali.use_l2_cache 1
settings put global mali.dynamic_power 1
) > /dev/null 2>&1

(
#GPU PERF TWEAK V2.0
settings put system ged_smart_boost 500
settings put system boost_upper_bound  write_value 80
settings put system gx_game_mode 1
settings put system enable_game_self_frc_detect 1
settings put system ged_boost_enable 1
settings put system gx_boost_on 1
settings put system boost_gpu_enable 1
settings put system enable_gpu_boost 1
settings put system ged_dvfs_enable 1
settings put system boost_amp 1
settings put system boost_extra 0
settings put system gx_3D_benchmark_on 0 
settings put system gpu_idle 0
) > /dev/null 2>&1

# GPU Perf TWEAK 
(
setprop debug.composition.type gpu
setprop debug.enabletr true
setprop debug.overlayui.enable 1
setprop debug.performance.tuning 1
setprop debug.hw3d.force 1
setprop debug.hw2d.force 1
setprop debug.hwui.disable_vsync true
settings put system composition.type gpu
settings put system sys.ui.hw 1
) > /dev/null 2>&1

# Enable GPU Mali optimizations
(
settings put global gpu.optimize.level 3
settings put global gpu.optimize.load_level 1
settings put global gpu.optimize.driver_version 2
settings put global gpu.optimize.preload 1
settings put global gpu.optimize.purgeable_limit 64
settings put global gpu.optimize.retry_max 4
settings put global gpu.optimize.texture_control true
settings put global gpu.optimize.memory_compaction true
settings put global gpu.optimize.hires_preload true
settings put global gpu.optimize.fork_detector true
settings put global gpu.optimize.fork_detector_threshold 10
settings put global gpu.optimize.max_job_count 2
settings put global gpu.optimize.max_target_duration 15
settings put global gpu.optimize.min_target_size 100
) > /dev/null 2>&1

#Optimalisasi Frekuensi GPU
(
settings put global persist.sys.gpu_rendering_mode 1
settings put global persist.sys.enable_gpu_boost true
settings put global persist.vendor.gpu_boost_mode 1
settings put global gpu.tuning.performance_mode 1
) > /dev/null 2>&1

#Confiogurasi GPU Adreno
( 
setprop debug.qti.config.zram true
settings put global config.disable.hw_accel false
settings put global product.gpu.driver 1
settings put global sf.compbypass.enable 0
settings put system video.accelerate.hw 1
setprop debug.pm.dyn_samplingrate 1
setprop debug.com.qc.hardware 1
setprop debug.qc.hardware true
setprop debug.qctwa.preservebuf 1
setprop debug.qctwa.statusbar 1
settings put system dev.pm.dyn_samplingrate1
settings put global config.enable.hw_accel true
) > /dev/null 2>&1
device_config put game_overlay com.activision.callofduty.shooter mode=2,fps=120:mode=3,fps=60
cmd game mode performance com.activision.callofduty.shooter 2>&1 >/dev/null
{
	setprop debug.composition.type gpu
	setprop debug.composition.type c2d
	setprop debug.hwui.renderer angle
	setprop debug.gr.swapinterval 60
	setprop debug.gr.numframebuffers 3
	setprop debug.egl.buffcount 4
	setprop debug.egl.force_msaa 1
	setprop debug.cpurend.vsync false	
	setprop debug.enabletr true
	setprop debug.egl.hw 1
	setprop debug.gralloc.gfx_ubwc_disable 0
	setprop debug.mdpcomp.logs 0
	setprop debug.performance.tuning 1
	setprop debug.sf.hw 1
	setprop debug.gr.swapinterval 1
	setprop debug.egl.swapinterval 1
	setprop debug.renderengine.backend opengles
	setprop debug.renderengine.backend vulkanthreaded
	setprop debug.angle.overlay FPS:vulkan*PipelineCache*
	setprop debug.javafx.animation.framerate 120
	setprop debug.systemuicompilerfilter speed
	setprop debug.app.performance_restricted false
	setprop debug.gr.numframebuffers 3
	setprop debug.egl.buffcount 4
	setprop debug.sf.set_idle_timer_ms 30
	setprop debug.sf.disable_backpressure 1
	setprop debug.sf.latch_unsignaled 1
	setprop debug.sf.enable_hwc_vds 1
	setprop debug.sf.early_phase_offset_ns 500000
	setprop debug.sf.early_app_phase_offset_ns 500000
	setprop debug.sf.early_gl_phase_offset_ns 3000000
	setprop debug.sf.early_gl_app_phase_offset_ns 15000000
	setprop debug.sf.high_fps_early_phase_offset_ns 6100000
	setprop debug.sf.high_fps_late_sf_phase_offset_ns 8000000
	setprop debug.sf.high_fps_early_gl_phase_offset_ns 9000000
	setprop debug.sf.high_fps_late_app_phase_offset_ns 1000000
	setprop debug.sf.high_fps_late_sf_phase_offset_ns 8000000
	setprop debug.sf.high_fps_early_gl_phase_offset_ns 9000000
	setprop debug.sf.phase_offset_threshold_for_next_vsync_ns 6100000	
	setprop debug.sf.showfps 0
	setprop debug.sf.showcpu 0
	setprop debug.sf.showbackground 0
	setprop debug.sf.shoupdates 0
	} > /dev/null 2>&1
	dumpsys deviceidle force-idle  > /dev/null 2>&1
{
	setprop debug.enable-vr-mode 1
	setprop debug.force-opengl 1
	setprop debug.hwc.force_gpu_vsync 1
	setprop debug.performance.profile 1
	} > /dev/null 2>&1

	cmd thermalservice override-status 0 > /dev/null 2>&1
	{
	settings put global power_check_max_cpu_1 75
	settings put global power_check_max_cpu_2 75
	settings put global power_check_max_cpu_3 50
	settings put global power_check_max_cpu_4 50
	settings put global always_finish_activities 1
	settings put secure game_auto_tempature 0
	settings put global sem_enhanced_cpu_responsiveness 1
	settings put global animator_duration_scale 0.0
	settings put global transition_animation_scale 0.0
	settings put global window_animation_scale 0.0
	settings put global app_standby_enabled 0
	} > /dev/null 2>&1
	{
	settings put global mobile_data_always_on 1
	settings put global wifi_scan_always_enabled 0
	} > /dev/null 2>&1
	{
	settings put system k2hd_effect 1
	settings put system tube_amp_effect 1
	} > /dev/null 2>&1
	{
	settings put secure tap_duration_threshold 0.0
	settings put secure touch_blocking_period 0.0
	} > /dev/null 2>&1
	pm trim-caches 64G > /dev/null 2>&1
	{
    setprop debug.composition.type gpu
    setprop debug.composition.type c2d
    setprop debug.hwui.renderer skiagl
    setprop debug.gr.swapinterval -60
    setprop debug.gr.numframebuffers 3
    setprop debug.egl.buffcount 4
    setprop debug.egl.force_msaa 1
    setprop debug.cpurend.vsync false
    setprop debug.enabletr true
    setprop debug.overlayui.enable 1
    setprop debug.egl.hw 0
    setprop debug.gralloc.gfx_ubwc_disable 0
    setprop debug.mdpcomp.logs 0
    setprop debug.egl.hw 1
    setprop debug.egl.profiler 0
    setprop debug.performance.tuning 1
    setprop debug.sf.hw 1
    setprop debug.egl.swapinterval -60
    setprop debug.renderengine.backend skiagl
    setprop debug.renderengine.backend skiaglthreaded
    setprop debug.angle.overlay FPS:skiagl*PipelineCache*
    setprop debug.javafx.animation.framerate 120
    setprop debug.systemuicompilerfilter speed
    setprop debug.app.performance_restricted false
    setprop debug.sf.set_idle_timer_ms 30
    setprop debug.sf.disable_backpressure 1
    setprop debug.sf.latch_unsignaled 1
    setprop debug.sf.enable_hwc_vds 1
    setprop debug.sf.early_phase_offset_ns 500000
    setprop debug.sf.early_app_phase_offset_ns 500000
    setprop debug.sf.early_gl_phase_offset_ns 3000000
    setprop debug.sf.early_gl_app_phase_offset_ns 15000000
    setprop debug.sf.high_fps_early_phase_offset_ns 6100000
    setprop debug.sf.high_fps_late_sf_phase_offset_ns 8000000
    setprop debug.sf.high_fps_early_gl_phase_offset_ns 9000000
    setprop debug.sf.high_fps_late_app_phase_offset_ns 1000000
    setprop debug.sf.high_fps_late_sf_phase_offset_ns 8000000
    setprop debug.sf.high_fps_early_gl_phase_offset_ns 9000000
    setprop debug.sf.phase_offset_threshold_for_next_vsync_ns 6100000
    setprop debug.sf.showfps 0
    setprop debug.sf.showcpu 0
    setprop debug.sf.showbackground 0
    setprop debug.sf.shoupdates 0
} > /dev/null 2>&1
{
    dumpsys deviceidle force-idle
    settings put global battery_tip_constans app_restriction_enabled=false
    settings put global power_check_max_cpu_1 75
    settings put global power_check_max_cpu_2 75
    settings put global power_check_max_cpu_3 50
    settings put global power_check_max_cpu_4 50
    device_config put activity_manager_native_boot use_freezer true
    settings put system pointer_speed 7
    settings put global cached_apps_freezer 1
    settings put system peak_refresh_rate 120.0
    settings put system min_refresh_rate 120.0
    settings put system user_refresh_rate 120.0
    settings put global activity_manager_constants max_cached_processes 3023 
    cmd thermalservice override-status 0
    cmd power set-fixed-performance-mode-enabled true
} > /dev/null 2>&1
        device_config put game_overlay com.activision.callofduty.shooter mode=2,skiagl=1,downscaleFactor=0.4,fps=120:mode=3,skiagl=0,downscaleFactor=0.4,fps=120
        cmd game mode performance com.activision.callofduty.shooter
        settings delete global updatable_driver_production_opt_in_apps
        settings delete global game_driver_opt_in_apps
        settings delete global updatable_driver_production_opt_out_apps
        settings delete global updatable_driver_prerelease_opt_in_apps
        settings put global updatable_driver_production_opt_in_apps "com.activision.callofduty.shooter"
echo "Done"
