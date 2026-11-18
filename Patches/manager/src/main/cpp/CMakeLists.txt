cmake_minimum_required(VERSION 3.22.1)

project(ax_turbo)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(emu_speed SHARED EmuTurbo.cpp)

add_library(ram_boost SHARED RamTurbo.cpp)

add_library(cpu_power SHARED CpuTurbo.cpp)

add_library(io_turbo SHARED IoTurbo.cpp)

add_library(net_turbo SHARED NetTurbo.cpp)

add_library(gfx_turbo SHARED GfxTurbo.cpp)

target_link_libraries(emu_speed log android)

target_link_libraries(ram_boost log android)

target_link_libraries(cpu_power log android)

target_link_libraries(io_turbo log android)

target_link_libraries(net_turbo log android)

target_link_libraries(gfx_turbo log android)

# AxElden LiveNative: read-only device telemetry exposed to the manager through JNI.
add_library(axelden SHARED axelden_live.cpp)
target_link_libraries(axelden log android)

# Additive read-only diagnostics target; retain the existing axelden library.
add_library(axelden_monitor SHARED axelden_monitor.cpp)
target_link_libraries(axelden_monitor log android)

# AxElden Android ARM64 release tuning. These flags apply only to AxElden's
# read-only telemetry libraries; existing project targets above remain intact.
foreach(axelden_target IN ITEMS axelden axelden_monitor)
    target_compile_options(${axelden_target} PRIVATE
        -O2
        -fvisibility=hidden
        -ffunction-sections
        -fdata-sections
        -fstack-protector-strong
        -D_FORTIFY_SOURCE=2
    )
    target_link_options(${axelden_target} PRIVATE
        -Wl,--gc-sections
        -Wl,-z,relro
        -Wl,-z,now
    )
    set_target_properties(${axelden_target} PROPERTIES
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN YES
    )
endforeach()
