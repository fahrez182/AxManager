package frb.axeron.manager.api

/**
 * ماژول تخصصی تزریق تنظیمات گرافیکی و فریم‌ریت
 * این ماژول از تکنیک "System Parameter Injection" استفاده می‌کند تا هیچ ردپایی در بازی نماند.
 */
class GameGraphicsModule(private val injector: AxBladeInjector) {

    /**
     * فعال‌سازی حالت Ultra Performance (برای ۱۲۰ فریم و کیفیت MAX)
     * @param packageName نام پکیج بازی (مثلاً برای کالاف)
     */
    suspend fun enableUltraPerformance(packageName: String): InjectionResult {
        
        // ما دستورات را به صورت یک زنجیره (Chain) می‌سازیم تا در یک لحظه اعمال شوند
        // ۱. اجبار سیستم به استفاده
        // ۱. اجبار سیستم به استفاده از حداکثر نرخ نوسازی صفحه (Peak Refresh Rate)
        // ۲. تنظیمات گرافیکی سیستمی برای کارت گرافیک (GPU Optimization)
        // ۳. تزریق پارامترهای اجرایی بازی برای جلوگیری از افت فریم در بتل رویال و مولتی‌پلیر
        
        val commandPayload = listOf(
            // قفل کردن روی بالاترین رفرش‌ریت صفحه (120Hz / 144Hz)
            "settings put system peak_refresh_rate 144.0",
            "settings put system min_refresh_rate 144.0",
            
            // بهینه‌سازی پردازنده گرافیکی (GPU Force Rendering) برای کیفیت MAX
            "setprop debug.hwui.render_dirty_regions false",
            "setprop debug.hwui.use_partial_updates false",
            
            // تزریق دستور اجرایی برای افزایش پرایوریتی (Priority) پردازش بازی
            "renice -n -20 \$(pgrep -f $packageName)",
            
            // پاکسازی کش حافظه موقت برای جلوگیری از لگ هنگام لندینگ در بتل رویال
            "sync && echo 3 > /proc/sys/vm/drop_caches"
        ).joinToString(" && ")

        // ارسال از طریق موتور تزریق پیشرفته Blade به داخل محیط بازی
        return injector.injectModuleToGame(packageName, commandPayload)
    }

    /**
     * بازگرداندن تنظیمات به حالت عادی (برای زمانی که از بازی خارج می‌شوی)
     */
    suspend fun resetToDefault(packageName: String): InjectionResult {
        val resetPayload = listOf(
            "settings put system peak_refresh_rate 60.0",
            "setprop debug.hwui.render_dirty_regions true",
            "sync"
        ).joinToString(" && ")

        return injector.injectModuleToGame(packageName, resetPayload)
    }
}
