cmake_minimum_required(VERSION 3.10)

# نام پروژه
project(AxManagerProject VERSION 1.0.0 LANGUAGES CXX)

# تنظیم استاندارد C++ (بسیار مهم برای کدهای سطح پایین)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# تنظیمات بهینه‌سازی برای سرعت حداکثری (Performance Priority)
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -ffast-math -flto")
set(CMAKE_CXX_FLAGS_DEBUG "-g -O0")

# مشخص کردن محل فایل‌های هدر (Include Directories)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

# --- لیست فایل‌های سورس (Sources) ---
# من تمام ماژول‌هایی که تا الان ساختیم را اینجا لیست می‌کنم

# 1. Core Engines (محیط، سخت‌افزار، ورودی، آنتی‌چیت)
set(CORE_SOURCES
    main.cpp
    include/axmanager/core/EnvironmentSimulator.cpp
    include/axmanager/core/SoCTuner.cpp
    include/axmanager/core/InputResponseEngine.cpp
    include/axmanager/core/AntiCheatShield.cpp
)

# 2. Memory Management & Advanced Modules (حافظه، اسکنر، هوک)
set(MEMORY_SOURCES
    include/axmanager/memory/core/ShadowMemoryEngine.cpp
    include/axmanager/memory/core/PatternScanner.cpp
    include/axmanager/memory/core/InlineHookEngine.cpp
)

# ترکیب تمام سورس‌ها
set(ALL_SOURCES ${CORE_SOURCES} ${MEMORY_SOURCES})

# --- ساخت کتابخانه اصلی (Shared Library) ---
# این فایل اصلی است که سایر اپلیکیشن‌ها یا موتورهای بازی به آن نیاز دارند
add_library(axmanager_core SHARED ${ALL_SOURCES})

# لینک کردن کتابخانه‌های سیستم اندروید و کتابخانه‌های ضروری
target_link_libraries(axmanager_core
    log            # برای استفاده از __android_log_print
    android        # برای دسترسی‌های سطح سیستم اندروید
    m              # کتابخانه ریاضی (Math library)
)

# تنظیمات اضافی برای معماری ARM64 (بسیار مهم برای بازی‌ها)
if(ANDROID)
    set_target_properties(axmanager_core PROPERTIES LINK_FLAGS "-Wl,--export-dynamic")
endif()

# نمایش پیام موفقیت در هنگام کامپایل
message(STATUS "AxManager Core Library Configuration Loaded Successfully!")
message(STATUS "Total Source Files Linked: ${ALL_SOURCES}")
