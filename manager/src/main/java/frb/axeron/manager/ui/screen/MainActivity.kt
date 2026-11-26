package frb.axeron.manager.ui.screen

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import frb.axeron.manager.R // <--- این خط خیلی مهم است (منابع پروژه)
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.GameGraphicsModule
import frb.axeron.manager.api.InjectionResult
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // تعریف موتورها و ماژول‌ها
    private lateinit var injector: AxBladeInjector
    private lateinit var graphicsModule: GameGraphicsModule

    // نام پکیج بازی مورد نظر
    private val COD_PACKAGE = "com.activision.callofduty.shooter"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // دقت کن که فایل layout.xml باید در پوشه res/layout ساخته شده باشد
        setContentView(R.layout.activity_main)

        // ۱. مقداردهی اولیه موتورها
        injector = AxBladeInjector()
        graphicsModule = GameGraphicsModule(injector)

        // ۲. پیدا کردن دکمه‌ها از فایل XML
        val btnActivate = findViewById<Button>(R.id.btnActivateFPS)
        val btnReset = findViewById<Button>(R.id.btnResetSettings)

        // --- عملکرد دکمه فعال‌سازی (Boost Mode) ---
        btnActivate.setOnClickListener {
            lifecycleScope.launch {
                Toast.makeText(this@MainActivity, "در حال تزریق ۱۲۰ فریم و گرافیک MAX...", Toast.LENGTH_SHORT).show()
                
                val result = graphicsModule.enableUltraPerformance(COD_PACKAGE)

                when (result) {
                    is InjectionResult.Success -> {
                        Toast.makeText(this@MainActivity, "موفقیت‌آمیز! حالا به بازی برگردید.", Toast.LENGTH_LONG).show()
                    }
                    is InjectionResult.Failure -> {
                        Toast.makeText(this@MainActivity, "خطا: ${result.reason}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // --- عملکرد دکمه بازگشت به حالت عادی (Reset Mode) ---
        btnReset.setOnClickListener {
            lifecycleScope.launch {
                Toast.makeText(this@MainActivity, "در حال بازگرداندن تنظیمات به حالت پیش‌فرض...", Toast.LENGTH_SHORT).show()
                
                // فراخوانی تابع ریست که در مرحله قبل برایت نوشتم
                val result = graphicsModule.resetToDefault(COD_PACKAGE)

                when (result) {
                    is InjectionResult.Success -> {
                        Toast.makeText(this@MainActivity, "تنظیمات گوشی کاملاً ریست شد.", Toast.LENGTH_LONG).show()
                    }
                    is InjectionResult.Failure -> {
                        Toast.makeText(this@MainActivity, "خطا در ریست کردن: ${result.reason}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
