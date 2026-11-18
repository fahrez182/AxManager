# ساخت AxElden برای Android ARM64

## ادغام انجام‌شده
- سه فایل سنسور به مسیر اصلی Kotlin پروژه v22 اضافه شده‌اند.
- CMake هدف‌های قبلی را نگه می‌دارد و فقط برای `axelden` و `axelden_monitor` گزینه‌های بهینه‌سازی و hardening اضافه می‌کند.
- Gradle ABI موردنیاز AxElden را به `arm64-v8a` محدود می‌کند؛ NDK version پروژه همچنان `29.0.14206865` است.
- فایل `AxEldenSensorMonitor.kt` خطای callback را مهار می‌کند تا exception رابط کاربری رشته callback سنسور را متوقف نکند.

## ساخت روی Linux/macOS با Android SDK نصب‌شده
```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export ANDROID_NDK_HOME="$ANDROID_SDK_ROOT/ndk/29.0.14206865"
./tools/axelden/build-arm64.sh
```

یا در Android Studio نسخه NDK `29.0.14206865` را نصب و task زیر را اجرا کنید:
```bash
./gradlew :manager:assembleDebug
```

## خروجی مورد انتظار
فایل‌های `.so` مخصوص ARM64 در مسیرهای intermediates مربوط به CMake و سپس داخل APK در مسیر `lib/arm64-v8a/` قرار می‌گیرند. فایل APK در `manager/build/outputs/apk/debug/` ساخته می‌شود.

## محدودیت اعتبارسنجی این بسته
محیط آماده‌سازی فعلی Android NDK و Android SDK ندارد؛ بنابراین این بسته هنوز با NDK کامپایل نشده و APK/کتابخانه `arm64-v8a` خروجی تأییدشده محسوب نمی‌شود. اسکریپت بالا با بررسی وجود SDK/NDK متوقف می‌شود و خروجی ساخت جعلی تولید نمی‌کند.

این پروژه فقط سنسورهای اندروید و تله‌متری مجاز دستگاه و خود AxManager را می‌خواند. به حافظه یا توابع داخلی Call of Duty وصل نمی‌شود و چیزی را در فرایند بازی تزریق یا تغییر نمی‌دهد.
