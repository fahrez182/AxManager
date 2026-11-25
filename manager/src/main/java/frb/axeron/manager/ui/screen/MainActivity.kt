import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.GameGraphicsModule
import frb.axeron.manager.api.InjectionResult
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var injector: AxBladeInjector
    private lateinit var graphicsModule: GameGraphicsModule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ۱. ساخت نمونه از موتور و ماژول
        injector = AxBladeInjector()
        graphicsModule = GameGraphicsModule(injector)

        val btnActivate = findViewById<Button>(R.id.btnActivateFPS)

        // ۲. رویداد کلیک روی دکمه فعال‌سازی
        btnActivate.setOnClickListener {
            lifecycleScope.launch {
                // نام پکیج رسمی کالاف دیوتی موبایل
                val callOfDutyPackage = "com.activision.callofduty.shooter"

                Toast.makeText(this@MainActivity, "در حال تزریق و اعمال تنظیمات...", Toast.LENGTH_SHORT).show()

                // ۳. اجرای عملیات تزریق
                val result = graphicsModule.enableUltraPerformance(callOfDutyPackage)

                // ۴. بررسی نتیجه
                when (result) {
                    is InjectionResult.Success -> {
                        Toast.makeText(this@MainActivity, "موفقیت‌آمیز! ۱۲۰ فریم و گرافیک MAX فعال شد.", Toast.LENGTH_LONG).show()
                    }
                    is InjectionResult.Failure -> {
                        Toast.makeText(this@MainActivity, "خطا در تزریق: ${result.reason}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
