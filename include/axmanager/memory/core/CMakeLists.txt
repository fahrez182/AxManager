cmake_minimum_required(VERSION 3.10)

# =================================================================
# PROJECT: AxManager 
# STATUS: High-Performance Sovereign System
# GOAL: Meta-Reality & Advanced Memory Spoofing
# =================================================================

project(AxManager)

# تنظیم استاندارد C++ برای استفاده از قابلیت‌های پیشرفته ریاضی و سیستم
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# پیدا کردن کتابخانه لاگ اندروید برای نمایش پیام‌های ما در Logcat
find_library(log-lib log)

# =================================================================
# 1. INCLUDE DIRECTORIES (نقشه راه دسترسی به تمام کدها)
# =================================================================
include_directories(
    include
    include/axmanager
    include/axmanager/core
    include/axmanager/memory
    include/axmanager/memory/core
)

# =================================================================
# 2. SOURCE FILES (لیست تمام قطعاتی که تا الان ساخته‌ایم)
# =================================================================

# --- بخش هسته اصلی (Core Modules: Env, SoC, Input, Shield) ---
set(CORE_SOURCES
    include/axmanager/core/EnvironmentSimulator.cpp
    include/axmanager/core/SoCTuner.cpp
    include/axmanager/core/InputResponseEngine.cpp
    include/axmanager/core/AntiCheatShield.cpp
)

# --- بخش مدیریت حافظه (Memory Modules: Shadow Memory) ---
set(MEMORY_SOURCES
    include/axmanager/memory/core/ShadowMemoryEngine.cpp
    include/axmanager/memory/core/include/axmanager/memory/core/PatternScanner.cpp
    include/axmanager/memory/core/InlineHookEngine.cpp
)

# ترکیب تمام ماژول‌ها در یک لیست واحد برای ساخت کتابخانه اصلی
set(ALL_AX_SOURCES
    ${CORE_SOURCES}
    ${MEMORY_SOURCES}
)

# =================================================================
# 3. BUILD TARGET (ساخت کتابخانه اصلی AxManager)
# =================================================================

add_library(
    axmanager_core
    SHARED
    ${ALL_AX_SOURCES}
)

# لینک کردن کتابخانه لاگ اندروید به پروژه
target_link_libraries(
    axmanager_core
    ${log-lib}
)

# =================================================================
# 4. FINAL CONFIRMATION (تایید نهایی در هنگام کامپایل)
# =================================================================
message(STATUS "--------------------------------------------------")
message(STATUS " AxManager Build Configuration Loaded Successfully")
message(STATUS " Status: Ready for Meta-Reality Evolution")
message(STATUS " Modules: [Memory, Env, SoC, Input, Shield] - INTEGRATED")
message(STATUS "--------------------------------------------------")
