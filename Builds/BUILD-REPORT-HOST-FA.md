# گزارش خروجی AxElden

## خروجی کامپایل‌شده میزبان x86_64
- `host-x86_64/axelden_live.o`
- `host-x86_64/axelden_monitor.o`
- `host-x86_64/libaxelden_live_host_x86_64.so`
- `host-x86_64/libaxelden_monitor_host_x86_64.so`

## خروجی اسمبلی x86_64
- `host-x86_64/axelden_live_x86_64.s`
- `host-x86_64/axelden_monitor_x86_64.s`

## سورس هدف Android ARM64
- `android-arm64-source/axelden_live.cpp`
- `android-arm64-source/axelden_monitor.cpp`
- `android-arm64-source/BUILD-ANDROID-ARM64.md`

کامپایل میزبان با Clang 17، C++17 و `-O2` انجام شد. نوع فایل‌های ELF و اسمبلی بررسی شد و نمادهای JNI در کتابخانه‌ها بررسی شدند.

محدودیت مهم: این محیط Android NDK و sysroot اندروید ندارد؛ بنابراین خروجی ARM64 اندروید، APK و تست روی گوشی ساخته/تأیید نشده‌اند. فایل‌های x86_64 فقط برای میزبان x86_64 هستند و نباید روی گوشی ARM64 نصب شوند.

این کدها فقط خواندن telemetry سیستم و APIهای مجاز را انجام می‌دهند؛ هیچ hook/injection یا تغییر حافظه Call of Duty ندارند.
