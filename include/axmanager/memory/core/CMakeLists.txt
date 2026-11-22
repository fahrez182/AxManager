cmake_minimum_required(VERSION 3.10)

project(AxManager)

# تنظیم نسخه C++ به 17 یا بالاتر برای پشتیبانی از ویژگی‌های جدید
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# پیدا کردن کتابخانه لاگ اندروید
find_library(log-lib log)

# تعریف مسیرهای اصلی (Include Directories)
include_directories(
    include
    include/axmanager
    include/axmanager/core
    include/axmanager/memory
    include/axmanager/memory/core
)

# لیست تمام فایل‌های منبع (Source Files)
# من تمام فایل‌هایی که تا الان ساختیم را اینجا اضافه کردم
set(SOURCE_FILES
    # ماژول‌های اصلی (Core Modules)
    include/axmanager/core/EnvironmentSimulator.cpp
    include/axmanager/core/SoCTuner.cpp
    
    # ماژول‌های حافظه (Memory Modules)
    include/axmanager/memory/core/ShadowMemoryEngine.cpp
    # اگر فایل‌های هدر با پسوند .cpp داری اینجا اضافه کن
)

# ساخت کتابخانه اصلی AxManager
add_library(
    axmanager_core
    SHARED
    ${SOURCE_FILES}
)

# لینک کردن کتابخانه‌های مورد نیاز به AxManager
target_link_libraries(
    axmanager_core
    ${log-lib}
)

# پیام برای تایید در هنگام Build
message(STATUS "AxManager: All modules (Memory, Env, SoC) have been integrated successfully.")
