cmake_minimum_required(VERSION 3.22.1)

project(ax_turbo)

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
