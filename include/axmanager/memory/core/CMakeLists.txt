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

# پیدا کردن کتابخانه‌های سیستمی اندروید
find_library(log-lib log)

# =================================================================
# 1. INCLUDE DIRECTORIES (نقشه راه دسترسی به کدها)
# =================================================================
include_directories(
    include
    include/axmanager
    include/axmanager/core
    include/axmanager/memory
    include/axmanager/memory/core
)

# =================================================================
# 2. SOURCE FILES (لیست تمام اعضای خانواده AxManager)
# =================================================================

set(CORE_SOURCES
    # --- ماژول مدیریت محیط (Environment Simulator) ---
    include/axmanager/core/EnvironmentSimulator.cpp
    
    # --- ماژول مدیریت سخت‌افزار و فرکانس (SoC Tuner) ---
    include/axmanager/core/SoCTuner.cpp
    
    # --- ماژول مدیریت ورودی و حذف تأخیر (Input Response Engine) ---
    include/axmanager/core/InputResponseEngine.cpp
    
    # --- ماژول محافظتی (Anti-Cheat Shield) - آماده برای اضافه شدن ---
    # include/axmanager/core/AntiCheatShield.cpp 
)

set(MEMORY_SOURCES
    # --- ماژول اصلی حافظه سایه (Shadow Memory Engine) ---
    include/axmanager/memory/core/ShadowMemoryEngine.cpp
)

# ترکیب تمام منابع در یک لیست واحد
set(ALL_AX_SOURCES
    ${CORE_SOURCES}
    ${MEMORY_SOURCES}
)

# =================================================================
# 3. BUILD TARGET (ساخت هسته اصلی AxManager)
# =================================================================

add_library(
    axmanager_core
    SHARED
    ${ALL_AX_SOURCES}
)

# لینک کردن کتابخانه‌های مورد نیاز (مانند سیستم لاگ اندروید)
target_link_libraries(
    axmanager_core
    ${log-lib}
)

# =================================================================
# 4. FINAL CONFIRMATION
# =================================================================
message(STATUS "--------------------------------------------------")
message(STATUS " AxManager Build Configuration Loaded Successfully")
message(STATUS " Status: Ready for Meta-Reality Evolution")
message(STATUS " Modules: [Memory, Env, SoC, Input] - INTEGRATED")
message(STATUS "--------------------------------------------------")
